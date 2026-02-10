import { useState, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { parseSSEResponse } from '../lib/sse';
import { ensureValidAccessToken } from '../lib/authToken';
import type { ChatRequest, ChatMessage, UseChatOptions, UseChatReturn, ChatResponseBlock } from '../types/chat';
import { isContentBlockType, MessageRole } from '../types/chat';
import type { TokenPairResponse } from '../types/auth';

const DEFAULT_API = '/api/chat/stream';

async function refreshAccessToken(): Promise<TokenPairResponse | null> {
  const { refreshToken } = useAuthStore.getState();
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      return null;
    }

    const data = await response.json();
    return data.data || data;
  } catch {
    return null;
  }
}

interface ConsumeStreamOptions {
  onConversationId?: (id: number) => void;
  onFinish?: (message: ChatMessage) => void;
}

async function consumeStreamIntoLastAssistantMessage(
  response: Response,
  messagesRef: React.MutableRefObject<ChatMessage[]>,
  setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>,
  options: ConsumeStreamOptions,
  initialContent?: string,
  initialBlocks?: ChatResponseBlock[]
): Promise<void> {
  let accumulatedContent = initialContent ?? '';
  const accumulatedBlocks: ChatResponseBlock[] = initialBlocks ? [...initialBlocks] : [];

  for await (const block of parseSSEResponse(response)) {
    const lastMessage = messagesRef.current[messagesRef.current.length - 1];
    if (lastMessage?.role !== MessageRole.ASSISTANT) continue;

    if (block.conversationId != null) {
      options.onConversationId?.(block.conversationId);
    }

    if (isContentBlockType(block.type)) {
      accumulatedContent += block.data ?? '';
    }
    accumulatedBlocks.push(block);

    setMessages((prev) => {
      const updated = [...prev];
      const last = updated[updated.length - 1];
      if (last?.role !== 'assistant') return prev;
      updated[updated.length - 1] = {
        ...last,
        content: accumulatedContent,
        blocks: [...accumulatedBlocks],
      };
      return updated;
    });

    if (block.done) {
      const last = messagesRef.current[messagesRef.current.length - 1];
      options.onFinish?.({
        ...last,
        content: accumulatedContent,
        blocks: accumulatedBlocks,
      });
      break;
    }
  }
}

async function fetchWithAuthRetry(
  url: string,
  body: object,
  signal: AbortSignal,
  retryCount = 0
): Promise<Response> {
  const token = await ensureValidAccessToken();
  if (!token) throw new Error('Not authenticated');

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
    signal,
  });

  if (response.status === 401 && retryCount === 0) {
    const tokens = await refreshAccessToken();
    if (tokens) {
      const { user, setAuth } = useAuthStore.getState();
      setAuth(user, tokens.accessToken, tokens.refreshToken);
      return fetchWithAuthRetry(url, body, signal, 1);
    }
    const { setAuth, openLoginModal } = useAuthStore.getState();
    setAuth(null, null, null);
    openLoginModal();
    throw new Error('Session expired, please login again');
  }

  return response;
}

export function useChat(options: UseChatOptions = {}): UseChatReturn {
  const { api = DEFAULT_API } = options;
  const [messages, setMessages] = useState<ChatMessage[]>(options.initialMessages || []);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error>();

  const abortControllerRef = useRef<AbortController | null>(null);
  const submittingRef = useRef(false);
  const messagesRef = useRef(messages);
  messagesRef.current = messages;

  const appendMessage = useCallback((message: ChatMessage) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  const processStream = useCallback(
    async (request: ChatRequest) => {
      abortControllerRef.current = new AbortController();
      setIsLoading(true);
      setError(undefined);

      try {
        const response = await fetchWithAuthRetry(
          api,
          request,
          abortControllerRef.current.signal
        );

        if (!response.ok) {
          const err = new Error(`Stream request failed: ${response.status} ${response.statusText}`);
          setError(err);
          options.onError?.(err);
          return;
        }

        options.onResponse?.(response);

        const assistantMessage: ChatMessage = {
          id: crypto.randomUUID(),
          role: MessageRole.ASSISTANT,
          content: '',
          blocks: [],
          createdAt: new Date(),
        };
        appendMessage(assistantMessage);

        await consumeStreamIntoLastAssistantMessage(response, messagesRef, setMessages, {
          onConversationId: options.onConversationId,
          onFinish: options.onFinish,
        });
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err);
          options.onError?.(err);
        }
      } finally {
        submittingRef.current = false;
        setIsLoading(false);
      }
    },
    [api, appendMessage, options]
  );

  /** Shared core: append user message and start stream. Does not touch input state. */
  const submitCore = useCallback(
    async (text: string) => {
      const trimmed = (text ?? '').trim();
      if (submittingRef.current || !trimmed) return;
      submittingRef.current = true;
      setIsLoading(true);

      const userMessage: ChatMessage = {
        id: crypto.randomUUID(),
        role: MessageRole.USER,
        content: trimmed,
        createdAt: new Date(),
      };
      appendMessage(userMessage);

      const request: ChatRequest = {
        message: trimmed,
        ...(options.body as Partial<ChatRequest>),
      };
      await processStream(request);
    },
    [appendMessage, processStream, options.body]
  );

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!input.trim() || isLoading) return;
      const messageText = input.trim();
      setInput('');
      await submitCore(messageText);
    },
    [input, isLoading, setInput, submitCore]
  );

  /** Send a specific message (e.g. next from queue) without using input. */
  const submitMessage = useCallback(
    async (message: string) => {
      await submitCore(message ?? '');
    },
    [submitCore]
  );

  /** Submit user answer to askUserQuestion tool and continue (no user message appended; streams into last assistant message). */
  const submitToolAnswer = useCallback(
    async (toolCallId: string, answer: string) => {
      const trimmed = (answer ?? '').trim();
      if (submittingRef.current || !trimmed) return;

      const body: Record<string, unknown> = {
        ...(options.body as Record<string, unknown>),
        toolCallId,
        answer: trimmed,
      };
      const conversationId = body.conversationId as number | undefined;
      if (conversationId == null) {
        setError(new Error('conversationId is required for submit-tool-answer'));
        return;
      }

      submittingRef.current = true;
      setIsLoading(true);
      setError(undefined);
      abortControllerRef.current = new AbortController();

      try {
        const response = await fetchWithAuthRetry(
          '/api/chat/submit-tool-answer',
          body,
          abortControllerRef.current.signal
        );

        if (!response.ok) {
          const err = new Error(`Submit tool answer failed: ${response.status} ${response.statusText}`);
          setError(err);
          options.onError?.(err);
          return;
        }

        const lastMessage = messagesRef.current[messagesRef.current.length - 1];
        if (lastMessage?.role !== MessageRole.ASSISTANT) {
          setIsLoading(false);
          submittingRef.current = false;
          return;
        }

        await consumeStreamIntoLastAssistantMessage(response, messagesRef, setMessages, {
          onConversationId: options.onConversationId,
          onFinish: options.onFinish,
        }, lastMessage.content ?? '', lastMessage.blocks);
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err);
          options.onError?.(err);
        }
      } finally {
        submittingRef.current = false;
        setIsLoading(false);
      }
    },
    [options.body, options.onConversationId, options.onFinish, options.onError]
  );

  const stop = useCallback(() => {
    abortControllerRef.current?.abort();
    submittingRef.current = false;
    setIsLoading(false);
  }, []);

  const reload = useCallback(async () => {
    const lastMessage = messagesRef.current[messagesRef.current.length - 1];
    if (lastMessage?.role === MessageRole.USER) {
      const request: ChatRequest = {
        message: lastMessage.content,
        ...(options.body as Partial<ChatRequest>),
      };
      await processStream(request);
    }
  }, [processStream, options.body]);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setInput(e.target.value);
  }, []);

  return {
    messages,
    setMessages,
    input,
    setInput,
    handleInputChange,
    handleSubmit,
    submitMessage,
    submitToolAnswer,
    isLoading,
    stop,
    reload,
    error,
  };
}

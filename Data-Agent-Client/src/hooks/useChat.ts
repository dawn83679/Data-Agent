import { useState, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { parseSSEResponse } from '../lib/sse';
import { ensureValidAccessToken } from '../lib/authToken';
import type { ChatRequest, ChatMessage, UseChatOptions, UseChatReturn, ChatResponseBlock } from '../types/chat';
import { isContentBlockType, MessageRole } from '../types/chat';
import type { TokenPairResponse } from '../types/auth';
import {
  CHAT_STREAM_API,
  NOT_AUTHENTICATED,
  SESSION_EXPIRED_MESSAGE,
} from '../constants/chat';

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
  onBlockReceived?: () => void;
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
    options.onBlockReceived?.();
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
  if (!token) throw new Error(NOT_AUTHENTICATED);

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
    throw new Error(SESSION_EXPIRED_MESSAGE);
  }

  return response;
}

/** After this many ms with no block received, show the Planning indicator again. */
const GAP_THRESHOLD_MS = 800;

export function useChat(options: UseChatOptions = {}): UseChatReturn {
  const { api = CHAT_STREAM_API } = options;
  const [messages, setMessages] = useState<ChatMessage[]>(options.initialMessages || []);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isWaiting, setIsWaiting] = useState(false);
  const [error, setError] = useState<Error>();

  const abortControllerRef = useRef<AbortController | null>(null);
  const submittingRef = useRef(false);
  const messagesRef = useRef(messages);
  const lastStreamEventAtRef = useRef(0);
  const waitingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  messagesRef.current = messages;

  function scheduleWaiting() {
    if (waitingTimerRef.current) clearTimeout(waitingTimerRef.current);
    waitingTimerRef.current = setTimeout(() => setIsWaiting(true), GAP_THRESHOLD_MS);
  }

  function cancelWaiting() {
    if (waitingTimerRef.current) clearTimeout(waitingTimerRef.current);
    waitingTimerRef.current = null;
    setIsWaiting(false);
  }


  const appendMessage = useCallback((message: ChatMessage) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  const processStream = useCallback(
    async (request: ChatRequest) => {
      abortControllerRef.current = new AbortController();
      setIsLoading(true);
      setIsWaiting(true);
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
        lastStreamEventAtRef.current = Date.now();

        await consumeStreamIntoLastAssistantMessage(response, messagesRef, setMessages, {
          onConversationId: options.onConversationId,
          onFinish: options.onFinish,
          onBlockReceived: () => {
            setIsWaiting(false);
            scheduleWaiting();
            lastStreamEventAtRef.current = Date.now();
          },
        });
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err);
          options.onError?.(err);
        }
      } finally {
        submittingRef.current = false;
        setIsLoading(false);
        cancelWaiting();
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

  const stop = useCallback(() => {
    abortControllerRef.current?.abort();
    submittingRef.current = false;
    setIsLoading(false);
    cancelWaiting();
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
    isLoading,
    isWaiting,
    stop,
    reload,
    error,
  };
}

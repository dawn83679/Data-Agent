import { useState, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { parseSSEResponse } from '../lib/sse';
import { ensureValidAccessToken } from '../lib/authToken';
import type { ChatRequest, ChatMessage, UseChatOptions, UseChatReturn, ChatResponseBlock } from '../types/chat';
import { isContentBlockType } from '../types/chat';
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

export function useChat(options: UseChatOptions = {}): UseChatReturn {
  const { api = DEFAULT_API } = options;
  const [messages, setMessages] = useState<ChatMessage[]>(options.initialMessages || []);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error>();

  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesRef = useRef(messages);
  messagesRef.current = messages;

  const { user, setAuth, openLoginModal } = useAuthStore();

  const appendMessage = useCallback((message: ChatMessage) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  const processStream = useCallback(
    async (request: ChatRequest, retryCount = 0) => {
      const token = await ensureValidAccessToken();
      if (!token) {
        const err = new Error('Not authenticated');
        setError(err);
        options.onError?.(err);
        return;
      }

      abortControllerRef.current = new AbortController();
      setIsLoading(true);
      setError(undefined);

      try {
        const response = await fetch(api, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(request),
          signal: abortControllerRef.current.signal,
        });

        // Handle 401 - attempt token refresh
        if (response.status === 401 && retryCount === 0) {
          const tokens = await refreshAccessToken();
          if (tokens) {
            setAuth(user, tokens.accessToken, tokens.refreshToken);
            // Retry with new token
            await processStream(request, retryCount + 1);
            return;
          } else {
            // Refresh failed - clear auth and show login modal
            setAuth(null, null, null);
            openLoginModal();
            const err = new Error('Session expired, please login again');
            setError(err);
            options.onError?.(err);
            setIsLoading(false);
            return;
          }
        }

        if (!response.ok) {
          const err = new Error(`Stream request failed: ${response.status} ${response.statusText}`);
          setError(err);
          options.onError?.(err);
          setIsLoading(false);
          return;
        }

        options.onResponse?.(response);

        // Create assistant message placeholder
        const assistantMessage: ChatMessage = {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: '',
          blocks: [],
          createdAt: new Date(),
        };
        appendMessage(assistantMessage);

        // Accumulate in loop — ref/state may not update between chunks, so don't rely on lastMessage.content
        let accumulatedContent = '';
        const accumulatedBlocks: ChatResponseBlock[] = [];

        for await (const block of parseSSEResponse(response)) {
          const lastMessage = messagesRef.current[messagesRef.current.length - 1];
          if (lastMessage?.role !== 'assistant') continue;

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
            options.onFinish?.({
              ...lastMessage,
              content: accumulatedContent,
              blocks: accumulatedBlocks,
            });
            break;
          }
        }
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err);
          options.onError?.(err);
        }
      } finally {
        setIsLoading(false);
      }
    },
    [api, user, setAuth, openLoginModal, appendMessage, options]
  );

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!input.trim() || isLoading) return;

      const userMessage: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        content: input,
        createdAt: new Date(),
      };
      appendMessage(userMessage);

      const request: ChatRequest = {
        message: input.trim(),
        ...(options.body as Partial<ChatRequest>),
      };
      setInput('');

      await processStream(request);
    },
    [input, isLoading, appendMessage, processStream, options.body]
  );

  const stop = useCallback(() => {
    abortControllerRef.current?.abort();
    setIsLoading(false);
  }, []);

  const reload = useCallback(async () => {
    const lastMessage = messagesRef.current[messagesRef.current.length - 1];
    if (lastMessage?.role === 'user') {
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
    isLoading,
    stop,
    reload,
    error,
  };
}

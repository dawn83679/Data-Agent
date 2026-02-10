import { useState, useCallback, useRef } from 'react';

export interface UseMessageQueueReturn {
  queue: string[];
  addToQueue: (text: string) => void;
  removeFromQueue: (index: number) => void;
  /** Pass this to useChat onFinish. Sends next queued message when stream ends. */
  drainOnFinish: (message: unknown) => void;
  /** Call each render so the queue can use current submitMessage (e.g. from useChat). */
  setSubmitMessage: (fn: (message: string) => Promise<void>) => void;
}

/**
 * Manages a queue of messages to send after the current stream ends.
 * Decoupled from useChat: pass drainOnFinish to onFinish, and setSubmitMessage(submitMessage) so the queue can send.
 */
export function useMessageQueue(): UseMessageQueueReturn {
  const [queue, setQueue] = useState<string[]>([]);
  const submitMessageRef = useRef<(message: string) => Promise<void>>(() => Promise.resolve());

  const addToQueue = useCallback((text: string) => {
    const trimmed = text.trim();
    if (trimmed) setQueue((q) => [...q, trimmed]);
  }, []);

  const removeFromQueue = useCallback((index: number) => {
    setQueue((q) => q.filter((_, i) => i !== index));
  }, []);

  const drainOnFinish = useCallback((_message: unknown) => {
    setQueue((prev) => {
      if (prev.length === 0) return prev;
      const [first, ...rest] = prev;
      setTimeout(() => submitMessageRef.current(first), 0);
      return rest;
    });
  }, []);

  const setSubmitMessage = useCallback((fn: (message: string) => Promise<void>) => {
    submitMessageRef.current = fn;
  }, []);

  return { queue, addToQueue, removeFromQueue, drainOnFinish, setSubmitMessage };
}

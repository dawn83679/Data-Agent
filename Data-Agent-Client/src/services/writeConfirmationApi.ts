import http from '../lib/http';

export async function confirmWriteOperation(confirmationToken: string): Promise<void> {
  await http.post('/chat/write-confirm/confirm', { confirmationToken });
}

export async function cancelWriteOperation(confirmationToken: string): Promise<void> {
  await http.post('/chat/write-confirm/cancel', { confirmationToken });
}

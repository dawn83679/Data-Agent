import http from '../lib/http';

export async function confirmWriteOperation(confirmationToken: string, supplementaryInput?: string): Promise<void> {
  await http.post('/chat/write-confirm/confirm', { confirmationToken, supplementaryInput });
}

export async function cancelWriteOperation(confirmationToken: string, supplementaryInput?: string): Promise<void> {
  await http.post('/chat/write-confirm/cancel', { confirmationToken, supplementaryInput });
}

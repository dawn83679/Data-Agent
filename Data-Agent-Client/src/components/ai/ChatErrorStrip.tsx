interface ChatErrorStripProps {
  error: Error;
}

export function ChatErrorStrip({ error }: ChatErrorStripProps) {
  return (
    <div className="px-3 py-2 bg-red-500/10 border-b border-red-500/20">
      <p className="text-xs text-red-500">Error: {error.message}</p>
    </div>
  );
}

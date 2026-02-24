import { useState } from 'react';
import { Copy, Check, RefreshCw } from 'lucide-react';
import { COPY_FEEDBACK_DELAY_MS } from '../../../constants/timing';

export interface ChartDisplayProps {
  imageUrl: string;
  chartType: string;
}

/**
 * Chart image display with hover-to-show copy button
 */
export function ChartDisplay({ imageUrl, chartType }: ChartDisplayProps) {
  const [imageError, setImageError] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleCopyLink = () => {
    navigator.clipboard.writeText(imageUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), COPY_FEEDBACK_DELAY_MS);
  };

  const handleRetryImage = () => {
    setImageError(false);
    const img = new Image();
    img.src = imageUrl;
  };

  if (imageError) {
    return (
      <div className="p-4 text-center space-y-3 rounded-lg bg-white dark:bg-gray-800 shadow-sm">
        <div className="text-sm text-red-600 dark:text-red-400">Failed to load chart image</div>
        <button
          type="button"
          onClick={handleRetryImage}
          className="inline-flex items-center gap-2 px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-md transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="group relative rounded-lg overflow-hidden bg-white dark:bg-gray-800 shadow-sm">
      <img
        src={imageUrl}
        alt={chartType}
        className="w-full h-auto"
        onError={() => setImageError(true)}
      />
      <button
        type="button"
        onClick={handleCopyLink}
        className="absolute top-2 right-2 p-2 bg-white/90 dark:bg-gray-800/90 rounded-lg shadow-sm hover:bg-white dark:hover:bg-gray-800 transition-all opacity-0 group-hover:opacity-100"
        title={copied ? 'Copied!' : 'Copy link'}
      >
        {copied ? (
          <Check className="w-4 h-4 text-green-600 dark:text-green-400" />
        ) : (
          <Copy className="w-4 h-4 text-gray-600 dark:text-gray-400" />
        )}
      </button>
    </div>
  );
}

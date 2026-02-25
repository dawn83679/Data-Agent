import { Settings2 } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
import { Button } from '../ui/Button';
import {
  TransactionMode,
  IsolationLevel,
  TRANSACTION_MODE_LABELS,
  ISOLATION_LEVEL_LABELS,
} from '../../constants/transactionSettings';

interface TransactionModeSelectorProps {
  transactionMode: TransactionMode;
  isolationLevel: IsolationLevel;
  onTransactionModeChange: (mode: TransactionMode) => void;
  onIsolationLevelChange: (level: IsolationLevel) => void;
}

export function TransactionModeSelector({
  transactionMode,
  isolationLevel,
  onTransactionModeChange,
  onIsolationLevelChange,
}: TransactionModeSelectorProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          title="Transaction settings"
        >
          <Settings2 className="w-3.5 h-3.5" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="min-w-[180px]">
        {/* Transaction Mode Section */}
        <DropdownMenuLabel className="text-xs font-semibold">Transaction Mode</DropdownMenuLabel>
        {Object.values(TransactionMode).map((mode) => (
          <DropdownMenuItem
            key={mode}
            onClick={() => onTransactionModeChange(mode)}
            className="text-xs"
          >
            <span className="flex items-center gap-2 w-full">
              {transactionMode === mode && <span>✓</span>}
              {TRANSACTION_MODE_LABELS[mode]}
            </span>
          </DropdownMenuItem>
        ))}

        <DropdownMenuSeparator className="my-1" />

        {/* Isolation Level Section */}
        <DropdownMenuLabel className="text-xs font-semibold">Isolation Level</DropdownMenuLabel>
        {Object.values(IsolationLevel).map((level) => (
          <DropdownMenuItem
            key={level}
            onClick={() => onIsolationLevelChange(level)}
            className="text-xs"
          >
            <span className="flex items-center gap-2 w-full">
              {isolationLevel === level && <span>✓</span>}
              {ISOLATION_LEVEL_LABELS[level]}
            </span>
          </DropdownMenuItem>
        ))}

        <DropdownMenuSeparator className="my-1" />

        {/* Description */}
        <div className="px-2 py-1.5 text-[10px] text-gray-500 italic max-w-[160px]">
          Current transaction detects only committed changes
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

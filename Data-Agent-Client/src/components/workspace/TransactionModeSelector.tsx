import { ChevronDown } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
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
        <button className="h-6 px-2 rounded flex items-center gap-1 text-[11px] hover:bg-accent/30 transition-colors">
          <span className="theme-text-primary font-medium">Tx: {TRANSACTION_MODE_LABELS[transactionMode]}</span>
          <ChevronDown className="w-3 h-3 theme-text-secondary" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="min-w-[180px]">
        <DropdownMenuLabel className="text-xs font-semibold">Transaction Mode</DropdownMenuLabel>
        {Object.values(TransactionMode).map((mode) => (
          <DropdownMenuItem
            key={mode}
            onClick={() => onTransactionModeChange(mode)}
            className="text-xs px-2 py-1.5"
          >
            <span className="w-4">
              {transactionMode === mode && <span>✓</span>}
            </span>
            <span className="ml-2">{TRANSACTION_MODE_LABELS[mode]}</span>
          </DropdownMenuItem>
        ))}

        <DropdownMenuSeparator className="my-1" />

        <DropdownMenuLabel className="text-xs font-semibold">Isolation Level</DropdownMenuLabel>
        {Object.values(IsolationLevel).map((level) => (
          <DropdownMenuItem
            key={level}
            onClick={() => onIsolationLevelChange(level)}
            className="text-xs px-2 py-1.5"
          >
            <span className="w-4">
              {isolationLevel === level && <span>✓</span>}
            </span>
            <span className="ml-2">{ISOLATION_LEVEL_LABELS[level]}</span>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

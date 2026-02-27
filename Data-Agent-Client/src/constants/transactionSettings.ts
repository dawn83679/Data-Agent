/**
 * Transaction settings constants and enums
 */

export const TransactionMode = {
  AUTO: 'auto',
  MANUAL: 'manual',
} as const;

export type TransactionMode = typeof TransactionMode[keyof typeof TransactionMode];

export const IsolationLevel = {
  DEFAULT: 'default',
  READ_UNCOMMITTED: 'read_uncommitted',
  READ_COMMITTED: 'read_committed',
  REPEATABLE_READ: 'repeatable_read',
  SERIALIZABLE: 'serializable',
} as const;

export type IsolationLevel = typeof IsolationLevel[keyof typeof IsolationLevel];

export const TRANSACTION_MODE_LABELS: Record<TransactionMode, string> = {
  [TransactionMode.AUTO]: 'Auto',
  [TransactionMode.MANUAL]: 'Manual',
};

export const ISOLATION_LEVEL_LABELS: Record<IsolationLevel, string> = {
  [IsolationLevel.DEFAULT]: 'Database Default',
  [IsolationLevel.READ_UNCOMMITTED]: 'Read Uncommitted',
  [IsolationLevel.READ_COMMITTED]: 'Read Committed',
  [IsolationLevel.REPEATABLE_READ]: 'Repeatable Read',
  [IsolationLevel.SERIALIZABLE]: 'Serializable',
};

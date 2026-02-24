/**
 * Color constants for UI components.
 * Centralized Tailwind color class definitions for consistent theming.
 */

export const ColorClasses = {
  // Primary colors
  PRIMARY_BLUE: 'text-blue-400',
  PRIMARY_BLUE_BG: 'bg-blue-500',
  PRIMARY_TEAL: 'text-teal-400',

  // Database explorer node type colors
  ROOT_DATABASE: 'text-blue-400',
  DATABASE: 'text-teal-400',
  SCHEMA: 'text-amber-500',
  TABLE: 'text-green-400',
  VIEW: 'text-indigo-400',
  TRIGGER: 'text-orange-400',
  FUNCTION: 'text-violet-400',
  PROCEDURE: 'text-violet-400',
  ROUTINE: 'text-violet-400',
  COLUMN: 'text-sky-400',
  COLUMN_PRIMARY_KEY: 'text-amber-500',
  INDEX: 'text-slate-400',
  KEY: 'text-yellow-400',

  // Status colors
  SUCCESS: 'text-green-500',
  SUCCESS_BG: 'bg-green-500',
  ERROR: 'text-red-400',
  ERROR_BG: 'bg-red-500',
  WARNING: 'text-orange-400',
  WARNING_BG: 'bg-orange-500',
  INFO: 'text-blue-400',
  INFO_BG: 'bg-blue-500',

  // Text colors
  TEXT_PRIMARY: 'text-gray-900',
  TEXT_SECONDARY: 'text-gray-600',
  TEXT_TERTIARY: 'text-gray-500',
  TEXT_DISABLED: 'text-gray-400',

  // Background colors
  BG_PRIMARY: 'bg-white',
  BG_SECONDARY: 'bg-gray-50',
  BG_TERTIARY: 'bg-gray-100',
  BG_HOVER: 'bg-gray-200',
  BG_SELECTED: 'bg-blue-100',
} as const;

export type ColorClass = (typeof ColorClasses)[keyof typeof ColorClasses];

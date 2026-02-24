/**
 * Layout and dimension constants.
 * Centralized Tailwind classes and dimension values for consistent UI spacing and sizing.
 */

// Icon sizes (Tailwind classes)
export const IconSize = {
  SMALL: 'w-2.5 h-2.5',
  MEDIUM: 'w-3 h-3',
  MEDIUM_LARGE: 'w-3.5 h-3.5',
  LARGE: 'w-4 h-4',
  XL: 'w-5 h-5',
  XXL: 'w-6 h-6',
} as const;

// Icon size (inline styles with minWidth/minHeight)
export const IconSizeStyle = {
  SMALL: { minWidth: 10, minHeight: 10 },
  MEDIUM: { minWidth: 12, minHeight: 12 },
  MEDIUM_LARGE: { minWidth: 14, minHeight: 14 },
  LARGE: { minWidth: 16, minHeight: 16 },
  XL: { minWidth: 20, minHeight: 20 },
  XXL: { minWidth: 24, minHeight: 24 },
} as const;

// Panel and splitter dimensions
export const PanelDimension = {
  DEFAULT_RESULTS_PANEL_WIDTH: '30%',
  MIN_RESULTS_PANEL_WIDTH: '15%',
  DEFAULT_EXPLORER_WIDTH: '25%',
  MIN_EXPLORER_WIDTH: '200px',
  MAX_EXPLORER_WIDTH: '50%',
  RESIZE_HANDLE_SIZE: 'h-1',
  RESIZE_HANDLE_CURSOR: 'cursor-row-resize',
} as const;

// Tab dimensions
export const TabDimension = {
  MIN_WIDTH: '120px',
  MAX_WIDTH: '200px',
  HEIGHT: 'h-9',
  TEXT_SIZE: 'text-[11px]',
  PADDING: 'px-2 py-1',
} as const;

// Spacing and gaps
export const Spacing = {
  // Gaps (flex/grid)
  ICON_GAP: 'gap-1.5',
  SMALL_GAP: 'gap-1',
  MEDIUM_GAP: 'gap-2',
  LARGE_GAP: 'gap-3',

  // Space (margin between elements)
  SPACE_X_SMALL: 'space-x-1',
  SPACE_X_MEDIUM: 'space-x-2',
  SPACE_Y_SMALL: 'space-y-1',
  SPACE_Y_MEDIUM: 'space-y-2',

  // Padding
  PANEL_PADDING: 'px-2',
  PANEL_PADDING_MEDIUM: 'px-3 py-2',
  DIALOG_PADDING: 'p-4',
} as const;

// Border radius
export const BorderRadius = {
  SMALL: 'rounded-sm',
  MEDIUM: 'rounded',
  LARGE: 'rounded-lg',
  FULL: 'rounded-full',
} as const;

// Text sizes
export const TextSize = {
  TINY: 'text-[10px]',
  SMALL: 'text-xs',
  MEDIUM: 'text-sm',
  LARGE: 'text-base',
  XL: 'text-lg',
} as const;

// Z-index layers
export const ZIndex = {
  BASE: 0,
  DROPDOWN: 10,
  STICKY: 20,
  FIXED: 30,
  MODAL_BACKDROP: 40,
  MODAL: 50,
  TOOLTIP: 60,
  NOTIFICATION: 70,
} as const;

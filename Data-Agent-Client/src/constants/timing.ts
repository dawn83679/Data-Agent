/**
 * Timing constants for delays, durations, and intervals.
 * Centralized management of all timing-related magic numbers.
 */

// Toast notifications
export const TOAST_DEFAULT_DURATION = 3000;
export const TOAST_SUCCESS_DURATION = 2500;
export const TOAST_ERROR_DURATION = 4000;
export const TOAST_WARNING_DURATION = 3500;

// Copy to clipboard feedback delay
export const COPY_FEEDBACK_DELAY_MS = 2000;
export const COPY_FEEDBACK_SHORT_MS = 1500;

// HTTP and network timeouts
export const HTTP_TIMEOUT_MS = 10000;
export const HTTP_TIMEOUT_SHORT_MS = 5000;

// Token management
export const TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000; // 5 minutes

// DOM and async operations
export const IMMEDIATE_TIMEOUT_MS = 0;
export const SHORT_DELAY_MS = 100;
export const DEBOUNCE_DELAY_MS = 300;
export const ANIMATION_DELAY_MS = 500;

// UI interactions
export const FOCUS_DELAY_MS = 50;
export const SCROLL_DELAY_MS = 100;

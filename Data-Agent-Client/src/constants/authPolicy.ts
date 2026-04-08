export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const PASSWORD_POLICY = {
  RESET_MIN_LENGTH: 8,
  REGISTER_MIN_LENGTH: 8,
  REQUIRE_LETTER: /[A-Za-z]/,
  REQUIRE_DIGIT: /\d/,
} as const;

export const PASSWORD_RESET_REDIRECT_DELAY_MS = 2000;

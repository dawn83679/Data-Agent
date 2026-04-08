export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const PASSWORD_POLICY = {
  RESET_MIN_LENGTH: 8,
<<<<<<< HEAD
  REGISTER_MIN_LENGTH: 8,
=======
  REGISTER_MIN_LENGTH: 6,
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
  REQUIRE_LETTER: /[A-Za-z]/,
  REQUIRE_DIGIT: /\d/,
} as const;

export const PASSWORD_RESET_REDIRECT_DELAY_MS = 2000;

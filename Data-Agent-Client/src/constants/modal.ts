export const AuthModalType = {
  LOGIN: 'login',
  REGISTER: 'register',
} as const;

export type AuthModalType = (typeof AuthModalType)[keyof typeof AuthModalType];

export enum ThemeEnum {
  DARK = 'dark',
  LIGHT = 'light',
}

export const THEMES = {
  DARK: ThemeEnum.DARK,
  LIGHT: ThemeEnum.LIGHT,
} as const;

export type Theme = ThemeEnum;

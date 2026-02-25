export enum LanguageEnum {
  EN = 'en',
  ZH = 'zh',
}

export const LANGUAGES = {
  EN: LanguageEnum.EN,
  ZH: LanguageEnum.ZH,
} as const;

export type Language = LanguageEnum;

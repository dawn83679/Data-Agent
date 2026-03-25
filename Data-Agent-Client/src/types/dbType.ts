export type IdentifierQuoteStyle = 'backtick' | 'double_quote' | 'none';

export interface DbTypeOption {
  code: string;
  displayName: string;
  supportDatabase: boolean;
  supportSchema: boolean;
  tableDoubleClickSelectTemplate?: string;
  identifierQuoteStyle?: IdentifierQuoteStyle;
}

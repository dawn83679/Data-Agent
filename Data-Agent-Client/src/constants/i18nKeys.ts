/**
 * Translation key constants for i18n
 * Grouped by namespace for better organization and type safety
 */

export const I18N_KEYS = {
  // Common keys
  COMMON: {
    SETTINGS: 'common.settings',
    LANGUAGE: 'common.language',
  },

  // Settings Modal keys
  SETTINGS_MODAL: {
    // Language
    LANG_ZH: 'settings.lang_zh',
    LANG_EN: 'settings.lang_en',

    // Appearance
    APPEARANCE: 'settings.appearance',
    DARK: 'settings.dark',
    LIGHT: 'settings.light',

    // Query Behavior
    QUERY_RESULTS: 'settings.query_results',
    RESULT_TABS_BEHAVIOR: 'settings.result_tabs_behavior',
    RESULT_MULTI: 'settings.result_multi',
    RESULT_OVERWRITE: 'settings.result_overwrite',

    // Table Double Click
    TABLE_DBLCLICK: 'settings.table_dblclick',
    TABLE_DBLCLICK_TABLE: 'settings.table_dblclick_table',
    TABLE_DBLCLICK_CONSOLE: 'settings.table_dblclick_console',

    // Console Target
    CONSOLE_TARGET: 'settings.console_target',
    CONSOLE_REUSE: 'settings.console_reuse',
    CONSOLE_NEW: 'settings.console_new',

    // Reset
    RESET_ALL: 'settings.reset_all',
    RESET_CONFIRM: 'settings.reset_confirm',
    PREFERENCES_RESET: 'settings.preferences_reset',

    // General
    DONE: 'settings.done',
  },

  // Workspace keys
  WORKSPACE: {
    TAB_CLOSE: 'workspace.tab_close',
    TAB_CLOSE_LEFT: 'workspace.tab_close_left',
    TAB_CLOSE_RIGHT: 'workspace.tab_close_right',
    TAB_CLOSE_OTHERS: 'workspace.tab_close_others',
    TAB_CLOSE_ALL: 'workspace.tab_close_all',
  },

  // Settings Page keys
  SETTINGS_PAGE: {
    BACK_TO_WORKSPACE: 'settingsPage.back_to_workspace',
    TITLE: 'settingsPage.title',
    SUBTITLE: 'settingsPage.subtitle',
    ACCOUNT: 'settingsPage.account',
  },
} as const;

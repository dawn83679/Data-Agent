import { ExplorerNodeType } from './explorer';

export interface DeleteDialogConfig {
  titleKey: string;
  messageKey: string;
  errorKey: string;
}

export const DELETE_DIALOG_CONFIG: Partial<Record<ExplorerNodeType, DeleteDialogConfig>> = {
  [ExplorerNodeType.TABLE]: {
    titleKey: 'explorer.delete_table',
    messageKey: 'explorer.delete_table_confirm',
    errorKey: 'explorer.delete_table_failed',
  },
  [ExplorerNodeType.VIEW]: {
    titleKey: 'explorer.delete_view',
    messageKey: 'explorer.delete_view_confirm',
    errorKey: 'explorer.delete_view_failed',
  },
  [ExplorerNodeType.FUNCTION]: {
    titleKey: 'explorer.delete_function',
    messageKey: 'explorer.delete_function_confirm',
    errorKey: 'explorer.delete_function_failed',
  },
  [ExplorerNodeType.PROCEDURE]: {
    titleKey: 'explorer.delete_procedure',
    messageKey: 'explorer.delete_procedure_confirm',
    errorKey: 'explorer.delete_procedure_failed',
  },
  [ExplorerNodeType.TRIGGER]: {
    titleKey: 'explorer.delete_trigger',
    messageKey: 'explorer.delete_trigger_confirm',
    errorKey: 'explorer.delete_trigger_failed',
  },
  [ExplorerNodeType.DB]: {
    titleKey: 'explorer.delete_database',
    messageKey: 'explorer.delete_database_confirm',
    errorKey: 'explorer.delete_database_failed',
  },
  [ExplorerNodeType.FOLDER]: {
    titleKey: 'explorer.delete_all_in_folder',
    messageKey: 'explorer.delete_all_in_folder_confirm',
    errorKey: 'explorer.delete_folder_failed',
  },
};

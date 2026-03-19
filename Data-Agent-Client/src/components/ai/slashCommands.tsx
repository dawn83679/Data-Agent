import { MessageSquarePlus, Clock, ListTodo, ShieldCheck } from 'lucide-react';
import { I18N_KEYS } from '../../constants/i18nKeys';

export interface SlashCommandItem {
  id: string;
  slug: string;
  labelKey: string;
  icon?: React.ReactNode;
}

export const SLASH_COMMAND_IDS = {
  NEW: 'new',
  HISTORY: 'history',
  PLAN: 'plan',
  PERMISSION: 'permission',
} as const;

export const SLASH_COMMANDS: SlashCommandItem[] = [
  {
    id: SLASH_COMMAND_IDS.NEW,
    slug: SLASH_COMMAND_IDS.NEW,
    labelKey: I18N_KEYS.AI.SLASH_COMMAND.NEW,
    icon: <MessageSquarePlus className="w-3.5 h-3.5 shrink-0" />,
  },
  {
    id: SLASH_COMMAND_IDS.HISTORY,
    slug: SLASH_COMMAND_IDS.HISTORY,
    labelKey: I18N_KEYS.AI.SLASH_COMMAND.HISTORY,
    icon: <Clock className="w-3.5 h-3.5 shrink-0" />,
  },
  {
    id: SLASH_COMMAND_IDS.PLAN,
    slug: SLASH_COMMAND_IDS.PLAN,
    labelKey: I18N_KEYS.AI.SLASH_COMMAND.PLAN,
    icon: <ListTodo className="w-3.5 h-3.5 shrink-0" />,
  },
  {
    id: SLASH_COMMAND_IDS.PERMISSION,
    slug: SLASH_COMMAND_IDS.PERMISSION,
    labelKey: I18N_KEYS.AI.SLASH_COMMAND.PERMISSION,
    icon: <ShieldCheck className="w-3.5 h-3.5 shrink-0" />,
  },
];

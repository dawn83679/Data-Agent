import { MessageSquarePlus } from 'lucide-react';

export interface SlashCommandItem {
  id: string;
  slug: string;
  label: string;
  icon?: React.ReactNode;
}

export const SLASH_COMMANDS: SlashCommandItem[] = [
  {
    id: 'new',
    slug: 'new',
    label: 'New conversation',
    icon: <MessageSquarePlus className="w-3.5 h-3.5 shrink-0" />,
  },
];

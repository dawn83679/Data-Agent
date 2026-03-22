import { useEffect, useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { conversationService } from '../../services/conversation.service';
import type { Conversation } from '../../types/conversation';
import { PERMISSION_CONVERSATION_LIST_PAGE_SIZE } from './permissionPageConstants';

interface UsePermissionConversationStateReturn {
  conversations: Conversation[];
  selectedConversationId: number | null;
  setSelectedConversationId: Dispatch<SetStateAction<number | null>>;
}

export function usePermissionConversationState(
  requestedConversationId: number | null,
): UsePermissionConversationStateReturn {
  const { t } = useTranslation();
  const toast = useToast();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<number | null>(requestedConversationId);

  useEffect(() => {
    setSelectedConversationId(requestedConversationId);
  }, [requestedConversationId]);

  useEffect(() => {
    const loadConversations = async () => {
      try {
        const response = await conversationService.getList({
          current: 1,
          size: PERMISSION_CONVERSATION_LIST_PAGE_SIZE,
        });
        setConversations(response.records);
      } catch (error) {
        toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.LOAD_FAILED)));
      }
    };

    void loadConversations();
  }, [t, toast]);

  return {
    conversations,
    selectedConversationId,
    setSelectedConversationId,
  };
}

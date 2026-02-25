import { useCallback } from 'react';
import { ConnectionFormModeEnum, type ConnectionFormMode } from '../constants/connectionFormMode';

interface ConnectionActionsProps {
  setConnectionModalMode: (mode: ConnectionFormMode) => void;
  setConnectionEditId: (id: number | undefined) => void;
  setInitialDbType: (type: string | undefined) => void;
  setConnectionModalOpen: (open: boolean) => void;
}

export function useConnectionActions({
  setConnectionModalMode,
  setConnectionEditId,
  setInitialDbType,
  setConnectionModalOpen,
}: ConnectionActionsProps) {
  const openCreateModal = useCallback((dbType?: string) => {
    setConnectionModalMode(ConnectionFormModeEnum.CREATE);
    setConnectionEditId(undefined);
    setInitialDbType(dbType);
    setConnectionModalOpen(true);
  }, [setConnectionModalMode, setConnectionEditId, setInitialDbType, setConnectionModalOpen]);

  const openEditModal = useCallback((connId: number) => {
    setConnectionModalMode(ConnectionFormModeEnum.EDIT);
    setConnectionEditId(connId);
    setConnectionModalOpen(true);
  }, [setConnectionModalMode, setConnectionEditId, setConnectionModalOpen]);

  return { openCreateModal, openEditModal };
}

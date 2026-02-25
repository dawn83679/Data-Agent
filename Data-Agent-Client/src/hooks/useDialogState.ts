import { useState } from 'react';
import { ExplorerNodeType } from '../constants/explorer';
import type { ExplorerNode } from '../types/explorer';
import type { ConnectionFormMode } from '../components/common/ConnectionFormModal';

interface DeleteState {
  type: ExplorerNodeType | null;
  node: ExplorerNode | null;
  isOpen: boolean;
  isPending: boolean;
}

export function useDialogState() {
  // Connection modal state
  const [connectionModalOpen, setConnectionModalOpen] = useState(false);
  const [connectionModalMode, setConnectionModalMode] = useState<ConnectionFormMode>('create');
  const [connectionEditId, setConnectionEditId] = useState<number | undefined>(undefined);
  const [initialDbType, setInitialDbType] = useState<string | undefined>(undefined);

  // Driver modal state
  const [driverModalOpen, setDriverModalOpen] = useState(false);
  const [selectedDriverDbType, setSelectedDriverDbType] = useState<string>('');

  // Delete connection dialog
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // DDL viewer state
  const [ddlDialogOpen, setDdlDialogOpen] = useState(false);
  const [selectedDdlNode, setSelectedDdlNode] = useState<ExplorerNode | null>(null);

  // Table data viewer state
  const [tableDataDialogOpen, setTableDataDialogOpen] = useState(false);
  const [selectedTableDataNode, setSelectedTableDataNode] = useState<ExplorerNode | null>(null);
  const [highlightColumn, setHighlightColumn] = useState<string | undefined>(undefined);

  // Entity delete dialog
  const [deleteState, setDeleteState] = useState<DeleteState>({
    type: null,
    node: null,
    isOpen: false,
    isPending: false,
  });

  return {
    // Connection modal
    connectionModalOpen,
    setConnectionModalOpen,
    connectionModalMode,
    setConnectionModalMode,
    connectionEditId,
    setConnectionEditId,
    initialDbType,
    setInitialDbType,

    // Driver modal
    driverModalOpen,
    setDriverModalOpen,
    selectedDriverDbType,
    setSelectedDriverDbType,

    // Delete connection
    deleteConfirmId,
    setDeleteConfirmId,

    // DDL viewer
    ddlDialogOpen,
    setDdlDialogOpen,
    selectedDdlNode,
    setSelectedDdlNode,

    // Table data viewer
    tableDataDialogOpen,
    setTableDataDialogOpen,
    selectedTableDataNode,
    setSelectedTableDataNode,
    highlightColumn,
    setHighlightColumn,

    // Entity delete
    deleteState,
    setDeleteState,
  };
}

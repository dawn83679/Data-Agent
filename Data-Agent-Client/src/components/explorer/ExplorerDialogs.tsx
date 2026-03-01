import { ConnectionFormModal, type ConnectionFormMode } from '../common/ConnectionFormModal';
import { DriverManageModal } from '../common/DriverManageModal';
import { DeleteConnectionDialog } from './DeleteConnectionDialog';
import { DeleteEntityDialog } from './DeleteEntityDialog';
import { DdlViewerDialog } from './DdlViewerDialog';
import { ExplorerNodeType } from '../../constants/explorer';

interface ExplorerDialogsProps {
  // Connection modal
  connectionModalOpen: boolean;
  onConnectionModalOpenChange: (open: boolean) => void;
  connectionModalMode: ConnectionFormMode;
  connectionEditId: number | undefined;
  initialDbType: string | undefined;

  // Driver modal
  driverModalOpen: boolean;
  onDriverModalOpenChange: (open: boolean) => void;
  selectedDriverDbType: string;

  // Delete connection dialog
  deleteConfirmId: number | null;
  onDeleteConfirmIdChange: (id: number | null) => void;
  onConfirmDeleteConnection: (id: number) => void;
  isDeleteConnectionPending: boolean;

  // DDL viewer
  ddlDialogOpen: boolean;
  onDdlDialogOpenChange: (open: boolean) => void;
  ddlConfig: any;

  // Entity delete dialog
  deleteState: any;
  onDeleteStateChange: (state: any) => void;
  onConfirmDelete: () => void;

  // Callbacks
  onConnectionSuccess: () => void;
}

export function ExplorerDialogs({
  connectionModalOpen,
  onConnectionModalOpenChange,
  connectionModalMode,
  connectionEditId,
  initialDbType,

  driverModalOpen,
  onDriverModalOpenChange,
  selectedDriverDbType,

  deleteConfirmId,
  onDeleteConfirmIdChange,
  onConfirmDeleteConnection,
  isDeleteConnectionPending,

  ddlDialogOpen,
  onDdlDialogOpenChange,
  ddlConfig,

  deleteState,
  onDeleteStateChange,
  onConfirmDelete,

  onConnectionSuccess,
}: ExplorerDialogsProps) {
  return (
    <>
      <ConnectionFormModal
        open={connectionModalOpen}
        onOpenChange={onConnectionModalOpenChange}
        mode={connectionModalMode}
        editId={connectionEditId}
        initialDbType={initialDbType}
        onSuccess={onConnectionSuccess}
      />

      <DriverManageModal
        open={driverModalOpen}
        onOpenChange={onDriverModalOpenChange}
        databaseType={selectedDriverDbType}
        onSelectDriver={() => {}}
      />

      <DeleteConnectionDialog
        open={deleteConfirmId != null}
        onOpenChange={(open) => !open && onDeleteConfirmIdChange(null)}
        connectionId={deleteConfirmId}
        onConfirm={(id) => {
          onConfirmDeleteConnection(id);
          onDeleteConfirmIdChange(null);
        }}
        isPending={isDeleteConnectionPending}
      />

      {ddlConfig && (
        <DdlViewerDialog
          open={ddlDialogOpen}
          onOpenChange={(open) => {
            onDdlDialogOpenChange(open);
            if (!open) {
              onDeleteStateChange({ ...deleteState, selectedDdlNode: null });
            }
          }}
          title={ddlConfig.title}
          displayName={ddlConfig.displayName}
          loadDdl={ddlConfig.loadDdl}
        />
      )}

      {deleteState.type && deleteState.node && (
        <DeleteEntityDialog
          open={deleteState.isOpen}
          onOpenChange={(open) => {
            onDeleteStateChange({ ...deleteState, isOpen: open });
            if (!open) {
              onDeleteStateChange({ type: null, node: null, isOpen: false, isPending: false });
            }
          }}
          entityName={deleteState.node.name}
          entityType={deleteState.type}
          onConfirm={onConfirmDelete}
          isPending={deleteState.isPending}
          itemCount={
            deleteState.type === ExplorerNodeType.FOLDER
              ? deleteState.node.children?.filter((c: any) => c.type !== 'empty').length
              : undefined
          }
        />
      )}
    </>
  );
}

import { Plus, RefreshCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { ROUTES } from '../constants/routes';
import { I18N_KEYS } from '../constants/i18nKeys';
import { WORKBENCH_GRID_LAYOUT_CLASS } from '../constants/uiLayout';
import {
  MemoryDetailDialog,
  MemoryFilterWorkbench,
  MemoryListPanel,
  MemoryOverviewHero,
} from '../features/memory/components';
import { useMemoryWorkbench } from '../features/memory/useMemoryWorkbench';

export default function Memories() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const workbench = useMemoryWorkbench();

  return (
    <div className="space-y-6 pb-8 theme-text-primary">
      <MemoryOverviewHero
        title={t(I18N_KEYS.MEMORY_PAGE.PAGE_TITLE)}
        description={t(I18N_KEYS.MEMORY_PAGE.PAGE_DESC)}
        backLabel={t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
        onBack={() => navigate(ROUTES.HOME)}
        actions={(
          <>
            <Button
              type="button"
              variant="outline"
              onClick={() => void workbench.refreshWorkspace()}
              disabled={workbench.listLoading || workbench.semanticLoading}
            >
              <RefreshCcw className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.MEMORY_PAGE.REFRESH)}
            </Button>
            <Button type="button" onClick={workbench.startCreateMemory}>
              <Plus className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.MEMORY_PAGE.NEW_MEMORY)}
            </Button>
          </>
        )}
      />

      <div className={WORKBENCH_GRID_LAYOUT_CLASS}>
        <MemoryFilterWorkbench
          filterForm={workbench.filterForm}
          semanticSearchEnabled={workbench.semanticSearchEnabled}
          metadataLoading={workbench.metadataLoading}
          filterMemoryTypeOptions={workbench.filterMemoryTypeOptions}
          filterScopeOptions={workbench.filterScopeOptions}
          onFilterInputChange={workbench.handleFilterInputChange}
          onSemanticToggle={workbench.handleSemanticToggle}
          onApplyFilters={workbench.handleApplyFilters}
          onResetFilters={workbench.handleResetFilters}
        />

        <MemoryListPanel
          title={t(I18N_KEYS.MEMORY_PAGE.SECTION_INVENTORY_TITLE)}
          description={t(
            workbench.isSemanticMode
              ? I18N_KEYS.MEMORY_PAGE.SEARCH_DESC
              : I18N_KEYS.MEMORY_PAGE.DETAIL_EMPTY_DESC,
          )}
          pageSummary={String(workbench.listItems.length)}
          pageLabel={t(I18N_KEYS.EXPLORER.PAGES)}
          previousLabel={t(I18N_KEYS.EXPLORER.PREVIOUS)}
          nextLabel={t(I18N_KEYS.EXPLORER.NEXT)}
          loadingLabel={t(I18N_KEYS.COMMON.LOADING)}
          emptyLabel={workbench.isSemanticMode ? t(I18N_KEYS.MEMORY_PAGE.SEARCH_EMPTY) : t(I18N_KEYS.MEMORY_PAGE.EMPTY)}
          resultModeLabel={workbench.isSemanticMode ? t(I18N_KEYS.MEMORY_PAGE.RESULT_MODE_SEMANTIC_SEARCH) : undefined}
          isLoading={(workbench.listLoading && !workbench.isSemanticMode) || workbench.semanticLoading}
          items={workbench.listItems}
          selectedId={workbench.selectedMemoryId}
          onSelect={(id) => {
            void workbench.openMemory(id);
          }}
          currentPage={workbench.page.current}
          totalPages={workbench.page.pages}
          disablePrevious={workbench.page.current === 1 || workbench.listLoading}
          disableNext={
            workbench.listLoading
            || (workbench.page.pages > 0 && workbench.page.current === workbench.page.pages)
            || workbench.page.records.length === 0
          }
          showPagination={!workbench.isSemanticMode && workbench.listItems.length > 0}
          onPrevious={workbench.goToPreviousPage}
          onNext={workbench.goToNextPage}
        />
      </div>

      <MemoryDetailDialog
        open={workbench.isDialogOpen}
        isEditing={workbench.isEditing}
        selectedMemoryId={workbench.selectedMemoryId}
        selectedMemory={workbench.selectedMemory}
        memoryForm={workbench.memoryForm}
        formErrors={workbench.formErrors}
        detailLoading={workbench.detailLoading}
        metadataLoading={workbench.metadataLoading}
        submitting={workbench.submitting}
        memoryTypeOptions={workbench.memoryTypeOptions}
        availableSubTypeOptions={workbench.availableSubTypeOptions}
        editorScopeOptions={workbench.editorScopeOptions}
        sourceTypeOptions={workbench.sourceTypeOptions}
        onClose={workbench.closeDetailDialog}
        onInputChange={workbench.handleFormInputChange}
        onSubmit={workbench.handleSubmit}
        onArchive={workbench.handleArchive}
        onRestore={workbench.handleRestore}
        onDelete={workbench.handleDelete}
      />
    </div>
  );
}

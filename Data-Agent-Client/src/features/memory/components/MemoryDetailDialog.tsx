import type { ChangeEvent, FormEvent } from 'react';
import { Ban, RotateCcw, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../../components/ui/Button';
import { WORKBENCH_DIALOG_BODY_CLASS, WORKBENCH_DIALOG_CHROME_CLASS } from '../../../constants/uiLayout';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/Dialog';
import { Input } from '../../../components/ui/Input';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { cn } from '../../../lib/utils';
import { MEMORY_ENABLE, type Memory, type MemoryScope, type MemorySubType } from '../../../types/memory';
import { MEMORY_DETAIL_CONTENT_MIN_HEIGHT_CLASS } from '../memoryPageConstants';
import type { MemoryFormState } from '../memoryPageModels';
import {
  formatDateTime,
  getEnableLabelKey,
  getEnableToneClassName,
  getMemoryOptionLabel,
  MEMORY_FORM_SELECT_CLASS_NAME,
  MEMORY_FORM_TEXTAREA_CLASS_NAME,
} from '../memoryPageUtils';

interface MemoryDetailDialogProps {
  open: boolean;
  isEditing: boolean;
  selectedMemoryId: number | null;
  selectedMemory: Memory | null;
  memoryForm: MemoryFormState;
  formErrors: Record<string, string>;
  detailLoading: boolean;
  metadataLoading: boolean;
  submitting: boolean;
  memoryTypeOptions: string[];
  availableSubTypeOptions: MemorySubType[];
  editorScopeOptions: MemoryScope[];
  sourceTypeOptions: string[];
  onClose: () => void;
  onInputChange: (
    field: keyof MemoryFormState,
  ) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => void;
  onSubmit: (event: FormEvent) => void | Promise<void>;
  onDisable: () => void | Promise<void>;
  onEnable: () => void | Promise<void>;
  onDelete: () => void | Promise<void>;
}

export function MemoryDetailDialog({
  open,
  isEditing,
  selectedMemoryId,
  selectedMemory,
  memoryForm,
  formErrors,
  detailLoading,
  metadataLoading,
  submitting,
  memoryTypeOptions,
  availableSubTypeOptions,
  editorScopeOptions,
  sourceTypeOptions,
  onClose,
  onInputChange,
  onSubmit,
  onDisable,
  onEnable,
  onDelete,
}: MemoryDetailDialogProps) {
  const { t } = useTranslation();

  const detailMeta = selectedMemory ? (
    <div className="grid gap-3 rounded-2xl border theme-border bg-[color:var(--bg-main)]/45 p-4 sm:grid-cols-2">
      {[
        [t(I18N_KEYS.MEMORY_PAGE.META_CREATED), formatDateTime(selectedMemory.createdAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_UPDATED), formatDateTime(selectedMemory.updatedAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_ACCESS_COUNT), String(selectedMemory.accessCount ?? 0)],
        [t(I18N_KEYS.MEMORY_PAGE.META_LAST_ACCESSED), formatDateTime(selectedMemory.lastAccessedAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_SOURCE), getMemoryOptionLabel(t, selectedMemory.sourceType)],
        [t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE), getMemoryOptionLabel(t, selectedMemory.subType)],
        [t(I18N_KEYS.MEMORY_PAGE.META_SCOPE), getMemoryOptionLabel(t, selectedMemory.scope)],
        [
          t(I18N_KEYS.MEMORY_PAGE.META_CONVERSATION),
          selectedMemory.conversationId == null ? '--' : String(selectedMemory.conversationId),
        ],
      ].map(([label, value]) => (
        <div
          key={label}
          className={cn(
            'rounded-xl bg-[color:var(--bg-panel)]/60 p-3',
          )}
        >
          <div className="text-xs uppercase tracking-wide theme-text-secondary">{label}</div>
          <div className="mt-1 text-sm theme-text-primary">{value}</div>
        </div>
      ))}
    </div>
  ) : null;

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => (!nextOpen ? onClose() : null)}>
      <DialogContent className={`max-w-5xl ${WORKBENCH_DIALOG_CHROME_CLASS}`}>
        <DialogHeader className="border-b theme-border px-6 py-5">
          <div className="flex flex-wrap items-start justify-between gap-3 pr-8">
            <div className="space-y-1">
              <DialogTitle className="text-xl theme-text-primary">
                {isEditing ? t(I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_TITLE) : t(I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_TITLE)}
              </DialogTitle>
              <DialogDescription className="theme-text-secondary">
                {isEditing ? t(I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_DESC) : t(I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_DESC)}
              </DialogDescription>
            </div>
            {isEditing && selectedMemory ? (
              <span className={cn('rounded-full border px-3 py-1 text-xs font-medium', getEnableToneClassName(selectedMemory.enable))}>
                {t(getEnableLabelKey(selectedMemory.enable))}
              </span>
            ) : null}
          </div>
        </DialogHeader>
        <div className={WORKBENCH_DIALOG_BODY_CLASS}>
          {detailLoading ? (
            <div className="rounded-2xl border border-dashed theme-border p-6 text-sm theme-text-secondary">
              {t(I18N_KEYS.COMMON.LOADING)}
            </div>
          ) : (
            <div className="space-y-6">
              <form onSubmit={onSubmit} className="space-y-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONVERSATION_ID)}</label>
                    <Input value={memoryForm.conversationId} onChange={onInputChange('conversationId')} placeholder="123" />
                    {formErrors.conversationId ? <p className="text-sm text-destructive">{formErrors.conversationId}</p> : null}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_MEMORY_TYPE)}</label>
                    <select
                      className={MEMORY_FORM_SELECT_CLASS_NAME}
                      value={memoryForm.memoryType}
                      onChange={onInputChange('memoryType')}
                      disabled={metadataLoading}
                    >
                      {memoryTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {getMemoryOptionLabel(t, option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.memoryType ? <p className="text-sm text-destructive">{formErrors.memoryType}</p> : null}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE)}</label>
                    <select
                      className={MEMORY_FORM_SELECT_CLASS_NAME}
                      value={memoryForm.subType}
                      onChange={onInputChange('subType')}
                      disabled={metadataLoading}
                    >
                      <option value="">{t(I18N_KEYS.MEMORY_PAGE.SELECT_SUB_TYPE_PLACEHOLDER)}</option>
                      {availableSubTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {getMemoryOptionLabel(t, option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.subType ? <p className="text-sm text-destructive">{formErrors.subType}</p> : null}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SCOPE)}</label>
                    <select
                      className={MEMORY_FORM_SELECT_CLASS_NAME}
                      value={memoryForm.scope}
                      onChange={onInputChange('scope')}
                      disabled={metadataLoading}
                    >
                      {editorScopeOptions.map((option) => (
                        <option key={option} value={option}>
                          {getMemoryOptionLabel(t, option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.scope ? <p className="text-sm text-destructive">{formErrors.scope}</p> : null}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SOURCE_TYPE)}</label>
                    <select
                      className={MEMORY_FORM_SELECT_CLASS_NAME}
                      value={memoryForm.sourceType}
                      onChange={onInputChange('sourceType')}
                      disabled={metadataLoading}
                    >
                      {sourceTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {getMemoryOptionLabel(t, option)}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_TITLE)}</label>
                    <Input value={memoryForm.title} onChange={onInputChange('title')} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_REASON)}</label>
                    <Input value={memoryForm.reason} onChange={onInputChange('reason')} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONTENT)}</label>
                    <textarea
                      className={cn(MEMORY_FORM_TEXTAREA_CLASS_NAME, MEMORY_DETAIL_CONTENT_MIN_HEIGHT_CLASS)}
                      value={memoryForm.content}
                      onChange={onInputChange('content')}
                    />
                    {formErrors.content ? <p className="text-sm text-destructive">{formErrors.content}</p> : null}
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 border-t theme-border pt-4">
                  <Button type="submit" disabled={submitting}>
                    {isEditing ? t(I18N_KEYS.MEMORY_PAGE.UPDATE_ACTION) : t(I18N_KEYS.MEMORY_PAGE.CREATE_ACTION)}
                  </Button>
                  <Button type="button" variant="outline" disabled={submitting} onClick={onClose}>
                    {t(I18N_KEYS.COMMON.CLOSE)}
                  </Button>
                  {selectedMemory && selectedMemory.enable === MEMORY_ENABLE.ENABLE ? (
                    <Button type="button" variant="outline" disabled={submitting} onClick={() => void onDisable()}>
                      <Ban className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.DISABLE)}
                    </Button>
                  ) : null}
                  {selectedMemory && selectedMemory.enable === MEMORY_ENABLE.DISABLE ? (
                    <Button type="button" variant="outline" disabled={submitting} onClick={() => void onEnable()}>
                      <RotateCcw className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.ENABLE)}
                    </Button>
                  ) : null}
                  {selectedMemoryId != null ? (
                    <Button type="button" variant="destructive" disabled={submitting} onClick={() => void onDelete()}>
                      <Trash2 className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.DELETE)}
                    </Button>
                  ) : null}
                </div>
              </form>

              {detailMeta}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

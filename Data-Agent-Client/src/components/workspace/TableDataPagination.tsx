import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../ui/Button';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface TableDataPaginationProps {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  startRow: number;
  endRow: number;
  pageSizeOptions: readonly number[];
  onPageSizeChange: (value: number) => void;
  onFirstPage: () => void;
  onPrevPage: () => void;
  onNextPage: () => void;
  onLastPage: () => void;
}

export function TableDataPagination({
  currentPage,
  pageSize,
  totalCount,
  totalPages,
  startRow,
  endRow,
  pageSizeOptions,
  onPageSizeChange,
  onFirstPage,
  onPrevPage,
  onNextPage,
  onLastPage,
}: TableDataPaginationProps) {
  const { t } = useTranslation();

  return (
    <div className="flex items-center justify-center gap-2 py-2 border-t theme-border shrink-0 text-[11px] theme-text-secondary">
      <span className="flex items-center gap-1">
        {t(I18N_KEYS.EXPLORER.ROWS_PER_PAGE)}
        <select
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
          className="h-6 px-1 rounded border theme-border theme-bg-main theme-text-primary text-[11px] ml-1"
        >
          {pageSizeOptions.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </span>
      <span>
        {startRow}-{endRow} / {totalCount}
      </span>
      <Button variant="ghost" size="sm" onClick={onFirstPage} disabled={currentPage <= 1} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.FIRST_PAGE)}>
        <ChevronsLeft className="w-4 h-4" />
      </Button>
      <Button variant="ghost" size="sm" onClick={onPrevPage} disabled={currentPage <= 1} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.PREVIOUS)}>
        <ChevronLeft className="w-4 h-4" />
      </Button>
      <span className="px-2">{currentPage}</span>
      <Button variant="ghost" size="sm" onClick={onNextPage} disabled={currentPage >= totalPages} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.NEXT)}>
        <ChevronRight className="w-4 h-4" />
      </Button>
      <Button variant="ghost" size="sm" onClick={onLastPage} disabled={currentPage >= totalPages} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.LAST_PAGE)}>
        <ChevronsRight className="w-4 h-4" />
      </Button>
    </div>
  );
}

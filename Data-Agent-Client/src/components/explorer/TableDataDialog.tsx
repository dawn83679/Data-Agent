import { useState, useEffect } from 'react';
import { Loader2, ChevronLeft, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { tableDataService, type TableDataResponse } from '../../services/tableData.service';

export interface TableDataDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  displayName: string;
  connectionId: number;
  objectName: string;
  objectType: 'table' | 'view';
  catalog?: string;
  schema?: string;
  highlightColumn?: string;
}

export function TableDataDialog({
  open,
  onOpenChange,
  title,
  displayName,
  connectionId,
  objectName,
  objectType,
  catalog,
  schema,
  highlightColumn,
}: TableDataDialogProps) {
  const { t } = useTranslation();
  const [data, setData] = useState<TableDataResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 100;

  useEffect(() => {
    if (open) {
      setCurrentPage(1);
      loadData(1);
    } else {
      setData(null);
      setError(null);
      setCurrentPage(1);
    }
  }, [open]);

  const loadData = async (page: number) => {
    setLoading(true);
    setError(null);
    try {
      let result: TableDataResponse;
      if (objectType === 'table') {
        result = await tableDataService.getTableData(
          String(connectionId),
          objectName,
          catalog,
          schema,
          page,
          pageSize
        );
      } else {
        result = await tableDataService.getViewData(
          String(connectionId),
          objectName,
          catalog,
          schema,
          page,
          pageSize
        );
      }
      setData(result);
      setCurrentPage(page);
    } catch (err: unknown) {
      console.error('Failed to load table data:', err);
      setError((err as Error).message || t('explorer.load_table_data_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handlePrevPage = () => {
    if (currentPage > 1) {
      loadData(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (data && currentPage < data.totalPages) {
      loadData(currentPage + 1);
    }
  };

  const formatCellValue = (value: unknown): string => {
    if (value === null || value === undefined) {
      return 'NULL';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription className="font-mono text-xs">
            {displayName}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-hidden flex flex-col gap-2">
          {loading && (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-6 h-6 animate-spin theme-text-secondary" />
            </div>
          )}

          {error && (
            <div className="p-4 bg-destructive/10 text-destructive rounded-md text-sm">
              {error}
            </div>
          )}

          {!loading && !error && data && (
            <>
              {/* Pagination info */}
              <div className="flex items-center justify-between text-xs theme-text-secondary">
                <span>
                  {t('explorer.total_records', { count: data.totalCount })}
                  {data.totalPages > 0 && ` (${data.totalPages} ${t('explorer.pages')})`}
                </span>
                <span>
                  {t('explorer.page_info', {
                    current: data.currentPage,
                    total: data.totalPages
                  })}
                </span>
              </div>

              {/* Data table */}
              <div className="flex-1 overflow-auto border theme-border rounded-md">
                <table className="w-full text-xs border-collapse">
                  <thead className="sticky top-0 bg-accent">
                    <tr>
                      {data.columns.map((col) => (
                        <th
                          key={col}
                          className={cn(
                            "border theme-border px-2 py-1.5 text-left font-semibold bg-accent",
                            highlightColumn === col && "bg-yellow-200 dark:bg-yellow-800"
                          )}
                        >
                          {col}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {data.rows.map((row, rowIndex) => (
                      <tr
                        key={rowIndex}
                        className="hover:bg-accent/50"
                      >
                        {data.columns.map((col) => (
                          <td
                            key={col}
                            className={cn(
                              "border theme-border px-2 py-1 max-w-xs truncate",
                              highlightColumn === col && "bg-yellow-100 dark:bg-yellow-900/30"
                            )}
                            title={formatCellValue(row[col])}
                          >
                            {formatCellValue(row[col])}
                          </td>
                        ))}
                      </tr>
                    ))}
                    {data.rows.length === 0 && (
                      <tr>
                        <td
                          colSpan={data.columns.length}
                          className="border theme-border px-2 py-4 text-center theme-text-secondary"
                        >
                          {t('explorer.no_data')}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination controls */}
              {data.totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 pt-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handlePrevPage}
                    disabled={currentPage <= 1}
                  >
                    <ChevronLeft className="w-4 h-4" />
                    {t('explorer.previous')}
                  </Button>
                  <span className="text-xs theme-text-secondary px-2">
                    {currentPage} / {data.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleNextPage}
                    disabled={currentPage >= data.totalPages}
                  >
                    {t('explorer.next')}
                    <ChevronRight className="w-4 h-4" />
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

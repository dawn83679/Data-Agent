import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../../components/ui/Card";
import { Button } from "../../../components/ui/Button";
import { cn } from "../../../lib/utils";

export interface MemoryListItemView {
  id: number;
  title: string;
  summary: string;
  tags: string[];
  statusLabel: string;
  statusToneClassName?: string;
  sourceLabel?: string;
  workspaceBindingLabel?: string;
  updatedAtLabel?: string;
}

export interface MemoryListPanelProps {
  title: string;
  description: string;
  pageSummary: string;
  pageLabel: string;
  previousLabel: string;
  nextLabel: string;
  loadingLabel: string;
  emptyLabel: string;
  resultModeLabel?: string;
  isLoading?: boolean;
  items: MemoryListItemView[];
  selectedId?: number | null;
  onSelect: (id: number) => void;
  currentPage: number;
  totalPages: number;
  disablePrevious?: boolean;
  disableNext?: boolean;
  showPagination?: boolean;
  onPrevious: () => void;
  onNext: () => void;
}

export function MemoryListPanel({
  title,
  description,
  pageSummary,
  pageLabel,
  previousLabel,
  nextLabel,
  loadingLabel,
  emptyLabel,
  resultModeLabel,
  isLoading,
  items,
  selectedId,
  onSelect,
  currentPage,
  totalPages,
  disablePrevious,
  disableNext,
  showPagination = true,
  onPrevious,
  onNext,
}: MemoryListPanelProps) {
  return (
    <Card className="border theme-border shadow-sm">
      <CardHeader className="border-b theme-border bg-[color:var(--bg-panel)]/50">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <CardTitle className="text-lg theme-text-primary">{title}</CardTitle>
            <CardDescription className="theme-text-secondary">{description}</CardDescription>
            {resultModeLabel ? (
              <span className="mt-2 inline-flex rounded-full border border-sky-500/35 bg-sky-500/10 px-2 py-1 text-[11px] font-medium text-sky-400">
                {resultModeLabel}
              </span>
            ) : null}
          </div>
          <div className="rounded-full border theme-border theme-bg-main px-3 py-1 text-xs theme-text-secondary">
            {pageSummary}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 p-4 md:p-6">
        {isLoading ? (
          <div className="rounded-lg border border-dashed theme-border p-6 text-sm theme-text-secondary">
            {loadingLabel}
          </div>
        ) : items.length === 0 ? (
          <div className="rounded-lg border border-dashed theme-border p-6 text-sm theme-text-secondary">
            {emptyLabel}
          </div>
        ) : (
          <div className="space-y-3">
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => onSelect(item.id)}
                className={cn(
                  "w-full rounded-2xl border theme-border bg-[color:var(--bg-main)]/60 p-4 text-left transition-all hover:border-primary/40 hover:bg-[var(--bg-hover)]",
                  selectedId === item.id && "border-primary bg-primary/10 shadow-sm"
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold theme-text-primary">{item.title}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] theme-text-secondary">
                      {item.tags.map((tag) => (
                        <span key={`${item.id}-${tag}`} className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                  <span className={cn("shrink-0 rounded-full border px-2 py-1 text-[11px] font-medium", item.statusToneClassName)}>
                    {item.statusLabel}
                  </span>
                </div>
                <p className="mt-3 line-clamp-3 text-sm theme-text-secondary">{item.summary}</p>
                <div className="mt-3 flex flex-wrap gap-3 text-[11px] theme-text-secondary">
                  {item.sourceLabel ? <span>{item.sourceLabel}</span> : null}
                  {item.workspaceBindingLabel ? <span>{item.workspaceBindingLabel}</span> : null}
                  {item.updatedAtLabel ? <span>{item.updatedAtLabel}</span> : null}
                </div>
              </button>
            ))}
          </div>
        )}
        {showPagination ? (
          <div className="flex items-center justify-between gap-2 border-t theme-border pt-3">
            <span className="text-xs theme-text-secondary">
              {pageLabel}: {currentPage} / {Math.max(totalPages, 1)}
            </span>
            <div className="flex items-center gap-2">
              <Button type="button" variant="outline" size="sm" disabled={disablePrevious} onClick={onPrevious}>
                {previousLabel}
              </Button>
              <Button type="button" variant="outline" size="sm" disabled={disableNext} onClick={onNext}>
                {nextLabel}
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

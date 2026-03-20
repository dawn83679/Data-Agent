import type { ReactNode } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../../components/ui/Card";
import { cn } from "../../../lib/utils";

export interface MemoryDetailPanelProps {
  title: string;
  description: string;
  loadingLabel: string;
  isLoading?: boolean;
  statusLabel?: string;
  statusToneClassName?: string;
  editor: ReactNode;
  meta?: ReactNode;
  sticky?: boolean;
}

export function MemoryDetailPanel({
  title,
  description,
  loadingLabel,
  isLoading,
  statusLabel,
  statusToneClassName,
  editor,
  meta,
  sticky = true,
}: MemoryDetailPanelProps) {
  return (
    <Card className={cn("border theme-border shadow-sm", sticky && "xl:sticky xl:top-4 xl:self-start")}>
      <CardHeader className="border-b theme-border bg-[color:var(--bg-panel)]/50">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <CardTitle className="text-lg theme-text-primary">{title}</CardTitle>
            <CardDescription className="theme-text-secondary">{description}</CardDescription>
          </div>
          {statusLabel ? (
            <span className={cn("rounded-full border px-3 py-1 text-xs font-medium", statusToneClassName)}>
              {statusLabel}
            </span>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-5 p-4 md:p-6">
        {isLoading ? (
          <div className="rounded-lg border border-dashed theme-border p-5 text-sm theme-text-secondary">
            {loadingLabel}
          </div>
        ) : (
          editor
        )}
        {meta}
      </CardContent>
    </Card>
  );
}

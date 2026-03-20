import type { ReactNode } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/Card';
import { cn } from '../../../lib/utils';

export interface MemoryWorkbenchPanelProps {
  title: string;
  description: string;
  badgeLabel?: string;
  statusLabel?: string;
  statusToneClassName?: string;
  actions?: ReactNode;
  children: ReactNode;
}

export function MemoryWorkbenchPanel({
  title,
  description,
  badgeLabel,
  statusLabel,
  statusToneClassName,
  actions,
  children,
}: MemoryWorkbenchPanelProps) {
  return (
    <Card className="border theme-border shadow-sm">
      <CardHeader className="border-b theme-border bg-[color:var(--bg-panel)]/45">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <CardTitle className="text-lg theme-text-primary">{title}</CardTitle>
              {badgeLabel ? (
                <span className="inline-flex rounded-full border border-sky-500/35 bg-sky-500/10 px-2 py-1 text-[11px] font-medium text-sky-400">
                  {badgeLabel}
                </span>
              ) : null}
              {statusLabel ? (
                <span className={cn('inline-flex rounded-full border px-2 py-1 text-[11px] font-medium', statusToneClassName)}>
                  {statusLabel}
                </span>
              ) : null}
            </div>
            <CardDescription className="theme-text-secondary">{description}</CardDescription>
          </div>
          {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
        </div>
      </CardHeader>
      <CardContent className="p-4 md:p-6">{children}</CardContent>
    </Card>
  );
}

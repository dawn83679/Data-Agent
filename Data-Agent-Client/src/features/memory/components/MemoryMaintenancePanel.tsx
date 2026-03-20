import { Archive, RefreshCcw } from "lucide-react";
import { Button } from "../../../components/ui/Button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../../components/ui/Card";

export interface MemoryMaintenanceStats {
  activeCount: number;
  archivedCount: number;
  hiddenCount: number;
  duplicateCount: number;
  generatedAt: string;
}

export interface MemoryMaintenancePanelProps {
  title: string;
  description: string;
  activeLabel: string;
  archivedLabel: string;
  hiddenLabel: string;
  duplicateLabel: string;
  generatedAtLabel: string;
  runLabel: string;
  refreshLabel?: string;
  onRefresh: () => void;
  onRun: () => void;
  loading?: boolean;
  stats: MemoryMaintenanceStats;
}

export function MemoryMaintenancePanel({
  title,
  description,
  activeLabel,
  archivedLabel,
  hiddenLabel,
  duplicateLabel,
  generatedAtLabel,
  runLabel,
  refreshLabel,
  onRefresh,
  onRun,
  loading,
  stats,
}: MemoryMaintenancePanelProps) {
  return (
    <Card className="border theme-border shadow-sm">
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <div>
            <CardTitle className="text-base theme-text-primary">{title}</CardTitle>
            <CardDescription className="theme-text-secondary">{description}</CardDescription>
          </div>
          <Button type="button" variant="ghost" size="sm" onClick={onRefresh} disabled={loading} title={refreshLabel ?? title}>
            <RefreshCcw className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="grid grid-cols-2 gap-2">
          <div className="rounded-lg border theme-border theme-bg-main p-2">
            <p className="text-[11px] uppercase tracking-wide theme-text-secondary">{activeLabel}</p>
            <p className="mt-1 text-lg font-semibold theme-text-primary">{stats.activeCount}</p>
          </div>
          <div className="rounded-lg border theme-border theme-bg-main p-2">
            <p className="text-[11px] uppercase tracking-wide theme-text-secondary">{archivedLabel}</p>
            <p className="mt-1 text-lg font-semibold theme-text-primary">{stats.archivedCount}</p>
          </div>
          <div className="rounded-lg border theme-border theme-bg-main p-2">
            <p className="text-[11px] uppercase tracking-wide theme-text-secondary">{hiddenLabel}</p>
            <p className="mt-1 text-lg font-semibold theme-text-primary">{stats.hiddenCount}</p>
          </div>
          <div className="rounded-lg border theme-border theme-bg-main p-2">
            <p className="text-[11px] uppercase tracking-wide theme-text-secondary">{duplicateLabel}</p>
            <p className="mt-1 text-lg font-semibold theme-text-primary">{stats.duplicateCount}</p>
          </div>
        </div>
        <div className="rounded-lg border border-dashed theme-border p-2 text-xs theme-text-secondary">
          {generatedAtLabel}: {stats.generatedAt || "--"}
        </div>
        <Button type="button" className="w-full" onClick={onRun} disabled={loading}>
          <Archive className="mr-2 h-4 w-4" />
          {runLabel}
        </Button>
      </CardContent>
    </Card>
  );
}

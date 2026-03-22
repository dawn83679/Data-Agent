import type { ReactNode } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Button } from '../../../components/ui/Button';

export interface MemoryOverviewHeroProps {
  title: string;
  description?: string;
  backLabel: string;
  onBack: () => void;
  actions?: ReactNode;
}

export function MemoryOverviewHero({
  title,
  description,
  backLabel,
  onBack,
  actions,
}: MemoryOverviewHeroProps) {
  return (
    <section className="relative overflow-hidden rounded-3xl border theme-border theme-bg-panel px-5 py-5 shadow-sm animate-fade-in">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(16,185,129,0.14),transparent_30%),radial-gradient(circle_at_bottom_left,rgba(59,130,246,0.14),transparent_26%)]" />
      <div className="relative flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-8 rounded-full border theme-border theme-bg-main px-3 theme-text-secondary hover:theme-text-primary"
            onClick={onBack}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            {backLabel}
          </Button>
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight theme-text-primary">{title}</h1>
            {description ? <p className="max-w-3xl text-sm theme-text-secondary">{description}</p> : null}
          </div>
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
    </section>
  );
}

import type { ReactNode } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/Card';

export interface MemoryControlCenterProps {
  title: string;
  description: string;
  children: ReactNode;
}

export function MemoryControlCenter({ title, description, children }: MemoryControlCenterProps) {
  return (
    <Card className="border theme-border shadow-sm xl:sticky xl:top-4 xl:self-start">
      <CardHeader className="border-b theme-border bg-[color:var(--bg-panel)]/45">
        <CardTitle className="text-lg theme-text-primary">{title}</CardTitle>
        <CardDescription className="theme-text-secondary">{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6 p-4 md:p-5">{children}</CardContent>
    </Card>
  );
}

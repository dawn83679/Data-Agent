import type { ReactNode } from "react";

export interface MemoryControlSidebarProps {
  filtersSection: ReactNode;
  searchSection: ReactNode;
  maintenanceSection?: ReactNode;
  sticky?: boolean;
}

export function MemoryControlSidebar({
  filtersSection,
  searchSection,
  maintenanceSection,
  sticky = true,
}: MemoryControlSidebarProps) {
  return (
    <aside className={sticky ? "space-y-6 xl:sticky xl:top-4 xl:self-start" : "space-y-6"}>
      {filtersSection}
      {searchSection}
      {maintenanceSection}
    </aside>
  );
}

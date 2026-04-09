import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useAuthStore } from "../../store/authStore";
import { I18N_KEYS } from "../../constants/i18nKeys";
import type { UserOrganizationMembership } from "../../types/auth";
import { cn } from "../../lib/utils";

function roleLabel(t: (k: string) => string, role: string): string {
    const u = role.toUpperCase();
    if (u === "ADMIN") return t(I18N_KEYS.COMMON.WORKSPACE_ROLE_ADMIN);
    if (u === "COMMON") return t(I18N_KEYS.COMMON.WORKSPACE_ROLE_MEMBER);
    return role;
}

export function WorkspaceSwitcher() {
    const { t } = useTranslation();
    const accessToken = useAuthStore((s) => s.accessToken);
    const user = useAuthStore((s) => s.user);
    const workspaceType = useAuthStore((s) => s.workspaceType);
    const workspaceOrgId = useAuthStore((s) => s.workspaceOrgId);
    const setWorkspacePersonal = useAuthStore((s) => s.setWorkspacePersonal);
    const setWorkspaceOrganization = useAuthStore((s) => s.setWorkspaceOrganization);

    const orgs = user?.organizations ?? [];

    const options = useMemo(() => {
        const list: { value: string; label: string }[] = [
            { value: "PERSONAL", label: t(I18N_KEYS.COMMON.WORKSPACE_PERSONAL) },
        ];
        for (const o of orgs as UserOrganizationMembership[]) {
            list.push({
                value: `ORG:${o.orgId}`,
                label: `${o.orgName} (${roleLabel(t, String(o.roleCode))})`,
            });
        }
        return list;
    }, [orgs, t]);

    const selectValue =
        workspaceType === "ORGANIZATION" && workspaceOrgId != null
            ? `ORG:${workspaceOrgId}`
            : "PERSONAL";

    if (!accessToken) {
        return null;
    }

    return (
        <label className="flex items-center gap-2 text-xs shrink-0 min-w-0">
            <span className="theme-text-secondary truncate hidden sm:inline">
                {t(I18N_KEYS.COMMON.WORKSPACE)}
            </span>
            <select
                className={cn(
                    "max-w-[200px] sm:max-w-[240px] h-7 rounded-md border theme-border",
                    "bg-[color:var(--bg-popup)] theme-text-primary px-2 py-0.5 text-xs cursor-pointer",
                    "focus:outline-none focus:ring-1 focus:ring-ring"
                )}
                value={selectValue}
                aria-label={t(I18N_KEYS.COMMON.WORKSPACE)}
                onChange={(e) => {
                    const v = e.target.value;
                    if (v === "PERSONAL") {
                        setWorkspacePersonal();
                        return;
                    }
                    if (v.startsWith("ORG:")) {
                        const id = Number(v.slice(4));
                        if (!Number.isNaN(id)) {
                            setWorkspaceOrganization(id);
                        }
                    }
                }}
            >
                {options.map((o) => (
                    <option key={o.value} value={o.value}>
                        {o.label}
                    </option>
                ))}
            </select>
        </label>
    );
}

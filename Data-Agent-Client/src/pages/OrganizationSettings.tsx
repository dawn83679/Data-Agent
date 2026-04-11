import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { I18N_KEYS } from '../constants/i18nKeys';
import { ROUTES } from '../constants/routes';
import { useToast } from '../hooks/useToast';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { resolveErrorMessage } from '../lib/errorMessage';
import {
  organizationService,
  type ManagedOrganization,
  type MyOrganizationMembership,
  type OrganizationMemberRow,
} from '../services/organization.service';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store/authStore';

export default function OrganizationSettings() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useToast();
  const [managed, setManaged] = useState<ManagedOrganization[]>([]);
  const [memberships, setMemberships] = useState<MyOrganizationMembership[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingMemberships, setLoadingMemberships] = useState(true);
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [members, setMembers] = useState<OrganizationMemberRow[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(false);

  const [newOrgCode, setNewOrgCode] = useState('');
  const [newOrgName, setNewOrgName] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<'ADMIN' | 'COMMON'>('COMMON');

  const refreshUser = useCallback(async () => {
    const { accessToken, refreshToken, user, setAuth, rememberMe } = useAuthStore.getState();
    if (!accessToken) return;
    try {
      const full = await authService.getCurrentUser();
      setAuth({ ...(user ?? { id: 0, username: '', email: '' }), ...full }, accessToken, refreshToken, rememberMe);
      useAuthStore.getState().reconcileWorkspaceWithUser();
    } catch {
      /* ignore */
    }
  }, []);

  const loadManaged = useCallback(async () => {
    setLoadingList(true);
    setLoadingMemberships(true);
    try {
      const [managedResult, memResult] = await Promise.allSettled([
        organizationService.listManaged(),
        organizationService.listMemberships(),
      ]);
      if (managedResult.status === 'fulfilled') {
        const list = managedResult.value;
        setManaged(list);
        setSelectedOrgId((prev) => {
          if (prev != null && list.some((o) => o.id === prev)) {
            return prev;
          }
          return list[0]?.id ?? null;
        });
      } else {
        toast.error(
          resolveErrorMessage(managedResult.reason, t(I18N_KEYS.ORGANIZATION_PAGE.LOAD_MANAGED_FAILED))
        );
        setManaged([]);
      }
      if (memResult.status === 'fulfilled') {
        setMemberships(memResult.value);
      } else {
        toast.error(
          resolveErrorMessage(memResult.reason, t(I18N_KEYS.ORGANIZATION_PAGE.LOAD_MEMBERSHIPS_FAILED))
        );
        setMemberships([]);
      }
    } finally {
      setLoadingList(false);
      setLoadingMemberships(false);
    }
  }, [t, toast]);

  const loadMembers = useCallback(
    async (orgId: number) => {
      setLoadingMembers(true);
      try {
        const rows = await organizationService.listMembers(orgId);
        setMembers(rows);
      } catch (e) {
        toast.error(resolveErrorMessage(e, t(I18N_KEYS.ORGANIZATION_PAGE.LOAD_MEMBERS_FAILED)));
        setMembers([]);
      } finally {
        setLoadingMembers(false);
      }
    },
    [t]
  );

  useEffect(() => {
    loadManaged();
  }, [loadManaged]);

  useEffect(() => {
    if (selectedOrgId != null) {
      loadMembers(selectedOrgId);
    } else {
      setMembers([]);
    }
  }, [selectedOrgId, loadMembers]);

  const handleCreateOrg = async () => {
    const code = newOrgCode.trim();
    const name = newOrgName.trim();
    if (!code || !name) {
      toast.error(t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_VALIDATION));
      return;
    }
    try {
      const created = await organizationService.create(code, name);
      toast.success(t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_SUCCESS));
      setNewOrgCode('');
      setNewOrgName('');
      await loadManaged();
      setSelectedOrgId(created.id);
      await refreshUser();
    } catch (e) {
      toast.error(resolveErrorMessage(e, t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_FAILED)));
    }
  };

  const handleInvite = async () => {
    if (selectedOrgId == null) return;
    const email = inviteEmail.trim();
    if (!email) {
      toast.error(t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_EMAIL_REQUIRED));
      return;
    }
    try {
      await organizationService.addMember(selectedOrgId, email, inviteRole);
      toast.success(t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_SUCCESS));
      setInviteEmail('');
      await loadMembers(selectedOrgId);
      await refreshUser();
    } catch (e) {
      toast.error(resolveErrorMessage(e, t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_FAILED)));
    }
  };

  const handleRemoveMember = async (row: OrganizationMemberRow) => {
    if (selectedOrgId == null) return;
    if (row.roleCode?.toUpperCase() === 'ADMIN') return;
    if (!window.confirm(t(I18N_KEYS.ORGANIZATION_PAGE.REMOVE_MEMBER_CONFIRM))) {
      return;
    }
    try {
      await organizationService.removeMember(selectedOrgId, row.memberId);
      toast.success(t(I18N_KEYS.ORGANIZATION_PAGE.REMOVE_MEMBER_SUCCESS));
      await loadMembers(selectedOrgId);
      await refreshUser();
    } catch (e) {
      toast.error(resolveErrorMessage(e, t(I18N_KEYS.ORGANIZATION_PAGE.REMOVE_MEMBER_FAILED)));
    }
  };

  const handleLeaveOrganization = async (row: MyOrganizationMembership) => {
    if (!window.confirm(t(I18N_KEYS.ORGANIZATION_PAGE.LEAVE_ORG_CONFIRM))) {
      return;
    }
    try {
      await organizationService.leaveOrganization(row.id);
      toast.success(t(I18N_KEYS.ORGANIZATION_PAGE.LEAVE_ORG_SUCCESS));
      await loadManaged();
      await refreshUser();
    } catch (e) {
      toast.error(resolveErrorMessage(e, t(I18N_KEYS.ORGANIZATION_PAGE.LEAVE_ORG_FAILED)));
    }
  };

  const selectedOrg = managed.find((o) => o.id === selectedOrgId) ?? null;

  const membershipRoleLabel = (code: string) => {
    const c = code.trim().toUpperCase();
    if (c === 'ADMIN') return t(I18N_KEYS.ORGANIZATION_PAGE.ROLE_ADMIN);
    if (c === 'COMMON') return t(I18N_KEYS.ORGANIZATION_PAGE.ROLE_COMMON);
    return code;
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate(ROUTES.HOME)}
          className="h-8 px-2 -ml-2 theme-text-secondary hover:theme-text-primary"
        >
          <ArrowLeft className="h-4 w-4 mr-1" />
          {t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
        </Button>
      </div>

      <div className="bg-card rounded-lg border border-border p-6 space-y-6">
        <div className="mb-6">
          <h2 className="text-xl font-semibold">{t(I18N_KEYS.ORGANIZATION_PAGE.PAGE_TITLE)}</h2>
          <p className="text-sm text-muted-foreground mt-1">{t(I18N_KEYS.ORGANIZATION_PAGE.PAGE_DESC)}</p>
        </div>

        <section className="mb-10 space-y-3 rounded-lg border border-border p-4">
          <h3 className="text-sm font-medium">{t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_SECTION)}</h3>
          <p className="text-xs text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_HINT)}</p>
          <div className="flex flex-col sm:flex-row gap-2 sm:items-end">
            <label className="flex flex-col gap-1 text-xs">
              <span>{t(I18N_KEYS.ORGANIZATION_PAGE.FIELD_CODE)}</span>
              <Input
                value={newOrgCode}
                onChange={(e) => setNewOrgCode(e.target.value)}
                placeholder="demo-org"
                className="w-full sm:w-48"
              />
            </label>
            <label className="flex flex-col gap-1 text-xs flex-1 min-w-0">
              <span>{t(I18N_KEYS.ORGANIZATION_PAGE.FIELD_NAME)}</span>
              <Input
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
                placeholder={t(I18N_KEYS.ORGANIZATION_PAGE.NAME_PLACEHOLDER)}
              />
            </label>
            <Button type="button" size="sm" onClick={handleCreateOrg}>
              {t(I18N_KEYS.ORGANIZATION_PAGE.CREATE_BUTTON)}
            </Button>
          </div>
        </section>

        <section className="mb-10 space-y-3 rounded-lg border border-border p-4">
          <h3 className="text-sm font-medium">{t(I18N_KEYS.ORGANIZATION_PAGE.MEMBERSHIPS_SECTION)}</h3>
          {loadingMemberships ? (
            <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.LOADING_MEMBERSHIPS)}</p>
          ) : memberships.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.NO_MEMBERSHIPS)}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse">
                <thead>
                  <tr className="border-b border-border text-left text-xs text-muted-foreground">
                    <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ORG_NAME)}</th>
                    <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ORG_CODE)}</th>
                    <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ROLE)}</th>
                    <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ACTIONS)}</th>
                  </tr>
                </thead>
                <tbody>
                  {memberships.map((row) => (
                    <tr key={row.id} className="border-b border-border/60">
                      <td className="py-2 pr-2">{row.orgName}</td>
                      <td className="py-2 pr-2">{row.orgCode}</td>
                      <td className="py-2 pr-2">{membershipRoleLabel(row.roleCode)}</td>
                      <td className="py-2 pr-2">
                        <Button
                          type="button"
                          size="sm"
                          variant="ghost"
                          onClick={() => handleLeaveOrganization(row)}
                        >
                          {t(I18N_KEYS.ORGANIZATION_PAGE.LEAVE_ORG_BUTTON)}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="space-y-4">
          <h3 className="text-sm font-medium">{t(I18N_KEYS.ORGANIZATION_PAGE.MANAGE_SECTION)}</h3>
          {loadingList ? (
            <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.LOADING_MANAGED)}</p>
          ) : managed.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.NO_ORGS)}</p>
          ) : (
            <>
              <label className="flex flex-col gap-1 text-xs max-w-md">
                <span>{t(I18N_KEYS.ORGANIZATION_PAGE.SELECT_ORG)}</span>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  value={selectedOrgId ?? ''}
                  onChange={(e) => setSelectedOrgId(Number(e.target.value) || null)}
                >
                  {managed.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.orgName} ({o.orgCode})
                    </option>
                  ))}
                </select>
              </label>

              {selectedOrg && (
                <div className="rounded-lg border border-border p-4 space-y-4">
                  <div className="flex flex-col sm:flex-row gap-2 sm:items-end">
                    <label className="flex flex-col gap-1 text-xs flex-1 min-w-0">
                      <span>{t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_EMAIL)}</span>
                      <Input
                        type="email"
                        value={inviteEmail}
                        onChange={(e) => setInviteEmail(e.target.value)}
                        placeholder={t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_EMAIL_PLACEHOLDER)}
                      />
                    </label>
                    <label className="flex flex-col gap-1 text-xs">
                      <span>{t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_ROLE)}</span>
                      <select
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        value={inviteRole}
                        onChange={(e) => setInviteRole(e.target.value as 'ADMIN' | 'COMMON')}
                      >
                        <option value="COMMON">{t(I18N_KEYS.ORGANIZATION_PAGE.ROLE_COMMON)}</option>
                        <option value="ADMIN">{t(I18N_KEYS.ORGANIZATION_PAGE.ROLE_ADMIN)}</option>
                      </select>
                    </label>
                    <Button type="button" size="sm" onClick={handleInvite}>
                      {t(I18N_KEYS.ORGANIZATION_PAGE.INVITE_BUTTON)}
                    </Button>
                  </div>

                  <div>
                    <h4 className="text-xs font-medium mb-2">{t(I18N_KEYS.ORGANIZATION_PAGE.MEMBER_LIST)}</h4>
                    {loadingMembers ? (
                      <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.LOADING_MEMBERS)}</p>
                    ) : members.length === 0 ? (
                      <p className="text-sm text-muted-foreground">{t(I18N_KEYS.ORGANIZATION_PAGE.NO_MEMBERS)}</p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm border-collapse">
                          <thead>
                            <tr className="border-b border-border text-left text-xs text-muted-foreground">
                              <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_EMAIL)}</th>
                              <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_USERNAME)}</th>
                              <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ROLE)}</th>
                              <th className="py-2 pr-2">{t(I18N_KEYS.ORGANIZATION_PAGE.COL_ACTIONS)}</th>
                            </tr>
                          </thead>
                          <tbody>
                            {members.map((m) => (
                              <tr key={m.memberId} className="border-b border-border/60">
                                <td className="py-2 pr-2">{m.email}</td>
                                <td className="py-2 pr-2">{m.username}</td>
                                <td className="py-2 pr-2">{membershipRoleLabel(m.roleCode)}</td>
                                <td className="py-2 pr-2">
                                  {m.roleCode?.toUpperCase() === 'COMMON' ? (
                                    <Button
                                      type="button"
                                      size="sm"
                                      variant="ghost"
                                      onClick={() => handleRemoveMember(m)}
                                    >
                                      {t(I18N_KEYS.ORGANIZATION_PAGE.REMOVE_MEMBER_BUTTON)}
                                    </Button>
                                  ) : null}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}

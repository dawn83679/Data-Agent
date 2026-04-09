-- ===============================================
-- Organization workspace + AI permission rule workspace columns
-- 1) sys_organizations, sys_organization_members,
--    sys_organization_member_roles, sys_organization_connection_permissions
-- 2) ai_permission_rule: workspace_type, org_id + updated unique index
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_organizations (
    id          BIGSERIAL PRIMARY KEY,
    org_code    VARCHAR(64)  NOT NULL,
    org_name    VARCHAR(128) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organizations IS 'Organization information table';
COMMENT ON COLUMN sys_organizations.id IS 'Primary key';
COMMENT ON COLUMN sys_organizations.org_code IS 'Unique organization code';
COMMENT ON COLUMN sys_organizations.org_name IS 'Organization display name';
COMMENT ON COLUMN sys_organizations.status IS '1=enabled, 0=disabled';
COMMENT ON COLUMN sys_organizations.remark IS 'Remark';
COMMENT ON COLUMN sys_organizations.created_by IS 'Creator user id (sys_users.id)';
COMMENT ON COLUMN sys_organizations.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organizations.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organizations_code ON sys_organizations (org_code);
CREATE INDEX IF NOT EXISTS idx_sys_organizations_status ON sys_organizations (status);

-- ===============================================
-- sys_organization_members
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_organization_members (
    id         BIGSERIAL PRIMARY KEY,
    org_id     BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    status     SMALLINT  NOT NULL DEFAULT 1,
    joined_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_members IS 'Organization membership table';
COMMENT ON COLUMN sys_organization_members.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_members.org_id IS 'Organization id';
COMMENT ON COLUMN sys_organization_members.user_id IS 'User id (sys_users.id)';
COMMENT ON COLUMN sys_organization_members.status IS '1=active, 0=inactive';
COMMENT ON COLUMN sys_organization_members.joined_at IS 'Join time';
COMMENT ON COLUMN sys_organization_members.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_members.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_members_org_user ON sys_organization_members (org_id, user_id);
CREATE INDEX IF NOT EXISTS idx_sys_organization_members_user_id ON sys_organization_members (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_organization_members_org_status ON sys_organization_members (org_id, status);

-- ===============================================
-- sys_organization_member_roles
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_organization_member_roles (
    id                       BIGSERIAL PRIMARY KEY,
    organization_member_id   BIGINT    NOT NULL,
    role_code                VARCHAR(32) NOT NULL,
    active                   BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_member_roles IS 'Effective role for an organization member';
COMMENT ON COLUMN sys_organization_member_roles.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_member_roles.organization_member_id IS 'References sys_organization_members.id';
COMMENT ON COLUMN sys_organization_member_roles.role_code IS 'ADMIN or COMMON';
COMMENT ON COLUMN sys_organization_member_roles.active IS 'Whether this role row is the current effective one';
COMMENT ON COLUMN sys_organization_member_roles.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_member_roles.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_member_roles_active_member ON sys_organization_member_roles (organization_member_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_sys_organization_member_roles_role_code ON sys_organization_member_roles (role_code);

-- ===============================================
-- sys_organization_connection_permissions
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_organization_connection_permissions (
    id             BIGSERIAL PRIMARY KEY,
    org_id         BIGINT    NOT NULL,
    connection_id  BIGINT    NOT NULL,
    enabled        BOOLEAN   NOT NULL DEFAULT TRUE,
    granted_by     BIGINT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_connection_permissions IS 'Org-level authorization to use a db connection';
COMMENT ON COLUMN sys_organization_connection_permissions.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_connection_permissions.org_id IS 'Organization id';
COMMENT ON COLUMN sys_organization_connection_permissions.connection_id IS 'db_connections.id';
COMMENT ON COLUMN sys_organization_connection_permissions.enabled IS 'Whether the grant is active';
COMMENT ON COLUMN sys_organization_connection_permissions.granted_by IS 'User id of grant operator';
COMMENT ON COLUMN sys_organization_connection_permissions.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_connection_permissions.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_connection_permissions_org_conn ON sys_organization_connection_permissions (org_id, connection_id);
CREATE INDEX IF NOT EXISTS idx_sys_organization_connection_permissions_connection ON sys_organization_connection_permissions (connection_id);

-- ===============================================
-- ai_permission_rule workspace dimension
-- ===============================================

ALTER TABLE ai_permission_rule
    ADD COLUMN IF NOT EXISTS workspace_type VARCHAR(32) NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE ai_permission_rule
    ADD COLUMN IF NOT EXISTS org_id BIGINT;

COMMENT ON COLUMN ai_permission_rule.workspace_type IS 'PERSONAL or ORGANIZATION';
COMMENT ON COLUMN ai_permission_rule.org_id IS 'Organization id when workspace_type is ORGANIZATION';

DROP INDEX IF EXISTS uq_ai_permission_rule_scope_match;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_permission_rule_scope_match
    ON ai_permission_rule (
        user_id,
        workspace_type,
        COALESCE(org_id, -1),
        scope_type,
        COALESCE(conversation_id, -1),
        connection_id,
        catalog_match_mode,
        COALESCE(catalog_name, '__NULL__'),
        schema_match_mode,
        COALESCE(schema_name, '__NULL__')
    );

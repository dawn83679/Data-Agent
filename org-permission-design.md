# Data-Agent 个人版/组织版工作空间与权限改造设计文档

## 1. 文档目的

本文档用于设计当前阶段的组织、角色、数据源授权与工作空间切换方案，目标是满足以下业务诉求：

1. 区分管理员用户和普通用户。
2. 普通用户登录后只能看到模型对话界面，不显示数据库 Explorer、SQL 工作台、连接管理等页面能力。
3. 普通用户不能修改数据库中的数据。
4. 一个用户可以属于多个组织，并且在不同组织下的角色可以不同。
5. 数据源可以按组织授权，组织内成员可在当前组织上下文下使用被授权的数据源。
6. AI Tool 执行时需要做权限校验，避免绕过前端直接操作数据库。
7. 用户可以在“个人版”和“组织版”之间切换。
8. 个人版下可以使用完整功能，但仅操作个人拥有的数据源。

本文档明确采用的是“当前业务简化模型”，不是标准 RBAC，而是：

- 组织成员关系
- 组织内角色
- 组织级数据源授权
- 个人工作空间与组织工作空间切换
- 基于角色和资源授权的访问控制

## 2. 设计目标

### 2.1 目标

- 引入组织维度。
- 引入组织内角色维度。
- 引入组织对数据源的授权维度。
- 引入“个人版 / 组织版”工作空间维度。
- 将现有仅按 `userId` 隔离的权限模型升级为“工作空间 + 用户 + 组织角色”上下文模型。
- 保证前端隐藏页面时，后端仍然具备强制权限校验能力。
- 保持对现有 `db_connections.user_id` 所有权模型的兼容。

### 2.2 非目标

- 暂不实现标准 RBAC 中的 `role -> permission` 通用配置体系。
- 暂不引入“菜单权限表”“按钮权限表”“通用资源权限表”。
- 暂不改变 `db_connections` 的所有权语义，仍然保留“连接由创建者拥有”的模型。
- 暂不支持同一用户在同一组织下配置多个同时生效的角色。

## 3. 当前系统现状

### 3.1 当前后端现状

当前项目中的多个核心能力仍然仅按登录用户 `userId` 进行判断：

- `db_connections` 表通过 `user_id` 绑定连接创建者。
- `DbConnectionServiceImpl` 的 `getOwnedById/getAllConnections` 都是按 `RequestContext.userId` 直接查自己的连接。
- `ConnectionManager` 中活动连接的所有权校验也是直接比较 `active.userId == loginUserId`。
- `ai_permission_rule` 当前也是按用户维度控制 AI 写入确认与默认放行规则。

这意味着如果只在前端隐藏页面，而不改后端，普通用户仍有机会通过接口直接访问或修改数据。

### 3.2 当前前端现状

前端首页默认是工作台模式：

- 左侧：Database Explorer
- 中间：SQL 编辑与结果面板
- 右侧：AI Assistant

当前没有组织切换，也没有角色驱动的页面裁剪，更没有“个人版 / 组织版”切换概念。

## 4. 总体设计原则

### 4.1 核心原则

1. 连接所有权不变  
   连接仍由创建者用户拥有，保留 `db_connections.user_id` 语义。

2. 组织授权只扩展“可使用范围”，不直接改变连接 owner  
   即组织成员可以在组织上下文下使用某个连接，但不等于他拥有该连接。

3. 个人版与组织版必须是显式工作空间  
   后端不依赖“有没有传 orgId”去猜模式，而是明确知道当前是 `PERSONAL` 还是 `ORGANIZATION`。

4. 页面可见性和后端权限必须同时控制  
   前端负责隐藏，后端负责兜底。

5. 普通用户只保留“AI 对话 + 已授权数据源读能力”  
   不保留 Explorer、SQL 工作台、连接维护、数据库写入等能力。

6. 数据写入权限必须双重限制  
   先过工作空间/角色判断，再过 AI 写入规则判断。

## 5. 术语说明

### 5.1 用户

系统登录用户，对应现有 `sys_users`。

### 5.2 组织

用户的业务归属单元。用户可以加入多个组织。

### 5.3 个人版工作空间

用户以“个人”身份进入系统，仅使用自己拥有的数据源和完整工作台能力。

### 5.4 组织版工作空间

用户以某个组织成员身份进入系统，能力范围取决于该组织下的角色与组织授权数据源。

### 5.5 当前组织

当工作空间为组织版时，用户本次请求选择的组织上下文。

### 5.6 连接所有者

`db_connections.user_id` 对应的用户。

### 5.7 组织授权连接

指某个 `db_connection` 被授权给某个组织，组织成员在当前组织上下文下可以访问该连接。

## 6. 工作空间模型设计

当前系统明确区分两种工作空间模式：

### 6.1 工作空间类型

- `PERSONAL`
- `ORGANIZATION`

### 6.2 PERSONAL 模式

权限特点：

- 可使用完整工作台功能。
- 可看到 Explorer。
- 可使用 SQL 编辑区。
- 可管理自己的连接。
- 可对自己拥有的数据源使用 AI。
- 可执行数据库写操作，但仍需经过现有 AI 写入确认与默认放行规则。

边界：

- 只能访问自己拥有的连接。
- 不能借由个人版访问组织授权数据源。

### 6.3 ORGANIZATION 模式

权限特点：

- 需要绑定当前组织。
- 数据源可访问范围由“组织授权 + owner 兼容规则”决定。
- 页面能力由组织角色决定。

### 6.4 为什么要引入工作空间模型

如果只用“当前组织 ID”表达上下文，会有两个问题：

1. 无法明确区分“个人版”和“未选择组织”。
2. 后端很多逻辑会退化成“没传 orgId 就当个人模式”，语义不稳定。

因此建议明确引入：

- `workspaceType`
- `orgId`

## 7. 数据模型设计

当前阶段按你的要求设计为 4 张表。

### 7.1 组织表 `sys_organizations`

用途：存储组织基础信息。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL PK | 主键 |
| org_code | VARCHAR(64) | 组织编码，唯一 |
| org_name | VARCHAR(128) | 组织名称 |
| status | SMALLINT | 状态，1=启用，0=停用 |
| remark | VARCHAR(500) | 备注 |
| created_by | BIGINT | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

示例 DDL：

```sql
CREATE TABLE IF NOT EXISTS sys_organizations (
    id          BIGSERIAL PRIMARY KEY,
    org_code    VARCHAR(64) NOT NULL,
    org_name    VARCHAR(128) NOT NULL,
    status      SMALLINT NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organizations IS 'Organization information table';
COMMENT ON COLUMN sys_organizations.id IS 'Primary key';
COMMENT ON COLUMN sys_organizations.org_code IS 'Unique organization code';
COMMENT ON COLUMN sys_organizations.org_name IS 'Organization name';
COMMENT ON COLUMN sys_organizations.status IS 'Organization status: 1=enabled, 0=disabled';
COMMENT ON COLUMN sys_organizations.remark IS 'Remark';
COMMENT ON COLUMN sys_organizations.created_by IS 'Creator user id';
COMMENT ON COLUMN sys_organizations.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organizations.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organizations_code ON sys_organizations(org_code);
CREATE INDEX IF NOT EXISTS idx_sys_organizations_status ON sys_organizations(status);
```

### 7.2 组织成员表 `sys_organization_members`

用途：表达“用户属于哪些组织”。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL PK | 主键 |
| org_id | BIGINT | 组织 ID |
| user_id | BIGINT | 用户 ID |
| status | SMALLINT | 状态，1=有效，0=失效 |
| joined_at | TIMESTAMP | 加入时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

示例 DDL：

```sql
CREATE TABLE IF NOT EXISTS sys_organization_members (
    id          BIGSERIAL PRIMARY KEY,
    org_id      BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    status      SMALLINT NOT NULL DEFAULT 1,
    joined_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_members IS 'Organization member table';
COMMENT ON COLUMN sys_organization_members.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_members.org_id IS 'Organization id';
COMMENT ON COLUMN sys_organization_members.user_id IS 'User id';
COMMENT ON COLUMN sys_organization_members.status IS 'Member status: 1=active, 0=inactive';
COMMENT ON COLUMN sys_organization_members.joined_at IS 'Join time';
COMMENT ON COLUMN sys_organization_members.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_members.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_members_org_user
    ON sys_organization_members(org_id, user_id);

CREATE INDEX IF NOT EXISTS idx_sys_organization_members_user_id
    ON sys_organization_members(user_id);

CREATE INDEX IF NOT EXISTS idx_sys_organization_members_org_status
    ON sys_organization_members(org_id, status);
```

### 7.3 组织成员角色表 `sys_organization_member_roles`

用途：表达“用户在某个组织里的角色”。

说明：

- 该表和 `sys_organization_members` 分离，是为了符合当前 4 张表设计要求。
- 当前业务约束下，同一个组织成员关系只允许 1 个有效角色。
- 表结构保留后续扩展空间，如果未来要支持角色历史、角色变更记录或多个角色，可以继续演进。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL PK | 主键 |
| organization_member_id | BIGINT | 关联 `sys_organization_members.id` |
| role_code | VARCHAR(32) | 角色编码，取值 `ADMIN` / `COMMON` |
| active | BOOLEAN | 是否当前生效 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

示例 DDL：

```sql
CREATE TABLE IF NOT EXISTS sys_organization_member_roles (
    id                      BIGSERIAL PRIMARY KEY,
    organization_member_id  BIGINT NOT NULL,
    role_code               VARCHAR(32) NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_member_roles IS 'Organization member role table';
COMMENT ON COLUMN sys_organization_member_roles.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_member_roles.organization_member_id IS 'Organization member id';
COMMENT ON COLUMN sys_organization_member_roles.role_code IS 'Role code: ADMIN or COMMON';
COMMENT ON COLUMN sys_organization_member_roles.active IS 'Whether the role is currently active';
COMMENT ON COLUMN sys_organization_member_roles.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_member_roles.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_member_roles_active_member
    ON sys_organization_member_roles(organization_member_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_sys_organization_member_roles_role_code
    ON sys_organization_member_roles(role_code);
```

### 7.4 组织数据源权限表 `sys_organization_connection_permissions`

用途：表达“某个组织被授权使用哪些数据源”。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL PK | 主键 |
| org_id | BIGINT | 组织 ID |
| connection_id | BIGINT | 数据源连接 ID，对应 `db_connections.id` |
| enabled | BOOLEAN | 是否启用 |
| granted_by | BIGINT | 授权人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

示例 DDL：

```sql
CREATE TABLE IF NOT EXISTS sys_organization_connection_permissions (
    id             BIGSERIAL PRIMARY KEY,
    org_id         BIGINT NOT NULL,
    connection_id  BIGINT NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    granted_by     BIGINT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_organization_connection_permissions IS 'Organization connection permission table';
COMMENT ON COLUMN sys_organization_connection_permissions.id IS 'Primary key';
COMMENT ON COLUMN sys_organization_connection_permissions.org_id IS 'Organization id';
COMMENT ON COLUMN sys_organization_connection_permissions.connection_id IS 'Database connection id';
COMMENT ON COLUMN sys_organization_connection_permissions.enabled IS 'Whether the permission is enabled';
COMMENT ON COLUMN sys_organization_connection_permissions.granted_by IS 'User id of the grant operator';
COMMENT ON COLUMN sys_organization_connection_permissions.created_at IS 'Creation time';
COMMENT ON COLUMN sys_organization_connection_permissions.updated_at IS 'Last update time';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_organization_connection_permissions_org_conn
    ON sys_organization_connection_permissions(org_id, connection_id);

CREATE INDEX IF NOT EXISTS idx_sys_organization_connection_permissions_connection
    ON sys_organization_connection_permissions(connection_id);
```

## 8. 枚举设计

### 8.1 Java 角色枚举

```java
public enum OrganizationRoleEnum {
    ADMIN,
    COMMON
}
```

建议位置：

`edu.zsc.ai.common.enums.org.OrganizationRoleEnum`

### 8.2 工作空间枚举

```java
public enum WorkspaceTypeEnum {
    PERSONAL,
    ORGANIZATION
}
```

建议位置：

`edu.zsc.ai.common.enums.org.WorkspaceTypeEnum`

### 8.3 角色语义

#### ADMIN

- 可看到完整工作台页面。
- 可看到 Explorer。
- 可看到 SQL 编辑区。
- 可直接使用被授权数据源。
- 可执行数据库写操作，但仍需经过现有 AI 写入规则与确认机制。

#### COMMON

- 只能看到 AI 对话界面。
- 不能看到 Explorer。
- 不能看到连接管理页面。
- 不能直接进入 SQL 工作台。
- 可以在 AI 会话中使用当前组织授权的数据源做只读分析。
- 不能执行数据库写操作。

## 9. 权限模型设计

### 9.1 数据源访问判定规则

对任意 `connectionId`，访问判定顺序如下：

#### 规则 1：PERSONAL 模式下 owner 可访问

当满足以下条件时允许：

```text
workspaceType == PERSONAL
and loginUserId == db_connections.user_id
```

说明：

- PERSONAL 模式只允许访问自己的连接。
- PERSONAL 模式下不走组织授权逻辑。

#### 规则 2：ORGANIZATION 模式下 owner 仍可访问

当满足以下条件时允许：

```text
workspaceType == ORGANIZATION
and loginUserId == db_connections.user_id
```

说明：

- 这是现有 owner 兼容规则，建议保留。
- 目的是避免 owner 切到组织版后反而不能访问自己的连接。

#### 规则 3：ORGANIZATION 模式下组织授权可访问

当满足以下条件时允许：

```text
1. workspaceType == ORGANIZATION
2. 当前请求存在 currentOrgId
3. loginUserId 属于 currentOrgId
4. 当前组织内角色有效
5. sys_organization_connection_permissions(org_id=currentOrgId, connection_id=xxx, enabled=true) 存在
```

说明：

- 该规则允许组织成员在当前组织上下文下使用被授权连接。
- 这条规则是“扩展可访问范围”，不是改变 owner。

#### 规则 4：其他情况拒绝

任何不满足上述条件的数据源全部拒绝。

### 9.2 页面访问规则

#### PERSONAL

保留完整工作台：

- Explorer
- SQL Editor
- Results Panel
- AI Assistant
- 连接管理
- 权限规则管理

#### ORGANIZATION + ADMIN

保留现有工作台：

- Explorer
- SQL Editor
- Results Panel
- AI Assistant

#### ORGANIZATION + COMMON

只保留：

- 顶部基础信息
- 工作空间切换
- 全宽 AI 对话区域

隐藏或禁用：

- Explorer
- SQL 编辑器
- 连接创建、编辑、删除
- Permission Rule Dialog
- 数据库对象右键菜单

### 9.3 数据写入规则

数据库写入操作包括但不限于：

- INSERT
- UPDATE
- DELETE
- TRUNCATE
- ALTER
- DROP
- CREATE
- GRANT
- REVOKE

判定规则：

1. 当前工作空间为 `PERSONAL`  
   - 仅允许写入自己的连接
   - 仍需进入现有 AI 写入规则

2. 当前工作空间为 `ORGANIZATION` 且角色为 `COMMON`  
   直接拒绝，不进入 AI 确认流程。

3. 当前工作空间为 `ORGANIZATION` 且角色为 `ADMIN`  
   先验证当前数据源是否可访问，再进入现有 AI 写入规则：
   - 已存在默认放行规则，则允许执行
   - 没有默认放行规则，则弹确认

### 9.4 AI Tool 执行规则

AI Tool 执行分为读和写两类。

#### 读类 Tool

包括：

- 元数据查询
- Explorer 子代理读库
- 执行只读 SQL
- 表结构查看
- 数据抽样查询

权限要求：

- PERSONAL 模式下，连接 owner 可读。
- ORGANIZATION 模式下，连接 owner 可读，或当前组织授权后可读。

#### 写类 Tool

包括：

- 执行非 SELECT SQL
- 删除表 / 视图 / 过程 / 函数 / 触发器
- 建表
- 其他变更数据库状态的 Tool

权限要求：

- PERSONAL 模式下，可对个人连接执行，但仍走现有写入确认逻辑。
- ORGANIZATION 模式下，必须是 `ADMIN`。
- ORGANIZATION 模式下，必须能访问目标连接。
- 再走现有写入确认逻辑。

## 10. 请求上下文设计

### 10.1 为什么必须引入当前工作空间上下文

用户现在既可能在个人版下工作，也可能在某个组织版下工作。因此权限判断不能只看“用户有哪些组织”，还必须知道：

- 本次请求属于个人版还是组织版
- 如果是组织版，当前是哪个组织

建议在每次请求中携带：

- `X-Workspace-Type: PERSONAL | ORGANIZATION`
- `X-Org-Id: {currentOrgId}`，仅在组织版时要求传递

### 10.2 RequestContext 扩展

当前 `RequestContextInfo` 只有：

- `conversationId`
- `userId`
- `connectionId`
- `catalog`
- `schema`

建议扩展为：

- `workspaceType`
- `orgId`
- `orgUserRelId`
- `orgRole`

建议结构：

```java
public class RequestContextInfo {
    private Long conversationId;
    private Long userId;
    private Long connectionId;
    private String catalog;
    private String schema;
    private WorkspaceTypeEnum workspaceType;
    private Long orgId;
    private Long orgUserRelId;
    private OrganizationRoleEnum orgRole;
}
```

### 10.3 拦截器处理逻辑

在认证通过后，统一解析请求头 `X-Workspace-Type` 和 `X-Org-Id`：

1. 如果 `workspaceType == PERSONAL`
   - 不要求传 `X-Org-Id`
   - 当前请求只允许走个人能力路径

2. 如果 `workspaceType == ORGANIZATION`
   - 必须传 `X-Org-Id`
   - 校验当前用户是否属于该组织
   - 查询当前组织角色
   - 写入 `RequestContext`

3. 如果未传 `X-Workspace-Type`
   - 建议前端初始化时默认传 `PERSONAL`
   - 后端不建议自行猜测

### 10.4 当前代码中的一个关键修正点

当前系统中：

- `SaTokenConfigure` 会先写入 `RequestContext.userId`
- `@EnableRequestContext` 的 `RequestContextAspect` 会根据 `BaseRequest` 重新构造 `RequestContext`

如果后续工作空间上下文放在拦截器里，而 `Aspect` 仍然直接覆盖上下文，就会把 `workspaceType/orgId/orgRole` 覆盖丢失。

因此需要修改 `RequestContextAspect`：

- 采用“合并已有上下文”的方式
- 只补充 `conversationId/connectionId/catalog/schema`
- 不要清空已经在拦截器中解析出的 `workspaceType/orgId/orgRole`

## 11. 后端服务设计

### 11.1 新增组织权限解析服务

建议新增统一服务，例如：

`OrgAccessService`

职责：

- 校验用户是否属于指定组织
- 查询用户在组织中的角色
- 断言当前组织上下文合法

建议接口：

```java
public interface OrgAccessService {
    OrgAccessContext getCurrentOrgAccess();
    void assertOrgMember(Long orgId, Long userId);
    OrganizationRoleEnum getRole(Long orgId, Long userId);
}
```

### 11.2 新增数据源访问控制服务

建议新增：

`ConnectionAccessService`

职责：

- 统一判断一个连接是否可访问
- 统一区分“可读”“可管理”“可写”
- 识别当前是 `PERSONAL` 还是 `ORGANIZATION` 模式

建议接口：

```java
public interface ConnectionAccessService {
    boolean canAccess(Long connectionId);
    void assertReadable(Long connectionId);
    void assertWritableByRole(Long connectionId);
    boolean isOwner(Long connectionId, Long userId);
}
```

建议规则：

- `PERSONAL` 模式
  - 只允许 owner 访问
- `ORGANIZATION` 模式
  - owner 访问或组织授权访问

### 11.3 对现有 `DbConnectionService` 的改造建议

当前 `DbConnectionServiceImpl` 中以下逻辑需要升级：

- `getOwnedById`
- `getConnectionById`
- `getAllConnections`
- `createConnection`
- `updateConnection`
- `deleteConnection`

建议拆分为两类接口：

#### 所有者管理接口

- 创建连接
- 修改连接配置
- 删除连接配置

这些接口建议仍然只允许 owner 操作，避免组织成员误改连接配置。

#### 可访问接口

- 查询连接详情
- 列出当前可用连接
- 打开连接

这些接口走 `ConnectionAccessService`。

### 11.4 对 `ConnectionManager` 的改造建议

当前 `ConnectionManager.ActiveConnection` 只记录 `userId`，并使用：

- `dbConnectionId`
- `databaseName`
- `schemaName`

作为缓存维度。

组织授权引入后，这里有两个风险：

1. 组织上下文不同但命中同一个活动连接。
2. 不同用户共享授权连接时仅按 `userId` 校验不够灵活。

建议修改：

- `ActiveConnection` 增加 `ownerUserId`
- 增加 `openedByUserId`
- 增加 `workspaceType`
- 增加 `orgId`

建议权限判断时，不直接比较缓存中的 `userId`，而是重新调用 `ConnectionAccessService.assertReadable(connectionId)`。

### 11.5 对 SQL 执行服务的改造建议

#### 直接 SQL 接口

前端直接调用的 `/api/db/sql/execute` 属于工作台能力。

建议：

- `PERSONAL` 允许访问。
- `ORGANIZATION + COMMON` 直接禁止访问该接口。
- `ORGANIZATION + ADMIN` 才允许通过该接口进入 SQL 工作台执行。

原因：

- 普通用户只能通过 AI 对话触发受控查询。
- 不应直接暴露工作台 SQL 执行能力。

#### AI 内部 SQL 执行

AI Tool 可以继续调用服务层执行只读 SQL，但必须先走：

- 连接可访问校验
- 角色可写校验

### 11.6 对 AI 写权限规则的改造建议

当前 `ai_permission_rule` 是按：

- `user_id`
- `conversation_id`
- `connection_id`

做控制。

引入工作空间后，建议增加：

- `workspace_type`
- `org_id`

原因：

- PERSONAL 与 ORGANIZATION 的写入规则不应串用。
- 同一用户在不同组织下的写入权限不应串用。
- 同一连接在不同组织下的策略可能不同。

建议扩展后维度为：

- `user_id`
- `workspace_type`
- `org_id`
- `scope_type`
- `conversation_id`
- `connection_id`
- `catalog/schema`

### 11.7 普通用户写操作拦截点

必须在多个层级同时拦截：

1. Controller 层  
   `ORGANIZATION + COMMON` 禁止进入工作台写接口。

2. Service 层  
   所有写服务必须再校验一次，避免内部误调用。

3. AI Tool 层  
   `executeNonSelectSql` 在进入确认逻辑之前，先判断：
   - 如果 `workspaceType == PERSONAL`，允许继续
   - 如果 `workspaceType == ORGANIZATION && role == COMMON`，直接返回禁止信息，不给确认卡片

## 12. 前端设计

### 12.1 用户信息模型扩展

当前 `UserResponse` / 前端 `User` 仅有用户基础信息。

建议扩展返回：

- `orgs`: 当前用户所属组织列表
- `currentWorkspaceType`: 当前选中的工作空间类型
- `currentOrgId`: 当前选中的组织，仅组织版时有值
- `currentOrgRole`: 当前组织角色，仅组织版时有值

组织列表项建议字段：

- `orgId`
- `orgName`
- `roleCode`

### 12.2 工作空间切换

前端不应该只做“组织切换”，而应该做“工作空间切换”：

- 个人版
- 组织版 A
- 组织版 B

建议放在顶部 Header。

切换后的行为：

1. 更新本地 `currentWorkspaceType`
2. 如果是组织版，更新 `currentOrgId`
3. 后续请求自动携带 `X-Workspace-Type`
4. 组织版同时携带 `X-Org-Id`
5. 刷新可用数据源列表
6. 刷新 AI 会话上下文

### 12.3 页面形态

#### PERSONAL 页面

保留现有布局：

- 左 Explorer
- 中 SQL 工作台
- 右 AI Assistant
- 可管理个人连接

#### ORGANIZATION + ADMIN 页面

保留现有布局：

- 左 Explorer
- 中 SQL 工作台
- 右 AI Assistant

#### ORGANIZATION + COMMON 页面

建议改为：

- 顶部 Header
- 中间全宽 AI 对话区域

不渲染：

- Explorer 面板
- SQL Editor
- Results Panel
- 连接管理弹窗
- 数据对象弹窗
- 权限规则弹窗

### 12.4 COMMON 用户如何使用数据源

普通用户虽然不显示 Explorer，但仍然需要让 AI 知道“当前组织授权了哪些数据源”。

建议方式：

- AI 上下文中仅展示或注入当前组织授权的数据源列表
- 普通用户可在聊天时选择或 mention 这些数据源
- AI 内部再通过受控 Tool 做只读探索

### 12.5 前端接口拦截

建议在 HTTP 请求层统一加 header：

```text
X-Workspace-Type: PERSONAL | ORGANIZATION
X-Org-Id: 当前组织ID（仅组织版）
```

并根据 `currentWorkspaceType/currentOrgRole` 做页面路由和按钮裁剪。

## 13. 权限判定流程

### 13.1 连接读取判定流程

```text
开始
 -> 获取 loginUserId
 -> 获取 workspaceType
 -> 若 workspaceType == PERSONAL
      -> 查询 db_connections.connectionId
      -> 若 connection.user_id == loginUserId
           -> 允许
      -> 否则拒绝
 -> 若 workspaceType == ORGANIZATION
      -> 获取 currentOrgId / currentOrgRole
      -> 查询 db_connections.connectionId
      -> 若 connection.user_id == loginUserId
           -> 允许
      -> 否则
           -> 校验用户属于 currentOrgId
           -> 校验 sys_organization_connection_permissions 存在且 enabled = true
           -> 是则允许
      -> 否则拒绝
```

### 13.2 AI 读 Tool 判定流程

```text
开始
 -> 校验连接是否可访问
 -> 若不可访问，拒绝
 -> 若可访问，允许执行读类 Tool
```

### 13.3 AI 写 Tool 判定流程

```text
开始
 -> 校验连接是否可访问
 -> 若不可访问，拒绝
 -> 判断 workspaceType
 -> 若 PERSONAL
      -> 校验 ai_permission_rule 是否命中默认放行
      -> 若命中，执行
      -> 若未命中，返回确认卡片
 -> 若 ORGANIZATION
      -> 判断当前角色
      -> 若 role == COMMON，直接拒绝
      -> 若 role == ADMIN
           -> 校验 ai_permission_rule 是否命中默认放行
           -> 若命中，执行
           -> 若未命中，返回确认卡片
```

## 14. 数据迁移与兼容策略

### 14.1 兼容原则

- 不改动现有 `sys_users`
- 不改动现有 `db_connections.user_id`
- 组织能力作为增量功能引入
- 个人版作为默认工作空间保留现有操作习惯

### 14.2 初始化建议

初次上线时可采用以下策略：

1. 创建默认组织，例如“默认组织”。
2. 将现有全部用户挂到默认组织。
3. 将现有用户默认角色设为 `ADMIN`。
4. 所有用户默认都保留 `PERSONAL` 工作空间。
5. 后续再按业务逐步把部分组织内用户调整为 `COMMON`。

这样可以保证：

- 老用户不受影响
- 新能力逐步切换

### 14.3 `db_connections` 唯一性修正建议

当前 `db_connections.name` 在表层是全局唯一，但服务层逻辑是按用户判重。

建议后续修正为：

- 删除全局唯一
- 改为 `unique(user_id, name)`

这样才能和当前业务含义一致。

## 15. 开发实施顺序

### 第一阶段：数据结构与工作空间上下文

1. 新增 4 张表 Flyway 脚本
2. 新增 `OrganizationRoleEnum`
3. 新增 `WorkspaceTypeEnum`
4. 扩展 `RequestContextInfo`
5. 实现 `X-Workspace-Type` / `X-Org-Id` 解析

### 第二阶段：后端权限能力

1. 新增 `OrgAccessService`
2. 新增 `ConnectionAccessService`
3. 改造 `DbConnectionServiceImpl`
4. 改造 `ConnectionManager`
5. 改造 AI 写权限服务

### 第三阶段：前端组织化

1. 扩展 `/api/user/me`
2. 前端 `authStore` 增加当前工作空间信息
3. Header 增加“个人版 / 组织版”切换
4. PERSONAL / ORGANIZATION-ADMIN / ORGANIZATION-COMMON 三种页面布局

### 第四阶段：AI Tool 与页面收口

1. 读类 Tool 接入连接访问校验
2. 写类 Tool 接入角色限制
3. 普通用户页面彻底隐藏工作台能力
4. 工作台接口加后端角色限制

## 16. 当前阶段建议的明确业务结论

为避免后续实现时反复讨论，当前阶段建议直接明确以下规则：

1. 一个用户可以属于多个组织。
2. 用户在不同组织中的角色可以不同。
3. 用户始终保留个人版工作空间。
4. 个人版下可以使用所有功能，但仅能使用自己拥有的数据源。
5. 组织版下必须有“当前组织”概念。
6. 组织版下 `COMMON` 只能使用 AI 对话，不显示 Explorer 和 SQL 工作台。
7. 组织版下 `COMMON` 允许通过 AI 使用当前组织授权的数据源做只读分析。
8. 组织版下 `COMMON` 不允许任何数据库写操作。
9. 组织版下 `ADMIN` 可以使用当前组织授权的数据源，并保留现有 AI 写入确认机制。
10. 连接配置的创建、修改、删除默认仍只允许 owner 处理。

## 17. 后续可扩展方向

如果后续权限变复杂，可以在当前方案基础上逐步升级到标准 RBAC：

- 引入角色表
- 引入权限表
- 引入角色权限关系表
- 将“是否显示 Explorer”“是否允许写库”“是否允许管理连接”从代码写死改为权限配置

当前阶段不建议直接做满，先按本文档落地即可。

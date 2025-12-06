# 完整数据库重构方案

## 目标
按照 00001-00004 SQL 文件重新构建整个项目的数据库层和业务层

## 表结构总览

### 00001connection.sql - 数据库连接管理
- `db_connections` - 数据库连接信息表

### 00002auth.sql - 认证授权系统
- `sys_users` - 用户信息表
- `sys_sessions` - 用户会话表  
- `sys_refresh_tokens` - 刷新令牌表

### 00003agent.sql - AI 对话系统
- `ai_conversation` - AI 对话表
- `ai_message` - AI 消息表
- `ai_message_block` - AI 消息块表
- `ai_compression_record` - 对话压缩记录表
- `ai_todo_task` - 待办任务表

### 00004conversation.sql - 对话表修正
- 删除 `ai_conversation` 的 `delete_flag` 字段

## 重构步骤

### 阶段 1: 数据库迁移文件重构

#### 1.1 删除现有的 Flyway 迁移文件
需要删除或重命名以下文件：
- `V2__Create_Users_Table.sql` (已过时)
- `V3__Create_Sessions_Table.sql` (如果存在)
- `V4__Create_Refresh_Tokens_Table.sql` (如果存在)
- `00005Email_Verification_Codes.sql` (保留，但需要调整)

#### 1.2 创建新的 Flyway 迁移文件
基于 00001-00004 创建标准的 Flyway 迁移文件：

**V1__Create_DB_Connections_Table.sql**
```sql
-- 从 00001connection.sql 复制内容
```

**V2__Create_Auth_Tables.sql**
```sql
-- 从 00002auth.sql 复制内容
-- 包含: sys_users, sys_sessions, sys_refresh_tokens
```

**V3__Create_AI_Tables.sql**
```sql
-- 从 00003agent.sql 复制内容
-- 包含: ai_conversation, ai_message, ai_message_block, 
--       ai_compression_record, ai_todo_task
```

**V4__Alter_AI_Conversation.sql**
```sql
-- 从 00004conversation.sql 复制内容
ALTER TABLE ai_conversation DROP COLUMN IF EXISTS delete_flag;
```

**00005Email_Verification_Codes.sql**
```sql
-- 保留现有内容，但需要调整外键引用
-- user_id 应该引用 sys_users 而不是 users
```

### 阶段 2: 实体类重构

#### 2.1 User 实体类 (已完成)
```java
@TableName("sys_users")
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String phone;
    private String avatarUrl;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2.2 Session 实体类
```java
@TableName("sys_sessions")
public class Session {
    private Long id;
    private Long userId;
    private String accessTokenHash;
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;
    private Integer active;  // SMALLINT -> Integer
    private LocalDateTime lastRefreshAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2.3 RefreshToken 实体类
```java
@TableName("sys_refresh_tokens")
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private Long sessionId;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Integer revoked;  // SMALLINT -> Integer
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2.4 新增 AI 相关实体类
需要创建以下新实体类：
- `AiConversation`
- `AiMessage`
- `AiMessageBlock`
- `AiCompressionRecord`
- `AiTodoTask`
- `DbConnection`

### 阶段 3: DTO 重构

#### 3.1 UserInfoResponse
```java
public class UserInfoResponse {
    private Long id;
    private String email;
    private String phone;
    private String username;
    private String avatarUrl;  // 改名
    private Boolean verified;  // 合并 emailVerified
    // 删除: phoneVerified
}
```

### 阶段 4: Service 层重构

#### 4.1 需要大量修改的文件

**AuthServiceImpl.java** - 关键修改点：
```java
// 旧代码
user.getPassword()          → user.getPasswordHash()
user.getEmailVerified()     → user.getVerified()
user.setEmailVerified()     → user.setVerified()
user.getStatus()            → 删除（功能移除）
user.setStatus()            → 删除（功能移除）
user.getAvatar()            → user.getAvatarUrl()
user.setAvatar()            → user.setAvatarUrl()
user.setPassword()          → user.setPasswordHash()
user.getOauthProvider()     → 删除（功能移除）
user.setOauthProvider()     → 删除（功能移除）
user.getOauthProviderId()   → 删除（功能移除）
user.setOauthProviderId()   → 删除（功能移除）
```

**UserServiceImpl.java** - 关键修改点：
```java
// 查询字段调整
User::getPassword           → User::getPasswordHash
User::getEmailVerified      → User::getVerified
User::getPhoneVerified      → 删除
User::getStatus             → 删除
User::getAvatar             → User::getAvatarUrl
```

#### 4.2 功能移除清单

以下功能需要完全移除或重新设计：

1. **Google OAuth 登录** ❌
   - 删除 `GoogleOAuthService` 及其实现
   - 删除 `GoogleOAuthProperties`
   - 删除 `GoogleOAuthConfigValidator`
   - 删除 `AuthController` 中的 OAuth 相关端点
   - 删除 `AuthServiceImpl` 中的 `googleLogin()` 等方法

2. **账户状态管理** ❌
   - 删除所有 `status` 字段的检查逻辑
   - 删除账户禁用/启用功能

3. **手机号验证** ❌
   - 删除 `phoneVerified` 相关逻辑
   - 只保留邮箱验证

4. **软删除** ❌
   - 删除 `@TableLogic` 注解
   - 改为物理删除

### 阶段 5: Mapper 层调整

#### 5.1 需要调整的 Mapper
- `UserMapper` - 表名改为 `sys_users`
- `SessionMapper` - 表名改为 `sys_sessions`  
- `RefreshTokenMapper` - 表名改为 `sys_refresh_tokens`

#### 5.2 新增 Mapper
- `DbConnectionMapper`
- `AiConversationMapper`
- `AiMessageMapper`
- `AiMessageBlockMapper`
- `AiCompressionRecordMapper`
- `AiTodoTaskMapper`

### 阶段 6: Controller 层调整

#### 6.1 AuthController
- 删除 Google OAuth 相关端点
- 调整响应 DTO 字段名

#### 6.2 UserController  
- 调整响应 DTO 字段名
- 删除账户状态管理端点

### 阶段 7: 测试文件调整

需要修改所有测试文件中的：
- 字段名引用
- Mock 数据
- 断言逻辑

## 数据迁移策略

### 如果有生产数据

#### 方案 A: 数据迁移脚本
```sql
-- 1. 创建新表
-- 执行 V1-V4 迁移脚本

-- 2. 迁移用户数据
INSERT INTO sys_users (id, username, email, password_hash, phone, avatar_url, verified, created_at, updated_at)
SELECT id, username, email, password, phone, avatar, 
       COALESCE(email_verified, false), 
       create_time, update_time
FROM users;

-- 3. 迁移会话数据
-- ... 类似处理

-- 4. 删除旧表
DROP TABLE users CASCADE;
DROP TABLE sessions CASCADE;
DROP TABLE refresh_tokens CASCADE;
```

#### 方案 B: 全新开始
- 备份现有数据
- 清空数据库
- 执行新的迁移脚本
- 重新导入必要数据

### 如果是开发环境

**推荐方案：清空重建**
```sql
-- 1. 删除所有表
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- 2. 重新执行 Flyway 迁移
-- Flyway 会自动执行 V1-V5
```

## 执行顺序

### 第一步：准备工作
1. ✅ 创建 Git 分支
2. ✅ 备份数据库
3. ✅ 创建 REFACTOR_PLAN.md（本文件）

### 第二步：数据库层
1. [ ] 创建新的 Flyway 迁移文件
2. [ ] 清空数据库并重新迁移
3. [ ] 验证表结构

### 第三步：实体层
1. [x] 修改 User.java（已完成）
2. [ ] 修改 Session.java
3. [ ] 修改 RefreshToken.java
4. [ ] 创建 AI 相关实体类
5. [ ] 创建 DbConnection 实体类

### 第四步：DTO 层
1. [ ] 修改 UserInfoResponse.java
2. [ ] 修改其他相关 DTO

### 第五步：Service 层
1. [ ] 修改 AuthServiceImpl.java
2. [ ] 修改 UserServiceImpl.java
3. [ ] 删除 GoogleOAuthServiceImpl.java
4. [ ] 修改其他 Service

### 第六步：Controller 层
1. [ ] 修改 AuthController.java
2. [ ] 修改 UserController.java

### 第七步：测试
1. [ ] 修改单元测试
2. [ ] 修改集成测试
3. [ ] 手动测试所有功能

### 第八步：清理
1. [ ] 删除无用的类和文件
2. [ ] 删除 00001-00004.sql 文件（已转为 Flyway）
3. [ ] 更新文档

## 风险评估

### 高风险项
1. **数据丢失** - OAuth 用户无法登录
2. **功能降级** - 账户管理功能缺失
3. **兼容性** - 前端需要同步修改

### 中风险项
1. **测试覆盖** - 需要大量测试工作
2. **回滚困难** - 数据结构变化大

### 低风险项
1. **性能影响** - 表结构优化可能提升性能
2. **代码质量** - 重构后代码更清晰

## 建议

### 推荐做法
1. **分阶段执行** - 不要一次性修改所有代码
2. **保留功能** - 建议在 sys_users 表中添加缺失字段
3. **充分测试** - 每个阶段都要测试
4. **文档更新** - 及时更新 API 文档

### 不推荐做法
1. ❌ 直接在生产环境执行
2. ❌ 跳过测试阶段
3. ❌ 不备份数据

## 时间估算

- 数据库层重构：2-4 小时
- 实体层重构：2-3 小时
- Service 层重构：4-6 小时
- Controller 层重构：1-2 小时
- 测试和调试：4-8 小时

**总计：13-23 小时**

## 下一步行动

你想要：
1. **完整重构** - 我帮你按照这个计划一步步执行
2. **保留功能** - 我帮你在 sys_users 表中添加缺失字段，保留所有现有功能
3. **部分重构** - 只重构某些模块

请告诉我你的选择！

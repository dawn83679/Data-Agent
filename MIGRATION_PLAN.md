# 数据库表结构迁移计划

## 概述
将现有的 `users` 表结构迁移到 `sys_users` 表结构（基于 00002auth.sql）

## 字段映射关系

### User 实体类字段变更
| 旧字段名 | 新字段名 | 类型 | 说明 |
|---------|---------|------|------|
| password | passwordHash | String | 密码哈希值 |
| avatar | avatarUrl | String | 头像URL |
| emailVerified | verified | Boolean | 邮箱验证状态（合并） |
| phoneVerified | ❌ 删除 | - | 已合并到 verified |
| oauthProvider | ❌ 删除 | - | OAuth 功能移除 |
| oauthProviderId | ❌ 删除 | - | OAuth 功能移除 |
| status | ❌ 删除 | - | 账户状态功能移除 |
| deleteFlag | ❌ 删除 | - | 软删除功能移除 |
| createTime | createdAt | LocalDateTime | 创建时间 |
| updateTime | updatedAt | LocalDateTime | 更新时间 |

## 需要修改的文件清单

### 1. 实体类和 DTO
- [x] User.java - 已修改
- [x] V2__Create_Users_Table.sql - 已修改
- [ ] UserInfoResponse.java - 需要修改字段名

### 2. Service 层
- [ ] AuthServiceImpl.java - 大量使用旧字段
- [ ] UserServiceImpl.java - 使用旧字段
- [ ] GoogleOAuthServiceImpl.java - OAuth 功能需要重新设计

### 3. Controller 层
- [ ] AuthController.java - 可能需要调整
- [ ] UserController.java - 可能需要调整

### 4. 其他相关文件
- [ ] Session.java - 检查是否需要调整
- [ ] 所有测试文件

## 功能影响分析

### ⚠️ 重大功能变更

1. **OAuth 登录功能**
   - 现状：支持 Google OAuth 登录
   - 影响：`oauthProvider` 和 `oauthProviderId` 字段被删除
   - 建议：需要重新设计 OAuth 存储方案或移除 OAuth 功能

2. **账户状态管理**
   - 现状：使用 `status` 字段管理账户启用/禁用
   - 影响：`status` 字段被删除
   - 建议：需要重新设计账户状态管理或移除此功能

3. **软删除功能**
   - 现状：使用 `deleteFlag` 实现软删除
   - 影响：`deleteFlag` 字段被删除
   - 建议：改为物理删除或重新设计

4. **手机验证**
   - 现状：`phoneVerified` 独立字段
   - 影响：合并到 `verified` 字段
   - 建议：只保留邮箱验证，或重新设计验证逻辑

## 迁移步骤

### 步骤 1: 修改 DTO 和响应对象
- [ ] 修改 UserInfoResponse.java

### 步骤 2: 修改 Service 层
- [ ] 修改 AuthServiceImpl.java
- [ ] 修改 UserServiceImpl.java
- [ ] 处理 OAuth 相关代码

### 步骤 3: 修改 Controller 层
- [ ] 检查并修改 AuthController.java
- [ ] 检查并修改 UserController.java

### 步骤 4: 数据迁移
- [ ] 编写数据迁移脚本（如果需要）
- [ ] 测试迁移流程

### 步骤 5: 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 端到端测试

## 风险提示

1. **数据丢失风险**：OAuth 用户信息、账户状态、软删除标记将丢失
2. **功能降级**：OAuth 登录、账户禁用、软删除功能将不可用
3. **兼容性问题**：前端可能需要同步修改

## 建议

建议保留原有功能，在 `sys_users` 表中添加缺失的字段：
- oauth_provider VARCHAR(50)
- oauth_provider_id VARCHAR(255)
- status INTEGER DEFAULT 0
- delete_flag INTEGER DEFAULT 0
- phone_verified BOOLEAN DEFAULT false

这样可以避免功能降级和数据丢失。

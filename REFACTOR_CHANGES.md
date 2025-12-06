# 重构变更记录

## 已完成的变更

### ✅ 阶段 1: 数据库表结构调整
- [x] 00002auth.sql - 添加 OAuth 字段
- [x] V2__Create_Users_Table.sql - 添加 OAuth 字段
- [x] User.java - 添加 oauthProvider 和 oauthProviderId 字段

## 字段映射表

### User 实体类字段变更
| 旧字段名 | 新字段名 | 状态 | 说明 |
|---------|---------|------|------|
| password | passwordHash | ✅ 已改 | 密码哈希值 |
| avatar | avatarUrl | ✅ 已改 | 头像URL |
| emailVerified | verified | ✅ 已改 | 邮箱验证状态 |
| phoneVerified | ❌ 删除 | - | 手机验证功能移除 |
| oauthProvider | oauthProvider | ✅ 保留 | OAuth 提供商 |
| oauthProviderId | oauthProviderId | ✅ 保留 | OAuth 用户ID |
| status | ❌ 删除 | - | 账户状态功能移除 |
| deleteFlag | ❌ 删除 | - | 软删除功能移除 |
| createTime | createdAt | ✅ 已改 | 创建时间 |
| updateTime | updatedAt | ✅ 已改 | 更新时间 |

## 待修改文件清单

### 阶段 2: DTO 层
- [ ] UserInfoResponse.java - 修改字段名

### 阶段 3: Service 层  
- [ ] AuthServiceImpl.java - 大量字段名修改
- [ ] UserServiceImpl.java - 字段名修改
- [ ] GoogleOAuthServiceImpl.java - 保持不变（OAuth 保留）

### 阶段 4: Controller 层
- [ ] AuthController.java - 检查并调整
- [ ] UserController.java - 检查并调整

### 阶段 5: 其他
- [ ] Session.java - 检查表名
- [ ] RefreshToken.java - 检查表名
- [ ] Mapper 文件 - 检查表名

## 代码修改规则

### 必须修改的方法调用
```java
// 密码相关
user.getPassword()          → user.getPasswordHash()
user.setPassword()          → user.setPasswordHash()

// 头像相关
user.getAvatar()            → user.getAvatarUrl()
user.setAvatar()            → user.setAvatarUrl()

// 验证状态
user.getEmailVerified()     → user.getVerified()
user.setEmailVerified()     → user.setVerified()
user.getPhoneVerified()     → 删除此调用
user.setPhoneVerified()     → 删除此调用

// OAuth (保留不变)
user.getOauthProvider()     → 保持不变
user.setOauthProvider()     → 保持不变
user.getOauthProviderId()   → 保持不变
user.setOauthProviderId()   → 保持不变

// 账户状态 (删除)
user.getStatus()            → 删除此调用
user.setStatus()            → 删除此调用

// 时间字段
user.getCreateTime()        → user.getCreatedAt()
user.getUpdateTime()        → user.getUpdatedAt()
```

### 必须删除的代码块
1. 所有 `status` 字段的检查逻辑
2. 所有 `phoneVerified` 相关逻辑
3. 所有 `deleteFlag` 相关逻辑
4. MyBatis-Plus 的 `@TableLogic` 注解

## 下一步
继续修改 DTO 和 Service 层代码...

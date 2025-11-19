# 后端认证API接口说明

本文档说明了前端登录功能所需的后端API接口。所有接口的基础路径为 `/api/auth`。

## 通用响应格式

所有接口统一使用以下响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 具体数据
  }
}
```

- `code`: 状态码，200表示成功，其他值表示失败
- `message`: 响应消息
- `data`: 响应数据

## 1. 用户登录

**接口地址**: `POST /api/auth/login`

**请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user": {
      "id": 1,
      "username": "string",
      "email": "string",
      "avatar": "string",
      "roles": ["string"]
    },
    "tokens": {
      "accessToken": "string",
      "refreshToken": "string",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
  }
}
```

## 2. 用户注册

**接口地址**: `POST /api/auth/register`

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "confirmPassword": "string"
}
```

**响应数据**: 同登录接口

## 3. 刷新Token

**接口地址**: `POST /api/auth/refresh`

**请求体**:
```json
{
  "refreshToken": "string"
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "accessToken": "string",
    "refreshToken": "string",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

## 4. 获取当前用户信息

**接口地址**: `GET /api/auth/me`

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "string",
    "email": "string",
    "avatar": "string",
    "roles": ["string"]
  }
}
```

## 5. 用户登出

**接口地址**: `POST /api/auth/logout`

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

## 6. Google OAuth登录

### 6.1 获取授权URL

**接口地址**: `GET /api/auth/oauth/google/url`

**响应数据**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "url": "https://accounts.google.com/o/oauth2/v2/auth?..."
  }
}
```

### 6.2 OAuth回调处理

**接口地址**: `POST /api/auth/oauth/google`

**请求体**:
```json
{
  "code": "string",
  "provider": "google"
}
```

**响应数据**: 同登录接口

## 7. GitHub OAuth登录

### 7.1 获取授权URL

**接口地址**: `GET /api/auth/oauth/github/url`

**响应数据**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "url": "https://github.com/login/oauth/authorize?..."
  }
}
```

### 7.2 OAuth回调处理

**接口地址**: `POST /api/auth/oauth/github`

**请求体**:
```json
{
  "code": "string",
  "provider": "github"
}
```

**响应数据**: 同登录接口

## Token机制说明

1. **Access Token (短期Token)**
   - 用于API请求的身份验证
   - 有效期较短（建议15分钟-1小时）
   - 存储在 `localStorage` 的 `access_token` 键中
   - 每次请求自动添加到 `Authorization` 请求头

2. **Refresh Token (长期Token)**
   - 用于刷新Access Token
   - 有效期较长（建议7-30天）
   - 存储在 `localStorage` 的 `refresh_token` 键中
   - 当Access Token过期时，前端会自动使用Refresh Token刷新

3. **自动刷新机制**
   - 当API返回401状态码时，前端会自动调用刷新接口
   - 刷新成功后，自动重试原请求
   - 刷新失败时，清除Token并跳转到登录页

## 数据库设计建议

### 用户表 (users)
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,  -- 加密后的密码
  email VARCHAR(100),
  avatar VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Token表 (refresh_tokens)
```sql
CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(255) UNIQUE NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_token (token),
  INDEX idx_user_id (user_id)
);
```

### OAuth关联表 (oauth_providers)
```sql
CREATE TABLE oauth_providers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL,  -- 'google' or 'github'
  provider_user_id VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uk_provider_user (provider, provider_user_id),
  INDEX idx_user_id (user_id)
);
```

## 安全建议

1. **密码加密**: 使用BCrypt等安全算法加密存储密码
2. **Token安全**: 
   - 使用JWT签名确保Token完整性
   - 设置合理的过期时间
   - 支持Token黑名单机制（登出时）
3. **OAuth安全**:
   - 验证OAuth回调的state参数防止CSRF攻击
   - 安全存储OAuth Client ID和Secret
4. **HTTPS**: 生产环境必须使用HTTPS
5. **CORS**: 配置适当的CORS策略

## 注意事项

1. 所有需要认证的接口都需要在请求头中包含 `Authorization: Bearer {accessToken}`
2. 前端会自动处理Token刷新，后端只需在Token无效时返回401状态码
3. OAuth登录流程：
   - 前端调用获取授权URL接口
   - 用户跳转到OAuth提供商授权页面
   - 授权后回调到前端，前端获取code参数
   - 前端调用OAuth登录接口，传入code
   - 后端验证code并创建/更新用户，返回Token


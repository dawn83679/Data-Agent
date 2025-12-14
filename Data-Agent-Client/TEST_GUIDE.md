# API 接口测试指南

## 方式一：使用测试页面（推荐）

### 1. 启动开发服务器

```bash
cd Data-Agent-Client
npm run dev
```

### 2. 访问测试页面

在浏览器中打开：`http://localhost:5173/api-test`

### 3. 测试接口

测试页面提供了所有接口的可视化测试界面，包括：

- **数据库连接管理**
  - 获取所有连接
  - 测试连接
  - 创建连接配置

- **驱动管理**
  - 获取可用驱动列表
  - 获取已安装驱动列表

- **AI 对话管理**
  - 创建对话
  - 获取对话列表
  - 发送聊天消息（流式响应）

### 4. 查看结果

- 成功的结果会显示在对应的区域
- 错误信息会显示在底部的错误区域
- 所有请求和响应也会在浏览器控制台输出

---

## 方式二：在 Vue 组件中测试

### 1. 创建测试组件

在任意 Vue 组件中：

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { connectionApi, driverApi, chatApi } from '@/api'

onMounted(async () => {
  try {
    // 测试获取连接列表
    const result = await connectionApi.getConnections()
    console.log('连接列表:', result.data)
  } catch (error: any) {
    console.error('错误:', error.message)
  }
})
</script>
```

### 2. 使用示例代码

参考 `src/api/example.ts` 中的示例函数：

```typescript
import { exampleGetConnections, exampleTestConnection } from '@/api/example'

// 在组件中使用
exampleGetConnections()
exampleTestConnection()
```

---

## 方式三：浏览器控制台测试

### 1. 打开浏览器控制台

在浏览器中按 `F12` 打开开发者工具，切换到 Console 标签

### 2. 导入 API（需要先访问页面）

由于是模块化代码，需要在页面加载后通过全局对象访问，或者直接在组件中测试。

### 3. 使用示例

如果页面已经加载了 API，可以在控制台直接调用：

```javascript
// 注意：这需要在页面中已经导入了 API
// 实际使用建议在 Vue 组件中测试
```

---

## 方式四：使用 Postman/Thunder Client 测试

### 1. 配置基础 URL

```
http://localhost:8081
```

### 2. 测试接口示例

#### 获取连接列表
```
GET http://localhost:8081/api/connections
```

#### 测试连接
```
POST http://localhost:8081/api/connections/test
Content-Type: application/json

{
  "dbType": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "database": "test",
  "username": "root",
  "password": "password",
  "driverJarPath": "/path/to/driver.jar"
}
```

#### 获取可用驱动
```
GET http://localhost:8081/api/drivers/available?databaseType=MySQL
```

#### 创建对话（需要认证）
```
POST http://localhost:8081/api/ai/conversation/create
Content-Type: application/json
Authorization: Bearer {your-token}

{
  "title": "测试对话"
}
```

---

## 配置说明

### 开发环境代理配置

如果使用 Vite 代理，需要在 `vite.config.ts` 中添加：

```typescript
export default defineConfig({
  // ... 其他配置
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      }
    }
  }
})
```

### 环境变量配置

或者创建 `.env` 文件：

```env
VITE_API_BASE_URL=http://localhost:8081
```

---

## 常见问题

### 1. CORS 错误

**问题**：浏览器控制台显示 CORS 错误

**解决**：
- 使用 Vite 代理（推荐）
- 或者配置后端 CORS

### 2. 401 未授权错误

**问题**：AI 对话相关接口返回 401

**解决**：
- 确保已登录并获取 token
- Token 存储在 `localStorage` 的 `satoken` key 中
- 检查后端认证配置

### 3. 连接测试失败

**问题**：测试数据库连接失败

**解决**：
- 检查数据库服务是否运行
- 检查连接参数是否正确
- 检查驱动 JAR 路径是否存在

### 4. 流式响应不工作

**问题**：聊天消息无法接收

**解决**：
- 检查是否使用了 `createSSEConnection` 工具函数
- 检查网络连接
- 查看浏览器控制台的错误信息

---

## 测试检查清单

- [ ] 后端服务运行在 `http://localhost:8081`
- [ ] 前端开发服务器已启动
- [ ] 已配置代理或环境变量
- [ ] 浏览器控制台无 CORS 错误
- [ ] 可以访问测试页面 `/api-test`
- [ ] 可以成功调用不需要认证的接口
- [ ] 已登录并获取 token（测试需要认证的接口）

---

## 下一步

测试通过后，可以：

1. 在业务组件中使用这些 API
2. 根据实际需求调整错误处理
3. 添加加载状态和用户提示
4. 优化用户体验


# Data-Agent Client

Data-Agent 前端客户端，基于 Vue 3 + TypeScript + Vite 构建的现代化数据库连接与驱动管理平台。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **语言**: TypeScript
- **构建工具**: Vite
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **样式**: CSS Variables (设计系统)

## 项目结构

```
src/
├── api/              # API 接口封装
│   ├── driver.ts     # 驱动管理 API
│   ├── connection.ts # 连接管理 API
│   └── index.ts      # 统一导出
├── assets/           # 静态资源
│   ├── base.css      # 基础样式
│   ├── variables.css # CSS 变量定义
│   └── main.css      # 主样式文件
├── components/       # 组件
│   ├── driver/       # 驱动管理相关组件
│   │   ├── DriverList.vue
│   │   └── DriverDownload.vue
│   └── connection/   # 连接管理相关组件
│       ├── ConnectionList.vue
│       └── ConnectionForm.vue
├── router/           # 路由配置
│   └── index.ts
├── stores/           # Pinia 状态管理
│   ├── driver.ts     # 驱动管理 Store
│   └── connection.ts # 连接管理 Store
├── types/            # TypeScript 类型定义
│   ├── api.ts
│   ├── driver.ts
│   └── connection.ts
├── utils/            # 工具函数
│   └── request.ts    # HTTP 请求封装
└── views/            # 页面视图
    ├── HomeView.vue
    ├── DriverView.vue
    └── ConnectionView.vue
```

## 功能特性

### 驱动管理
- ✅ 查看已安装驱动列表
- ✅ 从 Maven Central 下载驱动
- ✅ 删除本地驱动
- ✅ 按数据库类型筛选

### 连接管理
- ✅ 创建数据库连接配置
- ✅ 编辑连接配置
- ✅ 删除连接配置
- ✅ 查看连接列表

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 `http://localhost:5173`

### 构建生产版本

```bash
npm run build
```

### 类型检查

```bash
npm run type-check
```

### 代码格式化

```bash
npm run format
```

## 环境变量

创建 `.env` 文件配置后端 API 地址：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 设计系统

项目使用 CSS Variables 定义统一的设计系统，包括：

- **颜色系统**: 主色、成功、危险、警告等语义化颜色
- **间距系统**: xs, sm, md, lg, xl 等统一间距
- **圆角系统**: sm, md, lg 等圆角规范
- **阴影系统**: sm, md, lg 等阴影效果

所有样式变量定义在 `src/assets/variables.css` 中。

## API 接口

### 驱动管理

- `GET /api/drivers/available` - 获取可用驱动列表
- `GET /api/drivers/installed` - 获取已安装驱动列表
- `POST /api/drivers/download` - 下载驱动
- `DELETE /api/drivers/{databaseType}/{version}` - 删除驱动

### 连接管理

- `GET /api/connections` - 获取所有连接
- `GET /api/connections/{id}` - 获取连接详情
- `POST /api/connections/create` - 创建连接
- `PUT /api/connections/{id}` - 更新连接
- `DELETE /api/connections/{id}` - 删除连接
- `POST /api/connections/test` - 测试连接
- `POST /api/connections/open` - 打开连接

## 浏览器支持

- Chrome (最新版)
- Firefox (最新版)
- Safari (最新版)
- Edge (最新版)

## 许可证

MIT

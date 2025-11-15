# 国际化使用指南

## 概述

项目已完成国际化（i18n）支持，支持中文（zh_CN）和英文（en_US）两种语言。

## 文件结构

```
resources/
└── i18n/
    ├── messages.properties          # 默认消息（英文）
    ├── messages_zh_CN.properties    # 中文消息
    └── messages_en_US.properties    # 英文消息

config/
└── I18nConfig.java                  # 国际化配置

utils/
└── I18nUtils.java                   # 国际化工具类
```

## 使用方式

### 1. 切换语言

通过 URL 参数切换语言：

```
# 切换到中文
http://localhost:8080/api/xxx?lang=zh_CN

# 切换到英文
http://localhost:8080/api/xxx?lang=en_US
```

### 2. 在代码中使用

#### 方式一：使用 I18nUtils 工具类

```java
@RestController
public class UserController {
    
    @Autowired
    private I18nUtils i18nUtils;
    
    @GetMapping("/test")
    public ApiResponse<String> test() {
        // 获取国际化消息
        String message = i18nUtils.getMessage("error.success");
        return ApiResponse.success(message);
    }
    
    @GetMapping("/test2")
    public ApiResponse<String> test2() {
        // 带参数的国际化消息
        String message = i18nUtils.getMessage("error.params", new Object[]{"username"});
        return ApiResponse.success(message);
    }
}
```

#### 方式二：直接使用 ErrorCode

```java
@RestController
public class UserController {
    
    @GetMapping("/test")
    public ApiResponse<String> test() {
        // ErrorCode 已经使用国际化 key
        throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
}
```

### 3. 添加新的国际化消息

#### 步骤 1：在配置文件中添加消息

```properties
# messages_zh_CN.properties
user.not.found=用户不存在
user.password.error=密码错误

# messages_en_US.properties
user.not.found=User not found
user.password.error=Password error
```

#### 步骤 2：在代码中使用

```java
String message = i18nUtils.getMessage("user.not.found");
```

### 4. 添加新的错误码

#### 步骤 1：在 ErrorCode 中添加

```java
public enum ErrorCode {
    // ...
    USER_NOT_FOUND(50700, "user.not.found"),
    USER_PASSWORD_ERROR(50701, "user.password.error");
}
```

#### 步骤 2：在配置文件中添加消息

```properties
# messages_zh_CN.properties
user.not.found=用户不存在
user.password.error=密码错误
```

#### 步骤 3：使用

```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
```

## 当前支持的错误码

| 错误码 | Key | 中文 | 英文 |
|--------|-----|------|------|
| 0 | error.success | 成功 | Success |
| 40000 | error.params | 请求参数错误 | Invalid parameters |
| 40100 | error.not.login | 未登录 | Not logged in |
| 40101 | error.no.auth | 无权限 | No permission |
| 40400 | error.not.found | 请求数据不存在 | Resource not found |
| 40300 | error.forbidden | 禁止访问 | Access forbidden |
| 50000 | error.system | 系统内部异常 | System internal error |
| 50001 | error.operation | 操作失败 | Operation failed |
| 50100 | error.db.connection | 数据库连接失败 | Database connection failed |
| ... | ... | ... | ... |

完整列表请查看 `ErrorCode.java` 和 `messages*.properties` 文件。

## 测试示例

### 测试 1：默认语言（中文）

```bash
curl http://localhost:8080/api/test
# 返回: {"code": 0, "data": "成功", "message": "ok"}
```

### 测试 2：切换到英文

```bash
curl http://localhost:8080/api/test?lang=en_US
# 返回: {"code": 0, "data": "Success", "message": "ok"}
```

### 测试 3：错误消息国际化

```bash
# 中文
curl http://localhost:8080/api/error?lang=zh_CN
# 返回: {"code": 40000, "data": null, "message": "请求参数错误"}

# 英文
curl http://localhost:8080/api/error?lang=en_US
# 返回: {"code": 40000, "data": null, "message": "Invalid parameters"}
```

## 注意事项

1. **消息 Key 命名规范**
   - 使用小写字母和点号分隔
   - 格式：`模块.子模块.具体消息`
   - 示例：`error.db.connection`

2. **默认语言**
   - 系统默认语言为中文（zh_CN）
   - 可在 `I18nConfig.java` 中修改

3. **缓存时间**
   - 消息缓存时间为 3600 秒（1 小时）
   - 开发环境可设置为 -1（不缓存）

4. **找不到消息时**
   - 如果找不到对应的 key，会返回 key 本身
   - 例如：`getMessage("not.exist")` 返回 `"not.exist"`

5. **兼容性**
   - ErrorCode 的 `getMessage()` 方法返回国际化 key
   - 在 GlobalExceptionHandler 中自动转换为国际化消息
   - 旧代码无需修改即可支持国际化

## 扩展其他语言

### 添加日语支持

1. 创建 `messages_ja_JP.properties`

```properties
error.success=成功
error.params=パラメータエラー
# ...
```

2. 使用

```bash
curl http://localhost:8080/api/test?lang=ja_JP
```

## 常见问题

### Q: 为什么切换语言后没有生效？

A: 检查以下几点：
1. URL 参数是否正确：`?lang=zh_CN` 或 `?lang=en_US`
2. 配置文件是否存在且编码为 UTF-8
3. 是否重启了应用

### Q: 如何在非 Controller 中使用国际化？

A: 注入 `I18nUtils` 即可：

```java
@Service
public class UserService {
    @Autowired
    private I18nUtils i18nUtils;
    
    public void doSomething() {
        String message = i18nUtils.getMessage("error.success");
    }
}
```

### Q: 如何获取当前语言？

A: 使用 `I18nUtils.getCurrentLocale()`：

```java
Locale locale = i18nUtils.getCurrentLocale();
System.out.println(locale); // zh_CN 或 en_US
```

## 总结

国际化系统已完全集成到项目中，所有错误消息都支持中英文切换。开发新功能时，只需：

1. 在 `messages*.properties` 中添加消息
2. 在代码中使用 `i18nUtils.getMessage(key)` 获取消息
3. 或者在 `ErrorCode` 中添加新的错误码

系统会自动根据用户的语言设置返回对应的消息。

# Java 代码设计规范

## 数据库实体层设计规范（五件套）

对于数据库相关的业务实体，必须包含以下五个组件：

1. **Entity** - 实体类（domain/entity 包）
2. **Mapper** - 数据访问接口（mapper 包）
3. **Mapper XML** - SQL 映射文件（resources/mapper 目录）
4. **Service** - 业务接口（service 包）
5. **ServiceImpl** - 业务实现类（service/impl 包）

---

### 1. Entity（实体类）设计规范

#### 1.1 基本要求

- **包路径**：`{项目包名}.entity`
- **类命名**：使用大驼峰命名，与数据库表对应（如 `User`、`Order`）
- **注解要求**：
  - `@Data`：Lombok 注解，自动生成 getter/setter
  - `@TableName("table_name")`：指定对应的数据库表名

#### 1.2 示例

```java
@Data
@TableName("sys_users")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableLogic
    @TableField("delete_flag")
    private Integer deleteFlag;
    
    @TableField(value = "created_at")
    private LocalDateTime createdAt;
}
```

#### 1.4 注意事项

1.  所有字段必须有详细的 JavaDoc 注释（英文）
2.  时间类型统一使用 `LocalDateTime`
3.  数据库字段名使用下划线命名，Java 字段使用驼峰命名
4.  逻辑删除字段必须添加 `@TableLogic` 注解
5.  类上必须标注 `@author` 和 `@since`
6.  禁止在实体类中添加业务逻辑方法

---

### 2. Mapper（数据访问接口）设计规范

- **包路径**：`{项目包名}.mapper`
- **接口命名**：`{Entity}Mapper`，继承 `BaseMapper<Entity>`，使用 `@Mapper` 注解
- **禁止使用** `@Select`、`@Insert`、`@Update`、`@Delete` 注解编写 SQL
- **简单查询**：通过 Service 层或直接使用 `BaseMapper` 方法
- **复杂查询**：在 Mapper XML 中定义
- **方法参数**：必须使用 `@Param` 注解

**示例**：
```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    int batchUpdateStatus(@Param("orderIds") List<Long> orderIds,
                          @Param("userId") Long userId,
                          @Param("status") Integer status);
}
```

### 3. Mapper XML（SQL 映射文件）设计规范

- **文件位置**：`src/main/resources/mapper/`
- **文件命名**：`{Entity}Mapper.xml`
- **`namespace`**：必须与 Mapper 接口的完整类名一致
- **`id`**：必须与 Mapper 接口中的方法名一致
- **`resultType`**：使用完整类名
- **注意事项**：如果 Mapper 接口中没有自定义方法，XML 文件可以为空（但必须存在）

---

### 4. Service（业务接口）设计规范

- **包路径**：`{项目包名}.service`
- **接口命名**：`{Entity}Service`，继承 `IService<Entity>`
- **方法参数规范**：
  - 单个参数：直接传递参数（如 `String username`、`Long userId`）
  - 两个或以上参数：必须封装为 `xxxRequest` 实体类
  - **禁止使用枚举类型作为参数或返回值**：使用字符串或基本类型
- **注意事项**：
  - 继承 `IService<Entity>` 后自动拥有基础 CRUD 方法
  - 每个方法必须有详细的 JavaDoc 注释（包括参数和返回值）
  - 只定义方法签名，不包含实现
  - 在 Service 实现层内部可以使用枚举进行类型安全的处理

---

### 5. ServiceImpl（业务实现类）设计规范

- **包路径**：`{项目包名}.service.impl`
- **类命名**：`{Entity}ServiceImpl`，继承 `ServiceImpl<Mapper, Entity>` 实现 `Service`，使用 `@Service` 注解
- **依赖注入**：使用 `@Autowired` 注入其他服务
- **访问数据库**：使用 `baseMapper` 调用自定义 Mapper 方法，使用 `this.getById()`、`this.list()` 等继承方法
- **查询构建**：使用 `LambdaQueryWrapper` 构建查询条件（类型安全）
- **默认值字段**：对于有默认值的字段（如 `created_at`、`updated_at`、`delete_flag`），不需要手动设置，数据库会自动填充
- **逻辑删除查询**：MyBatis-Plus 方法会自动过滤已删除数据；如需查询已删除数据，必须在 Mapper XML 中自定义 SQL

### 6. MyBatis-Plus 配置规范

#### 6.1 application.yml 配置

```yaml
mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath:/mapper/**/*.xml
  
  # 全局配置
  global-config:
    db-config:
      # 逻辑删除字段名
      logic-delete-field: delete_flag
      # 逻辑删除值（已删除）
      logic-delete-value: 1
      # 逻辑未删除值（未删除）
      logic-not-delete-value: 0
  
  # MyBatis 配置
  configuration:
    # 日志实现（开发环境使用，生产环境建议关闭）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 下划线转驼峰
    map-underscore-to-camel-case: true
```

## 禁止魔法值规范（必须定义枚举）

**重要原则**：
- **禁止使用魔法值**：代码中不允许直接使用数字（0、1、2、3）或固定字符串
- **必须定义枚举**：数据库中的状态值必须定义为枚举后才能使用

### 1. 枚举基本要求

- **包路径**：`{项目包名}.enums`
- **类命名**：大驼峰命名 + `Enum` 后缀，使用 `@Getter` 注解
- **必须包含 `value` 字段**（对应数据库存储的值）
- **建议包含 `description` 字段**（用于前端显示和日志记录）
- **枚举值命名**：全大写 + 下划线（如 `NOT_DELETED`、`PENDING`）

### 2. 枚举示例

```java
@Getter
public enum DeleteStatusEnum {
    NOT_DELETED(0, "未删除"),
    DELETED(1, "已删除");

    private final Integer value;
    private final String description;

    DeleteStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
```

**使用规范**：使用枚举的 `getValue()` 方法获取数据库存储值，禁止直接使用数字或字符串

### 3. 枚举使用限制

**重要原则**：
- **禁止在方法入参和出参中使用枚举类型**
- **方法参数**：使用字符串或基本类型（如 `String`、`Integer`），在方法内部转换为枚举
- **方法返回值**：返回字符串或基本类型，而不是枚举类型
- **允许在代码内部使用枚举**：用于类型安全的判断和转换

**原因**：
- 提高 API 的兼容性和可扩展性
- 避免枚举类型变化导致的接口变更
- 便于前端和其他系统调用（JSON 序列化更友好）

**示例**：
```java
// ❌ 错误：方法参数使用枚举
public void updateStatus(OrderStatusEnum status) {
    // ...
}

// ✅ 正确：方法参数使用字符串，内部转换为枚举
public void updateStatus(String status) {
    OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(status);
    // ...
}

// ❌ 错误：方法返回值使用枚举
public OrderStatusEnum getStatus(Long orderId) {
    // ...
}

// ✅ 正确：方法返回值使用字符串
public String getStatus(Long orderId) {
    OrderStatusEnum statusEnum = ...;
    return statusEnum.getValue();
}
```

**DTO 设计**：
```java
// ❌ 错误：DTO 字段使用枚举类型
@Data
public class OrderRequest {
    private OrderStatusEnum status;  // 禁止
}

// ✅ 正确：DTO 字段使用字符串
@Data
public class OrderRequest {
    private String status;  // 使用字符串
}
```

---

## 禁止字符串满天飞规范（必须定义常量）

**重要原则**：
- **禁止字符串满天飞**：错误信息、固定字符串、Redis Key 等必须定义为常量
- **统一管理**：相同类型的常量应该放在同一个常量类中

### 1. 常量基本要求

- **包路径**：`{项目包名}.constant`
- **类命名**：大驼峰命名 + `Constant` 后缀
- **命名规范**：全大写 + 下划线（如 `SUCCESS_MESSAGE`、`USER_INFO_PREFIX`）
- **修饰符**：`public static final`

### 2. 常量示例

```java
public class ResponseConstant {
    public static final int SUCCESS = 200;
    public static final String SUCCESS_MESSAGE = "success";
    public static final String USER_NOT_FOUND_MESSAGE = "user not found";
}

public class RedisKeyConstant {
    public static final String USER_INFO_PREFIX = "user:info:";
    public static final long USER_INFO_TTL = 3600;  // 1 hour
}
```

**使用规范**：错误信息、固定字符串、Redis Key 等必须使用常量，禁止在代码中直接写字符串

---

## DTO 设计规范

### 1. Request DTO

- **包路径**：`{项目包名}.model.dto.request`
- **类命名**：大驼峰命名 + `Request` 后缀，使用 `@Data` 注解
- **分页请求**：必须继承 `PageRequest`，使用 `@EqualsAndHashCode(callSuper = true)`
- **重要原则**：
  - **禁止使用枚举类型**：DTO 字段必须使用字符串或基本类型，不能使用枚举
  - 在 Service 层接收 Request 后，将字符串转换为枚举进行业务处理

**PageRequest 基类**：
```java
@Data
public class PageRequest {
    @Min(value = 1)
    private Integer page = 1;
    
    @Min(value = 1)
    @Max(value = 100)
    private Integer pageSize = 20;
    
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
```

**CacheableRequest 接口**（需要缓存的 Request 必须实现）：
```java
public interface CacheableRequest {
    // 标记接口，无方法定义
}

// 实现示例
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryUserListRequest extends PageRequest implements CacheableRequest {
    private Integer status;
    private String keyword;
}
```

### 2. Response DTO

- **包路径**：`{项目包名}.model.dto.response`
- **类命名**：大驼峰命名 + `Response` 后缀，使用 `@Data` 注解
- **重要原则**：
  - 只包含需要返回给前端的字段
  - 禁止包含敏感字段（如 `passwordHash`、`deleteFlag`）
  - 禁止直接返回 Entity
  - 使用 `BeanUtils.copyProperties()` 转换 Entity 到 Response
  - **禁止使用枚举类型**：DTO 字段必须使用字符串或基本类型，不能使用枚举

**PageResponse 封装**：
```java
@Data
public class PageResponse<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    
    public void calculateTotalPages() {
        if (total != null && pageSize != null && pageSize > 0) {
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }
    }
}
```

### 3. ApiResponse 统一返回封装

- **包路径**：`{项目包名}.model.dto.response`
- **重要原则**：所有 Controller 接口返回必须封装为 `ApiResponse<T>`
- **返回类型**：
  - 单个对象：`ApiResponse<XXXResponse>`
  - 分页查询：`ApiResponse<PageResponse<XXXResponse>>`
  - 列表查询：`ApiResponse<List<XXXResponse>>`
  - 无返回数据：`ApiResponse<Void>`

**ApiResponse 设计**：
```java
@Data
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseConstant.SUCCESS, 
            ResponseConstant.SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseConstant.SUCCESS, 
            ResponseConstant.SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(ResponseConstant.PARAM_ERROR, message, null);
    }
}
```

---

## Controller 设计规范

### 1. 参数设计规范

- **GET 请求**：
  - 简单查询参数（如单个 ID）：使用 `@PathVariable` 或 `@RequestParam`
  - 多个查询参数：使用 `@ModelAttribute` + `Request` DTO
- **POST/PUT 请求**：使用 `@RequestBody` + `Request` DTO
- **路径参数**：使用 `@PathVariable`（如 `/api/users/{id}`）
- **重要原则**：
  - **禁止使用枚举类型作为参数或返回值**：Controller 方法的参数和返回值必须使用字符串或基本类型，不能使用枚举
  - DTO 中的枚举字段必须使用字符串类型

**示例**：
```java
// GET 单个参数
@GetMapping("/{id}")
public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
    return ApiResponse.success(userService.getUserById(id));
}

// GET 多个参数（分页查询）
@GetMapping
public ApiResponse<PageResponse<UserResponse>> queryUserList(
        @ModelAttribute QueryUserListRequest request) {
    return ApiResponse.success(userService.queryUserList(request));
}

// POST 创建
@PostMapping
public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
    return ApiResponse.success(userService.createUser(request));
}
```

### 2. 返回值设计规范

- **返回类型**：必须是 `ApiResponse<T>`
- **泛型 `T` 类型**：
  - 单个对象：`ApiResponse<XXXResponse>`
  - 分页查询：`ApiResponse<PageResponse<XXXResponse>>`
  - 列表查询：`ApiResponse<List<XXXResponse>>`
  - 无返回数据：`ApiResponse<Void>`
- **禁止**：
  - 直接返回 Entity（如 `User`、`Order`）
  - 返回类型中包含枚举（Response DTO 中的字段必须使用字符串，不能使用枚举）

**示例**：
```java
@DeleteMapping("/{id}")
public ApiResponse<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ApiResponse.success();
}
```

### 3. 业务逻辑规范

**重要原则**：
- **禁止在 Controller 中编写业务逻辑**
- Controller 只负责接收请求参数、调用 Service 方法、返回响应结果
- 所有业务逻辑必须在 Service 层实现

---

## 工具类设计规范

### 1. Java 常见常用工具类

#### 1.1 Java 标准库

- **`java.util.Collections`**：集合操作工具类（排序、查找、同步等）
- **`java.util.Arrays`**：数组操作工具类（排序、查找、填充等）
- **`java.util.Objects`**：对象操作工具类（判空、比较、哈希等）
- **`java.time`**：时间日期工具类（`LocalDateTime`、`DateTimeFormatter` 等）
- **`java.nio.file.Files`**：文件操作工具类（读写、复制、删除等）

#### 1.2 Apache Commons

- **`org.apache.commons.lang3.StringUtils`**：字符串操作工具类
- **`org.apache.commons.lang3.CollectionUtils`**：集合操作工具类
- **`org.apache.commons.lang3.ArrayUtils`**：数组操作工具类
- **`org.apache.commons.io.FileUtils`**：文件操作工具类
- **`org.apache.commons.codec.digest.DigestUtils`**：加密摘要工具类（MD5、SHA等）
- **`org.apache.commons.lang3.DateUtils`**：日期操作工具类

#### 1.3 Google Guava

- **`com.google.common.base.Strings`**：字符串工具类
- **`com.google.common.collect.Lists/Sets/Maps`**：集合工具类
- **`com.google.common.base.Preconditions`**：参数校验工具类
- **`com.google.common.cache.CacheBuilder`**：缓存构建工具

#### 1.4 Hutool

- **`cn.hutool.core.util.StrUtil`**：字符串工具类
- **`cn.hutool.core.collection.CollUtil`**：集合工具类
- **`cn.hutool.core.util.DateUtil`**：日期工具类
- **`cn.hutool.core.io.FileUtil`**：文件工具类
- **`cn.hutool.core.util.IdUtil`**：ID生成工具类（UUID、雪花ID等）

#### 1.5 Jackson

- **`com.fasterxml.jackson.databind.ObjectMapper`**：JSON序列化/反序列化工具类
- **`com.fasterxml.jackson.databind.JsonNode`**：JSON节点操作工具
- **`com.fasterxml.jackson.annotation`**：JSON注解（`@JsonIgnore`、`@JsonProperty` 等）

### 2. 自定义工具类设计规范

- **包路径**：`{项目包名}.util`
- **类命名**：大驼峰命名 + `Util` 后缀（如 `FileUtil`、`RedisUtil`）
- **设计原则**：
  - 优先使用上述常用工具类，不要重复造轮子
  - 只在第三方库无法满足业务特定需求时创建自定义工具类
  - 单一职责，每个工具类只负责一类功能
  - 参数使用接口或抽象类，而非具体实现类（便于扩展）
- **禁止**：将业务逻辑放在工具类中

---

## 策略模式 + 注册表模式设计规范

### 1. 适用场景

当需要根据不同的类型执行不同的处理逻辑时，使用策略模式 + 注册表模式的组合设计。

### 2. 设计结构

#### 2.1 策略接口定义

- **接口命名**：`{功能}Handler`
- **方法定义**：统一的处理方法，参数类型使用接口或抽象类，便于扩展
- **包路径**：`{项目包名}.handler` 或 `{项目包名}.{模块}.handler`

**接口示例**：
```java
public interface XxxHandler {
    ResultType handle(ParamType param);
}
```

#### 2.2 具体策略实现

- **类命名**：`{类型}{功能}Handler`
- **使用 `@Component` 注解**，由 Spring 自动管理
- **实现策略接口**，编写具体的处理逻辑

**实现示例**：
```java
@Component
public class TypeXxxHandler implements XxxHandler {
    @Override
    public ResultType handle(ParamType param) {
        // 具体处理逻辑
    }
}
```

#### 2.3 注册表设计

- **类命名**：`{功能}HandlerRegistry`
- **使用 `@Component` 注解**
- **依赖注入**：通过构造函数注入所有策略实现（`List<XxxHandler>`）
- **注册时机**：在 `@PostConstruct` 方法中注册，建立类型到处理器的映射
- **存储结构**：使用 `ConcurrentHashMap<String, XxxHandler>` 存储映射关系
- **查找方法**：提供 `getHandler(String type)` 方法按类型查找处理器
- **统一调用**：提供统一的方法调用对应的处理器

**注册表示例**：
```java
@Component
@RequiredArgsConstructor
public class XxxHandlerRegistry {
    private final List<XxxHandler> handlers;
    private final Map<String, XxxHandler> handlerMap = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        for (XxxHandler handler : handlers) {
            if (handler instanceof TypeXxxHandler) {
                handlerMap.put(TypeEnum.TYPE.name(), handler);
            }
            // 其他类型的注册...
        }
    }
    
    public XxxHandler getHandler(String type) {
        return handlerMap.get(type);
    }
    
    public ResultType process(String type, ParamType param) {
        XxxHandler handler = getHandler(type);
        return handler != null ? handler.handle(param) : null;
    }
}
```

### 3. 设计原则

- **统一接口**：所有策略实现统一的接口，便于管理和替换
- **策略模式**：将不同的处理逻辑封装为独立的策略类
- **注册表模式**：统一管理策略，通过类型快速查找对应的处理器
- **依赖注入**：利用 Spring 的依赖注入自动发现和注册所有策略实现
- **扩展性**：新增类型只需实现对应的策略接口和注册逻辑，无需修改现有代码
- **类型安全**：使用枚举或常量定义类型，避免硬编码字符串

### 4. 注意事项

- 注册逻辑要明确，避免类型冲突
- 使用 `ConcurrentHashMap` 保证线程安全
- 处理器不存在时应返回 `null` 或抛出明确的异常
- 注册表的初始化逻辑应在 `@PostConstruct` 中完成

---

## 装饰器模式设计规范

### 1. 适用场景

当需要在不修改原有类的情况下，动态地为对象添加额外的功能（如缓存、日志、权限校验、性能监控等）时，使用装饰器模式。

### 2. 设计结构

#### 2.1 装饰器类设计

- **类命名**：`{功能}{原始类名}`（如 `CacheXxxService`、`LogXxxService`）
- **实现相同的接口**：装饰器必须实现与被装饰对象相同的接口
- **依赖注入**：通过 `@Qualifier` 注入被装饰的原始实现
- **使用 `@Primary` 注解**：确保装饰器优先被注入
- **包路径**：与被装饰类在同一个包下，或专门的装饰器包

**装饰器示例**：
```java
@Service
@Primary
public class CacheXxxService implements XxxService {
    
    @Qualifier("xxxServiceImpl")
    @Autowired
    private XxxService xxxService;
    
    @Autowired
    private CacheUtil cacheUtil;
    
    @Override
    public ResultType queryMethod(RequestType request) {
        // 查询缓存
        ResultType cached = cacheUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 调用原始服务
        ResultType result = xxxService.queryMethod(request);
        
        // 写入缓存
        cacheUtil.set(cacheKey, result);
        return result;
    }
    
    @Override
    public ResultType updateMethod(RequestType request) {
        // 调用原始服务
        ResultType result = xxxService.updateMethod(request);
        
        // 清除相关缓存
        cacheUtil.invalidate(pattern);
        return result;
    }
}
```

### 3. 设计原则

- **不修改原有类**：装饰器模式通过包装的方式添加功能，不修改原始类
- **透明性**：装饰器实现相同的接口，对外部调用者透明
- **单一职责**：每个装饰器只负责一种功能的增强（缓存、日志、权限等）
- **可组合**：可以嵌套多个装饰器（如缓存装饰器 + 日志装饰器）
- **依赖注入优先级**：使用 `@Primary` 确保装饰器优先被注入

### 4. 注意事项

- 装饰器应该委托给原始对象，而不是直接实现业务逻辑
- 对于读操作，先查缓存，缓存未命中再调用原始服务并写入缓存
- 对于写操作，执行后清除相关缓存，保证数据一致性
- 使用 `@Qualifier` 明确指定被装饰的原始实现，避免循环依赖
- 装饰器可以添加额外的工具类依赖（如 `CacheUtil`、`LogUtil`）

---

## CORS 跨域配置规范

**配置方式**：实现 `WebMvcConfigurer` 接口，重写 `addCorsMappings` 方法

```java
@Component
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 配置属性类设计规范

- **类命名**：`{功能}Properties`，使用 `@Data`、`@Component`、`@ConfigurationProperties(prefix = "xxx")` 注解
- **包路径**：`{项目包名}.config`
- **配置绑定**：通过 `prefix` 指定 yml 配置前缀，字段使用驼峰命名（支持 kebab-case 自动映射）
- **设计原则**：大量配置值应使用 `@ConfigurationProperties` 批量绑定，而非使用 `@Value` 逐个注入

**示例**：
```java
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
}
```

---

# 安全设计

## 认证与授权

### 认证

两种互斥模式，由 `elibrary.security.mode` 选择：

| 模式 | 使用场景 | 校验 |
| --- | --- | --- |
| `hmac` | 本地、自动化测试 | HS256 共享密钥（至少 32 字节）、issuer、exp、sub |
| `issuer` | 生产 | OIDC issuer 发现 + JWKS、issuer、exp、sub；服务不签发令牌 |

- 开发令牌端点只在 `dev` profile 且 HMAC 模式启用，请求体 `{userId, role}`；它绝不进入生产镜像的运行配置。
- 生产配置必须提供 `ELIBRARY_JWT_ISSUER_URI`；启动时校验配置存在。
- 建议在生产 IdP 中配置短 TTL，并通过 WAF/负载均衡增加限流。

### 身份与授权

- `Actor` 只由 `JwtAuthenticationToken` 解析得到：`sub` → `userId`，`ROLE_ADMIN` → `admin=true`。
- 任何请求参数/请求体中的 `userId` 都不参与身份判定；`BorrowBookRequest` 甚至没有该字段。
- 角色映射：`roles` claim → `ROLE_` 前缀权限；仅 `ROLE_USER`/`ROLE_ADMIN` 可通过业务端点的方法级授权。
- 所有权：查看/归还借阅时比较 `loan.userId` 与 `actor.userId`；ADMIN 绕过。403 不会泄露“该借阅属于谁”。

| 资源/操作 | 匿名 | USER | ADMIN |
| --- | --- | --- | --- |
| 健康检查、OpenAPI | 允许 | 允许 | 允许 |
| 开发令牌（仅 dev） | 允许 | 允许 | 允许 |
| 书目搜索/详情 | 401 | 允许 | 允许 |
| 借书 | 401 | 允许 | 允许 |
| 查看/归还自己的借阅 | 401 | 允许 | 允许 |
| 查看/归还他人借阅 | 401 | 403 | 允许 |
| `/me/loans` | 401 | 自己的记录 | 自己的记录 |

### 传输与浏览器安全

- 生产必须在负载均衡/WAF 终止 TLS；应用信任转发头以生成正确的外部 URL。
- CORS 由 `library-common` 的 `SecurityConfig` 统一配置：只允许配置中的白名单 origin，方法限定 GET/POST/OPTIONS，头限定 Authorization/Content-Type/Accept/X-Trace-Id；暴露 `Location`/`X-Trace-Id`。
- API 同时回答 OPTIONS 预检和实际响应，不依赖额外组件；预检与认证错误都返回 RFC 7807 ProblemDetail。
- 本地 H5 通过 nginx 同源代理调用 API，浏览器侧没有跨域预检；但 API 仍按 `Origin` 白名单校验 nginx 转发流量，因此 8088 的实际访问地址也必须列入白名单。跨域浏览器客户端直接访问 API 8080。
- nginx 返回 CSP、`X-Content-Type-Options`、`X-Frame-Options: DENY`、`Referrer-Policy`、`Permissions-Policy`；页面不加载第三方脚本，不使用内联脚本。
- 无 Cookie 会话，CSRF 关闭是安全的；Bearer token 由前端保存在 sessionStorage（仅本地演示）。
- 前端所有动态文本使用 `textContent`，不拼接 HTML，降低 XSS 风险。

### 输入与错误

- Bean Validation：`bookId` 非空、分页范围、搜索长度、用户 ID 格式、角色枚举。
- Jackson 拒绝未知/畸形 JSON；UUID 解析失败返回 400 `MALFORMED_REQUEST`。
- JPA 参数绑定避免 SQL 拼接；标题/作者搜索使用 `locate` 而不是拼接 `LIKE` 通配符，用户输入 `%`/`_` 不具特殊含义。
- 错误响应不包含异常类型、SQL、堆栈或类名；内部异常只记录到服务端日志并关联 traceId。
- 日志不记录令牌、Authorization 头或请求体敏感信息。

### 密钥与配置

- JWT 密钥、数据库密码、issuer URI 全部来自环境变量/密钥管理；仓库只有本地 dev 默认值。
- `.env` 被 `.gitignore` 排除；`.env.example` 只含占位符。
- Docker 镜像以非 root 用户运行；基础镜像只包含 JRE。
- 已知生产加固项：当前未强制校验 `aud`（audience），接入真实 IdP 时应增加 audience 校验并限定受信算法；当前也未内置限流，建议在 WAF/负载均衡层完成。


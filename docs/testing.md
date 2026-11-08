# 测试策略

## 测试分层与运行

### 分层

| 层次 | 文件 | 数量 | 验证内容 |
| --- | --- | --- | --- |
| 领域单测 | `LoanTest` | 5 | 14 天借期、归还状态转换、幂等且保留 returnedAt、参数校验、毫秒截断 |
| 领域单测 | `BookTest` | 4 | ISBN 规范化、非法输入、ISBN-10 X、UUID 生成 |
| 共享单测 | `PageQueryTest` | 2 | 分页上限、总页数计算 |
| 集成测试 | `LibraryApiIT` | 12 | 完整 HTTP + 真实 PostgreSQL + Flyway，含缓存失效 |

`LibraryApiIT` 覆盖：

1. 借阅/归还生命周期，`dueAt` 精确等于 14 天，可借数量变化。
2. 重复归还幂等，`returnedAt` 不变，活跃列表为空。
3. 达到总副本数后返回 409 `BOOK_NOT_AVAILABLE`。
4. 同一用户重复借同一书返回 409 `LOAN_ALREADY_ACTIVE`。
5. 非本人 403、ADMIN 可查看/归还。
6. 身份来自 token，请求体伪造 `userId` 无效。
7. 缺 token/坏 token 401，错误角色 403，`WWW-Authenticate` 存在。
8. 参数校验、非法状态、空 body、坏 JSON、404、trace 头与错误码契约。
9. 搜索不区分大小写、分页元数据稳定。
10. 20 个并发用户抢 3 本副本：恰好 3 个 201、17 个 409 `BOOK_NOT_AVAILABLE`，数据库活跃数为 3。
11. CORS 白名单覆盖 localhost、127.0.0.1 与局域网 IP，预检返回 200 和对应 `Access-Control-Allow-Origin`。
12. 书目详情和搜索结果先入缓存；借书/归还提交后缓存被失效，`availableCopies` 立即反映最新状态。

### 为什么用 Testcontainers PostgreSQL

- 本地开发、测试和生产都使用 PostgreSQL，避免本地与生产数据库行为不一致。
- 集成测试启动真实 `postgres:16-alpine`，执行与开发/生产相同的 Flyway common+postgresql 迁移（含触发器），确保约束、锁和类型映射真实有效。
- 容器在整个测试 JVM 内复用；每个测试前 `TRUNCATE TABLE loan, book CASCADE` 并清空 Caffeine 缓存，保证可重复、无顺序依赖。
- 不 mock Repository、事务、安全过滤器或数据库；只有时间由真实系统 UTC Clock 驱动（领域单测可传固定 Instant）。

### 运行方式

```bash
./mvnw test       # 11 个单元测试，无需 Docker
./mvnw verify     # 追加 12 个集成测试，共 23 个，需要 Docker
```

Surefire 运行 `*Test`，Failsafe 只在 `verify` 阶段运行 `*IT`。Testcontainers 需要本机 Docker daemon 可访问。

### 未覆盖/后续

- 浏览器端 E2E（可用 Playwright 补充登录、借还、分页流程）。
- API 超时/重试与故障注入测试。
- OIDC issuer 模式的集成测试（需要模拟 JWKS 服务器）。
- 负载/容量测试、数据库故障切换演练。

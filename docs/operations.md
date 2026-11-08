# 高可用与运维

## 运行与可观测性

### 无状态与扩容

- API 不保存 HTTP Session、不写本地磁盘、不使用内存锁。
- 多实例部署时，任何实例都可处理任何已认证请求；数据库是唯一协调点。
- 扩容指标建议：CPU、请求延迟、数据库连接池饱和度；连接池上限 10（本地）/20（测试），生产按 PG 连接上限与实例数规划。

### 健康探针

| 端点 | 用途 | 包含 |
| --- | --- | --- |
| `/actuator/health/liveness` | 进程是否存活，失败应重启 | livenessState |
| `/actuator/health/readiness` | 是否可接流量，失败应从 LB 摘除 | readinessState + db |
| `/actuator/health` | 聚合健康 | 全部 |
| `/actuator/info` | 版本/构建信息 | — |

健康端点匿名开放，且 `show-details: never`，不泄露数据库地址等细节。API 的 readiness 包含数据库连接。

### 优雅停机

- `server.shutdown: graceful`，`spring.lifecycle.timeout-per-shutdown-phase: 20s`。
- 收到 SIGTERM 后停止接收新请求，等待进行中的请求完成；容器编排应先把实例从 LB/readiness 摘除，再发送终止信号。
- 事务边界短，20 秒足够覆盖正常请求。

### 配置外部化

- 所有环境差异通过环境变量/profile 注入：数据库 URL、用户名、密码、JWT 模式、issuer、CORS、端口。
- `application-dev.yml` 使用 Compose/本机 PostgreSQL 并加载种子数据；`application-test.yml` 使用 Testcontainers PG；`application-prod.yml` 使用托管 PostgreSQL 和 OIDC issuer。
- 不引入配置中心：单体服务的配置项较少，环境变量 + 密钥管理足够；未来多环境复杂化再评估。

### 本地开发（不用 Compose）

需要 JDK 21、Maven（或使用仓库自带的 `./mvnw`），以及运行中的 PostgreSQL。可以先只启动数据库容器：

```bash
docker compose up -d postgres

# dev profile 会启用开发令牌端点、加载种子数据，并连接 localhost:5432
./mvnw -pl library-api -am spring-boot:run -Dspring-boot.run.profiles=dev
```

API 在 <http://localhost:8080>，Swagger UI 在 <http://localhost:8080/swagger-ui.html>。H5 页面仍建议用 Compose 启动，因为 nginx 需要把 `/api` 代理到 API。

### 数据库运维

- 生产使用托管 PostgreSQL，多可用区、自动备份、时间点恢复。
- Flyway 在启动时执行迁移；多实例并发启动由 Flyway 的 schema history 锁串行化。
- 迁移变更应向后兼容（先加列/索引，再切代码），避免滚动发布期间新旧实例不兼容。
- 监控慢查询、锁等待、连接数、活跃借阅增长；必要时为 `loan(book_id, status='ACTIVE')` 建部分索引。

### 交付物

- API 使用多阶段 Dockerfile，运行阶段仅含 JRE 且以非 root 运行；web 使用 nginx 静态镜像；Compose 提供 PostgreSQL 16、健康检查、依赖顺序和数据库持久卷。
- 生产拓扑为 LB + 多实例 API + 托管 PG，不要求完整 Kubernetes 清单。

## 可观测性

### 关联 ID

- API 的 `TraceIdFilter` 优先复用合法的 `X-Trace-Id`，否则生成 UUID；写入 MDC，并在响应头回写。日志 pattern：`%5P [traceId:%X{traceId:-}]`。
- ProblemDetail 的 `traceId` 与响应头一致，用户报错时可直接定位日志。

### 日志与指标

- 借阅成功记录 `Borrowed loanId=… bookId=… userId=… dueAt=…`；重复归还为调试日志；未映射冲突记录 WARN；未预期异常记录 ERROR + 堆栈 + traceId。
- 不记录 Authorization、token 或密码。
- 当前暴露 Actuator `health`/`info`；Micrometer 指标可按需追加暴露，生产应在管理网络或认证后开放。
- 未内置 Prometheus/OpenTelemetry 后端；traceId 可直接接入标准追踪。建议监控请求量/延迟/错误率、借阅成功/冲突、连接池、锁等待与 readiness。

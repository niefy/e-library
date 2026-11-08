# E-Library Service

一个迷你数字图书馆服务：读者可以浏览书目、借书、还书并查看自己的借阅记录。

项目故意做成**模块化单体**，而不是微服务：业务规模只需要一个后端服务，但代码仍按限界上下文和端口/适配器分层，保持可替换、可测试、可演进。完整设计见 [docs/architecture.md](docs/architecture.md)。

- Java 21 · Spring Boot 3.3 · Spring Security OAuth2 Resource Server
- SQLite（本地演示）· PostgreSQL（集成测试/生产）· Flyway
- Spring Cloud Gateway（统一入口）· nginx 托管的 H5 客户端
- JUnit 5 · AssertJ · Testcontainers · Maven Failsafe

## 目录

```
library-api/      后端 API（e-library-service）
library-gateway/  Spring Cloud Gateway，边缘路由与 traceId
library-web/      静态 H5 页面 + nginx 配置
docs/             架构文档与 ADR
docker-compose.yml
```

## 快速开始

前置条件：Docker 及 Docker Compose。首次运行需要下载镜像和 Maven 依赖，视网络通常 5–10 分钟；之后启动约 10 秒。

```bash
docker compose up --build -d
docker compose ps          # 等待三个服务都变为 healthy
```

地址入口：

| 地址 | 说明 |
| --- | --- |
| <http://localhost:8088> | H5 客户端（nginx 同源代理 `/api`） |
| <http://localhost:8081> | 网关（API 统一入口） |
| <http://localhost:8080> | API 直连；Swagger UI：`/swagger-ui.html` |
| <http://localhost:8080/actuator/health/readiness> | 就绪探针 |

停止并清空 SQLite 数据：

```bash
docker compose down -v
```

可选：复制 `.env.example` 为 `.env` 覆盖本地开发密钥。仓库不包含任何真实密钥。

## API 示例

以下命令通过网关（8081）调用。示例使用 `jq` 提取 token；没有 `jq` 可手动复制响应中的 `token`。

### 1. 获取开发令牌

```bash
BASE=http://localhost:8081

TOKEN=$(curl -s -X POST "$BASE/api/v1/dev/tokens" \
  -H 'Content-Type: application/json' \
  -d '{"userId":"alice","role":"USER"}' | jq -r .token)

curl -s "$BASE/api/v1/books?q=clean&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 2. 借书

种子数据包含 8 本书。下面的 ID 是本地初始化脚本中的《Clean Code》。

```bash
curl -i -X POST "$BASE/api/v1/loans" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"bookId":"11111111-1111-1111-1111-111111111111"}'
```

成功返回 `201 Created`、`Location: /api/v1/loans/{id}`，以及：

```json
{
  "id": "…",
  "bookId": "11111111-1111-1111-1111-111111111111",
  "bookTitle": "Clean Code",
  "status": "ACTIVE",
  "borrowedAt": "2026-11-08T06:45:00Z",
  "dueAt": "2026-11-22T06:45:00Z"
}
```

请求体里没有 `userId`：用户身份只来自 Bearer token。即使额外传入 `userId`，也会被忽略。

### 3. 查看与归还

```bash
curl -s "$BASE/api/v1/me/loans?status=ACTIVE&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq

LOAN_ID=<上一步返回的 id>
curl -X POST "$BASE/api/v1/loans/$LOAN_ID/return" \
  -H "Authorization: Bearer $TOKEN" | jq
```

重复归还同一借阅返回 `200`，`returnedAt` 保持不变（幂等）。

### 4. 错误契约

```bash
curl -i "$BASE/api/v1/books?size=51" -H "Authorization: Bearer $TOKEN"
```

返回 `400`、`Content-Type: application/problem+json`，响应头带 `X-Trace-Id`：

```json
{
  "type": "https://elibrary.example/problems/validation-failed",
  "title": "Request validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/books",
  "code": "VALIDATION_FAILED",
  "traceId": "0d5f…",
  "errors": [{"field": "size", "message": "must be between 1 and 50"}]
}
```

## API 一览

所有端点前缀 `/api/v1`，除开发令牌和健康检查外都要求 `Authorization: Bearer <JWT>`，角色为 `USER` 或 `ADMIN`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/books?q=&page=&size=` | 按书名、作者、ISBN 不区分大小写搜索；`size` 默认 20、最大 50 |
| GET | `/books/{id}` | 书目详情与实时可借数量 |
| POST | `/loans` | 借书；body 仅 `{"bookId":"…"}` |
| POST | `/loans/{id}/return` | 归还；重复调用幂等 |
| GET | `/loans/{id}` | 查看借阅；仅本人或 ADMIN |
| GET | `/me/loans?status=ACTIVE|RETURNED&page=&size=` | 当前用户的借阅记录 |

常见错误码：`BOOK_NOT_FOUND`、`BOOK_NOT_AVAILABLE`、`LOAN_ALREADY_ACTIVE`、`LOAN_NOT_FOUND`、`ACCESS_DENIED`、`VALIDATION_FAILED`、`MALFORMED_REQUEST`、`CONCURRENT_MODIFICATION`。无法解析的 JSON、UUID、分页数字或枚举值返回 400 `MALFORMED_REQUEST`；业务字段校验失败返回 400 `VALIDATION_FAILED`。HTTP 状态、错误码和 traceId 的组合是稳定契约。

## 本地开发（不用 Compose）

需要 JDK 21、Maven（或使用仓库自带的 `./mvnw`）。

```bash
# 终端 1：API，dev profile 会启用开发令牌端点并写入 ./data/library.db
./mvnw -pl library-api spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2：网关
./mvnw -pl library-gateway spring-boot:run
```

API 直连 <http://localhost:8080>，Swagger UI 在 <http://localhost:8080/swagger-ui.html>。此模式下用 `curl` 通过 8081 或 8080 调试；H5 页面仍建议用 Compose 启动（nginx 需要把 `/api` 代理到网关）。

## 测试

```bash
# 单元测试 + 网关过滤器/CORS：17 个，无需 Docker
./mvnw test

# 全部测试：17 个单元/网关测试 + 14 个集成测试（需要 Docker）
./mvnw verify
```

集成测试分两组：真实 PostgreSQL 16 容器（11 个）覆盖借阅/归还生命周期、重复归还、容量限制、越权、token 身份来源、错误契约、搜索分页、CORS 白名单，以及 **20 个并发用户争抢 3 本副本**；本地 SQLite profile（3 个）覆盖**并发容量、同用户重复借阅和事务模式**，确保 Compose 演示环境不会出现 500 或超借。核心逻辑不 mock。

## 生产配置

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://db.internal:5432/elibrary
DATABASE_USERNAME=…
DATABASE_PASSWORD=…
ELIBRARY_SECURITY_MODE=issuer
ELIBRARY_JWT_ISSUER_URI=https://idp.example.com/realms/library
ELIBRARY_CORS_ALLOWED_ORIGINS=https://library.example.com
```

生产 profile 使用 OIDC issuer/JWKS 校验，不加载开发令牌端点；数据库为 PostgreSQL；密钥只通过环境变量/密钥管理系统注入。`ELIBRARY_CORS_ALLOWED_ORIGINS` 需同时配置到 API 与网关（网关负责回答浏览器预检，API 仍保留直接调用时的白名单校验）。应用是无状态的，可水平扩展——SQLite 仅用于单机本地演示。

浏览器地址必须与白名单**逐字匹配**（含协议、主机、端口）。默认包含 `http://localhost:8088`、`http://127.0.0.1:8088` 和 `http://192.168.68.108:8088`；如果从其他主机或局域网 IP 访问，请把实际地址加入 `.env` 的 `ELIBRARY_CORS_ALLOWED_ORIGINS` 后重新执行 `docker compose up -d`，否则 nginx 转发携带的 `Origin` 会被网关以 403 拒绝。

## 假设

- 令牌 `sub` 是稳定用户标识，`roles` claim 包含 `USER`/`ADMIN`；用户目录由外部 IdP 管理，本服务不存用户表。
- 借期固定 14 天，`dueAt = borrowedAt + 14d`，全部时间使用 UTC `Instant`。
- `Book.totalCopies` 是当前总副本数；本项目不提供图书入库/上下架 API，种子数据仅用于本地演示。
- 同一本书可被不同用户同时借阅，只要活跃借阅数小于总副本数；同一用户同一本书最多一个活跃借阅。
- 所有书目查询也需要登录，不做匿名访问。

## 未实现功能

| 未实现 | 原因 |
| --- | --- |
| 逾期调度、罚金、催还通知 | 需要定时任务、时区/计费规则和通知渠道；本作业只保存 `dueAt`，把规则变化留给业务演进 |
| 预约/排队/续借 | 会引入新的聚合和公平性规则，超出核心借还闭环 |
| 全文搜索、推荐 | 当前 `locate` 搜索满足演示；大规模检索应使用 PostgreSQL FTS/trigram 或独立搜索引擎 |
| 真实 IdP、用户注册 | 本地用 dev token 保证 5 分钟可运行；生产通过 OIDC issuer 对接 |
| Kafka/消息队列、缓存、Redis、分布式锁 | 没有异步流程，借阅一致性由数据库事务和行锁保证，引入这些组件只会增加运维复杂度 |
| 微服务、服务发现、配置中心 | 业务边界尚不需要独立部署；模块化单体保留清晰边界，未来可拆分 |
| 图书管理后台 | 题目聚焦读者侧 API 与并发正确性；书目由 Flyway 种子/运维导入 |
| 指标大盘、链路追踪后端 | 已提供 traceId、健康探针与结构化日志；生产可接 Prometheus/OpenTelemetry，不在本地强制搭基础设施 |

## 关键权衡

- **SQLite 本地 / PostgreSQL 生产**：本地零依赖、开箱即用；生产必须使用支持行锁和多实例并发的 PostgreSQL。两者跑同一组 Flyway 迁移，SQLite 用触发器兜底容量约束，并以 `transaction_mode=IMMEDIATE` 在事务开始时就取得写锁，避免并发写入出现 `SQLITE_BUSY_SNAPSHOT`。
- **悲观行锁优先**：借书先锁 `book` 行，再统计并插入，避免“先查后插”的竞态；`@Version` 和数据库部分唯一索引/触发器是第二道防线。详情与备选方案见架构文档 ADR。
- **偏移分页**：实现简单、UI 友好；数据量很大或深分页时再切换游标分页。
- **开发令牌**：只在 `dev` profile 且 `hmac` 模式启用；生产必须关闭并使用 OIDC。

更完整的质量属性、时序图、数据约束、并发分析与演进风险见 [docs/architecture.md](docs/architecture.md)。

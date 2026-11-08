# E-Library Service

一个迷你数字图书馆服务：读者可以浏览书目、借书、还书并查看自己的借阅记录。

项目故意做成**模块化单体**，而不是微服务：业务规模只需要一个后端服务，但代码仍按限界上下文和端口/适配器分层，保持可替换、可测试、可演进。

## 技术栈

- Java 21 · Spring Boot 3.3 · Spring Security OAuth2 Resource Server
- PostgreSQL 16（Compose 开发/集成测试/生产）· Flyway
- `library-common` 认证/缓存/分页/错误契约共享模块 · nginx 托管的 H5 客户端
- Spring Cache + Caffeine（注解式缓存）· JUnit 5 · AssertJ · Testcontainers

## 目录

```
library-common/   认证、缓存、分页、错误契约与 traceId 共享内核
library-api/      后端 API（e-library-service）
library-web/      静态 H5 页面 + nginx 配置
docs/             架构与设计文档
docker-compose.yml
```

## 快速开始

前置条件：Docker 及 Docker Compose。首次运行需要下载镜像和 Maven 依赖，视网络通常 5–10 分钟；之后启动约 10 秒。

```bash
docker compose up --build -d
docker compose ps          # 等待 api 和 web 都变为 healthy
```

入口：

| 地址 | 说明 |
| --- | --- |
| <http://localhost:8088> | H5 客户端（nginx 同源代理 `/api`） |
| <http://localhost:8080> | API；Swagger UI：`/swagger-ui.html` |
| <http://localhost:8080/actuator/health/readiness> | 就绪探针 |
| `localhost:5432` 或 `内网IP:5432` | PostgreSQL 16（用户/库：`elibrary`，密码见 `POSTGRES_PASSWORD`） |

停止并清空 PostgreSQL 数据：

```bash
docker compose down -v
```

可选：复制 `.env.example` 为 `.env` 覆盖本地开发密钥。仓库不包含任何真实密钥。

## API 一览

所有端点前缀 `/api/v1`，除开发令牌和健康检查外都要求 `Authorization: Bearer <JWT>`，角色为 `USER` 或 `ADMIN`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/books?q=&page=&size=` | 按书名、作者、ISBN 不区分大小写搜索；`size` 默认 20、最大 50 |
| GET | `/books/{id}` | 书目详情与实时可借数量 |
| POST | `/loans` | 借书；body 仅 `{"bookId":"…"}`，用户身份只来自 token |
| POST | `/loans/{id}/return` | 归还；重复调用幂等 |
| GET | `/loans/{id}` | 查看借阅；仅本人或 ADMIN |
| GET | `/me/loans?status=ACTIVE\|RETURNED&page=&size=` | 当前用户的借阅记录 |

完整的请求/响应、错误码和 curl 示例见 [API 契约](docs/api-contract.md)。

## 测试

```bash
# 单元测试：11 个，无需 Docker
./mvnw test

# 全部测试：11 个单元测试 + 12 个集成测试（需要 Docker）
./mvnw verify
```

集成测试使用真实 PostgreSQL 16（Testcontainers），覆盖借还生命周期、幂等归还、并发不超借、授权、错误契约、CORS 与缓存失效。详见 [测试策略](docs/testing.md)。

## 文档

文档索引与推荐阅读路径：[docs/README.md](docs/README.md)。

- [架构总览](docs/architecture.md)
- [领域模型](docs/domain-model.md)
- [API 契约](docs/api-contract.md)
- [数据模型](docs/data-model.md)
- [借阅流程、并发与缓存](docs/concurrency-and-cache.md)
- [安全设计](docs/security.md)
- [高可用与运维](docs/operations.md)
- [测试策略](docs/testing.md)
- [风险与演进](docs/risks-and-evolution.md)
- [关键配置](docs/configuration.md)

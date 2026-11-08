# 关键配置

## 配置参考

| 配置 | 本地默认 | 生产 |
| --- | --- | --- |
| 数据库 | `jdbc:postgresql://localhost:5432/elibrary`（Compose 使用 `postgres:5432`） | `DATABASE_URL` → 托管 PostgreSQL |
| 安全模式 | `hmac` + dev profile | `issuer` + `ELIBRARY_JWT_ISSUER_URI` |
| 令牌 TTL | 8 小时（仅开发） | 由 IdP 决定，建议短 TTL |
| CORS | `http://localhost:8088,http://127.0.0.1:8088,http://192.168.68.108:8088,http://localhost:8080,http://127.0.0.1:8080`（由 `common.security` 应用） | 显式生产域名，注入 API |
| 缓存 | Caffeine，TTL 30s，最大 10000 条，`sync=true` | 可调 TTL/容量；跨实例强一致时替换为 Redis |
| 分页上限 | 50 | 50 |
| 健康探针 | `/actuator/health/{liveness,readiness}` | 同左，LB 使用 readiness |
| 停机超时 | 20s | 配合编排摘流 |
| 日志关联 | `X-Trace-Id` + MDC | 接入集中日志平台 |

### 本地开发数据库

Compose 会启动一个轻量的 `postgres:16-alpine` 容器：

```text
服务名：postgres
数据库：elibrary
用户：elibrary
密码：${POSTGRES_PASSWORD:-elibrary-dev-password}
宿主机端口：`${POSTGRES_BIND_ADDRESS:-0.0.0.0}:5432`
数据卷：postgres-data
```

容器内的 API 通过 `DATABASE_URL=jdbc:postgresql://postgres:5432/elibrary` 连接；在宿主机直接运行 `spring-boot:run` 时默认连接 `localhost:5432`。`docker compose down -v` 会清空本地数据库，下一次启动重新执行迁移并写入 8 本种子书。

### 生产环境变量

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://db.internal:5432/elibrary
DATABASE_USERNAME=…
DATABASE_PASSWORD=…
ELIBRARY_SECURITY_MODE=issuer
ELIBRARY_JWT_ISSUER_URI=https://idp.example.com/realms/library
ELIBRARY_CORS_ALLOWED_ORIGINS=https://library.example.com
ELIBRARY_CACHE_TTL=30s
ELIBRARY_CACHE_MAX_SIZE=10000
```

生产 profile 使用 OIDC issuer/JWKS 校验，不加载开发令牌端点；数据库为托管 PostgreSQL；密钥只通过环境变量/密钥管理系统注入。应用无状态，可水平扩展。

浏览器地址必须与 CORS 白名单**逐字匹配**（含协议、主机、端口）。默认包含 `http://localhost:8088`、`http://127.0.0.1:8088` 和 `http://192.168.68.108:8088`；如果从其他主机或局域网 IP 访问，请把实际地址加入 `.env` 的 `ELIBRARY_CORS_ALLOWED_ORIGINS` 后重新执行 `docker compose up -d`，否则 nginx 转发携带的 `Origin` 会被 API 以 403 拒绝。

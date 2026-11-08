# API 契约

## 约定与端点

### 通用约定

- 前缀：`/api/v1`。
- 内容类型：请求/响应 `application/json`；错误 `application/problem+json`。
- 认证：`Authorization: Bearer <JWT>`；`sub` 是用户 ID，`roles` claim 映射为 `ROLE_USER`/`ROLE_ADMIN`。
- 方法级授权：`@PreAuthorize("hasAnyRole('USER','ADMIN')")`。
- 分页：`page` 默认 0，`size` 默认 20，最大 50；`q` 最大 100 字符。
- 响应头：每个响应都带 `X-Trace-Id`；创建资源带 `Location`。
- 错误：RFC 7807，固定 `code` 字段，`traceId` 与响应头一致。

分页响应使用显式 envelope，而不是 Spring `Page`：

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### 端点

| 方法 | 路径 | 授权 | 成功 | 主要错误 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/dev/tokens` | 匿名，仅 dev+HMAC | 200 Token | 400 校验失败 |
| GET | `/api/v1/books?q=&page=&size=` | USER/ADMIN | 200 分页书目 | 401/403/400 |
| GET | `/api/v1/books/{id}` | USER/ADMIN | 200 书目 | 404 `BOOK_NOT_FOUND` |
| POST | `/api/v1/loans` | USER/ADMIN | 201 + Location | 404 `BOOK_NOT_FOUND`、409 `BOOK_NOT_AVAILABLE`、409 `LOAN_ALREADY_ACTIVE` |
| POST | `/api/v1/loans/{id}/return` | 本人或 ADMIN | 200 借阅 | 404 `LOAN_NOT_FOUND`、403 `ACCESS_DENIED` |
| GET | `/api/v1/loans/{id}` | 本人或 ADMIN | 200 借阅 | 404、403 |
| GET | `/api/v1/me/loans?status=&page=&size=` | USER/ADMIN | 200 分页借阅 | 400 非法状态 |

补充语义：

- 书目搜索 `q` 为空返回全部；匹配书名、作者、ISBN，均不区分大小写，按 `title, id` 稳定排序。
- `status` 只接受 `ACTIVE`/`RETURNED`；其他值返回 `400 MALFORMED_REQUEST`。
- ADMIN 能查看/归还任意用户的借阅；`/me/loans` 始终只返回 token 对应用户自己的记录，不接受 `userId` 参数。
- 重复归还同一个借阅返回 200，不创建新记录，`returnedAt` 不变。
- 借阅创建返回的 `bookTitle` 来自同一次事务读取的书目。

### 借书请求/响应

`BorrowBookRequest` 只有 `bookId` 字段；请求体即使包含 `userId` 也会被忽略，服务端始终使用 `@CurrentUser Actor`。成功返回 `201`、`Location: /api/v1/loans/{id}` 和借阅视图：

```json
{
  "id": "9f1c...",
  "bookId": "11111111-1111-1111-1111-111111111111",
  "bookTitle": "Clean Code",
  "status": "ACTIVE",
  "borrowedAt": "2026-11-08T06:45:00Z",
  "dueAt": "2026-11-22T06:45:00Z"
}
```

### curl 示例（本地）

以下命令直接调用 API（`http://localhost:8080`），也等价于经 nginx 访问 `/api`。示例使用 `jq` 提取 token；没有 `jq` 时可手动复制响应中的 `token`。

```bash
BASE=http://localhost:8080

# 1. 获取开发令牌（仅 dev profile）
TOKEN=$(curl -s -X POST "$BASE/api/v1/dev/tokens" \
  -H 'Content-Type: application/json' \
  -d '{"userId":"alice","role":"USER"}' | jq -r .token)

# 2. 搜索书目
curl -s "$BASE/api/v1/books?q=clean&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. 借书；种子数据中的《Clean Code》
curl -i -X POST "$BASE/api/v1/loans" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"bookId":"11111111-1111-1111-1111-111111111111"}'

# 4. 查看自己的活跃借阅
curl -s "$BASE/api/v1/me/loans?status=ACTIVE&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq

# 5. 归还，重复调用幂等
LOAN_ID=<上一步返回的 id>
curl -X POST "$BASE/api/v1/loans/$LOAN_ID/return" \
  -H "Authorization: Bearer $TOKEN" | jq

# 6. 触发校验错误，观察 ProblemDetail 与 X-Trace-Id
curl -i "$BASE/api/v1/books?size=51" -H "Authorization: Bearer $TOKEN"
```

成功借书返回 `201 Created`、`Location: /api/v1/loans/{id}` 和上述借阅视图。重复归还同一借阅返回 `200`，`returnedAt` 保持不变。

### 错误模型

```json
{
  "type": "https://elibrary.example/problems/loan-already-active",
  "title": "An active loan already exists",
  "status": 409,
  "detail": "An active loan already exists for this book",
  "instance": "/api/v1/loans",
  "code": "LOAN_ALREADY_ACTIVE",
  "traceId": "c3d1...",
  "errors": [
    {"field": "size", "message": "must be between 1 and 50"}
  ]
}
```

- `type` 是稳定的问题类型 URI；`code` 是机器可读枚举，客户端应基于它分支，而不是解析文案。
- `errors` 只在校验失败时出现。
- 5xx 只返回通用 `INTERNAL_ERROR`；堆栈仅写入服务端日志，并带同一个 traceId。
- 认证失败返回 401 和 `WWW-Authenticate: Bearer realm="e-library"`；权限不足返回 403，不把 403 伪装成 404。

| code | HTTP | 含义 |
| --- | --- | --- |
| `BOOK_NOT_FOUND` | 404 | 书目不存在 |
| `LOAN_NOT_FOUND` | 404 | 借阅不存在 |
| `BOOK_NOT_AVAILABLE` | 409 | 活跃借阅已达总副本数 |
| `LOAN_ALREADY_ACTIVE` | 409 | 该用户已有同一本书的活跃借阅 |
| `CONCURRENT_MODIFICATION` | 409 | 乐观锁冲突，可重试 |
| `DATA_CONFLICT` | 409 | 未映射的数据库约束冲突 |
| `ACCESS_DENIED` | 403 | 已认证但无权限 |
| `UNAUTHORIZED` | 401 | 未认证或令牌无效 |
| `VALIDATION_FAILED` | 400 | 参数/请求体校验失败 |
| `MALFORMED_REQUEST` | 400 | JSON、UUID、枚举值等无法解析 |
| `RESOURCE_NOT_FOUND` | 404 | 无对应路由/静态资源 |
| `METHOD_NOT_ALLOWED` | 405 | HTTP 方法不支持 |
| `INTERNAL_ERROR` | 500 | 未预期异常 |

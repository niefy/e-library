# E-Library 文档

本目录按“先总览、再专题”的方式组织。根目录 [README](../README.md) 只保留项目简介、快速开始和入口，设计细节从这里进入。

## 文档结构

| 文档 | 内容 |
| --- | --- |
| [架构总览](architecture.md) | 背景、范围与假设、质量属性、系统上下文、部署拓扑、模块与包结构 |
| [领域模型](domain-model.md) | 限界上下文、聚合与值对象、借阅状态机、核心不变量及其执行层次 |
| [API 契约](api-contract.md) | 端点、分页、错误模型、curl 示例 |
| [数据模型](data-model.md) | ER 图、表与约束、索引、Flyway 迁移、类型可移植性 |
| [借阅流程、并发与缓存](concurrency-and-cache.md) | 借书/归还时序、行锁与约束、SQLite 方案、多实例、Caffeine 缓存与失效 |
| [安全设计](security.md) | JWT 认证、角色与所有权授权、CORS、输入校验、密钥管理 |
| [高可用与运维](operations.md) | 无状态扩容、健康探针、优雅停机、配置外部化、日志与指标 |
| [测试策略](testing.md) | 领域单测、Testcontainers 集成测试、并发测试、运行命令与覆盖缺口 |
| [风险与演进](risks-and-evolution.md) | 风险清单、未实现功能、关键权衡、推荐演进顺序 |
| [关键配置](configuration.md) | 本地/生产配置对照、生产环境变量、CORS 必填项 |

## 推荐阅读路径

**快速上手**

1. 根目录 [README](../README.md)：启动服务、访问 H5/API。
2. [架构总览](architecture.md)：理解模块边界与部署形态。
3. [API 契约](api-contract.md)：用 curl 跑通借还闭环。

**设计评审**

1. [架构总览](architecture.md)：范围、质量属性、模块划分。
2. [领域模型](domain-model.md)：聚合、状态机和不变量。
3. [借阅流程、并发与缓存](concurrency-and-cache.md)：并发正确性与缓存一致性。
4. [数据模型](data-model.md) / [安全设计](security.md)：约束与信任边界。
5. [测试策略](testing.md) / [风险与演进](risks-and-evolution.md)：验证方式与已知限制。

**部署运维**

1. [架构总览](architecture.md) 的“本地/生产部署拓扑”。
2. [高可用与运维](operations.md)：探针、停机、日志。
3. [关键配置](configuration.md)：环境变量与 CORS 白名单。

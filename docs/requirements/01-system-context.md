# 系统上下文与确定性数据

## 1. 用途

本项目是一个前后端分离的金融衍生品流程 Demo，供教学、API/UI 自动化测试与测试生成研究使用。它不是生产交易系统，不处理真实资金、真实账户或真实市场连接。

本文档及同目录的其他需求、`docs/openapi.yaml`、`docs/state-machine.yaml`、`docs/permissions.yaml` 和 `coverage/coverage-plan.yaml` 是测试 oracle。测试不得把 Java/Vue 源码中的当前实现细节当成期望结果。

## 2. 运行时边界

- 后端 API 基础路径：`http://localhost:8080/api`
- 前端默认地址：`http://localhost:4173`
- 健康检查：`GET http://localhost:8080/actuator/health`
- `demo` profile 使用 H2 内存库，进程重启即丢失数据。
- `demo` profile 的业务时钟固定为 `2026-01-15T10:00:00Z`，业务日期为 `2026-01-15`。
- `POST /api/demo/reset` 必须清空交易与审计数据，然后重新写入下列固定 reference data。该调用完成后只保留一条 `DEMO_RESET` 审计事件。

## 3. 固定 reference data

### 组合

| code | name |
|---|---|
| `ALPHA` | Alpha Demo Portfolio |
| `BETA` | Beta Demo Portfolio |

### 市场快照

| underlying | spot | volatility | riskFreeRate | asOf |
|---|---:|---:|---:|---|
| `AAPL` | 100.00 | 0.20 | 0.03 | `2026-01-15T10:00:00Z` |
| `MSFT` | 250.00 | 0.24 | 0.03 | `2026-01-15T10:00:00Z` |

所有金额以 USD 表示；本 Demo 不建模汇率。

## 4. 简化身份

HTTP 使用 `Authorization: Bearer <token>`：

- `trader-token` → `TRADER`
- `risk-token` → `RISK_MANAGER`
- `approver-token` → `APPROVER`

缺失或未知 token 返回 HTTP 401，错误码 `UNAUTHENTICATED`。身份存在但没有操作权限返回 HTTP 403，错误码 `FORBIDDEN`。这不是生产认证方案。

## 5. 通用 HTTP 约定

- 请求与响应正文使用 JSON。
- 时间戳为 UTC ISO-8601，日期为 `YYYY-MM-DD`。
- 资源不存在返回 404。
- Bean Validation 或业务字段校验失败返回 400 和 `VALIDATION_ERROR`。
- 状态迁移、幂等键复用或版本冲突返回 409，并使用更具体的错误码。
- 错误正文统一包含 `timestamp`、`status`、`code`、`message`、`path`；字段校验可附带 `fieldErrors`。
- CORS 允许 Demo 前端来源，允许 `Authorization`、`Content-Type`、`Idempotency-Key` 请求头。

## 6. 非目标

不要求真实身份系统、持久数据库、精确交易日历、日计数惯例选择、收益率曲线、波动率曲面、币种转换、净额结算、监管报表或高可用部署。

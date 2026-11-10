# 系统上下文与确定性数据

## 1. 用途

本项目是一个前后端分离的金融衍生品流程 Demo，供教学、API/UI 自动化测试与测试生成研究使用。它不是生产交易系统，不处理真实资金、真实账户或真实市场连接。

本文档及同目录的其他需求、`docs/openapi.yaml`、`docs/state-machine.yaml`、`docs/permissions.yaml` 和 `docs/KNOWN-DEFECTS.md` 是测试 oracle。测试不得把 Java/Vue 源码中的当前实现细节当成期望结果。

## 2. 运行时边界

- 后端 API 基础路径：`http://localhost:8080/api`
- 前端默认地址：`http://localhost:4173`
- 健康检查：`GET http://localhost:8080/api/health`（容器探针也可使用 actuator）
- `demo` profile 使用 H2 内存库，进程重启即丢失数据。
- `demo` profile 的业务时钟固定为 `2026-01-15T10:00:00Z`。
- `POST /api/test/reset`（仅 demo profile）必须清空交易与审计数据，然后重新写入下列固定 reference data。该调用完成后只保留一条 `DEMO_RESET` 审计事件。

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

每个受保护 API 请求必须携带：

- `X-Demo-User`：非空用户名（Demo 固定映射见前端）
- `X-Demo-Role`：`trader` | `risk_manager` | `viewer`（大小写不敏感，服务端规范化为小写）

可选追踪头（服务端接受，不改变授权结果）：

- `X-Run-Id`
- `X-Case-Key`

创建交易另需：

- `Idempotency-Key`

缺失或非法身份头返回 HTTP 401，错误码 `UNAUTHENTICATED`。身份存在但没有操作权限返回 HTTP 403，错误码 `FORBIDDEN`。这不是生产认证方案。

角色职责：

- `trader`：创建/提交交易，只读市场、组合、估值、审计
- `risk_manager`：审批/拒绝、读取风险、审批队列
- `viewer`：只读（含审批队列只读），不可创建/提交/审批/读风险聚合

## 5. 通用 HTTP 约定

- 请求与响应正文使用 JSON。
- 时间戳为 UTC ISO-8601，日期为 `YYYY-MM-DD`。
- 资源不存在返回 404。
- Bean Validation 或业务字段校验失败返回 400 和 `VALIDATION_ERROR`。
- 状态迁移、幂等键复用或版本冲突返回 409，并使用更具体的错误码。
- 错误正文统一包含 `timestamp`、`status`、`code`、`message`、`path`；字段校验可附带 `fieldErrors`。
- CORS 允许 Demo 前端来源，允许 `Content-Type`、`Idempotency-Key`、`X-Demo-User`、`X-Demo-Role`、`X-Run-Id`、`X-Case-Key`。

## 6. 非目标

不要求真实身份系统、持久数据库、精确交易日历、日计数惯例选择、收益率曲线、波动率曲面、币种转换、净额结算、监管报表或高可用部署。

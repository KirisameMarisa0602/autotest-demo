# 金融衍生品交易 Demo

这是一个独立、可运行、刻意不生产化的前后端分离 Demo，用于教学、API/UI 自动化测试和测试生成研究。它不连接真实市场，不处理真实资金，也不具备生产认证、持久化、交易日历、精确曲线或高可用能力。

## 架构

- `backend/`：Java 21、Spring Boot 3、Maven、H2 内存库。
  - `market`：固定市场快照与 reference data API。
  - `portfolio`：组合。
  - `trade`：European option / forward、幂等创建、状态机、乐观锁、单笔 PV/Delta。
  - `risk`：获批交易聚合及 ±10% spot 压力场景。
  - `audit`：业务审计事件。
  - `demo`：固定 Clock、reset/seed。
  - `security` / `shared`：简化 Bearer 身份与统一错误响应。
- `frontend/`：Vue 3、Vite、TypeScript、Pinia、Vue Router。
- `docs/` 与 `coverage/`：独立于源码的测试 oracle 和稳定 case keys。
- `compose.yaml`：后端与前端一键启动。

后端是按领域包隔离的模块化单体，不拆分远程服务，以保持 Demo 代码量可维护。

## Demo 身份

请求使用演示头：

- `X-Demo-User: alice` + `X-Demo-Role: trader`
- `X-Demo-User: bob` + `X-Demo-Role: risk_manager`
- `X-Demo-User: carol` + `X-Demo-Role: viewer`

可选追踪头：`X-Run-Id`、`X-Case-Key`。审批与风险读取均由 `risk_manager` 完成。这不是安全认证方案，不得照搬到生产系统。

## 核心业务链

1. TRADER 创建 `DRAFT`（必须带 `Idempotency-Key`）。
2. TRADER 使用当前 `expectedVersion` 提交为 `SUBMITTED`。
3. APPROVER 使用当前 `expectedVersion` 批准为 `APPROVED`，或携带非空意见拒绝为 `REJECTED`。
4. RISK_MANAGER 查看仅由 `APPROVED` 交易组成的组合风险和压力场景。
5. 任意有效身份查看审计时间线。

`demo` profile 的时间固定为 `2026-01-15T10:00:00Z`。`POST /api/demo/reset` 会清空状态并重新写入 `ALPHA`、`BETA` 和 AAPL/MSFT 固定快照，适合作为测试前置步骤。

## 一键启动

要求 Docker Compose；镜像使用 Java 21 / Maven 3.9.9 和 Node 22 构建。

```powershell
Copy-Item .env.example .env
docker compose up --build
```

启动后：

- 前端：<http://localhost:4173>
- 后端健康检查：<http://localhost:8080/actuator/health>
- API 基址：<http://localhost:8080/api>

停止并移除容器：

```powershell
docker compose down
```

## 不使用 Docker 的开发方式

需要本机 Java 21、Maven 3.9+、Node 22：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "demo"
mvn spring-boot:run
```

另开终端：

```powershell
cd frontend
npm ci
npm run dev
```

Vite 开发服务器会把 `/api` 和 `/actuator` 代理到 `localhost:8080`。

## 主要 API

- `POST /api/demo/reset`
- `GET /api/market-data`
- `GET /api/portfolios`
- `POST /api/trades`
- `GET /api/trades?portfolioCode=ALPHA`
- `GET /api/trades/{id}`
- `GET /api/trades/{id}/valuation`
- `POST /api/trades/{id}/submit`
- `POST /api/trades/{id}/approve`
- `POST /api/trades/{id}/reject`
- `GET /api/risk/portfolios/{portfolioCode}`
- `GET /api/audit-events`

完整参数、响应与错误见 `docs/openapi.yaml`；状态机和权限分别见 `docs/state-machine.yaml`、`docs/permissions.yaml`。

## 测试与检查

后端（要求 Java 21）：

```powershell
cd backend
mvn test
```

前端：

```powershell
cd frontend
npm ci
npm test
npm run build
```

已启动整套服务后可执行：

```powershell
powershell -File scripts/smoke.ps1
```

`coverage/coverage-plan.yaml` 的 case key 是稳定标识。其 `sourceCommit` 保留为 `PENDING_COMMIT`，测试证据应运行以下命令解析当前基线 SHA；脚本不会改动工作树：

```powershell
powershell -File scripts/stamp-commit.ps1
```

## 供外部 Playwright 框架测试

本仓库不内置 Playwright，外部框架应：

1. 启动本 Demo，并等待两个容器健康。
2. 每个场景先带任意有效 token 调用 `POST /api/demo/reset`。
3. 使用 `docs/openapi.yaml` 和 `docs/requirements/` 生成 API 期望，不读取实现源码作为 oracle。
4. UI 优先使用 role/label，其次使用 `docs/requirements/04-ui-test-contract.md` 列出的稳定 `data-testid`。
5. 使用 `coverage/coverage-plan.yaml` 的稳定 case key 记录结果。

默认外部 green 流程可覆盖登录、创建、提交、审批、风险与审计。`RISK-SHORT-OPTION-DIRECTION` 是专门用于暴露下述已知产品缺陷的非默认用例。

## 已知限制、缺陷与需求空白

- **有意保留的确定性产品缺陷**：风险聚合对 SHORT 欧式期权遗漏方向符号，把它当作 LONG；单笔估值方向正确，SHORT forward 不受影响。详见 `KNOWN-BUG-RISK-SHORT-OPTION-SIGN`。普通 green 套件不执行该失败用例，也没有 disabled JUnit 掩盖它。
- **未批准的 spec gap**：同日到期期权应使用内在价值、剩余日内时间还是最小期限尚无结论。不得从当前实现反推规范，也不应自动修复；详见 `docs/requirements/02-trading-and-pricing.md`。
- H2 为内存库，重启即丢失；Clock 固定；token 固定；定价模型简化；审计不可替代合规审计。
- Demo 没有 TLS、密钥管理、真实用户、限流、持久消息、分布式事务、生产监控或灾备，因此绝对不能用于生产。

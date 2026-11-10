# 金融衍生品交易 Demo

故意不完善、链路复杂的前后端分离 Demo，供 Playwright 等自动化框架联调。非生产系统。

## 架构

- `backend/`：Java 21、Spring Boot 3.4、Maven、H2。模块：auth/rbac、marketdata、portfolio、trade、pricing、risk、approvals、audit、demo。
- `frontend/`：Vue 3、Vite、TypeScript、Pinia、Vue Router（nginx 容器）。
- `docs/`：独立 oracle（requirements / openapi / state-machine / permissions / KNOWN-DEFECTS）。
- `compose.yaml`：一键启动 backend + frontend。宿主机可不装 Node/Java 21；镜像内构建。

## 身份头

| Header | 必填 | 说明 |
|---|---|---|
| `X-Demo-User` | 是 | 如 `alice` / `bob` / `carol` |
| `X-Demo-Role` | 是 | `trader` / `risk_manager` / `viewer` |
| `X-Run-Id` | 否 | 运行追踪 |
| `X-Case-Key` | 否 | 用例追踪 |
| `Idempotency-Key` | 创建交易时 | 幂等创建 |

## 一键启动

```powershell
Copy-Item .env.example .env
docker compose up --build
```

- 前端：http://localhost:4173
- 健康检查：http://localhost:8080/api/health
- API：http://localhost:8080/api

```powershell
docker compose down
```

## 主要 API

- `GET /api/health`
- `POST /api/test/reset`（demo profile）
- `GET /api/market-data`（别名 `/api/market`）、`GET /api/portfolios`
- `POST/GET /api/trades`、`GET /api/trades/{id}/valuation`
- `POST /api/trades/{id}/submit|approve|reject`
- `GET /api/approvals`、`POST /api/approvals/{id}/approve|reject`
- `GET /api/risk/portfolios/{code}`
- `GET /api/audit-events`

状态机：`DRAFT → SUBMITTED → APPROVED|REJECTED`；非法迁移 / 版本冲突 → 409。

## 故意缺陷

见 `docs/KNOWN-DEFECTS.md`：

1. SHORT 欧式期权组合 **Delta** 聚合遗漏方向符号
2. 审批按钮 `data-testid="approve-trade-old"`
3. 同日到期“今日”用本地时区比较（需求故意模糊 UTC/本地）

## 验证

```powershell
# health
Invoke-RestMethod http://localhost:8080/api/health

# reset
$h = @{
  'X-Demo-User' = 'alice'
  'X-Demo-Role' = 'trader'
  'X-Run-Id' = 'smoke'
  'X-Case-Key' = 'reset'
}
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/test/reset -Headers $h

# 或
powershell -File scripts/smoke.ps1
```

联调入口在同级 `../playwright`：`npm run demo:cycle -- --skip-ai --keep-services`。候选产品修复补丁由框架侧 `patches/fix-short-delta.diff` 验证；验证器会在 patched worktree 重建 backend，结束后恢复本仓库基线镜像，故意缺陷仍留在 `main`。

## 非目标

- 生产认证、Temporal、管理大屏、自动合并远端 PR
- 随机 flaky 或安全漏洞植入
- 把源码结构当作业务 oracle（oracle 仅来自 `docs/`）

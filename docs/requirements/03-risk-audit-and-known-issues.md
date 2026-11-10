# 风险、压力场景与审计需求

## 1. 风险组合范围

`GET /api/risk/portfolios/{portfolioCode}` 只聚合该组合中状态为 `APPROVED` 的交易。`DRAFT`、`SUBMITTED`、`REJECTED` 均不得计入。

返回：

- `approvedTradeCount`：参与聚合的交易数；
- `presentValue`：每笔交易按单笔定价 oracle 得到的带方向 PV 之和；
- `delta`：每笔交易按单笔定价 oracle 得到的带方向 Delta 之和；
- `currency`：固定为 `USD`；
- `scenarios`：固定的 `SPOT_DOWN_10` 和 `SPOT_UP_10`。

空组合应返回 HTTP 200，计数为 0，PV 与 Delta 为 `0.000000`，仍返回两个零值压力场景。

## 2. 方向符号（权威期望）

所有产品都必须保留 `side` 方向。特别是对任意欧式期权：

- 只有一笔 LONG 时，组合 PV/Delta 必须等于该交易的单笔 PV/Delta。
- 把同一交易改为 SHORT 后，组合 PV/Delta 必须分别等于原值的相反数。
- 一笔 LONG 与一笔字段完全相同的 SHORT 同时获批后，组合 PV 和 Delta 必须在 `0.0001` 容差内净额为 0。

## 3. 压力场景

- `SPOT_DOWN_10`：把每个标的的 spot 乘以 `0.90` 后重新完整定价。
- `SPOT_UP_10`：把每个标的的 spot 乘以 `1.10` 后重新完整定价。
- 利率、波动率、到期日、数量和方向保持不变。
- `stressedPresentValue` 为压力 spot 下的带方向组合 PV。
- `pnl = stressedPresentValue - 基准 presentValue`。
- 输出四舍五入到 6 位小数；跨实现测试容差为 `0.0001`。

## 4. 已知确定性产品缺陷

当前基线故意保留缺陷 `KNOWN-BUG-RISK-SHORT-OPTION-SIGN`：风险聚合在处理 SHORT 欧式期权时遗漏方向符号，把它当作 LONG 聚合。单笔估值端点没有此缺陷，SHORT forward 也没有此缺陷。

这是供外部测试框架发现的确定性 product bug：

- 普通 green 单元/集成测试不得把错误结果断言为正确，也不得用 disabled JUnit 测试掩盖它。
- 默认 green 套件不执行该缺陷用例。
- 外部 Playwright/API 回归可执行 `RISK-SHORT-OPTION-DIRECTION`，并应按照第 2 节权威期望失败，从而报告产品缺陷。
- 不应在没有显式变更任务时自动修复该缺陷。

## 5. 审计事件

以下成功动作各产生一条不可修改的审计事件：

- 创建新交易：`TRADE_CREATED`
- 提交：`TRADE_SUBMITTED`
- 批准：`TRADE_APPROVED`
- 拒绝：`TRADE_REJECTED`
- demo 重置：`DEMO_RESET`

事件字段为 `id`、`eventType`、`aggregateType`、`aggregateId`、`actor`、`occurredAt`、`details`。

- 交易动作的 `aggregateType=TRADE`、`aggregateId=trade.id`。
- actor 为执行动作的角色名：`TRADER` 或 `APPROVER`；启动时重置可为 `SYSTEM`。
- 幂等重放、失败校验、拒绝访问、版本冲突和非法迁移不得新增审计事件。
- reset 必须先删除旧审计，再产生一条新的 `DEMO_RESET`。
- 查询按 `occurredAt` 倒序；固定 Clock 导致时间相同时，事件间顺序不作保证，测试应按类型和 aggregate id 匹配。

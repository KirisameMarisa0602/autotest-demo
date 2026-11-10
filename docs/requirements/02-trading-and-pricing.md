# 交易、状态机与定价需求

## 1. 交易输入

创建交易必须提供请求头 `Idempotency-Key`（非空，最长 100 字符）及以下字段：

- `portfolioCode`：已存在的组合代码，最长 40 字符。
- `tradeType`：`EUROPEAN_OPTION` 或 `FORWARD`。
- `side`：`LONG` 或 `SHORT`。
- `underlying`：已有市场快照的代码，只允许大写字母、数字、点和连字符，最长 40 字符。
- `quantity`：大于 0，最多 12 位整数和 6 位小数。
- `strike`：大于 0。对期权表示执行价，对远期表示交割价；最多 12 位整数和 6 位小数。
- `maturity`：早于“今日”时必须拒绝；等于“今日”时适用第 5 节的 spec gap，测试不得推断其结果。
- `optionType`：欧式期权必须为 `CALL` 或 `PUT`；远期必须省略。

创建成功返回 HTTP 201、`Location: /api/trades/{id}`，初始状态为 `DRAFT`，初始 `version=0`。

## 2. 幂等创建

- 首次使用某个 `Idempotency-Key` 时创建一笔交易。
- 相同 key 与语义相同的创建字段再次提交时，不创建新交易或新审计事件，返回 HTTP 200 和原交易的当前表示（同一个 `id`）。
- 比较数值时忽略无意义的尾随零，例如 `10` 与 `10.0` 语义相同。
- 相同 key 搭配任一不同创建字段时返回 HTTP 409、`IDEMPOTENCY_CONFLICT`。
- 幂等键只用于创建；状态迁移依赖乐观锁。

## 3. 状态与乐观锁

权威迁移见 `docs/state-machine.yaml`。

- `DRAFT` 只能由 `trader` 提交为 `SUBMITTED`。
- `SUBMITTED` 只能由 `risk_manager` 批准为 `APPROVED`，或拒绝为 `REJECTED`。
- `APPROVED`、`REJECTED` 为终态。
- 每个迁移请求都必须携带 `expectedVersion`。它必须与响应中的当前 `version` 完全相等。
- 成功迁移使 `version` 加 1。
- 版本不等返回 HTTP 409、`VERSION_CONFLICT`，不得改变交易。
- 不允许的迁移返回 HTTP 409、`INVALID_TRANSITION`，不得改变交易。
- 拒绝必须提供非空 `comment`；批准的 `comment` 可省略。意见最长 500 字符。

审批也可通过 `/api/approvals` 队列端点完成，语义与 `/api/trades/{id}/approve|reject` 相同。

## 4. 单笔定价 oracle

单笔估值端点对 `trader`、`risk_manager`、`viewer` 开放。输出 `presentValue`、`delta`、`currency=USD` 和市场快照时间。以下规则适用于到期日在业务日期之后的交易：

设：

- \(S\) 为 spot，\(K\) 为 `strike`；
- \(r\) 为连续复利无风险利率；
- \(\sigma\) 为波动率；
- \(T=\text{实际自然日天数}/365\)；
- \(q\) 为 `quantity`；
- \(d=+1\)（LONG）或 \(-1\)（SHORT）。

远期：

- 单位 PV = \(S-Ke^{-rT}\)
- 单位 Delta = \(1\)

欧式期权使用无分红 Black-Scholes：

- \(d_1=[\ln(S/K)+(r+\sigma^2/2)T]/(\sigma\sqrt{T})\)
- \(d_2=d_1-\sigma\sqrt{T}\)
- Call PV = \(S N(d_1)-Ke^{-rT}N(d_2)\)
- Put PV = \(Ke^{-rT}N(-d_2)-S N(-d_1)\)
- Call Delta = \(N(d_1)\)
- Put Delta = \(N(d_1)-1\)

最终 `presentValue = 单位 PV × q × d`，`delta = 单位 Delta × q × d`，四舍五入到 6 位小数。AI 生成的跨实现测试应允许绝对误差 `0.0001`，以容纳正态分布函数近似差异。

因此，在其他字段相同的情况下，SHORT 单笔估值的 PV 和 Delta 必须是 LONG 的相反数。

## 5. 尚未批准的需求空白：同日到期与时区

`maturity` 等于“今日”时应采用到期内在价值、日终前剩余时间，还是最小一天期限，目前没有业务批准结论。

同时，**“今日”究竟按 UTC 业务时钟还是按运行环境本地时区解释**也尚未拍板。需求故意保持模糊：

- 这是明确的 **spec gap**，不是可由开发者自行修复的 bug。
- 测试不得对同日到期的 PV、Delta 或创建是否应接受作出规范性断言。
- 当前实现表现不构成 oracle；未来必须先更新本需求，再更新实现和测试。
- 详见 `docs/KNOWN-DEFECTS.md` 中的 `SPEC-GAP-SAME-DAY-EXPIRY-TIMEZONE`。

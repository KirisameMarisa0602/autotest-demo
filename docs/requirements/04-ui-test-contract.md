# UI 与外部自动化契约

## 1. 页面

- `/login`：选择 `TRADER`、`APPROVER`、`RISK_MANAGER` 固定身份。
- `/trading`：创建草稿、查看交易、提交草稿。
- `/approvals`：查看 `SUBMITTED` 交易、批准或拒绝。
- `/risk`：读取组合风险和固定压力场景。
- `/audit`：读取审计事件时间线。

身份选择保存在浏览器 local storage：`demo-role` 与 `demo-token`。API client 对每个请求自动添加所选 token。

## 2. 稳定选择器

外部 Playwright 应优先使用无障碍 role/label；必要时可使用以下稳定 `data-testid`：

- 登录：`login-trader`、`login-approver`、`login-risk-manager`
- 全局身份：`global-role-switcher`、`logout-button`
- 导航：`nav-trading`、`nav-approvals`、`nav-risk`、`nav-audit`
- 交易输入：`trade-portfolio`、`trade-type`、`trade-side`、`trade-underlying`、`trade-quantity`、`trade-strike`、`trade-maturity`、`trade-option-type`、`idempotency-key`
- 交易动作/结果：`create-trade`、`refresh-trades`、`trade-row`、`trade-status`、`submit-trade`、`trade-error`、`trade-message`
- 审批：`approval-row`、`approval-comment`、`approve-trade`、`reject-trade`、`approval-error`
- 风险：`risk-portfolio`、`load-risk`、`risk-report`、`approved-trade-count`、`risk-pv`、`risk-delta`、`risk-scenario`
- 审计：`refresh-audit`、`audit-timeline`、`audit-event`

多行元素的 test id 故意相同，应使用 `.filter()`、`.nth()` 或 `data-trade-id` / `data-event-type` 缩小范围，不依赖 CSS 类名或 DOM 层级。

## 3. 外部测试隔离

每个独立场景开始前：

1. 用任一有效 token 调用 `POST /api/demo/reset`。
2. 再通过 UI 选择该场景所需身份。
3. 每个创建动作使用该测试 case 专属、稳定且唯一的 `Idempotency-Key`。

不要依赖测试执行顺序、数据库残留、生成的 UUID 值或固定 Clock 下相同时间戳事件的相对顺序。

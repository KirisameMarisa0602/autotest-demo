# Known Defects (intentional)

These defects are deterministic fixtures for external automation frameworks.
Do not “fix” them unless a change task explicitly asks to close the defect.

## KNOWN-BUG-RISK-SHORT-OPTION-DELTA

- **Module**: risk aggregation (`GET /api/risk/portfolios/{portfolioCode}`)
- **Symptom**: For an approved **SHORT European option**, portfolio **Delta** is aggregated **without** the side sign (SHORT is not multiplied by `-1`). The result looks like the LONG-side Delta.
- **Scope**:
  - Affects SHORT `EUROPEAN_OPTION` Delta only.
  - Present Value for the same SHORT option remains correctly signed.
  - Single-trade valuation (`GET /api/trades/{id}/valuation`) is correct for both PV and Delta.
  - SHORT `FORWARD` aggregation is correct.
- **Oracle**: `docs/requirements/03-risk-audit-and-known-issues.md` §2 — portfolio Delta must equal the signed single-trade Delta.
- **Locator in code**: `backend/.../risk/RiskService.java` short-option Delta branch.

## KNOWN-BUG-UI-APPROVE-TESTID

- **Module**: frontend approval desk
- **Symptom**: The approve button still exposes the stale locator `data-testid="approve-trade-old"` instead of the contract name `approve-trade`.
- **Oracle**: `docs/requirements/04-ui-test-contract.md` lists `approve-trade` as the stable selector; the live DOM intentionally diverges.
- **Locator in code**: `frontend/src/views/ApprovalDeskView.vue`.

## SPEC-GAP-SAME-DAY-EXPIRY-TIMEZONE

- **Module**: trade validation + pricing day count
- **Symptom / ambiguity**: When `maturity` equals “today”, the implementation compares dates using the **JVM local timezone** (`ZoneId.systemDefault()`), while demo Clock / documented business instant is UTC (`2026-01-15T10:00:00Z`).
- **Requirement stance**: UTC vs local is intentionally unresolved. Tests must not assert normative PV/Delta/accept-reject behavior for same-day maturity.
- **Oracle**: `docs/requirements/02-trading-and-pricing.md` §5.

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
- **Symptom / ambiguity**: When `maturity` equals “today”, the implementation floors the pricing day count to one day, and the two call sites disagree about what "today" means: pricing uses the injected business `Clock` (2026-01-15T10:00:00Z in `demo`), while creation validation compares against the JVM local date.
- **Requirement stance**: neither the same-day day-count convention nor UTC-vs-local is approved. Tests must not assert normative PV/Delta/accept-reject behavior for same-day maturity.
- **Oracle**: `docs/requirements/02-trading-and-pricing.md` §5.

## DOC-DRIFT-LIST-AND-HISTORY

- **Module**: published contract (`docs/openapi.yaml`, `docs/permissions.yaml`)
- **Symptom**: The published OpenAPI still documents `GET /api/trades` as a bare array without `page`/`size`/`status`, and does not document `GET /api/trades/{id}/history` at all. `docs/permissions.yaml` does not list the history operation either.
- **Why it is intentional**: a real project's published contract routinely lags the implementation. The requirements documents are authoritative; the OpenAPI is stale. External automation must detect the drift from source instead of trusting the contract.
- **Required behavior**: list/history behavior is defined by `docs/requirements/02-trading-and-pricing.md` §6 and §7. Do not "fix" the OpenAPI unless a change task explicitly asks for contract refresh.
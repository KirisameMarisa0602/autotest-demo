# Known deterministic defects

These defects are intentionally retained so the Playwright automation framework
can demonstrate product-bug detection, test healing, and specification gaps.
They must not be treated as green-suite oracles.

## KNOWN-BUG-RISK-SHORT-OPTION-SIGN

- Kind: `product_bug`
- Location: `backend/src/main/java/com/example/derivatives/risk/RiskService.java`
- Symptom: portfolio risk aggregation flips SHORT European option PV/Delta back
  to a LONG-looking sign after `PricingService` already applied `side.sign()`.
- Independent oracle: `docs/requirements/03-risk-audit-and-known-issues.md` §2
- Detection: Playwright `defect` project / case `derivatives.api.short-risk`
- Correct repair: remove the extra SHORT-option negate in risk aggregation only.

## KNOWN-BUG-UI-SUBMIT-LABEL-EN

- Kind: `test_bug` when encoded as a stale generated selector/name
- Location: trading desk submit control uses Chinese label `提交` and
  `data-testid="submit-trade"`
- Symptom: a generated healing candidate that clicks
  `getByRole('button', { name: 'Submit for approval' })` fails while product UI
  is correct
- Independent oracle: `docs/requirements/04-ui-test-contract.md`
- Detection: Playwright `healing` project /
  `tests/generated/derivatives-submit.healing.spec.ts`
- Correct repair: heal the generated locator/name only; do not change product
  oracle text.

## KNOWN-SPEC-GAP-SAME-DAY-EXPIRY

- Kind: `spec_gap`
- Location: `PricingService` uses local-date day count and clamps same-day tenor
  to one day
- Symptom: UTC vs local calendar cut-over for “today maturity” is intentionally
  underspecified
- Independent oracle gap: coverage target `boundary.same-day-expiry` remains
  `pending`
- Detection: planner/generator must exclude or escalate rather than invent an
  oracle

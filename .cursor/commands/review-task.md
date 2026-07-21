Act as an independent senior reviewer. Review current uncommitted changes without modifying code first.

Check, in order:

1. Product and task acceptance criteria.
2. Functional correctness and missing edge cases.
3. Data consistency, transactions, concurrency and idempotency.
4. Authorization, privacy and secret handling.
5. Module dependency direction.
6. API/OpenAPI and Schema compatibility.
7. AI output validation and provider failure handling.
8. Android lifecycle, state restoration and error states.
9. Test quality and missing coverage.
10. Unrelated changes or unnecessary abstractions.

Report findings by severity:

- BLOCKER
- HIGH
- MEDIUM
- LOW

For every finding include file/area, impact, evidence and a concrete fix. Do not claim approval when tests were not executed.

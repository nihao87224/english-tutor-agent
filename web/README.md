# English Tutor Agent Web

Web client for the V1.0 Expression Coach MVP.

## Scripts

```powershell
pnpm install
pnpm run dev
pnpm test
pnpm run e2e
pnpm run build
```

The app reads `VITE_API_BASE_URL` when present and otherwise talks to `http://localhost:8080`.

## M3-T01 Scope

- Vite + React + TypeScript project baseline.
- Typed REST API client for profile, planning, training, privacy and conversation entry points.
- POST SSE parsing for `status`, `text_delta`, `correction_ready` and `done`.
- Unit tests for headers, error handling and stream parsing.
- Playwright E2E with mocked backend contract responses.

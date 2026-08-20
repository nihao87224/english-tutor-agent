# English Tutor Agent Web

React + TypeScript + Vite Web client for the learner SaaS experience.

## Scripts

```powershell
pnpm install
pnpm run dev
pnpm test
pnpm run e2e
pnpm run build
```

`VITE_API_BASE_URL` controls the API origin. In production it must be an empty
string so the app uses same-origin `/api` through the Web Nginx container. When
the variable is not set, local development falls back to `http://localhost:8080`.

## Current scope

- Email/password login and registration.
- Access-token authenticated API client with cookie-based refresh support.
- Route guard for learner onboarding, today practice, summary and account pages.
- V2 deterministic daily prescription with priority goal, rationale, ordered blocks,
  estimated time and scene-specific Lin Muen task hero.
- Difficulty, available-time and topic feedback with idempotent prescription
  regeneration plus loading, empty, error, stale and fallback states.
- Responsive `focalPoint` image crop, native keyboard controls and zh-CN/en copy.
- `/api/v1/me/quota` display on today and account pages.
- Quota-exceeded UX for AI conversation requests.
- Email-scoped recent practice history for completed summaries.
- zh-CN/en i18n foundation without adding a heavy runtime dependency.
- Playwright learner smoke covering register -> onboarding -> practice -> quota
  consumed -> logout/login -> data preserved.
- Playwright V2 prescription coverage for a 390px viewport, keyboard regeneration,
  Lin Muen hero alt/crop and visible target/scene changes after backend recomposition.

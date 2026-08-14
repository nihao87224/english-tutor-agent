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

## SaaS-M6 Scope

- Email/password login and registration.
- Access-token authenticated API client with cookie-based refresh support.
- Route guard for learner onboarding, today practice, summary and account pages.
- `/api/v1/me/quota` display on today and account pages.
- Quota-exceeded UX for AI conversation requests.
- Email-scoped recent practice history for completed summaries.
- zh-CN/en i18n foundation without adding a heavy runtime dependency.
- Playwright learner smoke covering register -> onboarding -> practice -> quota
  consumed -> logout/login -> data preserved.

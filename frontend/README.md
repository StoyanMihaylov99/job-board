# Job Board — frontend

Angular 22 app for the Job Board API. See the root `README.md` for how to run the whole stack.

```bash
npm start      # dev server on http://localhost:4200 (proxies /api to :8080)
npm test       # Vitest unit tests
npm run build  # production build
```

## Structure
- `src/app/applications/` — dashboard page, pipeline bar, add/edit dialog, API service and models
- `src/app/shared/` — confirm dialog, status announcer (toast + live region), formatting helpers
- `src/styles.css` — design tokens and base styles (dark theme, amber accent)

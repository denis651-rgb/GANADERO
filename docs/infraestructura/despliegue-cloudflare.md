# Despliegue en Cloudflare Pages

- Directorio: `frontend-web`.
- Build: `npm ci && npm run build`.
- Salida: `dist`.
- Variables públicas: `VITE_API_URL`, `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, `VITE_AUTH_MODE=supabase`, `VITE_APP_ENV=production`.

No configure claves privadas. Registre el dominio y las rutas de recuperación/invitación en Supabase Auth. `_redirects` conserva el fallback SPA.

# Despliegue en Cloudflare Pages

- Directorio: `frontend-web`.
- Build: `npm ci && npm run build`.
- Salida: `dist`.
- Variables públicas: `VITE_API_URL`, `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`, `VITE_AUTH_MODE=supabase`, `VITE_APP_ENV=production`.
- `VITE_SUPABASE_ANON_KEY` se conserva como alternativa para proyectos antiguos; configure una sola clave pública.

No configure claves privadas. Registre el dominio y las rutas de recuperación/invitación en Supabase Auth. `_redirects` conserva el fallback SPA.

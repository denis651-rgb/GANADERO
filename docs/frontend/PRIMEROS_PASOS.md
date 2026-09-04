# Primeros pasos — Frontend

1. Iniciar el backend (ver `docs/backend/PRIMEROS_PASOS.md`), escuchando en el puerto 8080.
2. `cd frontend-web && npm install`
3. `npm run dev`
4. Abrir `http://localhost:5173`.

No hace falta configurar `.env.local`: por defecto el frontend apunta a `http://localhost:8080`
(`VITE_API_URL`, ver `frontend-web/.env.example`). No hay login: la app es de un solo
usuario local (`AuthContext` entrega un usuario fijo, sin llamadas de autenticación).

Para probarlo dentro de Electron en vez del navegador, ver `electron/README.md`.

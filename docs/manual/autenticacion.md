# Autenticación

Producción usa `VITE_AUTH_MODE=supabase`. Configure en Supabase las URLs permitidas para login, `/auth/restablecer-contrasena` y `/auth/aceptar-invitacion`. El backend valida issuer y JWKS; nunca recibe contraseñas del frontend.

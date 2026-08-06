# Primeros pasos

1. Copiar `.env.example` a `.env.local`.
2. Mantener `VITE_AUTH_MODE=mock` hasta que Supabase Auth esté configurado.
3. Iniciar el backend en el puerto 8080.
4. Ejecutar `npm install`.
5. Ejecutar `npm run dev`.
6. Ingresar a `http://localhost:5173`.
7. Comprobar que el panel muestre el estado de `/api/v1/system/status`.
8. Abrir Animales y comprobar el mensaje de endpoint pendiente.
9. Implementar la Fase 1 del backend.
10. Reemplazar los UUID manuales del formulario por selectores de propiedades y potreros.

## Despliegue Cloudflare Pages

- Framework: Vite.
- Comando: `npm install && npm run build`.
- Salida: `dist`.
- Variables: todas las `VITE_*` de producción.
- El archivo `public/_redirects` ya está incluido.

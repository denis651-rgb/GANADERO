# Arquitectura del frontend

## Responsabilidades

El frontend presenta la interfaz, formularios y estado visual. Spring Boot (local, embebido
en Electron) sigue siendo la única autoridad para las reglas de negocio; no hay lógica de
negocio ni acceso a datos directo desde React.

## Capas

```text
src/
├── app/          Enrutamiento, proveedores y layout
├── auth/         Usuario local fijo (sin login remoto ni sesión real)
├── features/     Módulos funcionales
└── shared/       API, componentes, hooks y estilos
```

## Flujo

```text
React → axios (http://127.0.0.1:<puerto>) → Spring Boot local → SQLite
```

En Electron, el proceso principal inyecta la URL base del backend embebido vía
`window.ganadero.apiBaseUrl` (ver `electron/src/preload.ts`); corriendo el frontend suelto
en el navegador se usa `VITE_API_URL` (por defecto `http://localhost:8080`).

## Reglas

1. No hay modo offline ni cola de sincronización: el backend local siempre está disponible.
2. Cada módulo se desarrolla verticalmente con backend, pantalla y pruebas.
3. Feedback al usuario tras una acción (crear/editar/eliminar/etc.):
   - **Éxito**: `showToast()` (`@/shared/toast/useToast`, ver skill `toast-guide`). Nunca un
     `<Alert>` inline además del toast para el mismo evento — es redundante (pasó antes en
     `ConfiguracionGeneralPage.tsx`, ver historial de git).
   - **Error, siempre**: `<Alert tone="danger">` inline, cerca de la acción que falló.
     Preferí leerlo directo de `mutation.error`/`query.error` (`normalizeApiError(error).message`);
     usá estado local (`useState`) solo cuando el error no sale de una única mutación (p. ej. un
     loop de varias llamadas, como en `FotosTab.tsx`).
   - Si en algún momento se necesita un formulario de carga repetida real (que se resetea y
     sigue montado para la siguiente entrada sin que el padre lo desmonte), un mensaje inline
     persistente puede justificarse ahí porque un toast de 4.5s se puede perder entre entradas
     sucesivas — pero hoy ningún formulario del proyecto se queda montado así; todos cierran
     apenas termina la acción, así que no hay excepción vigente a la regla de arriba.

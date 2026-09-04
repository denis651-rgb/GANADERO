# Ganadero Desktop (Electron)

Empaqueta el backend Spring Boot y el frontend React como una app de escritorio Windows,
con notificaciones nativas y ciclo de vida del backend embebido.

## Requisitos

- Node 22+, JDK 21 (con `jlink`/`jpackage`, es decir un JDK completo, no solo un JRE).
- `backend/` y `frontend-web/` con sus dependencias instaladas.

## Desarrollo

1. `cd frontend-web && npm run dev` (deja el servidor de Vite corriendo en `:5173`).
2. `cd electron && npm install && npm run dev`

En dev, Electron carga `http://localhost:5173` y lanza el backend usando el `java` del PATH
contra el jar en `backend/target/` (correr `mvn package` en `backend/` si no existe todavía).

## Empaquetado (instalador Windows)

```
cd electron
npm run dist
```

Esto compila el frontend (`vite build`), empaqueta el backend (`mvn package -DskipTests`),
genera un runtime Java recortado con jlink (`scripts/build-runtime.js`) y corre
`electron-builder` para producir `release/Ganadero Setup <version>.exe`.

Para regenerar solo el runtime (por ejemplo tras cambiar el JDK): `npm run package:runtime`.

### Sobre el runtime jlink

El set de módulos en `scripts/build-runtime.js` no sale solo de `jdeps` — el análisis estático
no detecta que Spring Data usa `javax.lang.model.SourceVersion` (módulo `java.compiler`) por
reflexión en su procesador AOT. El set actual fue verificado arrancando el jar real contra el
runtime generado. Si se agregan dependencias nuevas al backend, conviene volver a probar el
arranque completo (`java -jar backend/target/*.jar`) usando `electron/runtime/bin/java` antes
de asumir que el set de módulos sigue siendo suficiente.

## Estructura

- `src/main.ts` — proceso principal: ventana, bandeja del sistema, ciclo de vida.
- `src/backend.ts` — selección de puerto, arranque/parada del backend, recuperación de
  procesos huérfanos de un cierre abrupto anterior (Windows no liga el hijo al padre por sí solo).
- `src/notifications.ts` — sondea `/api/v1/alertas/pendientes-notificar` y dispara
  notificaciones nativas del OS.
- `src/frontend-protocol.ts` — sirve el build estático del frontend vía el esquema `app://`
  en producción (con fallback SPA a `index.html`).
- `src/preload.ts` — expone la URL base del backend al frontend (`window.ganadero.apiBaseUrl`).

## Pendiente / fuera de alcance de esta pasada

- El instalador no está firmado con un certificado de código real (electron-builder firma con
  un certificado de prueba); Windows SmartScreen mostrará advertencias hasta conseguir uno.
- Solo se armó el target Windows (nsis), tal como pide el plan ("al menos Windows").
- El guard local (PIN) ya tiene su almacenamiento y API en el backend, pero esta capa de
  Electron todavía no bloquea la ventana con una pantalla de PIN al iniciar.

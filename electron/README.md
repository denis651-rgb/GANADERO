# Ganadero Desktop (Electron)

## Google Calendar OAuth

La integración usa un cliente OAuth 2.0 de tipo **Aplicación de escritorio**, PKCE (`S256`) y un
callback temporal en `http://127.0.0.1:<puerto>/oauth2/callback`. La autorización siempre se abre
en el navegador predeterminado; la contraseña de Google nunca pasa por Ganadero.

1. En Google Cloud habilita Google Calendar API y crea un cliente OAuth de tipo `Desktop app`.
2. Descarga el JSON, renómbralo `google-oauth-client.json` y guárdalo en
   `%APPDATA%\Ganadero\google-oauth-client.json`.
3. También puede configurarse en desarrollo con `GANADERO_GOOGLE_CLIENT_ID` y, si Google lo
   entrega, `GANADERO_GOOGLE_CLIENT_SECRET`.
4. Abre `Mi finca → Configuración general → Google Calendar` y pulsa **Conectar Google**.

No se debe versionar el JSON real. El repositorio incluye `google-oauth-client.example.json` sólo
como referencia. Los tokens se guardan cifrados mediante `safeStorage` en el directorio de datos
de Ganadero y no se almacenan en SQLite ni se exponen al renderer.

Los únicos alcances solicitados son identidad básica (`openid email`) y
`https://www.googleapis.com/auth/calendar.app.created`, limitado a calendarios creados por la
aplicación. La creación del calendario y el consumo de la cola corresponden a la fase siguiente.

Empaqueta el backend Spring Boot y el frontend React como una app de escritorio Windows,
con notificaciones nativas y ciclo de vida del backend embebido.

## Requisitos

- Node 22+, JDK 21 (con `jlink`/`jpackage`, es decir un JDK completo, no solo un JRE).
- `backend/` y `frontend-web/` con sus dependencias instaladas.

## Desarrollo

Un solo comando: `cd electron && npm install && npm run dev:all`. Levanta Vite en `:5173`, compila
Electron, empaqueta el backend y abre la app cuando Vite responde. Al cerrar la ventana se detiene todo.

Alternativa en dos terminales:

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

### Riesgo asumido: auto-updater sin firma real

`updater.ts` deja `autoDownload = true` y `autoInstallOnAppQuit = true`: cada cliente instalado
revisa GitHub Releases al arrancar, descarga cualquier versión nueva publicada ahí y la instala
sola (al cerrar la app, o de inmediato si el usuario acepta el diálogo de "Reiniciar ahora"), sin
más verificación que HTTPS contra GitHub y el `latest.yml` que el propio electron-builder genera
en el momento de publicar. Como el instalador no está firmado con un certificado de código real
(punto anterior), esa cadena de confianza depende enteramente de quién tenga acceso de push al
repo y credenciales para publicar releases (`GH_TOKEN` en `npm run release`) — no hay una firma
independiente que un atacante con acceso al repo no pudiera también falsificar. Se acepta como
riesgo mientras el repo sea privado/controlado y no haya certificado de code-signing; si eso
cambia, hay que firmar el instalador antes de confiar en el auto-updater.

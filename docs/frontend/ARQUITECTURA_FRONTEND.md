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

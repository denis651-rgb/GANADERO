# Arquitectura del frontend

## Responsabilidades

El frontend presenta la interfaz, gestiona sesión, formularios, estado visual y almacenamiento local. Spring Boot continúa siendo la autoridad para empresa, permisos y reglas de negocio.

## Capas

```text
src/
├── app/          Enrutamiento, proveedores y layout
├── auth/         Supabase Auth y sesión
├── features/     Módulos funcionales
├── offline/      IndexedDB y cola local
├── sync/         Envío de operaciones pendientes
└── shared/       API, componentes, hooks y estilos
```

## Flujo conectado

```text
React → JWT Supabase → Spring Boot → PostgreSQL
```

## Flujo sin conexión

```text
Formulario → Dexie/IndexedDB → operación PENDING → sincronización → backend
```

## Reglas

1. No enviar `empresaId` desde los formularios de negocio.
2. No guardar `service_role` en variables VITE.
3. No acceder directamente desde React a tablas ganaderas.
4. Cada módulo se desarrolla verticalmente con backend, pantalla y pruebas.
5. El modo offline debe identificar operaciones con UUID e idempotencia.

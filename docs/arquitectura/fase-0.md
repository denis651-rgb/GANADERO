# Arquitectura de Fase 0

La Fase 0 conecta React con Supabase Auth y un backend Spring Boot que valida JWT, membresía activa, empresa, roles, permisos y propiedades. Las operaciones administrativas de Auth y Storage usan `service_role` exclusivamente desde backend.

El bootstrap público solo existe lógicamente cuando `APP_BOOTSTRAP_ENABLED=true`, exige token e idempotencia, crea el usuario externo antes de la transacción PostgreSQL y compensa únicamente usuarios creados por esa operación. En producción debe permanecer deshabilitado después del alta inicial.

Los despliegues de Render y Cloudflare deben realizarse desde `main` únicamente después de Backend CI, Frontend CI y Security.

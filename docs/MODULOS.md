# Reglas del monolito modular GANADERO

## Módulos iniciales

- `shared`: contratos técnicos comunes, respuestas API y utilidades sin negocio.
- `seguridad`: autenticación, autorización, membresías, roles y permisos.
- `empresas`: empresa ganadera y configuración.
- `propiedades`: establecimientos y alcance de usuarios.
- `animales`: registro, identificación y genealogía.
- `potreros`: sectores, potreros, agua, aforos y mantenimiento.
- `lotes`: lotes ganaderos y membresías.
- `movimientos`: cambios de potrero, lote y propiedad.
- `pesajes`: pesajes e indicadores productivos.
- `reproduccion`, `sanidad`, `inventario`, `alimentacion`, `comercial`, `finanzas`.
- `archivos`, `alertas`, `reportes`, `sincronizacion`, `auditoria`.

## Regla de dependencia

Un módulo no puede importar repositorios, entidades JPA ni clases de infraestructura de otro módulo.
La colaboración se realiza mediante:

1. Servicios públicos de la capa `application`.
2. DTO/contratos públicos mínimos.
3. Eventos de dominio cuando la operación no requiera respuesta inmediata.

## Estructura de cada módulo

```text
modulo/
├── api
├── application
├── domain
└── infrastructure
```

## Regla de base de datos

Flyway es la única autoridad para modificar el esquema. Hibernate usa `ddl-auto=validate`.

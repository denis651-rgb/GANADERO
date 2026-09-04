# Reglas del monolito modular GANADERO

## Módulos actuales

- `shared`: contratos técnicos comunes, respuestas API y utilidades sin negocio.
- `seguridad`: `LocalCurrentUserProvider`, único origen del usuario actual — app de un solo
  usuario local, sin login remoto ni matriz de roles/permisos.
- `propiedades`: configuración de la finca (fila única de settings) y sectores.
- `animales`: registro, identificación y genealogía.
- `potreros`: potreros, agua, aforos y mantenimiento.
- `lotes`: lotes ganaderos y membresías.
- `movimientos`: cambios de potrero, lote y propiedad.
- `pesajes`: pesajes e indicadores productivos.
- `reproduccion`, `sanidad`, `alertas`: ciclo reproductivo, sanidad animal y motor de alertas.
- `ventas`: registro de ventas de animales y su historial de precios.
- `alimentacion`: en desarrollo.
- `archivos`: almacenamiento local de imágenes/documentos (`LocalFileStorageClient`).
- `timeline`, `reportes`, `dashboard`, `auditoria`: soporte transversal y trazabilidad.

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

Flyway es la única autoridad para modificar el esquema (baseline único en
`backend/src/main/resources/db/migration`, SQLite).

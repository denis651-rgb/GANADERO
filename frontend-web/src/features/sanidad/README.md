# Módulo sanidad

Gestión de sanidad del hato: planes sanitarios, enfermedades, jornadas, casos clínicos y tratamientos con aplicaciones.

## Estructura

- `api.ts` — Tipos, labels y llamadas HTTP (enfermedades, planes + items + cálculo de próxima aplicación, jornadas con confirmación idempotente, casos clínicos, tratamientos con activación/regeneración/aplicaciones).
- `catalogs.ts` — `useSanidadCatalogs`: propiedades, potreros, categorías, lotes, usuarios y animales.
- `components/` — `ResumenPanel`, `PlanesPanel` (planes + items), `EnfermedadesPanel`, `JornadasPanel` + `JornadaPrepararModal` (selección de animales) + `JornadaConfirmarModal` (operationId con `crypto.randomUUID()`), `CasosPanel`, `TratamientosPanel` (protocolo de dosificación dinámico) + `AplicacionesModal`.
- `pages/SanidadPage.tsx` — Pestañas (Resumen, Planes sanitarios con sub-pestañas Planes/Enfermedades, Jornadas, Casos clínicos, Tratamientos) con queries compartidas e invalidación en `refresh()`.

## Notas

- `confirmarJornada` requiere `operationId` generado por el cliente para idempotencia.
- No existe aún un catálogo de productos sanitarios: los items de plan y tratamientos usan texto libre (`productoRecomendadoTexto`) y envían `productoId` como `undefined`.

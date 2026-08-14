# Módulo reproduccion

Gestión reproductiva del hato: detección de celo, servicios (monta e inseminación), diagnósticos de gestación, partos con crías, abortos y destetes.

## Estructura

- `api.ts` — Tipos, labels y llamadas HTTP:
  - `listCelos` / `registrarCelo` / `anularCelo` (`POST /celos/{id}/anular` con `version`).
  - `listServicios` / `registrarServicio` (el backend auto-incrementa `numeroIntento` y calcula `fechaDiagnosticoRecomendada`).
  - `listDiagnosticos` / `registrarDiagnostico` (resultado `POSITIVO` calcula `fechaProbableParto` y actualiza el servicio asociado).
  - `listPartos` / `registrarParto` (crea crías y, si `crearAnimal`, también el animal + parentesco + pesaje de nacimiento).
  - `listAbortos` / `registrarAborto` · `listDestetes` / `registrarDestete`.
- `catalogs.ts` — `useReproduccionCatalogs`: propiedades, potreros, lotes, usuarios y animales filtrados por sexo (`hembras`, `machos`, `animales`).
- `components/` — `ResumenPanel`, `CelosPanel`, `ServiciosPanel`, `DiagnosticosPanel`, `PartosPanel` (con filas dinámicas de crías), `AbortosPanel`, `DestetesPanel`.
- `pages/ReproduccionPage.tsx` — Pestañas (Resumen, Celos, Servicios, Diagnósticos, Partos y crías con sub-pestañas Partos/Abortos/Destetes) con queries compartidas e invalidación en `refresh()`.

## Notas de idempotencia

- `celos`, `servicios` y `diagnosticos` reciben `clienteUuid` generado por el cliente (`crypto.randomUUID()`): ante un duplicado, el servidor devuelve el registro existente.
- `partos`, `abortos` y `destetes` generan el id en el servidor; se usa el header `Idempotency-Key`.

# Reportes — hoja de ruta

Estado real (no "PROXIMAMENTE"): el módulo `reportes` está `LISTO` y funcionando.
`ReportesPage.tsx` ya tiene selector de período (trimestral/semestral/anual/personalizado,
`features/reportes/periodo.ts`) y tres reportes con exportación CSV
(`features/reportes/csv.ts`, sin dependencias, con BOM UTF-8 para Excel):

- Nacimientos (`GET /api/v1/reportes/nacimientos`)
- Muertes (`GET /api/v1/reportes/muertes`)
- Ventas (`GET /api/v1/reportes/ventas`)

Lo que falta son los otros tres bloques (Productividad, Reproducción, Sanidad) y varias
vistas de Ganado. Este documento es el mapa de qué existe para reutilizar y qué es
trabajo nuevo, para no reconstruir lo que ya está.

## Bloques propuestos

```text
REPORTES
Ganado
 ├ Inventario actual
 ├ Altas/bajas
 ├ Existencia por categoría
 ├ Existencia por potrero
 └ Existencia por lote

Productividad
 ├ Evolución de peso
 ├ GDP
 ├ Peso por categoría
 └ Animales sin pesaje

Reproducción
 ├ Preñez
 ├ Servicios
 ├ Partos
 ├ Abortos
 └ Destetes

Sanidad
 ├ Cumplimiento de plan
 ├ Vacunaciones
 ├ Tratamientos
 ├ Pendientes
 └ Períodos de retiro
```

## Ganado

Existencia por categoría/potrero/lote **ya está calculada**, pero en `dashboard`, no en
`reportes`, y solo como foto de "ahora": `JdbcDashboardRepository.animalesPorCategoria /
animalesPorPotrero / animalesPorLote` (agrupa `animal` con `estado='ACTIVO'`). No acepta
rango de fechas — para que sea un reporte real hay que reescribir esas consultas
parametrizadas por una fecha de corte (no se puede reconstruir inventario histórico solo
con el estado actual del animal salvo que se derive de `movimiento`/`evento_animal`, hay
que decidir el enfoque al implementar).

Altas: `reportes.nacidos()` ya existe (`origen='NACIDO'`). Falta altas por compra —
la fuente está en `compras` (`Compra`/`CompraDetalle`) y en `movimientos`
(`TipoMovimiento.INGRESO_COMPRA`).

Bajas: `reportes.muertos()` (vía `evento_animal`, no hay tabla dedicada — ver el
comentario en `JdbcReporteRepository.java:20-27`) y el reporte de `ventas` ya cubren las
dos causas principales. No hay bajas agregadas por otros tipos de movimiento.

`AnimalController.resumen()` da conteos filtrados (activos/hembras/machos) pero no
desgloses por dimensión — para eso ya están las `Distribucion` de dashboard.

## Productividad

`pesajes` ya tiene bastante:

- `animalesSinPesaje` / `countAnimalesSinPesaje` (`JdbcPesajeRepository.java:208,224`) — ya
  paginado, reutilizable tal cual.
- `indicadorLote(loteId)` (`JdbcPesajeRepository.java:170`) — promedio/mín/máx de peso por
  lote.
- `PesajeIndicadorService.indicadorAnimal(animalId)` — GDP punto a punto (últimos dos
  pesajes activos) + lista de evolución; es lo que la UI de pesajes llama "curva de
  crecimiento" (no es un concepto de backend, es la lista `evolucion` graficada).

Falta (trabajo nuevo real):

- **GDP de rebaño por período.** El stub `JdbcDashboardRepository.gananciaDiaria()` ya no existe:
  se quitó del dashboard junto con su indicador y con el aviso de ganancia negativa, porque los
  animales no se pesan todos los días y el promedio del rebaño no era confiable. Si Reportes
  lo necesita, hay que calcularlo aparte y por período entre pesajes reales.
- **Peso por categoría** — no existe ninguna agregación de peso agrupada por categoría en
  ningún lado; hay que escribirla.

## Reproducción

El dominio ya está completo: `celo`, `servicio`, `diagnostico_gestacion`, `parto`,
`cria_parto`, `aborto`, `destete`, encadenados por `gestacion_ciclo`
(`ReproduccionCicloService`, migración V11). No hay que modelar nada nuevo, solo escribir
consultas de agregación por período: conteos por resultado, % de preñez, servicios por
resultado, partos/abortos/destetes por rango de fechas. Es el bloque más barato de
construir porque todos los datos ya existen y son consistentes (el ciclo de gestación ya
garantiza que un servicio no se reutilice, que solo haya una gestación abierta por
hembra, etc. — ver `docs/backend/CONTROL_GESTACIONES.md`).

## Sanidad

Mismo caso que Reproducción: plan sanitario, `aplicacion_sanitaria` (estados
`APLICADO/NO_APLICADO/APLICADO_PARCIAL/RECHAZADO/POSPUESTO/ANULADO`),
`aplicacion_tratamiento` (estados `PENDIENTE/APLICADA/OMITIDA/ATRASADA/CANCELADA`), y
fechas de retiro de carne/leche por aplicación — todo ya está en tablas. Falta agregación
por estado/plan/período: cumplimiento de plan (% aplicado vs. programado), vacunaciones y
tratamientos por período, pendientes (ya existe una proyección tipo calendario en
`ProyectarCalendarioSanitarioService`, pero no una tasa de cumplimiento), y períodos de
retiro vigentes/próximos a vencer.

## Exportación

Hoy solo existe CSV, client-side, sin librerías (`features/reportes/csv.ts`). No hay PDF
ni Excel real (.xlsx) en todo el proyecto — cero dependencias de iText/jsPDF/Apache
POI/exceljs en `pom.xml` ni en `package.json`.

- **Excel/CSV**: ya resuelto, el CSV actual abre bien en Excel (BOM UTF-8 incluido).
- **PDF + imprimir**: usar impresión nativa del navegador (CSS de impresión +
  `window.print()`) en vez de sumar una librería — cubre "imprimir" y "guardar como PDF"
  sin dependencias nuevas. Si más adelante se quiere un botón "Descargar PDF" que genere
  el archivo sin pasar por el diálogo de impresión, ahí sí evaluar una librería (pasar por
  la skill `pick-ui-library` antes de elegir una).

## Arquitectura propuesta

Seguir el patrón ya usado en el resto del backend (`JdbcClient` + SQL crudo, sin JPA,
confirmado: cero `@Entity`/`extends JpaRepository` en todo `bo.com.ganadero`): un
repositorio dedicado por bloque dentro del módulo `reportes`
(`ReporteGanadoRepository`, `ReporteProductividadRepository`,
`ReporteReproduccionRepository`, `ReporteSanidadRepository`), parametrizados por
`desde`/`hasta`, reutilizando `periodo.ts` del frontend tal cual está.

**Punto abierto a resolver antes de implementar:** `docs/backend/MODULOS.md` establece
como regla que "un módulo no puede importar repositorios, entidades JPA ni clases de
infraestructura de otro módulo". El módulo `dashboard` ya hace consultas SQL crudas
contra tablas de `animales`/`potreros`/`lotes` sin importar clases Java de esos módulos
(por eso no rompe `ModularityTest`, que verifica dependencias a nivel de paquete/clase,
no de esquema de base de datos) — es el precedente directo a seguir para `reportes`. Pero
es una zona gris frente a la regla escrita, que parece apuntar más a la intención que a
la letra. Decidir explícitamente si `reportes` sigue el mismo camino (SQL crudo
transversal) o si en cambio cada módulo dueño (`reproduccion`, `sanidad`, `pesajes`,
`animales`) expone sus propias consultas de agregación en su capa `application` y
`reportes` solo las compone — más alineado con la regla escrita, pero más trabajo y toca
código de otros módulos en vez de aislarlo todo en `reportes`.

## Orden sugerido

1. **Reproducción** y **Sanidad** — mayor valor, dominio ya modelado, cero riesgo de tocar
   código existente.
2. **Productividad** — en paralelo, aprovechar para implementar el GDP de rebaño real
   (hoy stub `null`) y peso por categoría.
3. **Ganado** (existencia por fecha) — al final, porque implica reescribir consultas que
   ya existen en `dashboard` en vez de crear desde cero; conviene decidir primero el punto
   abierto de arquitectura de arriba, ya que afecta directamente a este bloque.

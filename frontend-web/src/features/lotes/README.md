# Módulo lotes

Mover un lote confirma un único registro en `movimiento`, con un detalle por animal en
`movimiento_detalle`. La preparación conserva el ID resultante y los eventos del timeline
de cada animal referencian ese mismo movimiento. Preparar o cancelar no confirma traslados.

El detalle del lote muestra dos historiales distintos:

- Entradas y salidas: membresías de los animales.
- Movimientos del lote: consulta paginada al registro canónico, incluyendo cambios de
  potrero que conservan las membresías. No crea otra tabla de traslados.

`GET /api/v1/movimientos?loteId=…` filtra por lote de origen/destino en la cabecera o en
los detalles históricos de los animales. Cada movimiento aparece una sola vez. El enlace
`/movimientos?movimientoId=…` abre su detalle y los animales afectados, aunque no figure
en la página actual del listado. Se requiere el permiso `MOVIMIENTO_VER`.

Al confirmar desde el asistente se invalidan las consultas del lote, movimientos,
animales y sus timelines para mostrar los datos actualizados.

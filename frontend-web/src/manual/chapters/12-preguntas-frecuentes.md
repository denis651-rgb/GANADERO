# Preguntas frecuentes

Este capítulo no sigue la plantilla de `CONVENCIONES.md` — es una lista de
preguntas y respuestas cortas, agrupadas por tema. En cada respuesta se enlaza
al capítulo donde está explicado el procedimiento completo.

## ¿Necesito conexión a Internet para usar GANADERO?

La aplicación funciona sin conexión para las tareas diarias del
establecimiento. Las operaciones que dependen de sincronización, como los
respaldos o Google Calendar, necesitan conexión. Ver [Primeros pasos](./01-primeros-pasos.md#antes-de-comenzar).

## ¿Cómo configuro mi finca al empezar?

Completa los datos de la propiedad y revisa las **Categorías por edad** desde
**Mi finca**. Ver [Mi finca](./02-mi-finca.md#configuracion-general).

## ¿Cómo registro un animal recién comprado?

Desde **Animales**, con el botón **Nuevo animal** y el origen **Comprado**, o
de una sola vez con el **Ingreso por lote de compra**. Ver [Registrar un animal](./03-animales.md#registrar-un-animal)
y [Ingreso por lote de compra](./03-animales.md#ingreso-por-lote-de-compra).

## ¿Qué significan las categorías por edad y por qué cambian?

Clasifican a los animales por sexo y edad, y se ajustan al cruzar los límites
de edad; se pueden modificar desde **Mi finca**. Ver [Categorías por edad](./02-mi-finca.md#categorias-por-edad).

## ¿Cómo muestro el código QR de un animal?

Desde la **ficha del animal**, en la sección de identificadores. Ver
[Identificadores y código QR](./03-animales.md#identificadores-y-codigo-qr).

## ¿Se puede registrar una compra de varios animales a la vez?

Sí, con la **Compra por lote**: compras un grupo completo y luego se asignan
los potreros y categorías. Ver [Compra por lote](./04-compras.md#compra-por-lote).

## ¿Cómo vendo un animal con su peso de salida?

En **Ventas → Registrar venta → Un animal**, eliges un **peso ya registrado**
o registras uno nuevo. Ver [Venta individual](./11-ventas.md#venta-individual).

## ¿Qué diferencia hay entre vender "En pie" y "Carneado"?

**En pie** cobra un precio por cabeza para todos los animales del lote;
**Carneado** cobra por kilo según el peso de cada uno. Ver [Venta por lote](./11-ventas.md#venta-por-lote).

## ¿Los animales vendidos desaparecen del inventario?

Al registrar la venta, el sistema genera el movimiento de salida
(**SALIDA_VENTA**) y el animal se retira; el historial queda en **Ventas** y
**Movimientos**. Ver [Resultado esperado](./11-ventas.md#resultado-esperado).

## ¿Por qué conviene agrupar animales en lotes?

Permite manejar, mover, pesar y controlar en conjunto a los animales que
comparten un mismo manejo, con cupo máximo y estado registrado. Ver [Crear un lote](./05-lotes.md#crear-un-lote).

## ¿Cómo agrego o retiro animales de un lote?

Dentro de la **ficha del lote** se usan las opciones de agregar y retirar
animales. Ver [Agregar animales al lote](./05-lotes.md#agregar-animales-al-lote)
y [Retirar animales del lote](./05-lotes.md#retirar-animales-del-lote).

## ¿Qué diferencia hay entre "mover un lote" y registrar un movimiento?

Mover el lote desplaza a todo el grupo dentro del establecimiento con un
asistente; los **Movimientos** registran traslados y cambios que se deben
validar y confirmar. Ver [Mover el lote](./05-lotes.md#mover-el-lote) y
[Registrar un movimiento](./06-movimientos.md#registrar-un-movimiento).

## ¿Por qué un movimiento queda pendiente?

Los movimientos se crean en estado **PENDIENTE** y recién se aplican al
validarse y confirmarse. Ver [Validar y confirmar un movimiento](./06-movimientos.md#validar-y-confirmar-un-movimiento).

## ¿Me equivoqué en un movimiento, puedo anularlo?

Sí: puedes anular un movimiento confirmado o deshacer la última confirmación.
Ver [Anular un movimiento](./06-movimientos.md#anular-un-movimiento).

## ¿Cómo peso a todo un lote de una vez?

Abriendo **Pesaje por lote** desde Pesajes: registras en un solo paso los
pesos de todos los animales del lote. Ver [Pesaje por lote](./07-pesajes.md#pesaje-por-lote).

## ¿Cuándo conviene registrar un pesaje?

Registra el peso en cada etapa clave (destete, recría, engorde) y siempre que
vayas a usar el peso para decidir una venta. Ver [Pesajes](./07-pesajes.md#para-que-sirve).

## ¿El peso de venta debe ser reciente?

Sí: para vender, el último pesaje no debe tener más de **30 días**; si es más
antiguo, el sistema lo avisa y conviene registrar un peso nuevo. Ver [Venta individual](./11-ventas.md#venta-individual).

## ¿Cómo sé cuándo una vaca está en celo?

Se registra el **celo** cuando se detecta y, si corresponde, se asocia un
**servicio**; el sistema también sugiere la fecha de diagnóstico. Ver [Celo](./08-reproduccion.md#celo)
y [Servicio](./08-reproduccion.md#servicio).

## ¿Cuándo se confirma una gestación?

Con el **diagnóstico de gestación** posterior al servicio; el sistema
recomienda la fecha de diagnóstico según el servicio registrado. Ver
[Diagnóstico de gestación](./08-reproduccion.md#diagnostico-de-gestacion).

## ¿Cómo registro las crías nacidas?

Con el **parto**: si marcas "Crear registro del animal en el inventario", la
cría queda dada de alta automáticamente. Ver [Parto](./08-reproduccion.md#parto).

## ¿Cómo planifico las actividades sanitarias del año?

Creas un **plan sanitario** con las actividades y luego las ejecutas como
jornadas por animal o por lote. Ver [Plan sanitario](./09-sanidad.md#plan-sanitario).

## ¿Cómo aplico una vacunación a todo un lote?

Desde **Jornada sanitaria**: eliges los animales y las actividades, y
ejecutas la jornada registrando los resultados. Ver [Jornada sanitaria](./09-sanidad.md#jornada-sanitaria).

## ¿Las actividades sanitarias se reflejan en un calendario?

Sí: las actividades activas se muestran en **Calendario** y pueden
sincronizarse con **Google Calendar**. Ver [Calendario y Google Calendar](./09-sanidad.md#calendario-y-google-calendar).

## ¿Qué significan las prioridades de una alerta?

Cada alerta tiene una prioridad que indica qué tan urgente es atenderla:
**INFO**, **WARNING**, **URGENTE** o **CRITICA**. Ver [Revisar el centro de alertas](./10-alertas.md#revisar-el-centro-de-alertas).

## ¿Cómo programo un aviso propio, por ejemplo una vacunación?

Con **Programar recordatorio** desde Alertas, indicando evento, primera
notificación, cantidad de avisos e intervalo. Ver [Programar recordatorios](./10-alertas.md#programar-recordatorios).

## ¿Cómo aseguro mis datos?

Crea respaldos desde **Mi finca → Respaldos**; los archivos quedan guardados
en tu Drive. Ver [Respaldos](./02-mi-finca.md#respaldos).

## ¿Puedo proteger la aplicación con un PIN?

Todavía no. Puedes guardar un PIN en **Mi finca** → **Configuración general**,
pero por ahora Ganadero no lo pide al abrirse, así que no protege el acceso.
Protege el equipo con la clave de inicio de sesión de Windows. Ver
[Configuración general](./02-mi-finca.md#configuracion-general).
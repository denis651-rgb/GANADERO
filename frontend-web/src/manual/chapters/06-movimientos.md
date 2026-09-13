# Movimientos

El módulo **Movimientos** registra cada traslado de animales entre
propiedades, potreros y lotes: cambios de potrero, cambios de lote, traslados
entre propiedades e ingresos y salidas de cuarentena. Cada movimiento pasa por
un estado (**PENDIENTE**, **CONFIRMADO**, **ANULADO** o **REVERTIDO**) y solo
la confirmación aplica el cambio en los animales, por lo que el flujo queda
siempre auditado.

Los movimientos de compra y venta no se crean desde aquí: se generan solos al
registrar una compra o una venta en sus propios módulos. Acá se gestionan los
traslados.

## Registrar un movimiento

### Para qué sirve

Crear un movimiento pendiente que, una vez confirmado, cambia la ubicación de
los animales seleccionados. Se indican el origen (la propiedad y, de forma
opcional, el potrero o el lote), el destino que corresponda al tipo y los
animales a mover.

### Antes de comenzar

- Los animales deben estar **ACTIVOS** y pertenecer a la **Propiedad** de
  origen que se elija.
- Decidir el tipo de movimiento y su destino:
  - **CAMBIO_POTRERO**: cambia de potrero (ej. rotación de pastoreo). Pide
    potrero de destino, distinto del de origen.
  - **CAMBIO_LOTE**: cambia de lote. Pide lote de destino, distinto del de
    origen.
  - **TRANSFERENCIA_PROPIEDAD**: traslada a otra propiedad. Pide propiedad de
    destino (distinta de la de origen) y potrero de destino.
  - **CUARENTENA**: ingreso de animales a un potrero de cuarentena.
  - **RETORNO_CUARENTENA**: salida de cuarentena a su potrero.

### Procedimiento

1. En el menú lateral abre **Movimientos** y presiona **Nuevo movimiento**.
2. En **Tipo**, elige la opción que corresponda (cambio de potrero, cambio de
   lote, transferencia entre propiedades, cuarentena o retorno de cuarentena).
3. Revisa la **Fecha** (viene con el día actual) y completa el **Motivo** y
   la **Observación** si quieres.
4. En **Origen (propiedad)** elige la propiedad actual de los animales.
   **Origen (potrero)** y **Origen (lote)** son opcionales: sirven para
   limitar la lista de animales disponibles.
5. Completa el **Destino** que pida el tipo elegido:
   - **Destino (potrero)** para un cambio de potrero y para cuarentena o
     retorno de cuarentena.
   - **Destino (lote)** para un cambio de lote.
   - **Destino (propiedad)** y **Destino (potrero)** para una transferencia
     entre propiedades.
6. En **Animales a mover**, marca con la casilla a los animales que se llevan.
   Usa el buscador "Buscar por código o nombre…" y revisa el contador de
   **seleccionados** y **disponibles**. Sin propiedad de origen seleccionada,
   no se muestra la lista.
7. Presiona **Crear movimiento**.

### Ejemplo

Los 12 novillos del **Potrero gordo** (propiedad El Roble) pasan al **Potrero
playa** por descanso del potrero. Se crea un movimiento de tipo
**CAMBIO_POTRERO** con origen **Potrero gordo**, destino **Potrero playa**,
motivo "Rotación de pastoreo por descanso" y los 12 novillos seleccionados.

### Resultado esperado

Aparece el mensaje **"Movimiento creado correctamente."** y el movimiento
queda en la lista con su **Tipo**, estado **PENDIENTE**, **Fecha**, **Origen**
y **Destino**. En este punto todavía no cambia nada en los animales: la
confirmación es el paso que los aplica.

## Validar y confirmar un movimiento

### Para qué sirve

Comprobar, animal por animal, que el movimiento es válido (estado, ubicación
y restricciones como cuarentena o capacidad del potrero de destino) antes de
aplicarlo. Solo un movimiento confirmado modifica la ubicación de los
animales.

### Antes de comenzar

Tener el movimiento creado en estado **PENDIENTE**. Si el potrero de destino
tiene configurada una **Capacidad recomendada**, la validación también
verificará la ocupación proyectada.

### Procedimiento

1. En **Movimientos**, abre con **Detalle** el movimiento **PENDIENTE**.
2. Presiona **Validar** (o directamente **Confirmar**). El sistema revisa cada
   animal y abre la ventana **Validación del movimiento** con **Total**,
   **Válidos**, **Inválidos** y **Resultado** (**VALIDO** o **RECHAZADO**).
3. Revisa la tabla por animal. Si hay inválidos, se muestra el **Estado**
   (**VALIDO** / **INVALIDO**) y el **Mensaje** del motivo; cuando corresponde,
   desde la **Acción** puedes ir a resolverlo, por ejemplo **Registrar prueba
   diagnóstica** para un animal en cuarentena que aún no la tiene.
4. Si el potrero de destino tiene **Capacidad recomendada**, la validación
   muestra **Capacidad recomendada**, **Ocupación actual**, **Ingreso** y
   **Ocupación proyectada** (en UA). Si la ocupación proyectada supera la
   capacidad, aparece un aviso: se puede **Continuar de todas formas** bajo
   la responsabilidad de quien confirma.
5. Con todo válido, presiona **Confirmar movimiento** (el botón queda
   deshabilitado si hay animales inválidos).

### Ejemplo

El movimiento de los 12 novillos al **Potrero playa** se valida: los 12
resultan **VÁLIDOS**. El potrero tiene una **Capacidad recomendada** de 20 UA,
una **Ocupación actual** de 12 UA y con el **Ingreso** de 10 novillos la
**Ocupación proyectada** queda en 22 UA: aparece el aviso y se presiona
**Continuar de todas formas**.

### Resultado esperado

Aparece el mensaje **"Movimiento confirmado."**, el movimiento pasa a
**CONFIRMADO** con su **Fecha de confirmación**, y cada animal cambia a su
nueva ubicación. El traslado queda registrado en la auditoría y en la línea
de tiempo de cada animal.

> Mientras está **PENDIENTE**, el movimiento no modifica los animales. La
> validación no se puede saltar: con inválidos el botón de confirmar está
> deshabilitado.

## Anular un movimiento

### Para qué sirve

Cancelar un movimiento que está **PENDIENTE** antes de que se aplique, por
ejemplo porque se creó con datos equivocados.

### Antes de comenzar

El movimiento debe estar en estado **PENDIENTE** (no se anulan movimientos ya
confirmados; esos se deshacen con la opción de revertir).

### Procedimiento

1. Abre con **Detalle** el movimiento **PENDIENTE** y presiona **Anular**.
2. En la ventana **Anular movimiento**, escribe el **Motivo (obligatorio)**;
   se necesita al menos 3 caracteres.
3. Presiona **Anular movimiento**.

### Ejemplo

Se creó por error un cambio de potrero para los novillos ya estaba en el
**Potrero playa**. Se anula con el motivo "Los animales ya estaban en ese
potrero; movimiento creado por error".

### Resultado esperado

El movimiento pasa a **ANULADO** y el detalle muestra **Cancelado el** (fecha
y hora) con el **Motivo de anulación**. No se puede confirmar, y los animales
no sufren cambios.

## Deshacer un movimiento

### Para qué sirve

Revertir un movimiento ya **CONFIRMADO**: los animales vuelven a la ubicación
que tenían antes del movimiento, dejando constancia de quién y por qué lo
deshizo.

### Antes de comenzar

El movimiento debe estar **CONFIRMADO**. Revisa bien el detalle: el texto lo
advierte: "Deshacer revierte los cambios de este movimiento en los animales.
Cerrar solo cierra esta ventana."

### Procedimiento

1. Abre con **Detalle** el movimiento **CONFIRMADO** y presiona **Deshacer
   movimiento**.
2. En la ventana **Revertir movimiento**, escribe el **Motivo (obligatorio)**.
3. Presiona **Revertir movimiento**.

### Ejemplo

El cambio de potrero de los 10 novillos se aplicó el mismo día pero la rotación
se descartó: se deshace con el motivo "Se cambió el plan de pastoreo; los
animales vuelven a su potrero anterior".

### Resultado esperado

Aparece el mensaje **"Movimiento revertido."**, el movimiento pasa a
**REVERTIDO** con **Deshecho el** (fecha, hora y motivo), y cada animal vuelve
a la ubicación anterior al movimiento. El detalle deja ver los botones **Ver
movimiento inverso** y **Ver movimiento revertido** para seguir la pareja.

## Problemas frecuentes

### El movimiento quedó PENDIENTE y los animales no cambiaron

Los cambios se aplican al confirmar. Abre el detalle del movimiento y presiona
**Confirmar** para validarlo y confirmarlo. Un movimiento **PENDIENTE** o
**ANULADO** no modifica a los animales.

### No puedo confirmar porque hay animales no válidos

La **Validación del movimiento** lista cada animal con su **Estado** y
**Mensaje**. Resuelve el motivo señalado: por ejemplo, un animal en cuarentena
sin prueba diagnóstica necesita que desde la **Acción** registres la
**prueba diagnóstica** (en [Sanidad](./09-sanidad.md)) y después vuelvas a
validar.

### Me avisa que superaré la capacidad del potrero de destino

El potrero de destino tiene una **Capacidad recomendada** y la **Ocupación
proyectada** la excede. La validación lo informa y deja **Continuar de todas
formas** bajo tu responsabilidad; el sistema no bloquea el movimiento.

### No aparecen animales en "Animales a mover"

Primero elige la **Origen (propiedad)**; sin ella no se muestra la lista. La
lista contiene solo animales **ACTIVOS** de esa propiedad (y del potrero o
lote indicado, si los filtraste). Revisa el estado y la propiedad del animal
en **Animales**.

### Anulé o revertí un movimiento por error

No se deshace desde la interfaz. Si lo anulaste, créalo de nuevo; si lo
revertiste, registra el movimiento correcto. Tanto las anulaciones como las
reversiones quedan en la auditoría.

### No encuentro la opción de compra o venta en el Tipo

Los movimientos de compra y de venta se generan desde los módulos **Compras**
y **Ventas** al registrar cada operación. En **Movimientos** se crean los
traslados: cambio de potrero, cambio de lote, transferencia entre propiedades
y cuarentena.
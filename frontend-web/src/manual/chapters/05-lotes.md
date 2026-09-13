# Lotes

El módulo **Lotes ganaderos** sirve para agrupar animales por manejo: un
lote es una agrupación operativa de animales que comparten una finalidad
(por ejemplo "novillos de engorde" o "vacas de recría"). Cada lote pertenece
a una propiedad, puede tener un límite de animales, y registra el historial
de quién entra y quién sale, además de los movimientos de traslado del grupo.

Este capítulo cubre el ciclo completo: crear el lote, consultarlo, sumar o
retirar animales, trasladarlo con el asistente de **Mover lote** y cerrarlo.

## Crear un lote

### Para qué sirve

Crear la agrupación donde se irán agregando animales. El lote queda ligado a
una propiedad y, opcionalmente, con una **Cantidad máxima de animales** para
controlar su ocupación.

### Antes de comenzar

- Tener una propiedad **ACTIVA** donde funcionará el lote.
- Decidir el nombre y, si quieres, la descripción y la fecha de apertura.
- Decidir si el lote tendrá cupo máximo. Si lo configuras, no se podrá
  superar.

### Procedimiento

1. Abre **Lotes ganaderos** en el menú lateral.
2. Presiona **Nuevo lote**.
3. Elige la **Propiedad** y escribe el **Nombre** (por ejemplo "Lote de
   engorde A").
4. Completa la **Descripción** y la **Fecha de apertura** si corresponden.
5. En **Cantidad máxima de animales**, deja el campo vacío para "sin límite"
   o escribe el cupo deseado.
6. Presiona **Crear lote**.

**Cambiar la cantidad máxima**

1. Abre la ficha del lote y presiona **Configurar cantidad máxima** en la
   tarjeta de **Ocupación del lote**.
2. Escribe el nuevo **Cantidad máxima de animales**. No puede ser menor que
   la ocupación actual.
3. Presiona **Guardar máximo**.

### Ejemplo

La estancia "El Roble" crea el **Lote de engorde A** en la propiedad El
Roble, con descripción "Novillos para vender en diciembre", fecha de
apertura 15/09/2026 y cantidad máxima de 60 animales.

### Resultado esperado

El lote aparece en **Lotes ganaderos** con su código asignado (por ejemplo
**LOT-2026-0001**), el estado **ACTIVO**, su **Propiedad**, la **Apertura**
y la ocupación **0 / 60 animales** con **60 cupos disponibles**.

> El código lo asigna el sistema: **LOT-{año}-{número}** (por ejemplo
> **LOT-2026-0001**). No hace falta prepararlo.

## Consultar la ficha del lote

### Para qué sirve

Ver en una sola pantalla dónde está el lote, cuántos animales tiene, quiénes
lo integran, el historial de entradas y salidas y los movimientos que
recorrió, y aplicar las acciones como agregar, retirar, mover, registrar un
control sanitario o cerrarlo.

### Antes de comenzar

Tener el lote creado. En **Lotes ganaderos**, presiona **Ver** sobre el lote
para abrir su ficha.

### Procedimiento

1. Abre el lote con **Ver**. El título muestra el código y el nombre.
2. Revisa los datos principales: **Propiedad**, **Potrero operativo actual**
   (si no hay un potrero único para todo el lote muestra **Mixto o sin
   definir**), **Estado**, **Apertura** y **Cierre**.
3. En **Ocupación del lote** se ve la cantidad de animales frente al cupo y
   los **cupos disponibles**.
4. En **Animales del lote** están los integrantes actuales con su **Ingreso**
   y **Motivo**. Puedes retirar uno presionando el ícono de la papelera o
   varios con **Retirar seleccionados (n)**.
5. Si el lote tuvo salidas, el **Historial de entradas y salidas** muestra
   el **Ingreso**, la **Salida**, el **Motivo de ingreso** y el **Motivo de
   salida** de cada animal.
6. **Movimientos del lote** lista los traslados del grupo. Con **Ver
   movimiento y animales** se abre el detalle del módulo
   [Movimientos](./06-movimientos.md).
7. Desde los botones superiores también puedes **Registrar control
   ectoparasitario** (todo el lote, ver [Sanidad](./09-sanidad.md)),
   **Mover lote** o **Cerrar lote**. Los botones de mover, cerrar y
   agregar/retirar solo están disponibles mientras el lote esté **ACTIVO**.

### Ejemplo

En la ficha de **LOT-2026-0001** (Lote de engorde A) se ven 12 animales en
la propiedad El Roble, el potrero **Potrero gordo**, la apertura del
15/09/2026 y la ocupación **12 / 60 animales**.

### Resultado esperado

La ficha reúne la información operativa del lote y todas sus acciones. Cada
entrada, salida y movimiento queda reflejado en las secciones
correspondientes y en la auditoría.

> Cuando el lote está **CERRADO**, se muestra el aviso "Este lote está
> cerrado y no admite más animales" y las opciones de modificar integrantes
> desaparecen.

## Agregar animales al lote

### Para qué sirve

Incorporar animales al lote en una sola operación, indicando fecha, motivo y
observaciones. La ocupación se actualiza y se respeta el cupo máximo.

### Antes de comenzar

- Los animales deben estar **ACTIVOS** y pertenecer a la misma **Propiedad**
  del lote.
- No deben ser integrantes actuales del lote. Si un animal ya pertenece a
  otro lote, aparece con un aviso y, al agregarlo, se moverá a este.

### Procedimiento

1. Abre la ficha del lote y presiona **Agregar animales**.
2. Busca con el campo "Buscar por código o nombre…" y marca con la casilla a
   los animales que quieras. El contador muestra cuántos llevas.
3. Revisa los **cupos disponibles**: si la selección supera el máximo, la
   pantalla lo avisa y no se incorporará ningún animal.
4. En **Modo**, elige:
   - **Parcial (procesa el resto)**: agrega los que pueda y avisa cuáles
     no entraron.
   - **Atómico (todo o nada)**: si alguno no se puede agregar, no se
     incorpora ninguno.
5. Completa la **Fecha de ingreso** (opcional), el **Motivo** y la
   **Observación**.
6. Presiona **Agregar {n} animal(es)**.

### Ejemplo

Al **Lote de engorde A** (cupo 60, ocupación 0) se agregan 12 novillos
Brahman de la propiedad: se marcan en la lista, se elige **Modo** **Atómico
(todo o nada)**, se anota el **Motivo** "Entra a engorde desde Potrero
gordo" y se presiona **Agregar 12 animal(es)**.

### Resultado esperado

Un mensaje confirma **"{n} animal(es) ingresado(s) al lote"**. La ocupación
pasa a **12 / 60 animales** (48 cupos disponibles) y cada animal figura en
**Animales del lote** con su fecha de ingreso y motivo.

> En modo **Parcial**, si algunos animales fallan se muestra el motivo junto
> a su código sin que se cancele el resto. En modo **Atómico**, un solo
> error impide la incorporación completa.

## Retirar animales del lote

### Para qué sirve

Sacar animales de la agrupación (por venta, cambio de manejo, salida a
cuarentena individual, etc.), dejando constancia de la **Fecha de salida** y
del **Motivo** en el historial.

### Antes de comenzar

Tener claros los animales a retirar y el motivo. Solo se puede hacer con el
lote **ACTIVO**.

### Procedimiento

1. Abre la ficha del lote y, en **Animales del lote**, marca con la casilla
   a los animales a retirar.
2. Presiona **Retirar seleccionados (n)** (o el ícono de la papelera en la
   fila de un solo animal).
3. Completa la **Fecha de salida** (opcional) y el **Motivo**.
4. Presiona **Retirar {n} animal(es)**.

### Ejemplo

Cinco novillos del **Lote de engorde A** salen a venta: se marcan en la lista
y se retiran con el **Motivo** "Salen a venta, feria de Warnes". El lote
queda con 7 animales y 53 cupos disponibles.

### Resultado esperado

Un mensaje confirma **"{n} animal(es) retirado(s) del lote"**. Los animales
dejan de integrar el lote, la ocupación disminuye y cada salida queda en el
**Historial de entradas y salidas** con su fecha y motivo.

> El animal retirado queda sin lote asignado; puede volver a agregarse cuando
> quieras siguiendo el procedimiento anterior.

## Mover el lote

### Para qué sirve

Trasladar el lote (todos o algunos de sus integrantes) a otra propiedad y
potrero como una operación validada: el sistema revisa en el paso de
**Validación** qué animales pueden moverse, muestra las restricciones
sanitarias y exige autorización cuando corresponde. El movimiento queda
registrado y auditado.

### Antes de comenzar

- Definir la **Propiedad de destino** y el **Potrero de destino**.
- Decidir la **Acción sobre el lote**: mantenerlo igual, cambiarlo a uno
  existente, crear uno nuevo o dejar los animales sin lote.
- Tener la **Fecha y hora efectiva** del traslado.

### Procedimiento

1. En la ficha del lote **ACTIVO**, presiona **Mover lote**.
2. **Integrantes**: se listan los miembros activos. Usa **Seleccionar
   todos** o marca solo algunos, y presiona **Siguiente**.
3. **Destino**: elige **Propiedad de destino** y **Potrero de destino** (debe
   pertenecer a esa propiedad). Define la **Acción sobre el lote**:
   - **Mantener el mismo lote**: solo si mueves a todos; en un movimiento
     parcial no se permite porque quedarían integrantes en el origen.
   - **Cambiar a un lote existente**: luego elige el **Lote de destino**.
   - **Crear un lote nuevo**: completa el **Nombre del nuevo lote**, el
     **Código (opcional)** y la **Descripción**.
   - **Dejar sin lote**: los animales dejan la agrupación.
   Indica la **Fecha y hora efectiva**, el **Motivo** y las **Observaciones**,
   y presiona **Validar**.
4. **Validación**: revisa **Encontrados**, **Elegibles**, **Excluidos** y
   **Seleccionados a mover**.
   - Los **Excluidos** (por ejemplo, animal en cuarentena activa) listan su
     **Motivo** y no se mueven.
   - Los **Elegibles** muestran sus **Restricciones**. Las de severidad
     **ADVERTENCIA** (por ejemplo, un tratamiento activo) piden escribir un
     **Motivo de la autorización…** para continuar; las **BLOQUEANTES**
     excluyen al animal.
   - Presiona **Continuar**.
5. **Confirmación**: revisa el resumen (animales a mover, los que permanecen
   en el origen y la acción sobre el lote) y presiona **Confirmar
   movimiento**.

### Ejemplo

El **Lote de engorde A** (12 novillos) se traslada a la propiedad "La
Esperanza", potrero **Potrero de llegada**, con la acción **Cambiar a un
lote existente** (el **Lote recría Norte**, LOT-2026-0004). Un novillo está
en cuarentena y aparece **Excluido**; otro tiene un tratamiento activo con
**ADVERTENCIA** y se escribe como motivo de la autorización "El tratamiento
concluye esta semana; se completa cuarentena en el destino".

### Resultado esperado

Un mensaje confirma **"Se movieron n animal(es)"**. Si el lote se mantuvo
junto con su identidad, se indica "junto con la identidad del lote"; si
algún integrante quedó en el origen, se indica que el resultado fue un
**lote dividido**. El traslado aparece en **Movimientos del lote** y en la
línea de tiempo de cada animal.

> El paso de **Validación** no puede saltarse: si hay advertencias sin
> autorizar, no se puede continuar. En un movimiento parcial no se permite
> **Mantener el mismo lote**, porque dejaría integrantes activos en el
> origen. Los movimientos son definitivos y quedan en la auditoría.

## Cerrar un lote

### Para qué sirve

Dar por terminada la vigencia de la agrupación. El lote queda **CERRADO**,
deja de admitir animales y deja de ofrecer las acciones de manejo.

### Antes de comenzar

Haber retirado o movido los animales que corresponda; al cerrar, la
agrupación ya no se usa para incorporar más animales.

### Procedimiento

1. En la ficha del lote **ACTIVO**, presiona **Cerrar lote**.
2. Escribe el **Motivo de cierre** (por ejemplo "Cierre del ciclo de
   engorde, lote vendido en su totalidad").
3. Presiona **Confirmar cierre**.

### Ejemplo

Terminado el engorde, el **Lote de engorde A** se cierra con el motivo
"Ciclo de engorde concluido". En la lista pasa a verse en estado **CERRADO**
con su **Fecha de cierre**.

### Resultado esperado

El lote cambia a **CERRADO**, se registra la fecha de cierre, aparece el
aviso de que no admite más animales y desaparecen los botones de agregar,
retirar, mover y cerrar.

> El cierre no se deshace desde la interfaz: antes de confirmar, revisa que
> el lote ya no necesite incorporar ni mover animales. El historial del lote
> y sus movimientos se conservan después del cierre.

## Problemas frecuentes

### No encuentro el lote al buscar

La búsqueda en **Lotes ganaderos** revisa el código y el nombre. Revisa
también el filtro de estado: un lote **CERRADO** no aparece si el filtro
está en **Activo**.

### No aparecen animales al agregar al lote

Solo se ofrecen animales **ACTIVOS** de la misma propiedad del lote que no
lo integren todavía. Si el animal pertenece a otro lote aparece igualmente
con un aviso y se moverá a este al agregarlo; si no aparece, revisa su
propiedad o su estado en **Animales**.

### Me dice que superé la cantidad máxima

El lote tiene un cupo. Reduce la selección a los **cupos disponibles** o
cambia la **Cantidad máxima del lote** desde la ficha. Cuando se supera el
cupo no se incorpora ningún animal.

### Agregué en modo Parcial y algunos animales no entraron

El modo **Parcial (procesa el resto)** ingresa los que puede y muestra el
motivo de cada error junto al código del animal. Revisa el estado del animal
y su propiedad antes de reintentar.

### Quiero mover el lote pero me pide autorizaciones

Los animales con una restricción de tipo **ADVERTENCIA** (por ejemplo, un
caso clínico o un tratamiento activo) requieren un **Motivo de la
autorización** para poder continuar. Si la restricción es **BLOQUEANTE**, el
animal queda **Excluido** y debe resolverse antes de que pueda moverse.

### No me deja mantener el mismo lote al mover parcial

La opción **Mantener el mismo lote** solo es válida cuando mueves a todos
los integrantes. En un movimiento parcial elige **Cambiar a un lote
existente**, **Crear un lote nuevo** o **Dejar sin lote**.

### El lote se cerró por error

El cierre no se revierte desde la interfaz: un lote **CERRADO** no admite
más animales ni acciones de manejo. Antes de cerrar, asegúrate de haber
retirado o movido lo que corresponde; el historial se conserva aunque el
lote esté cerrado.
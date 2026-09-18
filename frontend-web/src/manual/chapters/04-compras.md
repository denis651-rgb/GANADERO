# Compra de ganado

## Para qué sirve

Permite registrar animales que ingresan a la finca porque los compraste,
individualmente o en lote, junto con el proveedor y el precio pagado.

## Antes de comenzar

Debes tener registrada una propiedad y al menos un potrero de esa propiedad.

## Compra individual

1. Abre **Compras** en el menú lateral.
2. Presiona **Compra individual**.
3. Completa nombre (opcional), sexo y fecha de nacimiento o edad aproximada.
4. Elige propósito, y en **Origen** selecciona **Comprado**.
5. Selecciona raza; la categoría se calcula sola según sexo y edad.
6. Selecciona propiedad y potrero.
7. En **Datos de la compra**, busca al proveedor o registra uno nuevo, e
   indica el precio pagado por este animal.
8. Presiona **Guardar animal**.

![Datos de la compra: proveedor "Estancia El Roble" y precio pagado](/manual-images/compras/compra-individual.jpg)

## Compra por lote

1. Abre **Compras** en el menú lateral.
2. Presiona **Compra por lote**.
3. Completa los datos comunes del lote: raza, propósito, propiedad, potrero
   y fecha de ingreso.
4. Busca al proveedor o registra uno nuevo, elige la modalidad de precio
   (**Por unidad** o **Por tropa o punta**) e indica el precio.
5. Agrega una fila por animal (o usa **+1/+5/+10/+20**) y completa sexo,
   peso al ingreso y nacimiento o edad de cada uno. Podés usar "Aplicar a
   todas las filas" para completar varios animales a la vez con los mismos
   datos.
6. Si corresponde, marca **Enviar a cuarentena** y elige el potrero de
   cuarentena.
7. Presiona **Registrar lote**.

> **Importante — qué potrero queda al final:** el potrero de "Datos comunes del lote" (paso 3)
> es solo el punto de ingreso. Si marcás **Enviar a cuarentena**, apenas se confirma la compra
> el sistema mueve automáticamente a todo el lote al **potrero de cuarentena** que elegiste en
> el paso 6 — ese es el que queda registrado como ubicación final de los animales, no el de
> "Datos comunes". Si querés que el lote quede en el mismo potrero de ingreso, elegí ese mismo
> potrero también como potrero de cuarentena. Si no marcás la casilla, entonces sí quedan en el
> potrero de "Datos comunes del lote".

### Declarar el historial sanitario del lote

Al registrar el lote, la pantalla muestra los animales recién creados y te deja declarar el
historial sanitario que haya informado el proveedor (vacunas, desparasitaciones,
vitaminizaciones, etc.), si es que tiene. Es opcional: podés saltarlo con **Ir a Animales** sin
perder el registro de la compra, que ya quedó guardado.

Hay dos formas de declararlo, y podés combinarlas:

**Declarar historial individual** (un animal a la vez, con lo justo)

1. En la tabla de animales registrados, presiona **Declarar historial sanitario** en la fila
   del animal.
2. Elige el tipo de actividad (vacunación, desparasitación, vitaminización, prueba
   diagnóstica, control ectoparasitario, vigilancia epidemiológica, tratamiento preventivo u
   otra).
3. Si el tipo de actividad elegido coincide con un ítem activo del plan sanitario, aparece
   **Ítem del plan sanitario** para vincularlo (opcional; ver nota más abajo).
4. Completa la fecha y, si querés, el producto o medicamento informado por el proveedor y una
   observación (por ejemplo, el dato del certificado).
5. Presiona **Guardar**.

La fila de ese animal pasa a mostrar **Declarado**.

**Declarar historial grupal** (los mismos antecedentes para varios animales a la vez)

1. Presiona **Declarar historial grupal**, arriba de la tabla.
2. Selecciona los animales que comparten esos antecedentes (por defecto salen todos
   marcados).
3. Completa una fila por cada actividad — tipo, ítem del plan sanitario (opcional, si hay uno
   activo de ese tipo), fecha, producto o medicamento informado por el proveedor y
   observaciones — y presiona **Agregar actividad** si necesitás declarar más de una.
4. Presiona **Confirmar declaración**.

> Cada actividad declarada se aplica a **todos** los animales que dejaste seleccionados: si
> declarás 2 actividades para 3 animales, quedan 6 registros de historial, uno por animal y
> actividad.

> **Vincular un ítem del plan sanitario** (individual o grupal) hace que el calendario
> sanitario calcule la próxima aplicación desde la fecha que declaraste, y evita que
> aparezcan alertas de vacuna próxima o vencida para ese ítem en ese animal. Si dejás
> "Sin vincular", el antecedente igual queda guardado en el historial del animal, pero las
> alertas automáticas de ese ítem del plan siguen su curso normal.

## Ejemplo

- Proveedor: Estancia El Roble
- Fecha de recepción: 06/09/2026
- Raza: Nelore
- Sexo: Macho
- Edad aproximada: 18 meses
- Peso al ingreso: 280 kg (estimado)
- Precio: 5.000 BOB

## Resultado esperado

El o los animales quedan registrados en el inventario, en la propiedad y el
potrero seleccionados, con la compra confirmada asociada (visible en
**Compras**, con su proveedor y precio). En una compra por lote con **Enviar
a cuarentena** marcado, el potrero final es el de cuarentena, no el de
"Datos comunes del lote" (ver nota en la sección anterior).

> Importante: si indicás una edad aproximada en vez de la fecha de
> nacimiento exacta, el sistema calcula y guarda una fecha de nacimiento
> **estimada** — no la muestra como confirmada, y esa diferencia se conserva
> en el historial del animal.

## Problemas frecuentes

### No aparece el potrero que busco

El potrero solo aparece si pertenece a la propiedad que seleccionaste antes.
Revisa que elegiste primero la propiedad correcta.

### En compra por lote, la categoría de un animal queda vacía

Si no indicaste la fecha de nacimiento ni la edad aproximada de esa fila, el
sistema no puede calcular la categoría sola — elegila manualmente e indicá
el motivo en "Motivo de la categoría manual".

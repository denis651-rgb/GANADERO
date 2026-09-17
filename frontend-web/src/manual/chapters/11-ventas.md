# Ventas

El módulo **Ventas** registra la salida de animales por venta y mantiene el
historial de precios. Se puede vender **un animal** (especificando su peso de
salida) o **varios animales / lote** (en pie o carneado). Cada venta genera
automáticamente el movimiento de salida correspondiente.

> El módulo **Compras**, por el lado contrario, registra los ingresos de
> ganado; en esta sección solo se registran las ventas.

## Para qué sirve

Dejar constancia de cada venta (animal o lote, comprador, peso, precio y
modalidad), actualizar el inventario cuando un animal sale vendido y consultar
el historial de ventas con sus precios.

## Antes de comenzar

- Tener el animal o los animales registrados y en el inventario.
- Definir los datos de la operación: **Comprador**, precio, **Moneda**,
  fecha y, si corresponde, la **modalidad** (**En pie** o **Carneado**) y el
  peso de salida.
- Para la venta individual, el sistema usa un **peso ya registrado** o un
  **peso nuevo**. El último pesaje no debe tener más de **30 días** de
  antigüedad; si pasó ese tiempo, el sistema lo avisa y conviene registrar el
  peso nuevo.
- Para la venta por lote en modalidad **Carneado**, cada animal necesita un
  peso de salida mayor que cero.

## Venta individual

### Procedimiento

1. En el menú lateral abre **Ventas** y presiona **Registrar venta**.
2. En la tarjeta **Tipo de venta** elige **Un animal**.
3. En **Animal**, selecciona el animal a vender.
4. Completa la **Fecha de venta** (por defecto la fecha actual), el
   **Comprador** y la **Moneda**.
5. En **Peso de salida**, elige:
   - **Usar un peso ya registrado**: selecciona el peso **Medido** o
     **Estimado** de la lista. Si tiene más de **30 días**, el sistema te
     avisa de su antigüedad (también puedes ignorarlo y continuar) o puedes
     ir a registrar el peso nuevo.
   - **Registrar un peso nuevo**: completa el peso y el **Tipo de peso**
     (**Medido** o **Estimado**). Si el animal no tiene pesos previos, el
     sistema lo indica y conviene registrar el nuevo.
6. Escribe el **Precio** y, si quieres, el **Teléfono del comprador** y
   **Observaciones**.
7. Presiona **Guardar venta**.

> Usa siempre un peso **Medido** cuando sea posible; el precio y los
> indicadores de productividad dependen de que el peso de salida sea
> confiable.

### Venta por lote

1. En **Ventas**, presiona **Registrar venta** y elige **Varios animales /
   lote**.
2. Selecciona los animales con el buscador de múltiples animales.
3. Completa **Comprador**, **Fecha de venta** y **Modalidad de venta**:
   - **En pie — un precio por cabeza para todos**: se define un único
     **Precio por cabeza** para todo el lote.
   - **Carneado — precio por kilo según el peso de cada animal**: se define
     el **Precio por kilo** y el monto de cada animal se calcula con su peso
     de salida.
4. En la lista **Peso y monto por animal**, se precargan los últimos pesos
   registrados. Edita el **Peso de salida (kg)** de cada animal si hace
   falta; el **Monto** se recalcula solo.
   - En **Carneado**, todos los animales necesitan un peso mayor que cero; si
     alguno queda en cero, el sistema avisa antes de guardar.
5. Escribe **Observaciones** si quieres y presiona **Guardar venta de {n}
   animal(es)**.

> En la modalidad **En pie** el precio es por cabeza sin importar el peso; en
> **Carneado** el precio es por kilo y el monto depende del peso de cada
> animal.

## Ejemplo

El 25/09/2026 se venden 10 novillos de la propiedad "La Esperanza": una
vaquilla (ANI-000214, peso medido de 385 kg registrado hace 12 días) se vende
individual por Bs 4.200; los otros 9 se venden por lote a Fernando Roca en
modalidad **En pie** a Bs 4.000 por cabeza. En **Ventas** se registra la venta
individual con el peso ya registrado y, en otra operación, la venta por lote
seleccionando los 9 novillos con **Precio por cabeza** de 4.000.

## Resultado esperado

La venta queda en el listado con su **Fecha**, **Animal**, **Comprador**,
**Modalidad**, **Precio** y **Peso (kg)**. Los animales vendidos salen del
inventario y el sistema genera el movimiento **SALIDA_VENTA**, visible en
**Movimientos**. El historial de precios queda disponible para consultas
posteriores.

## Problemas frecuentes

### La lista no muestra el animal que quiero vender

Verifica que el animal esté activo en el inventario y, si configuraste el
filtro de la lista, que no lo estés ocultando. Los animales ya vendidos no
vuelven a aparecer para una segunda venta.

### Me avisa que el peso tiene más de 30 días

El último pesaje es antiguo para usarse como peso de venta. Elige **Registrar
un peso nuevo**, pesa y guarda, o continúa con el peso existente si aceptas
que la antigüedad reduce la confiabilidad.

### No me deja guardar la venta por lote en Carneado

En **Carneado** cada animal necesita un **Peso de salida** mayor que cero.
Revisa la columna de pesos en **Peso y monto por animal** y completa todos.

### En venta por lote no me aparece la modalidad que necesito

La **Modalidad de venta** se elige al momento de la operación: **En pie** (un
precio por cabeza para todos) o **Carneado** (precio por kilo según el peso de
cada animal). Si solo ves un precio, vuelve a elegir la modalidad antes de
llenar el monto.

### La venta se registró pero no veo el movimiento

La venta crea el movimiento de salida automáticamente. Entra a **Movimientos**
y, si lo necesitas, usa el filtro por tipo para encontrar el
**SALIDA_VENTA**; recuerda que el movimiento queda **pendiente** hasta
validarlo y confirmarlo.
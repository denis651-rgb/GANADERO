# Pesajes

El módulo **Pesajes y productividad** registra el peso de los animales, de uno
en uno o de un lote completo, y lo convierte en información de manejo: cada
control queda ligado al animal y desde la ficha del pesaje se ven los
**Indicadores de crecimiento** (variación, ganancia diaria, comparación con el
promedio del lote) y la curva de evolución del peso.

## Para qué sirve

Medir cómo crece cada animal para tomar decisiones de manejo (cuánto
suplementar, cuándo vender, si el animal responde bien) y hacer los pesajes
operativos de entrada, salida, nacimiento o destete, dejando el registro
histórico de cada control.

## Antes de comenzar

- Tener los animales **ACTIVOS** registrados en **Animales**.
- Para el pesaje por lote, tener un lote **ACTIVO** con integrantes.
- Decidir si el peso es **Medido** (báscula) o **Estimado**, y si registrarás
  la **Condición corporal** (escala del 1 al 9).

## Pesaje individual

Sigue estos pasos para registrar el peso de un solo animal:

1. En el menú lateral abre **Pesajes y productividad** y presiona **Registrar
   pesaje**.
2. En el buscador de animales, elige el animal que vas a pesar.
3. Deja la **Fecha** del día o ajústala, y escribe el **Peso (kg)**.
4. En **Tipo / motivo**, elige el que corresponde: **Rutina / control
   periódico**, **Nacimiento**, **Destete**, **Entrada de lote**, **Salida de
   lote**, **Venta**, **Compra**, **Pesada especial** u **Otro**.
5. En **Tipo de peso**, elige **Medido** o **Estimado**.
6. Opcional: completa la **Condición corporal** (1 a 9), la **Báscula**, la
   **Propiedad**, el **Potrero**, el **Lote** y las **Observaciones**.
7. Presiona **Guardar pesaje**.

> La **Propiedad**, el **Potrero** y el **Lote** se precargan con la ubicación
> actual del animal y se pueden ajustar. Si el lote actual del animal
> pertenece a otra propiedad, se omite del pesaje con un aviso.

## Pesaje por lote

Sigue estos pasos para pesar de una sola vez todos los animales activos de un
lote con el mismo peso:

1. En **Pesajes y productividad**, presiona **Pesaje por lote**.
2. En **Lote**, elige el lote **ACTIVO**.
3. En **Peso (kg)**, escribe el peso que se aplicará a todos los animales
   activos del lote.
4. En **Tipo de peso**, elige **Medido** o **Estimado**. Ajusta la **Fecha** y
   agrega **Observaciones** si quieres.
5. Presiona **Registrar pesaje del lote**.
6. Si algunos animales fallaron, revisa la tabla de errores con el **Animal**
   y el **Motivo**, y presiona **Reintentar solo los fallidos**.

> El pesaje por lote requiere conexión. Sin red, registra los pesajes
> individuales, que sí funcionan sin conexión.

## Ejemplo

En la estancia "El Roble" se hace el control periódico de los novillos del
**Lote de engorde A**. Se registra individualmente a **ANI-000214** (Lucero)
con 432 kg, tipo **Rutina / control periódico**, peso **Medido**, condición
corporal 3.75 y báscula "Báscula 1". Al resto del lote se le aplica el pesaje
por lote con **425 kg** por animal, **Medido**, mismo día.

## Resultado esperado

El pesaje aparece en la lista con su **Fecha**, el **Animal**, el **Peso**
(con la insignia **MEDIDO** o **ESTIMADO**), el **Tipo**, la **Condición** y
el estado **ACTIVO**. Abriéndolo con **Ver** se ven los **Datos del control**
(animal, fecha, peso, tipo, condición, báscula, responsable, dispositivo,
propiedad, potrero, lote), los **Indicadores de crecimiento** (último peso,
variación, ganancia diaria en kg/día, comparación con el promedio del lote) y
la curva **Evolución del peso del animal**, además del **Historial de pesajes
del animal**.

## Anular un pesaje

### Para qué sirve

Quitar un control registrado por error (por ejemplo, un peso mal leído). El
pesaje anulado deja de contar para la curva y los indicadores, pero el
registro se conserva con su motivo.

### Antes de comenzar

El pesaje debe estar en estado **ACTIVO**.

### Procedimiento

1. En la lista de pesajes, sobre el pesaje **ACTIVO**, presiona **Anular** (o
   abre su detalle con **Ver** y presiona **Anular pesaje**).
2. Escribe el **Motivo de anulación** (mínimo 3 caracteres).
3. Presiona **Confirmar anulación**.

### Ejemplo

Se pesó a **ANI-000301** (Dorado) y se anotó 480 kg cuando la báscula marcaba
460 kg. Se anula el control con el motivo "Peso mal leído en la báscula".

### Resultado esperado

El pesaje pasa a **ANULADO**. En el detalle aparece el **Motivo de
anulación**, y el control ya no se considera en los indicadores ni en la curva
de crecimiento.

## Problemas frecuentes

### Registré un peso por error

Anula el pesaje desde la lista o desde su detalle (ver **Anular un pesaje**).
El control **ANULADO** no cuenta para la curva de crecimiento ni para los
indicadores.

### El pesaje por lote falló para algunos animales

El formulario muestra la tabla de los que no se registraron con su **Motivo**.
Corrige lo indicado (por ejemplo el estado del animal o su lote) y presiona
**Reintentar solo los fallidos** para no reenviar a los que ya quedaron.

### No encuentro el animal en el buscador

El buscador ofrece animales **ACTIVOS**. Si no aparece, revisa su estado o su
propiedad en **Animales**; un animal **RETIRADO** no se ofrece para pesar.

### El pesaje por lote dice que el lote no tiene animales activos

Se pesan los integrantes del lote sin fecha de salida. Agrega animales al lote
(ver [Lotes](./05-lotes.md)) o usa el pesaje individual si el animal aún no
integra ningún lote.

### No veo la curva de crecimiento

La sección **Indicadores de crecimiento** muestra "Sin pesajes" cuando el
animal aún no tiene controles. Con un segundo pesaje aparecen la **Variación**,
la **Ganancia diaria** y la curva; con datos del lote también el **Vs. promedio
del lote**.
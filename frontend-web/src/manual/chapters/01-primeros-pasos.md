# Primeros pasos

Si acabas de instalar Ganadero, este capítulo te deja la finca lista para
trabajar. Ganadero es una aplicación de escritorio para Windows: todo lo que
registras se guarda en tu propia computadora, sin necesidad de cuenta en
Internet ni de estar conectado.

No hace falta completar todo de una sola vez. Sigue las secciones en orden
y, cuando una no corresponda a tu caso, simplemente saltéala.

## Antes de comenzar

- Ganadero instalado y abierto en tu computadora. Al abrir la aplicación
  aparece el **Panel principal**.
- Saber el nombre de tu establecimiento, su departamento y municipio, y su
  superficie aproximada en hectáreas.
- Tener en mente cómo se llaman los potreros o corrales de tu finca.

La aplicación ya viene con datos iniciales que puedes usar tal cual:
una propiedad llamada **Mi finca**, las categorías por edad más comunes,
razas bovinas y tipos de pasto. En las secciones siguientes solo las
revisas o las ajustas a tu operación.

## Completar los datos de tu finca

La **propiedad** es el establecimiento que registras (por ejemplo
"Estancia El Roble"). Puedes manejar varias propiedades, pero para empezar
basta con dejar bien cargada la primera.

1. Abre **Mi finca** → **Propiedades** en el menú lateral.
2. Ya existe una propiedad llamada **Mi finca**. Presiona el lápiz de
   editar y completa **Departamento**, **Municipio** y **Superficie (ha)**.
3. Si manejas más de un establecimiento, presiona **Nueva propiedad** y
   escribe su **Nombre**; el código (p. ej. `PRP-001`) se asigna solo al
   guardar.
4. Selecciona la propiedad y presiona **Añadir sector** para cada zona que
   quieras distinguir después, por ejemplo "Casa", "Laguna Norte",
   "Potrero gordo". El sector es opcional, pero ayuda a ubicar los potreros.

> El código de la propiedad y del sector lo asigna el sistema. No hace
> falta que los tengas preparados.

![Formulario para editar los datos de la propiedad "Mi finca"](/manual-images/primeros-pasos/editar-propiedad.jpg)

Cada propiedad guardada aparece con el estado **ACTIVA** y queda disponible
para registrar potreros y animales.

## Revisar las categorías por edad

Las categorías sirven para clasificar el hato según sexo y edad: **Ternero
y Ternera** (hasta 12 meses), **Vaquilla** y **Novillo** (13 a 35 meses), y
**Vaca** y **Toro** (36 meses en adelante). Cuando registro un animal con
fecha de nacimiento, la categoría se asigna sola usando estos rangos.

1. Abre **Mi finca** → **Categorías por edad**.
2. Verás las categorías predefinidas con su sexo y rango de meses.
3. Si tu operación necesita otra clasificación, presiona **Nueva
   categoría** y define sexo, edad mínima y máxima.

> **Buey** es una excepción manual: no participa del cálculo automático.
> Un animal solo queda como Buey si lo seleccionas tú al registrarlo o
> editarlo.

![Lista de categorías por edad predefinidas, con su rango de meses y estado](/manual-images/primeros-pasos/categorias-edad.jpg)

Las categorías predefinidas alcanzan para la mayoría de las fincas. Esto
solo lo revisas si tu forma de clasificar es distinta.

## Registrar los potreros

Un potrero es el lugar físico donde se mantienen los animales, y puede
pertenecer a un sector de tu propiedad.

1. Abre **Potreros** en el menú lateral.
2. Presiona **Nuevo potrero**.
3. Elige la **Propiedad** y, si existe, el **Sector**.
4. Escribe el **Nombre** (por ejemplo "Potrero de la laguna") y la
   **Superficie (ha)**.
5. Elige el **Tipo de pasto** y marca **Tiene agua** si corresponde. Al
   elegir superficie y pasto, la **Capacidad recomendada (UA)** se calcula
   sola; pode ajustarla al valor que prefieras.
6. Deja el **Estado** en **DISPONIBLE** y presiona **Crear potrero**.

![Formulario "Nuevo potrero" completo, con la capacidad recomendada calculada sola](/manual-images/primeros-pasos/nuevo-potrero.jpg)

El potrero queda con un código propio (p. ej. `PRP-001-POT-001`) y su
estado inicial **DISPONIBLE**. Después puedes cambiarlo a **OCUPADO**,
**DESCANSO** o **MANTENIMIENTO** según la rotación de pastoreo.

## Registrar tu primer animal

Con la finca y los potreros cargados, ya puedes registrar animales.

1. Abre **Animales** en el menú lateral.
2. Presiona **Nuevo animal**.
3. Completa la ficha: nombre (opcional), sexo, raza y fecha de nacimiento.
4. Si indicaste la fecha de nacimiento, la categoría se asigna sola según
   sexo y edad. Si no conoces la edad, selecciona tú la categoría y, si es
   una excepción manual como **Buey**, escribe el motivo.
5. Elige el potrero donde se encuentra y presiona **Guardar animal**.

![Formulario "Registrar animal": la categoría "Ternera" se asignó sola a partir del sexo y la fecha de nacimiento](/manual-images/primeros-pasos/nuevo-animal-categoria-automatica.jpg)

El animal aparece en la lista de **Animales** con su código, su categoría
y el potrero asignado. Desde ese mismo registro puedes abrir su ficha para
ver el historial completo.

![Lista de Animales con el animal recién registrado](/manual-images/primeros-pasos/lista-animales.jpg)

> Si compras varios animales juntos, en lugar de registrar uno por uno
> usa **Ingreso por lote de compra** desde la pantalla de **Animales**
> (ver capítulo de [Compras](./04-compras.md)).

## Resultado esperado

Después de estos pasos, en el **Panel principal** verás los totales de tu
operación: animales, potreros, lotes y las alertas que necesitan atención.

![Panel principal con los animales registrados y las alertas de atención requerida](/manual-images/primeros-pasos/panel-principal.jpg)

Para seguir, lee los capítulos en el orden de tu día a día:

- [Animales](./03-animales.md) — registrar, identificar y ubicar el hato.
- [Compras](./04-compras.md) — ingresar animales por compra individual o lote.
- [Lotes ganaderos](./05-lotes.md) — agrupar animales por manejo.
- [Pesajes](./07-pesajes.md) — controlar peso y ganancia.
- [Sanidad](./09-sanidad.md) — planes sanitarios y vacunación.

El manual está disponible siempre, incluso sin conexión, desde **Manual de
usuario** en el menú lateral.

## Problemas frecuentes

### ¿Necesito crear una cuenta o una conexión a Internet?

No. Ganadero es de un solo usuario local: todos los datos e imágenes se
guardan en tu computadora. No se sincroniza nada con ningún servidor.

### No aparece la propiedad "Mi finca"

La aplicación crea esa propiedad al instalarse. Si la desactivaste o la
eliminaste, presiona **Nueva propiedad** en **Mi finca** → **Propiedades** y
regístrala con su nombre. Los códigos se asignan de forma automática.

### La categoría del animal no es la que esperaba

Si el animal tiene fecha de nacimiento, la categoría se calcula por sexo y
edad. Revisa que la fecha sea correcta en la ficha del animal; al corregirla
puedes volver a guardar y la categoría se recalcula. Los animales con edad
desconocida quedan con la categoría que seleccionas manualmente.

### No encuentro el potrero al registrar un animal

Los potreros disponibles para asignar son los de propiedades **ACTIVAS**
que están en estado **DISPONIBLE** u **OCUPADO**. Si no aparece, revisa el
estado del potrero en la pantalla de **Potreros**.
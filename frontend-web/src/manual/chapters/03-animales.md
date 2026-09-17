# Animales

El módulo **Animales** reúne todo lo que tiene que ver con el hato: dar de
alta cada animal, revisar su ficha y su historial, cambiar su estado cuando
sale del servicio, identificarlo (aretes, chips, código QR) y guardar su
genealogía y sus fotografías. Es uno de los módulos que más se usan en el
día a día.

Tienes dos formas de dar de alta animales: uno por uno con **Nuevo animal**,
o varios comprados juntos con **Ingreso por lote de compra**. Las secciones
siguientes cubren ambas y, después, todo lo que se hace desde la ficha del
animal.

## Registrar un animal

### Para qué sirve

Dar de alta un animal nacido en la finca, comprado individualmente o
transferido, creando su ficha propia. Todo lo que le ocurra después
(estados, pesos, sanidad, reproducción, venta) queda registrado a partir
de esa ficha.

### Antes de comenzar

- Tener una propiedad **ACTIVA** y, si es posible, el potrero donde quedará
  el animal (registrado en **Potreros** y en estado **DISPONIBLE** u
  **OCUPADO**).
- Saber la raza del animal (el catálogo de razas ya viene cargado).
- Para animales comprados: los datos del proveedor y el precio pagado.
- Decidir si conoces la fecha de nacimiento o solo la edad aproximada.

### Procedimiento

1. Abre **Animales** en el menú lateral y presiona **Nuevo animal**.
2. En **Información básica**, escribe el **Nombre opcional** y elige el
   **Sexo** (**Hembra** o **Macho**).
3. En **Nacimiento**, elige cómo conoces la edad:
   - **Fecha conocida**: completa **Fecha de nacimiento**.
   - **Edad aproximada**: escribe la **Edad aproximada** y su **Unidad**
     (**Días**, **Meses** o **Años**). El campo **Nacimiento calculado**
     muestra la fecha que se guardará como estimada y, si quieres, completa
     el **Detalle de la estimación**.
   - **Totalmente desconocido**: solo disponible para animales comprados o
     transferidos.
4. En **Clasificación**, elige el **Propósito** (**Carne**, **Leche**,
   **Reproducción** o **Doble propósito**) y el **Origen** (**Nacido**,
   **Comprado** o **Transferido**).
5. Elige la **Raza**. La **Categoría** se asigna sola según sexo y edad y
   se muestra fija; si no conoces la edad, selecciónala tú y, si es una
   excepción manual como **Buey**, completa el **Motivo de la categoría
   manual** (queda anotado en el historial del animal).
6. En **Ubicación**, elige la **Propiedad** y el **Potrero**.
7. En **Información adicional**, revisa el **Ingreso al hato** (en animales
   nacidos en la finca coincide con el nacimiento) o completa la **Fecha de
   recepción** para los comprados o transferidos. Llena el **Peso al ingreso
   (kg)** y el **Tipo de peso al ingreso** (**Estimado** o **Medido**).
   Agrega las **Observaciones** que quieras.
8. Si el **Origen** es **Comprado**, completa **Datos de la compra**:
   selecciona o registra el **Proveedor**, y escribe el **Precio de
   compra** y la **Moneda**.
9. Revisa la vista previa **Ficha del registro** y el **Avance del
   registro** (cuántos campos obligatorios llevas), y presiona **Guardar
   animal**.

### Ejemplo

En la estancia "El Roble" nace una ternera de raza Brahman. Se abre
**Nuevo animal** y se completa: **Nombre opcional** "Lucera", **Sexo**
**Hembra**, **Nacimiento** **Fecha conocida** con fecha de nacimiento
12/07/2026, **Propósito** **Carne**, **Origen** **Nacido**, **Raza**
Brahman, **Propiedad** El Roble, **Potrero** Perinete de la casa, **Peso al
ingreso (kg)** 38 con tipo **Estimado**. Al escribir el sexo y el nacimiento,
la **Categoría** queda **Ternera** de forma automática.

### Resultado esperado

El sistema asigna un código único (por ejemplo **ANI-000142**) y el animal
aparece en la lista de **Animales** con su sexo, edad, categoría, ubicación
y estado **ACTIVO**. Desde ahí, con **Ver**, se abre su ficha. Si el origen
era **Comprado**, al guardar se genera además una compra confirmada, visible
en el módulo **Compras**.

> La categoría solo se calcula sola cuando el animal tiene fecha de
> nacimiento (conocida o estimada). Con edad totalmente desconocida debes
> elegirla a mano. Las excepciones manuales se conservan aunque reclasifiques
> el hato (ver [Categorías por edad](./02-mi-finca.md#categorias-por-edad)).

## Ingreso por lote de compra

### Para qué sirve

Registrar de una sola vez varios animales comprados juntos, escribiendo una
sola vez los datos comunes (raza, propiedad, proveedor y precio) y los
particulares de cada animal. La operación genera una sola compra con todos
los animales.

### Antes de comenzar

- Tener el proveedor (o los datos para crear uno nuevo).
- Saber la **Propiedad** y el **Potrero** de recepción.
- Decidir la **Modalidad de precio**: **Por unidad** (un precio por animal)
  o **Por tropa o punta** (un precio total repartido).
- Llevar la lista de animales: sexo, edad aproximada o fecha de nacimiento,
  y peso al ingreso.

### Procedimiento

1. En **Animales**, presiona **Ingreso por lote de compra**.
2. En **Datos comunes del lote**, elige la **Raza**, el **Propósito**, la
   **Propiedad**, el **Potrero** y la **Fecha de ingreso** (no puede ser
   futura).
3. En **Proveedor y precios**, selecciona o registra el proveedor, elige la
   **Modalidad de precio** y completa:
   - **Por unidad**: escribe el **Precio unitario** (se aplica a todos).
   - **Por tropa o punta**: escribe el **Precio total del lote** (se reparte
     entre los animales).
   Verifica el **Total calculado**.
4. En **Animales del lote**, agrega las filas necesarias con la cantidad y
   **Agregar**, o los botones **+1**, **+5**, **+10**, **+20**. Para la
   primera vez conviene dejar una fila visible y usar **Duplicar animal**.
5. Si varios animales comparten sexo, peso y edad, usa **Aplicar a todas las
   filas:** con el **Sexo**, **Peso (kg)**, **Tipo de peso**, **Edad** y
   **Unidad**, y presiona **Aplicar**; luego ajusta cada fila.
6. Completa por fila el **Nombre**, **Sexo**, **Categoría** (automática por
   sexo y edad, o selección manual con motivo), **Peso al ingreso (kg)** con
   su **Tipo de peso**, **Nacimiento o edad** (**Totalmente desconocido**,
   **Edad aproximada** o **Fecha conocida**), **Precio (ajuste)** opcional y
   **Observaciones**.
7. Si corresponde, marca **Enviar a cuarentena** y elige el **Potrero de
   cuarentena** (puede ser el mismo de ingreso).
8. Presiona **Registrar lote**.
9. En la pantalla de resultado, revisa los **Código** de los animales. Puedes
   registrar el historial sanitario de todos con **Declarar historial
   grupal**, o por cada animal con **Declarar historial sanitario**. Termina
   con **Ir a Animales**.

### Ejemplo

Una compra de 8 vaquillonas proveniente de la feria de Montero: se llenan los
datos comunes (razas Brahman, propósito **Carne**, propiedad El Roble,
potrero Potrero gordo), se agregan 8 filas, se aplica a todas **Sexo**
**Hembra**, **Peso (kg)** 215, **Tipo de peso** **Estimado** y **Edad** 15
**Meses**, y se ajustan el nombre y la categoría de cada una. Se selecciona al
proveedor "Mercado Agro Santa Cruz S.R.L." y se registra el precio **Por
tropa o punta** de 11 200 Bs; el sistema reparte 1 400 Bs por animal.

### Resultado esperado

Cada animal queda con su código propio y estado **ACTIVO**, y en **Compras**
aparece una sola compra **CONFIRMADA** con el proveedor, la modalidad y el
precio asignado por animal. Si marcaste **Enviar a cuarentena**, los animales
quedan registrados con un movimiento de cuarentena hacia ese potrero.

> La compra necesita siempre un proveedor (existente o nuevo), y la categoría
> se sugiere solo si declaras una edad. En la modalidad **Por tropa o punta**,
> si ajustas el precio de algunas filas, todas deben quedar con precio manual
> para poder guardar.

## Ficha del animal

### Para qué sirve

Ver todo lo que el sistema sabe de un animal en una sola pantalla: datos
principales, ubicación, compra, pesos, alertas sanitarias, historial de
categorías, y sus pestañas de línea de tiempo, identificadores, fotografías
y genealogía. Desde aquí también se editan los datos y se cambia el estado.

### Antes de comenzar

Tener el animal registrado (ver [Registrar un animal](#registrar-un-animal)).
En **Animales**, los datos aparecen en la lista; la ficha es la vista
completa.

### Procedimiento

1. En **Animales**, usa **Ver** (o el nombre del animal) para abrir su ficha.
2. Revisa **Datos principales**: **Sexo**, **Raza**, **Categoría**,
   **Propósito**, **Nacimiento** (con la marca **CONFIRMADA**,
   **ESTIMADA** o **DESCONOCIDA**), **Edad declarada**, **Origen**,
   **Ingreso al hato** o **Fecha de recepción**, **Precio de compra**,
   **Peso al ingreso**, **Peso al nacer** y **Condición corporal**.
3. En **Ubicación y observaciones** se muestra la propiedad, el potrero y el
   lote actual del animal.
4. Si fue comprado, la sección **Compra** enlaza con el **Código de compra**
   y muestra proveedor, documento, **Fecha de recepción**, **Modalidad** y
   **Precio asignado**.
5. En **Peso**, revisa el **Último peso medido** y el **Último peso
   estimado**. Con **Ver pesajes** se abre el historial completo del módulo
   **Pesajes**.
6. El **Calendario sanitario** lista las alertas activas del animal
   (vacunas por vencer o vencidas, revisión de ingreso). El **Resumen
   sanitario** muestra la **Última vacunación**, el **Último tratamiento**,
   el **Control neonatal**, el **Control ectoparasitario** y el **Examen
   reproductivo**, con acceso a **Ver historial sanitario**.
7. **Historial de categorías** lista los cambios de categoría del animal con
   su tipo (**Automático**, **Manual** o **Corrección**).
8. Usa las pestañas para las secciones restantes: **Línea de tiempo** (filtra
   por tipo, módulo o fechas), **Identificadores**, **Fotografías** y
   **Genealogía**.
9. Para corregir un dato, presiona **Editar**. En la pantalla **Editar
   {código}** puedes cambiar nacimiento, propósito, categoría, ubicación,
   pesos y observaciones (incluye corregir el **Peso al nacer** si se
   confunde con el de compra) y presiona **Guardar cambios**.

### Ejemplo

En la ficha de **ANI-000142** (Lucera) se muestra: sexo **HEMBRA**, raza
Brahman, categoría **Ternera**, propósito **CARNE**, nacimiento **CONFIRMADA**
con fecha 12/07/2026, origen **NACIDO**, **Peso al ingreso** 38 kg (estimado)
y ubicación El Roble / Perinete de la casa. El **Calendario sanitario**
recuerda la **REVISIÓN SANITARIA DE INGRESO** pendiente para los terneros
recién nacidos.

### Resultado esperado

La ficha reúne el historial completo del animal en un solo lugar y cada
operación nueva (pesaje, vacunación, cambio de estado, fotografía) se refleja
ahí y en su **Línea de tiempo**.

> La ausencia de alertas en el **Calendario sanitario** no confirma vacunas:
> los antecedentes no documentados se consideran desconocidos. Los cambios
> hechos con **Editar** quedan registrados en el historial del animal.

## Cambiar estado

### Para qué sirve

Registrar que un animal deja de estar en servicio (vendido, muerto, perdido,
transferido o descartado) o vuelve a estar activo. El estado se muestra en
toda la aplicación, determina si el animal cuenta como activo en el hato y
queda como evento en su historial.

### Antes de comenzar

Tener claro el motivo del cambio. Para estados que no sean **ACTIVO** se
pide confirmación antes de aplicar.

### Procedimiento

1. Abre la ficha del animal y baja a la sección **Cambiar estado**.
2. En el selector, elige el nuevo estado: **ACTIVO**, **VENDIDO**,
   **MUERTO**, **PERDIDO**, **TRANSFERIDO** o **DESCARTADO**.
3. Escribe el **Motivo del cambio** (por ejemplo "Vendida en la feria de
   Warnes, lote 12").
4. Presiona **Actualizar estado**.
5. Si el nuevo estado no es **ACTIVO**, se abre **Confirmar cambio de
   estado**: revisa el resumen del cambio y presiona **Confirmar cambio**.

### Ejemplo

La vaca "La Milanera" (ANI-000214) se vende en la feria de Warnes. En su
ficha se elige **VENDIDO**, se escribe el motivo "Vendida en la feria de
Warnes, lote 12, trece quintales a 1 650 Bs el quintal", y se confirma el
cambio. Su marca de estado pasa de **ACTIVO** a **VENDIDO**.

### Resultado esperado

El estado del animal se actualiza en la ficha y en la lista de **Animales**,
deja de contar en los totales de activos del resumen del hato, y el cambio
con su motivo aparece en la **Línea de tiempo**.

> El registro de una venta con precio de salida se hace en el módulo
> **Ventas**; el cambio de estado a **VENDIDO** desde la ficha es solo el
> estado operativo del animal. Ambos quedan en el historial. Un cambio hecho
> por error se puede volver a **ACTIVO** desde la misma sección.

## Identificadores y código QR

### Para qué sirve

Asociar al animal los identificadores físicos (arete, chip RFID, tatuaje u
otro) y generar su **código QR** firmado, para verificarlo con la cámara e
imprimir las etiquetas del hato.

### Antes de comenzar

- El animal debe estar **ACTIVO**.
- Tener a mano el número de cada identificador (arete, RFID, tatuaje).
- Para imprimir o escanear: una impresora de etiquetas QR y el acceso
  **Escanear QR** desde el **Panel principal**.

### Procedimiento

1. Abre la ficha del animal y la pestaña **Identificadores**.
2. Presiona **Asignar identificador** y completa el formulario: **Tipo**
   (**ARETE**, **RFID**, **TATUAJE** u **OTRO**), **Valor**,
   **Observaciones** (opcional) y marca **Identificador principal** si será
   el de referencia del animal. Presiona **Guardar identificador**.
3. Presiona **Generar QR** y, en la ventana, marca **Marcar como
   identificador principal** si quieres que el QR sea la referencia. Presiona
   **Generar QR**. El código se genera y firma en el servidor; no se asigna
   manualmente.
4. Desde la tabla de identificadores puedes:
   - **Ver QR**: abre la tarjeta del código con su contenido técnico.
   - **Hacer principal**: cambiar el identificador de referencia.
   - **Reemplazar QR**: generar un QR nuevo con nueva firma (el anterior
     queda retirado e inválido). Pide un **Motivo del reemplazo** de al
     menos 5 caracteres.
   - **Retirar**: quitar el identificador del animal con su **Motivo del
     retiro** (también mínimo 5 caracteres).
5. En la lista de **Animales**, marca con la casilla a los animales que
   quieras imprimir y presiona **Imprimir QR ({cantidad})**.
6. Para verificar un QR en campo, usa el acceso del **Panel principal**:
   **Escanear QR** abre la cámara y confirma que el código corresponde a un
   animal activo.

### Ejemplo

Lucera (ANI-000142) recibe el arete **BR-0051**: se asigna con tipo **ARETE**,
valor **BR-0051**, marcado como **Identificador principal**. Después se
presiona **Generar QR** marcando **Marcar como identificador principal**. Para
la señalización de la finca, en **Animales** se selecciona a Lucera y se
presiona **Imprimir QR (1)**.

### Resultado esperado

La pestaña **Identificadores** lista cada identificador con su **Tipo**,
**Valor**, **Estado** (**ACTIVO** o **RETIRADO**), fechas de **Asignación** y
**Retiro**, y el de referencia lleva la marca **Principal**. El QR activo se
verifica con el escáner y se imprime desde la lista.

> El QR no contiene datos sensibles y solo es válido mientras esté **ACTIVO**:
> al reemplazarlo o retirarlo deja de funcionar. Solo los animales **ACTIVOS**
> pueden generar QR; para los demás el botón no está disponible (los botones
> de la tabla solo operan sobre filas en estado **ACTIVO**).

## Genealogía

### Para qué sirve

Registrar la madre y el padre de cada animal, estén o no en el sistema, para
llevar la trazabilidad del hato y tener el historial de progenitores.

### Antes de comenzar

Saber si el progenitor ya tiene ficha en el sistema o si conviene registrarlo
como externo. Para un progenitor externo conviene conocer su raza y, si
existe, su **Registro genealógico**.

### Procedimiento

1. Abre la ficha del animal y la pestaña **Genealogía**.
2. Presiona **Registrar progenitor**.
3. Elige el **Tipo de progenitor**: **Madre** o **Padre**.
4. Completa el progenitor:
   - Con ficha en el sistema: elige **Animal progenitor** de la lista.
   - Sin ficha: marca **Progenitor externo (sin ficha en el sistema)** y
     completa **Nombre externo**, **Raza** y **Registro genealógico**.
5. Presiona **Guardar parentesco**.
6. Para quitar una relación, presiona **Eliminar** en la fila y confirma la
   eliminación.

### Ejemplo

Lucera (ANI-000142) queda registrada con su madre, la vaquilla **ANI-000087**
(registro genealógico **AB-4581**), y con su padre externo "Toro de la
Hacienda San Roque" (raza Brahman, registro genealógico **BR-1120**), porque
ese toro no tiene ficha en la finca.

### Resultado esperado

En la pestaña **Genealogía** quedan listadas las relaciones con su **Rol**
(**MADRE** o **PADRE**), el **Progenitor** (código y nombre, o nombre
externo), el **Registro genealógico** y la **Fecha** de registro. Las
relaciones se muestran en el historial del animal.

> Eliminar un parentesco no elimina al animal progenitor; solo quita la
> relación. La operación no se puede deshacer.

## Fotografías

### Para qué sirve

Guardar imágenes del animal para identificarlo visualmente. La fotografía
principal se muestra en la lista de **Animales** y en las pantallas que lo
referencian.

### Antes de comenzar

Tener las fotografías en tu computadora. Se aceptan **JPG**, **PNG** y
**WebP**.

### Procedimiento

1. Abre la ficha del animal y la pestaña **Fotografías**.
2. Presiona **Subir fotos** y selecciona una o varias imágenes. La
   aplicación las comprime automáticamente.
3. Para cambiar la imagen de referencia, presiona **Reemplazar principal**
   (solo disponible si ya existe una principal) y elige el archivo nuevo.
4. Presiona una foto para ampliarla. Ahí puedes:
   - **Marcar principal**: convertirla en la imagen de referencia.
   - **Eliminar**: quitar la foto, confirmando con **Eliminar
     definitivamente**.

### Ejemplo

En la ficha de Lucera se sube la foto de perfil tomada al nacer y se marca
como **principal**. Después se agregan dos fotos más del costado y del
frente; al abrirlas se ven la fecha, autor, dimensiones y peso de cada
archivo.

### Resultado esperado

La galería muestra las fotografías con la insignia **Principal** en la de
referencia. En la lista de **Animales**, la foto principal aparece junto al
nombre del animal. Las subidas y eliminaciones quedan registradas en la
auditoría y en la **Línea de tiempo**.

> Al eliminar la fotografía principal el animal queda sin foto de referencia;
> vuelve a subir una y márcala como principal cuando puedas.

## Problemas frecuentes

### No encuentro un animal al buscarlo

La búsqueda en **Animales** revisa el código, el nombre, la raza y el valor
de los identificadores (**ARETE**, **RFID**, tatuaje o QR). Si no aparece,
revisa los filtros de estado, sexo, propiedad o potrero: el animal puede
estar en un estado distinto al que filtras.

### La categoría no se asigna como esperaba

La categoría automática depende del sexo y de la fecha de nacimiento. Si la
fecha es incorrecta, corrígela con **Editar** en la ficha y vuelve a guardar:
la categoría se recalcula. Los animales con edad desconocida o las
excepciones manuales (como **Buey**) conservan la categoría elegida a mano.

### No encuentro el potrero al registrar un animal

El potrero debe pertenecer a una propiedad **ACTIVA** y estar en estado
**DISPONIBLE** u **OCUPADO**. Verifica el estado del potrero en **Potreros**
y que la propiedad seleccionada sea la correcta.

### Al guardar un animal comprado me pide el proveedor

Si el origen es **Comprado**, la operación genera una compra, y una compra
necesita proveedor. Selecciónalo en **Datos de la compra** (o regístralo como
nuevo) y vuelve a intentar.

### El botón de generar QR no me deja

El código QR solo se genera para animales **ACTIVOS**. Si el animal está en
otro estado (por ejemplo **DESCARTADO**), primero vuelve a **ACTIVO** desde
**Cambiar estado** y luego genera el QR desde la pestaña **Identificadores**.

### Al imprimir QRs me avisa que ninguno tiene QR activo

La impresión toma las casillas marcadas en **Animales**. Si un animal no
tiene QR activo, se salta; cuando ninguno lo tiene, la aplicación avisa.
Genera los QRs desde la ficha de cada animal antes de imprimir.

### No me deja reemplazar o retirar un identificador

El **Motivo del reemplazo** y el **Motivo del retiro** deben tener al menos
5 caracteres. Escribe un motivo real, por ejemplo "Arete roto, se repone con
BR-0052".

### Cambié el estado por error

Puedes volver a cambiar el estado a **ACTIVO** desde la misma sección
**Cambiar estado**, con su motivo. El cambio anterior queda en el historial
del animal: no se borra, pero el estado operativo queda corregido.
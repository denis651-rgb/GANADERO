# Reproducción

El módulo **Reproducción** lleva el calendario reproductivo del hato: la
detección de celo, los servicios (monta e inseminación), los diagnósticos de
gestación, los partos con sus crías, los abortos y los destetes. Con esa
información el sistema sugiere cuándo hacer la evaluación de cada vaca y deja
un historial completo por hembra.

Todas las operaciones se hacen desde la misma pantalla **Reproducción** (en el
menú lateral, dentro de **Campo**), usando sus pestañas: **Resumen**,
**Celos**, **Servicios**, **Diagnósticos** y **Partos y crías** (que a su vez
tiene las sub-pestañas **Partos**, **Abortos** y **Destetes**).

## Celo

### Para qué sirve

Registrar la detección de celo (estro) de una hembra como insumo para
programar el servicio. El sistema revisa el historial de la misma hembra y
avisa si hay un celo muy cercano: a menos de 24 horas lo marca como posible
**duplicado** y a menos de 18 días como **intervalo corto**, para que el
registro no quede a ciegas.

### Antes de comenzar

- Tener la hembra **ACTIVA**.
- Definir la fecha y hora de la detección y el tipo (visual, toro marcador,
  podómetro, sensor u otro).

### Procedimiento

1. En **Reproducción**, abre la pestaña **Celos** y presiona **Registrar
   celo**.
2. En **Animal**, busca y elige la hembra. Ajusta la **Fecha y hora de
   detección**.
3. En **Tipo de detección**, elige **Visual**, **Toro marcador**,
   **Podómetro**, **Sensor** u **Otro**. Opcional: **Intensidad** (**Baja**,
   **Media** o **Alta**).
4. Si existe otro celo cercano de la misma hembra:
   - **Posible duplicado** (menos de 24 horas): en **Cómo deseas continuar**
     elige **Agregar observación al celo existente (sin crear otro)** (escribe
     la observación arriba) o **Registrar un celo separado con
     justificación**, escribiendo la **Justificación para un registro
     separado** y marcando "He revisado el celo existente y confirmo el
     intervalo si registro uno separado".
   - **Intervalo corto** (menos de 18 días): escribe una **Justificación**
     si vas a registrar el celo igualmente.
5. Completa **Propiedad**, **Potrero**, **Lote** y **Observaciones** si
   corresponden.
6. Presiona **Guardar**.

### Ejemplo

El 15/09/2026 a las 07:30 se detecta de forma visual el celo de **ANI-000142**
(Lucera), de **Intensidad** **ALTA**, en el potrero Perinete de la casa. No hay
otro celo reciente de la misma vaca, así que se guarda sin advertencias.

### Resultado esperado

El celo aparece en la pestaña **Celos** con su **Fecha de detección**, **Tipo**,
**Intensidad** y estado **Activo**. Queda disponible para asociarse al
**Servicio** de esa hembra.

> Si anulas un celo (botón **Anular** en su fila, con el **Motivo de
> anulación**), deja de contar para el seguimiento reproductivo y ya no se
> ofrece como **Celo asociado** de un servicio.

## Servicio

### Para qué sirve

Registrar la monta o la inseminación de una hembra. El sistema asigna el
**número de intento** automáticamente (#1, #2, …) y calcula la **fecha
recomendada para el diagnóstico**, que luego se resume en **Resumen** como
"Próximos diagnósticos recomendados".

### Antes de comenzar

- Tener la hembra **ACTIVA**.
- Si existe un celo **ACTIVO** de esa hembra, se puede asociar; si no, el
  servicio se registra "Sin asociar".
- Definir el **Tipo de servicio**: **Monta natural**, **Inseminación
  artificial** o **Transferencia embrionaria**. Para la monta se necesita el
  **Macho**; para la inseminación, el **Código de semen** y el **Proveedor de
  semen**.

### Procedimiento

1. En **Reproducción**, abre la pestaña **Servicios** y presiona **Registrar
   servicio**.
2. En **Hembra**, busca y elige a la vaca.
3. En **Celo asociado**, selecciona el celo de esta hembra si lo hay
   (opcional; solo se muestran los **Activos** de esa hembra).
4. Completa la **Fecha y hora del servicio** y elige el **Tipo de servicio**.
5. Según el tipo, completa el **Macho** (monta natural) o el **Código de
   semen** y **Proveedor de semen** (inseminación artificial).
6. Ajusta **Propiedad**, **Potrero**, **Lote** y **Observaciones**.
7. Presiona **Registrar servicio**.

### Ejemplo

Lucera (**ANI-000142**), con el celo detectado el 15/09/2026, se insemina ese
mismo día a las 10:00 con el semen BAY01-234 del proveedor "Semex Bolivia".
El sistema la registra como **Intento #1** y recomienda el diagnóstico para el
10/10/2026.

### Resultado esperado

El servicio aparece en **Servicios** con **Hembra**, **Fecha**, **Tipo**,
**Macho / semen**, **Intento #1** y estado **Registrado** (que queda
**Pendiente diagnóstico** para el seguimiento). Se guarda la fecha recomendada
del diagnóstico y queda visible en **Resumen**.

> Con cada servicio nuevo de la misma hembra el **Intento** sube (#2, #3, …).
> El ciclo del servicio se cierra recién con el diagnóstico de gestación.

## Diagnóstico de gestación

### Para qué sirve

Registrar el resultado de la evaluación de gestación (palpación, ecografía,
análisis de sangre u otro). Un resultado **Positivo** abre la gestación,
calcula la **fecha probable de parto** y actualiza el estado del servicio
asociado.

### Antes de comenzar

- Hembra **ACTIVA**.
- Opcional: el **Servicio asociado** (solo se listan los vigentes de esa
  hembra, no los anulados ni finalizados).
- Decidir el resultado: **Positivo**, **Negativo**, **Dudoso** o **Pérdida de
  gestación**.

### Procedimiento

1. En **Reproducción**, abre la pestaña **Diagnósticos** y presiona
   **Registrar diagnóstico**.
2. En **Animal**, busca y elige la hembra. En **Servicio asociado**, elige el
   servicio si lo tiene.
3. Completa la **Fecha y hora del diagnóstico** y elige el **Resultado**.
4. Si el resultado es **Positivo**, elige el **Método** (**Palpación**,
   **Ecografía**, **Análisis de sangre** u **Otro**) y, opcionalmente, los
   **Días de gestación estimados**.
5. Revisa que la **Propiedad**, el **Potrero** y el **Lote** correspondan (se
   cargan del animal; el **Lote** es informativo y no cambia la pertenencia).
6. Agrega **Observaciones** si quieres y presiona **Registrar diagnóstico**.

### Ejemplo

El 10/10/2026, la ecografía de Lucera (**ANI-000142**) da **Positivo** con 30
días de gestación estimados, asociada al servicio del 15/09 (#1). El sistema
calcula la fecha probable de parto alrededor del 22/03/2027.

### Resultado esperado

El diagnóstico aparece en **Diagnósticos** con **Resultado**, **Método** y
**Parto probable**. El servicio pasa a **Gestación confirmada**, se abre la
gestación de la hembra y el seguimiento queda visible en **Resumen**.

> Con **Negativo** o **Pérdida de gestación**, el servicio pasa a
> **No preñada** / fica la hembra disponible para un nuevo ciclo. Con
> **Dudoso** se recomienda repetir el diagnóstico.

## Parto

### Para qué sirve

Registrar el parto de una hembra con los datos del evento y de cada cría:
sexo, peso al nacer, estado al nacer y, cuando corresponde, crear el registro
de cada cría en el inventario con su raza, nombre y potrero inicial.

### Antes de comenzar

- Tener identificada la gestación que finaliza: un **diagnóstico positivo
  previo** o, si no se conoce, una gestación con **antecedentes
  desconocidos** (por ejemplo una vaca comprada gestante). Una gestación solo
  finaliza una vez: por parto o por aborto.
- Tener los datos del parto: tipo, dificultad, si fue asistido, y de cada
  cría.

### Procedimiento

1. En **Reproducción**, abre **Partos y crías** → sub-pestaña **Partos** y
   presiona **Registrar parto**.
2. En **Madre**, busca y elige a la vaca.
3. En **Gestación que finaliza**, elige **Usar diagnóstico positivo previo** y
   selecciona el diagnóstico, o **Registrar gestación con antecedentes
   desconocidos** completando la **Fecha de confirmación**, el **Inicio
   estimado** (opcional) y el campo de **Antecedentes y evidencia de la
   confirmación**.
4. Completa **Fecha del parto**, **Tipo de parto** (**Normal**, **Prematuro**,
   **Distócico**, **Cesárea**, **Otro**) y **Dificultad** (**Sin asistencia**,
   **Asistencia leve**, **Asistencia moderada**, **Asistencia difícil**,
   **Cesárea**). Marca **Parto asistido** si corresponde.
5. Opcional: **Resultado de la madre** (ej. "Buena, en observación…") y
   **Observaciones**.
6. En **Crías**, completa para cada una: **Sexo**, **Peso al nacer (kg)**,
   **Estado al nacer** (**Vivo**, **Muerto**, **Natimuerto**) y **Hora de
   nacimiento**.
7. Marca **Crear registro del animal en el inventario** para que la cría nazca
   con ficha propia, y completa la **Raza de la cría**, el **Nombre del
   animal** y, opcionalmente, el **Potrero inicial**. La raza no se hereda
   automáticamente de la madre.
8. Usa **Agregar otra cría** para hermanos o **Quitar cría** para eliminar una
   fila. Presiona **Registrar parto**.

### Ejemplo

Lucera (**ANI-000142**) pare el 22/03/2027 un ternero **Vivo** con **Parto
asistido** de dificultad **Asistencia leve**: peso de 41 kg, raza Brahman,
nombre "Dorado", potrero inicial Perinete de la casa. Se deja marcado **Crear
registro del animal en el inventario**.

### Resultado esperado

El parto aparece en **Partos y crías** con **Madre**, **Fecha**, **Tipo**,
**Dificultad**, cantidad de **Crías** y estado **Activo**. Cada cría creada
recibe su código (por ejemplo **ANI-000301**), queda en su potrero inicial y su
**Peso al nacer** queda como control de tipo **Nacimiento**. La gestación se
cierra como terminada por parto.

> Registra el **Peso al nacer (kg)** y la **Hora de nacimiento** de las crías
> vivas: son la base del control neonatal y de la curva de crecimiento.

## Aborto

### Para qué sirve

Registrar la pérdida de una gestación (aborto o pérdida) con su causa y
diagnóstico, cerrando la gestación abierta para que la hembra vuelva a estar
disponible.

### Antes de comenzar

- Hembra **ACTIVA** con una gestación abierta o con antecedentes.
- Conocer la fecha del evento y, si se sabe, la edad gestacional estimada, la
  causa y el diagnóstico.

### Procedimiento

1. En **Reproducción**, abre **Partos y crías** → sub-pestaña **Abortos** y
   presiona **Registrar aborto**.
2. En **Animal**, busca y elige a la vaca.
3. En **Gestación que finaliza**, elige **Usar diagnóstico positivo previo** o
   **Registrar gestación con antecedentes desconocidos** (igual que en el
   parto).
4. Completa la **Fecha del evento** y, si se conoce, la **Edad gestacional
   estimada (días)**.
5. Opcional: **Causa**, **Diagnóstico** y **Observaciones**.
6. Presiona **Registrar aborto**.

### Ejemplo

El 15/01/2027 se registra la pérdida de la gestación de Lucera
(**ANI-000142**) a los 120 días, con causa "Neospora sospechosa" y diagnóstico
"Se descarta infección febril".

### Resultado esperado

El aborto aparece en la sub-pestaña **Abortos** con su fecha y causa, la
gestación se cierra como terminada por aborto y la hembra queda disponible
para el siguiente ciclo reproductivo.

## Destete

### Para qué sirve

Registrar el destete de una cría con su **Peso al destete (kg)**, dejando
constancia del tipo de destete y su motivo y cerrando la etapa de lactancia.

### Antes de comenzar

- La cría debe estar en el inventario (habitualmente se creó en el
  **Parto**).
- La **Madre** se completa automáticamente desde el parto de la cría.
- Definir el **Tipo de destete**: **Normal**, **Precoz**, **Temporal**,
  **Forzado** u **Otro**, y el peso al destete.

### Procedimiento

1. En **Reproducción**, abre **Partos y crías** → sub-pestaña **Destetes** y
   presiona **Registrar destete**.
2. En **Cría**, busca y elige a la cría. Verifica la **Madre** que se rellena
   sola.
3. Completa la **Fecha del destete** y el **Peso al destete (kg)**.
4. Elige el **Tipo de destete** y escribe el **Motivo** si quieres.
5. Presiona **Registrar destete**.

### Ejemplo

El 02/09/2027 se desteta a **ANI-000301** (Dorado) con 190 kg, destete de tipo
**Normal** y motivo "Alcanzó el peso de destete del programa".

### Resultado esperado

El destete aparece en **Destetes** con **Cría**, **Madre**, **Fecha del
destete**, **Peso al destete (kg)**, **Tipo de destete** y estado **Activo**, y
el peso queda como referencia del animal para el análisis de crecimiento.

## Problemas frecuentes

### Me avisa de un posible duplicado o intervalo corto de celo

El sistema detectó otro celo de la misma hembra. Si es un duplicado (menos de
24 horas), elige **Agregar observación al celo existente (sin crear otro)** o
**Registrar un celo separado con justificación** completando la
**Justificación** y confirmando el intervalo. Si es un intervalo corto (menos
de 18 días), deja una justificación al registrar.

### No aparece el celo para asociarlo al servicio

La lista **Celo asociado** solo muestra los celos **Activos** de esa hembra.
Registra el celo primero (y no lo anules) para que quede disponible.

### El estado del servicio no cambia con el diagnóstico

Un diagnóstico **Positivo** pasa el servicio a **Gestación confirmada**;
**Negativo** o **Pérdida de gestación** deja la hembra disponible y **Dudoso**
mantiene el **Pendiente diagnóstico**. Revisa el estado del servicio en la
lista con su insignia.

### Me pide la gestación al registrar parto o aborto

La gestación debe identificarse: usa el **diagnóstico positivo previo** o,
si no se conoce, **Registrar gestación con antecedentes desconocidos**.
Recuerda que una misma gestación solo finaliza una vez: por parto o por
aborto.

### La cría no aparece en el inventario después del parto

El registro del animal solo se crea si marcaste **Crear registro del animal en
el inventario** en la fila de esa cría. Si quedó sin crear, dala de alta con
**Nuevo animal** en [Animales](./03-animales.md).

### No me deja guardar el destete

Al elegir la cría, el botón se habilita recién cuando se identifica la
**Madre**. Si la **Madre** muestra "No se pudo identificar", revisa que la
cría tenga un parto registrado; sin madre registrada no se puede completar el
destete.
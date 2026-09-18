# Sanidad

Gestiona la salud del hato: planes sanitarios, jornadas de vacunación o
desparasitación, controles individuales, casos clínicos, tratamientos y su
sincronización con un calendario externo.

## Plan sanitario

### Para qué sirve

Agrupar las vacunaciones, desparasitaciones y demás actividades sanitarias
que se deben cumplir en la finca. Para cada actividad defines a quién
aplica, cuándo corresponde, con qué dosis y cuánto antes debe avisarte el
sistema. Con eso Ganadero arma solo el calendario de la finca y te avisa
antes de cada fecha.

### Antes de comenzar

- Si vas a limitar una actividad a una o varias categorías de animal, esas
  categorías deben existir ya en **Mi finca**.
- Necesitas el permiso para administrar planes sanitarios. Sin él puedes ver
  los planes, pero no aparecen los botones para crearlos ni modificarlos.
- Ten a mano el calendario oficial de SENASAG (campañas) y las indicaciones
  de tu veterinario: dosis, vía de aplicación y días de retiro.

### Procedimiento

**Crear y activar el plan**

1. Abre **Sanidad** en el menú lateral y entra a la pestaña **Planes
   sanitarios**.
2. Presiona **Nuevo plan** e indica nombre, fecha de inicio, fecha de fin
   (opcional) y descripción.
3. Presiona **Crear plan**. El plan queda en estado **BORRADOR**.
4. Cuando esté listo, presiona **Activar** y luego **Confirmar**. Si ya hay
   otro plan activo con actividades del mismo tipo, verás un aviso: no te
   impide activar, solo te advierte que las actividades podrían solaparse.

**Agregar una actividad**

5. En la fila del plan presiona **Agregar actividad**. Puedes hacerlo con el
   plan en **BORRADOR** (para ir preparándolo) o ya **ACTIVO**.
6. En **Datos generales** escribe el nombre, elige el tipo de actividad y
   la clasificación regulatoria.
7. En **Cuándo se programa** elige la modalidad y completa los campos que
   aparecen según cuál sea.
8. En **A quién aplica** limita la actividad por categoría, sexo y edad, o
   déjala sin restricción.
9. En **Medicamento recomendado** indica el producto, el principio activo y
   las instrucciones (es solo informativo, no descuenta ningún inventario).
10. En **Dosis** elige el tipo de cálculo y, si corresponde, la cantidad, la
    unidad y el peso de referencia.
11. En **Vía y lugar de aplicación** indica cómo y dónde se aplica.
12. En **Alertas** define la hora prevista, cuántos días antes avisar y los
    horarios de aviso.
13. Presiona **Agregar actividad**. Si algún dato no cumple las reglas, el
    botón queda bloqueado y el campo muestra debajo qué corregir.

### Estados de un plan

| Estado | Qué significa | Qué puedes hacer |
| --- | --- | --- |
| **BORRADOR** | El plan se está preparando. | Agregar y modificar actividades. Aún no genera calendario ni se ofrece en las jornadas. Puedes **Activarlo** o **Anularlo**. |
| **ACTIVO** | El plan está vigente. | Agregar y modificar actividades. Genera el calendario y sus actividades se ofrecen al preparar una jornada. Puedes **Finalizarlo** o **Anularlo**. |
| **FINALIZADO** | Cerraste el plan al terminar su período. | Solo consultarlo. Ya no admite cambios en sus actividades. |
| **ANULADO** | El plan se canceló. | Solo consultarlo. Ya no admite cambios en sus actividades. |

Finalizar o anular un plan **no se puede deshacer**. Al hacerlo, el sistema
cancela las fechas pendientes de sus actividades en el calendario y sus
avisos. Lo que ya se aplicó queda registrado en el historial de cada animal.

### Los campos del formulario «Nuevo plan»

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Nombre** (obligatorio) | Un nombre que identifique el plan. Ej.: *Calendario sanitario 2026*. | Hasta 160 caracteres. |
| **Fecha de inicio** (obligatorio) | Desde cuándo rige el plan. | También sirve de referencia para las actividades periódicas que se calculan "desde la fecha inicial del plan". |
| **Fecha de fin** (opcional) | Hasta cuándo esperas que rija el plan. | Es informativa: el plan sigue **ACTIVO** hasta que tú lo finalices con **Finalizar**. No puede ser anterior a la fecha de inicio: el calendario no deja elegirla y, si la escribes igual, ves "La fecha de fin no puede ser anterior a la fecha de inicio" y **Crear plan** queda bloqueado. |
| **Descripción** (opcional) | El objetivo del plan. | Hasta 2000 caracteres. |

El plan aplica a todas tus propiedades.

### Los campos del formulario «Agregar actividad»

El formulario se divide en siete bloques, en este orden. Los campos marcados
como **obligatorio** siempre hay que completarlos; el resto es opcional. La
columna **Reglas** dice qué se revisa antes de dejarte guardar.

#### Datos generales

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Nombre de la actividad** (obligatorio) | Un nombre corto que identifique la actividad dentro del plan. Ej.: *Vacunación Fiebre Aftosa*. | Hasta 200 caracteres. |
| **Tipo de actividad** (obligatorio) | Qué hace la actividad: **Vacunación**, **Desparasitación**, **Vitaminización**, **Prueba diagnóstica**, **Control ectoparasitario**, **Vigilancia epidemiológica**, **Tratamiento preventivo** u **Otra actividad**. | Empieza en **Vacunación**. Define dónde se puede usar: una jornada de vacunación solo ofrece actividades de tipo Vacunación. |
| **Clasificación regulatoria** (obligatorio) | Por qué existe en el plan: **Obligatorio SENASAG**, **Según campaña/riesgo**, **Recomendado por veterinario** o **Configurable**. | Empieza vacía y hay que elegir una. Se muestra como una etiqueta de color junto al nombre en la tabla del plan. Es informativa: no cambia cuándo se programa ni impide aplicarla. |
| **Actividad obligatoria** | Marca si la actividad no debería saltearse. | Si eliges **Obligatorio SENASAG** queda marcada y bloqueada. También es informativa. |
| **Descripción** (opcional) | El objetivo o contexto de la actividad, en tus palabras. | Hasta 2000 caracteres. |

#### Cuándo se programa

Elige la **Modalidad** (obligatorio). Empieza en **Manual**; al cambiarla
aparecen los campos propios de cada una.

**Modalidad Por edad**

Para una actividad que corresponde a una edad concreta del animal, por
ejemplo desparasitar al destete. Cada animal tiene su propia fecha: su fecha
de nacimiento más la edad objetivo.

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Edad objetivo** (obligatorio) | La edad a la que corresponde. Ej.: *7*. | Número entero, 1 o más. |
| **Unidad de la edad objetivo** | **Días**, **Meses** o **Años**. | Empieza en **Meses**. Un mes se cuenta como 30 días y un año como 365. |
| **Ventana anticipada (días)** | Cuántos días antes de la fecha pasa a considerarse próxima a aplicar. | 0 o más. Empieza en 0. |
| **Ventana posterior (días)** | Días de gracia después de la fecha. | 0 o más. Empieza en 0. Ver **Importante** más abajo. |
| **Una sola vez en la vida del animal** | Evita repetirla a quien ya la recibió. | Marcada por defecto. Si el animal ya recibió esta actividad —incluso en una versión anterior o declarada por un proveedor— no se le vuelve a programar. Desmarcada, se programa aunque ya la tenga. |
| **Excluir animales con fecha de nacimiento estimada** | Deja fuera a los animales cuya fecha de nacimiento no está confirmada. | Sin marcar, entran. |

Los animales sin fecha de nacimiento nunca entran en esta modalidad, porque
no se puede calcular su fecha.

**Modalidad Periódica**

Para una actividad que se repite cada cierto tiempo, por ejemplo la
vacunación contra aftosa.

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Frecuencia** (obligatorio) | Cada cuánto se repite. Ej.: *6*. | Número entero, 1 o más. Empieza en 90. |
| **Unidad de frecuencia** | **Días**, **Semanas**, **Meses** o **Años**. | Empieza en **Días**. Una semana son 7 días, un mes 30 y un año 365. |
| **Se calcula desde** | El punto de partida: **Fecha de ingreso** (a la finca), **Fecha de nacimiento**, **Última aplicación**, **Fecha inicial del plan** o **Fecha configurada**. | Empieza en **Última aplicación**: cuenta desde la última vez que se le aplicó esta actividad al animal (incluidas las aplicaciones hechas con versiones anteriores de la actividad) y, si nunca se le aplicó, desde el día en que creaste la actividad. Cada vez que registras una aplicación, las fechas pendientes de ese animal se recalculan desde ella y las que ya no corresponden se cancelan. **Fecha configurada** cuenta desde el día en que creaste la actividad. En todos los casos la primera fecha cae una frecuencia después del punto de partida. |
| **Tolerancia anticipada (días)** | Cuántos días antes de cada fecha pasa a considerarse próxima a aplicar. | 0 o más. Empieza en 0. |
| **Tolerancia posterior (días)** | Días de gracia después de cada fecha. | 0 o más. Empieza en 0. Ver **Importante**. |

El sistema agenda hasta 36 repeticiones por animal, dentro del período de
**Proyección del calendario (meses)** (ver más abajo).

**Modalidad Fecha programada**

Para algo que se hace una sola vez en un día y hora concretos.

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Fecha y hora programada** (obligatorio) | Cuándo se hace. | Se programa una sola vez. Para repetirla usa la modalidad **Periódica**. Solo se agenda si cae dentro del período de proyección del calendario. |

**Modalidad Por hallazgo**

Para una actividad que se dispara cuando se detecta algo, no por fecha.

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Hallazgos que activan esta actividad** | Marca uno o más: **Caso clínico abierto**, **Control ectoparasitario con carga alta**, **Examen reproductivo no apto** o **Control neonatal con alerta**. | Hay que marcar al menos uno: si no, ves "Elige al menos un hallazgo que active la actividad" y el botón queda bloqueado. |
| **Plazo para resolverlo (días)** | En cuántos días debería resolverse. | 0 o más. Opcional. |
| **Requiere validación veterinaria** | Marca si un veterinario debe validarla. | Sin marcar por defecto. |

**Modalidad Manual**

No se agenda sola: se elige al preparar una jornada. No pide más datos y no
genera fechas en el calendario.

**Proyección del calendario (meses):** en la misma pestaña **Planes
sanitarios** está este ajuste (entre 1 y 24 meses, por defecto 12). Define
hasta cuántos meses hacia adelante el sistema agenda las fechas.

#### A quién aplica

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Categorías** | Marca una o varias categorías de animal. | Sin marcar aplica a todas. Debajo ves cuántas marcaste. |
| **Sexo aplicable** | **Ambos**, **Macho** o **Hembra**. | Empieza en **Ambos**. |
| **Unidad de edad** | **Días**, **Meses** o **Años** para los dos campos siguientes. | Empieza en **Meses**. |
| **Desde** / **Hasta** | El rango de edad de los animales a los que corresponde. | Números enteros, 0 o más. **Hasta** debe ser igual o mayor que **Desde**; si no, ves "La edad máxima no puede ser menor que la mínima". Deja uno vacío para no limitar ese extremo. |
| **Sin límite máximo** | Deja abierto el tope de edad. | Al marcarla, **Hasta** se desactiva. |
| **Incluir animales con edad desconocida** | Deja entrar a los animales sin fecha de nacimiento. | Solo importa si pusiste un rango de edad. Sin marcar, esos animales quedan excluidos. |

Debajo del rango ves una línea que confirma lo que se guardará, por ejemplo
"Se guardará el rango de 180 a 240 días". La edad se evalúa en la **fecha de
la actividad**, es decir, la edad que tendrá el animal ese día.

> **La edad objetivo debe caber en el rango.** Si la actividad es **Por
> edad**, el animal tiene exactamente la edad objetivo el día de la
> actividad. Si esa edad queda fuera del rango de **Desde** y **Hasta**,
> ningún animal sería elegible y la actividad nunca se programaría. Por eso
> el formulario lo avisa (por ejemplo "La edad objetivo (210 días) supera la
> edad máxima de los animales elegibles (180 días). Ajusta una de las dos")
> y bloquea el botón hasta que lo corrijas.

#### Medicamento recomendado (informativo, no es inventario)

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Producto recomendado** | Nombre comercial del medicamento o la vacuna sugeridos. Ej.: *Ivermectina 1%*. | Hasta 300 caracteres. |
| **Principio activo** | El componente activo del producto. Ej.: *Ivermectina*. | Hasta 200 caracteres. |
| **Instrucciones del veterinario** | Indicaciones para quien la aplique. Ej.: *Pesar al animal o usar el último peso medido vigente*. | Hasta 2000 caracteres. Se copian a cada aplicación que registres. |

Estos datos no descuentan ningún inventario: solo informan al preparar la
jornada y al armar la planilla de campo.

#### Dosis

Primero elige el **Tipo de cálculo**:

- **Fija por animal**: todos reciben la misma cantidad.
- **Por peso**: la cantidad depende del peso de cada animal.
- **Según indicación**: la define quien aplica, según la etiqueta o el veterinario.
- **No aplica (sin medicamento)**: para actividades como vigilancia o
  controles; no se piden más datos. Es la opción con la que empieza.

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Cantidad** (obligatorio si el cálculo es **Por peso**) | La dosis en números. Ej.: *1*. | Mayor que 0. Si la dejas vacía en una dosis por peso, ves "La dosis por peso requiere una cantidad". |
| **Unidad** | ml, mg, g, tableta, dosis, gota, aplicación, ml por kg, ml por 10 kg, ml por 50 kg, mg por kg u otra. | El formulario no la exige, pero elígela: sin unidad la dosis se muestra sin unidad. |
| **Detalle de unidad** | Solo si elegiste la unidad "otra": cuál es. | Hasta 100 caracteres. |
| **Peso de referencia (kg)** (obligatorio si el cálculo es **Por peso**) | El peso al que corresponde la cantidad. Ej.: *50*. | 0,1 o más. Con las unidades "ml por kg", "ml por 10 kg", "ml por 50 kg" y "mg por kg" se completa solo (1, 10 o 50) y no se puede cambiar. Con "ml", "mg" u otras lo escribes tú. |
| **Dosis mínima** (opcional) | Límite hacia abajo. | Solo en dosis por peso. Mayor que 0. |
| **Dosis máxima** (opcional) | Límite hacia arriba. | Solo en dosis por peso. Mayor que 0 y no menor que la mínima: si no, ves "La dosis máxima no puede ser menor que la mínima". |

**Cómo se calcula la dosis por peso.** Al confirmar una jornada, el sistema
calcula: cantidad × peso del animal ÷ peso de referencia. Después la ajusta
al mínimo o al máximo si se pasa. Usa el peso medido registrado del animal y,
si no tiene, el estimado. Si el animal no tiene ningún peso registrado, la
jornada no puede calcular la dosis.

Ejemplo: 1 ml por cada 50 kg (cantidad *1*, peso de referencia *50*), con
mínimo *2* y máximo *10*:

| Animal | Cálculo | Dosis |
| --- | --- | --- |
| Ternero de 180 kg | 1 × 180 ÷ 50 | 3,6 ml |
| Ternero de 60 kg | 1 × 60 ÷ 50 = 1,2, por debajo del mínimo | 2 ml |
| Novillo de 600 kg | 1 × 600 ÷ 50 = 12, por encima del máximo | 10 ml |

> No pongas como peso de referencia el peso "típico" de tus animales. Debe
> ser el peso al que corresponde la cantidad que escribiste, tal como dice
> la etiqueta. Si la etiqueta dice "1 ml cada 50 kg", el peso de referencia
> es 50, aunque tus terneros pesen 200.

#### Vía y lugar de aplicación

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Vía de administración** | Cómo se aplica: **Subcutánea**, **Intramuscular**, **Intravenosa**, **Oral**, **Tópica**, **Pour-on**, **Intranasal**, **Otra** o **No aplica**. | Opcional. |
| **Detalle de la vía** | Solo si elegiste **Otra**: cuál. | Obligatorio en ese caso. Hasta 100 caracteres. |
| **Lugar anatómico** | Dónde va la dosis: **Cuello**, **Tabla del cuello**, **Región escapular**, **Lomo**, **Línea dorsal**, **Boca**, **Fosa nasal**, **Todo el cuerpo**, **Otro** o **No aplica**. | Ver la tabla siguiente. |
| **Detalle del lugar** | Solo si elegiste **Otro**: cuál. | Obligatorio en ese caso. Hasta 100 caracteres. |

La vía y el lugar tienen que ser compatibles:

| Si la vía es… | El lugar debe ser… | Si no, ves… |
| --- | --- | --- |
| **Subcutánea**, **Intramuscular** o **Intravenosa** | Cualquiera menos "No aplica"; es obligatorio elegirlo. | "Una vía inyectable requiere indicar el lugar anatómico". |
| **Oral** | **Boca**, **No aplica** o ninguno. | "La vía oral solo admite «Boca» o «No aplica» como lugar". |
| **Pour-on** | **Línea dorsal**, **Lomo** o ninguno. | "Pour-on solo admite «Línea dorsal» o «Lomo» como lugar". |
| **Otra** | Cualquiera, pero pide el detalle de la vía. | "Indica el detalle de la vía". |

Si eliges **Otro** como lugar, siempre hay que escribir el detalle
("Indica el detalle del lugar"), aunque no hayas elegido vía.

#### Alertas

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Hora prevista de ejecución** (obligatorio) | A qué hora del día se hace la actividad, en hora de Bolivia. | Formato hora:minutos. Empieza en 08:00. |
| **Días de alerta** | Cuántos días antes se avisa. Ej.: *7* = aviso una semana antes; *0* = el mismo día. | 0 o más. Empieza en 0. |
| **Horarios de aviso** (obligatorio) | Las horas del día en que quieres el aviso, separadas por coma. Ej.: *07:00, 08:00*. | Entre 1 y 5 horas, sin repetir. Formato hora:minutos. Empieza en 08:00. |

**Cómo se avisa.** Ganadero crea **un solo aviso por fecha y lugar**, no uno
por animal: si la actividad corresponde a 60 animales de un mismo potrero,
verás un aviso que indica cuántos animales alcanza. El aviso sale los **días
de alerta** antes de la fecha, a la hora más temprana de la lista; la lista
completa queda registrada junto con el aviso.

#### Solo al editar una actividad

Cuando editas una actividad ya guardada, el formulario agrega dos campos al
final:

| Campo | Qué escribir | Reglas |
| --- | --- | --- |
| **Motivo del cambio** | Por qué haces el cambio. Ej.: *Cambio de protocolo del veterinario*. | Hasta 500 caracteres. Pasa a ser obligatorio si la actividad ya se usó (ver más abajo). |
| **Vigente desde** | Desde qué fecha y hora rige la nueva versión. | Pasa a ser obligatorio si la actividad ya se usó. |

### Avisos del formulario

Mientras completas el formulario, el botón **Agregar actividad** (o
**Guardar cambios**) se bloquea si algo no cumple las reglas, y el campo
afectado muestra debajo qué corregir. Estos son los avisos más comunes:

| Aviso | Qué pasó | Cómo se resuelve |
| --- | --- | --- |
| La edad objetivo supera la edad máxima (o es menor que la mínima) de los animales elegibles | En una actividad **Por edad**, la edad objetivo queda fuera del rango de **A quién aplica**. | Cambia la edad objetivo o amplía el rango. |
| La edad máxima no puede ser menor que la mínima | En el rango de edad, **Hasta** es menor que **Desde**. | Corrige uno de los dos. |
| Una vía inyectable requiere indicar el lugar anatómico | Elegiste una vía inyectable y no un lugar. | Elige el lugar (por ejemplo **Tabla del cuello**). |
| La vía oral / Pour-on solo admite… | El lugar no es compatible con la vía. | Cambia el lugar o la vía. |
| Indica el detalle de la vía / del lugar | Elegiste **Otra** o **Otro** y no escribiste cuál. | Escribe el detalle. |
| La dosis por peso requiere una cantidad | La dosis es **Por peso** y falta la cantidad. | Escribe la cantidad. |
| La dosis máxima no puede ser menor que la mínima | El máximo es menor que el mínimo. | Corrige uno de los dos. |
| Elige al menos un hallazgo que active la actividad | La modalidad es **Por hallazgo** y no marcaste ninguno. | Marca al menos uno. |

Si intentas agregar una actividad a un plan **FINALIZADO** o **ANULADO**,
el sistema lo rechaza: esos planes ya no admiten cambios.

### Qué pasa después de guardar la actividad

1. **Aparece en la tabla del plan.** Ves su nombre con la clasificación, la
   modalidad, el medicamento, la dosis, la vía y lugar, la alerta, la versión
   (v1, v2…) y su estado (**ACTIVO** o **INACTIVO**). Para verla, abre el
   plan con la flecha que está a la izquierda de su nombre.
2. **Se agenda en el calendario** (solo las modalidades **Por edad**,
   **Periódica** y **Fecha programada**, y solo si el plan está **ACTIVO**).
   Las fechas se generan solas en cuanto guardas la actividad (o activas el
   plan), sin que tengas que hacer nada más; además se revisan cada vez que
   abres Ganadero y cada madrugada (a las 00:10) si la aplicación está en
   ejecución. Si tienes Google Calendar conectado y la sincronización
   automática activa, también se actualizan cada minuto. Las fechas aparecen
   en **Sanidad** → **Calendario**.
3. **Genera avisos.** Un aviso por fecha y lugar, como se explicó en
   **Alertas**.
4. **Se ofrece en las jornadas.** Al preparar una jornada del mismo tipo
   aparece en la lista solo si el plan está **ACTIVO** y la actividad está
   activa. Ahí el sistema vuelve a comprobar, animal por animal, categoría,
   sexo y edad, y te explica en **Excluidos** por qué alguno no entra. Al
   confirmar la jornada, la aplicación cierra en el calendario la fecha más
   cercana al día en que aplicaste, aunque ya estuviera vencida. El aviso de
   esa fecha se cierra cuando todos los animales del grupo la recibieron.

### Editar, desactivar y versiones

- **Editar.** En la fila de la actividad presiona el lápiz (**Editar
  actividad**). El formulario se abre con los datos guardados de la
  actividad, incluidos los de su modalidad. Solo se pueden editar
  actividades de planes en **BORRADOR** o **ACTIVO**.
- **Si la actividad nunca se usó**, los cambios se guardan sobre la misma
  actividad.
- **Si ya se usó** (tiene fechas en el calendario o aplicaciones) y cambias
  algo que altera lo que se aplica o a quién, el sistema **no sobrescribe**:
  crea una **versión nueva**. Pide entonces el **Motivo del cambio** y
  **Vigente desde**; si los dejas vacíos, te avisa "Esta actividad ya se
  usó: indica el motivo del cambio" y los marca como obligatorios.
- **Qué crea una versión nueva:** cambiar el nombre, el tipo, la modalidad o
  sus datos, el medicamento, la dosis (incluido el peso de referencia), la
  vía, el lugar, la categoría, el sexo, el rango de edad, **Incluir animales
  con edad desconocida**, las instrucciones o las alertas.
- **Qué no la crea:** cambiar la descripción, la clasificación regulatoria o
  **Actividad obligatoria**. Esos cambios se guardan directamente.
- **Qué pasa con el calendario al crear una versión.** Las fechas pendientes
  de la versión anterior se cancelan y la nueva versión las vuelve a
  generar con sus datos, para que ningún animal tenga la misma actividad
  programada dos veces. Lo ya aplicado y lo ya vencido se conserva en la
  versión anterior. En la tabla del plan ves siempre la versión vigente.
- **Desactivar.** El ícono de encendido (**Desactivar actividad**) deja la
  actividad en **INACTIVO**. El sistema pide confirmación y cancela sus
  fechas pendientes del calendario. Al **Activar** de nuevo, recupera las
  fechas pendientes que todavía no habían vencido.

### Ejemplo

**Desparasitación de terneros al destete (modalidad Por edad)**

- Plan: Calendario sanitario 2026 (estado **ACTIVO**)
- **Nombre de la actividad:** Desparasitación al destete
- **Tipo de actividad:** Desparasitación
- **Clasificación regulatoria:** Recomendado por veterinario · sin marcar
  obligatoria
- **Modalidad:** Por edad · **Edad objetivo:** 7 · **Unidad:** Meses
- **Ventana anticipada:** 15 días · **Ventana posterior:** 30 días
- **Una sola vez en la vida del animal:** marcada
- **Categorías:** las categorías de terneros de tu finca · **Sexo:** Ambos
- **Edad de los animales elegibles:** Desde 6 hasta 8 meses (queda en 180 a
  240 días, y 7 meses son 210 días, así que cabe)
- **Producto recomendado:** Ivermectina 1% · **Principio activo:**
  Ivermectina
- **Instrucciones:** Pesar al animal antes de aplicar
- **Tipo de cálculo:** Por peso · **Cantidad:** 1 · **Unidad:** ml por 50 kg
  (el peso de referencia queda en 50) · **Dosis mínima:** 2 · **Dosis
  máxima:** 10
- **Vía:** Subcutánea · **Lugar:** Región escapular
- **Hora prevista:** 08:00 · **Días de alerta:** 7 · **Horarios de aviso:**
  07:00, 08:00

Qué hace el sistema con esa actividad:

- Un ternero nacido el 01/03/2026 cumple 7 meses (210 días) el 27/09/2026.
  Desde el 12/09/2026 figura como próximo a aplicar y el aviso sale el
  20/09/2026 a las 07:00.
- Una ternera de 3 meses tiene su fecha más adelante: queda agendada, y
  pasa a "próxima" 15 días antes.
- Un novillo de 3 años no recibe nada: su fecha ya pasó antes de que crearas
  la actividad.
- Un animal sin fecha de nacimiento, o uno que ya recibió esta
  desparasitación, tampoco.
- Un intento de guardar con **Edad objetivo** de 7 meses y elegibles solo
  hasta 6 meses se bloquea con el aviso de edad.

**Vacunación contra aftosa (modalidad Periódica)**

- **Nombre de la actividad:** Vacunación Fiebre Aftosa
- **Tipo de actividad:** Vacunación · **Clasificación regulatoria:**
  Obligatorio SENASAG (la casilla de obligatoria queda marcada sola)
- **Modalidad:** Periódica · **Frecuencia:** 6 · **Unidad:** Meses ·
  **Se calcula desde:** Fecha inicial del plan
- **Tolerancia anticipada:** 15 días · **Tolerancia posterior:** 30 días
- **Producto recomendado:** Vacuna antiaftosa
- **Tipo de cálculo:** Fija por animal · **Cantidad:** 2 · **Unidad:** ml
- **Vía:** Subcutánea · **Lugar:** Tabla del cuello
- **Hora prevista:** 08:00 · **Días de alerta:** 3 · **Horarios de aviso:**
  06:00, 07:00

Las fechas y la dosis de las campañas las define SENASAG y el fabricante:
confirma siempre el calendario vigente y la etiqueta del producto antes de
cargarlos.

### Resultado esperado

La actividad queda **ACTIVA** dentro del plan, en la versión 1. Desde ese
momento el sistema proyecta las próximas fechas para los animales elegibles
y genera **un aviso por fecha y lugar**, no uno por animal. La actividad
puede elegirse al preparar una jornada del mismo tipo, mientras el plan esté
**ACTIVO**.

### Importante

> Las **ventanas y tolerancias posteriores** no atrasan el momento en que
> una fecha se marca como **vencida**: una fecha pasa a vencida cuando
> llega el día previsto. Sirven para decidir si al crear la actividad se
> agenda una fecha que ya pasó. Las **anticipadas** sí adelantan el momento
> en que la actividad pasa a considerarse próxima.

> Si aplicas una actividad después de su fecha prevista, la aplicación
> igual cierra esa fecha: el sistema toma la fecha del calendario más
> cercana al día en que aplicaste, esté pendiente o ya vencida. Una fecha
> vencida hace mucho, cuando había otras más cercanas al día de la
> aplicación, se queda vencida como registro de lo que no se cumplió a
> tiempo.

> Al crear una actividad, el sistema no agenda fechas que ya habían pasado
> antes de que la actividad existiera: así el calendario no se llena de
> fechas vencidas del pasado. Si una fecha se venció estando la actividad
> ya vigente, esa sí queda registrada como vencida.

> La clasificación regulatoria y **Actividad obligatoria** son solo
> informativas: no impiden desactivar la actividad ni aplicarla.

> Si editas una actividad que ya fue usada, el sistema no sobrescribe los
> datos anteriores: crea una nueva versión (indica el motivo del cambio)
> para saber exactamente qué se aplicó en cada momento.

### Edades mínimas para examen reproductivo

Dentro de **Sanidad** → **Planes sanitarios** hay una sección aparte (no es una actividad
del plan) llamada "Edades mínimas para examen reproductivo", con dos campos: una edad en
meses para **machos** y otra para **hembras**.

**Para qué sirve:** es el requisito mínimo de edad, por sexo, para poder **registrar** un
examen reproductivo a un animal puntual (ver "Controles individuales" más abajo). No hay
valores predefinidos — los define quien tenga permiso de administración, junto con el
responsable veterinario, según el criterio de la finca.

**Cómo se aplica:** al abrir **Controles** con un animal seleccionado, el sistema calcula
su edad actual y **deshabilita** el botón "Registrar examen reproductivo" si todavía no
alcanzó la edad mínima configurada para su sexo; el aviso debajo del botón indica la edad
mínima exigida. Además, al guardar se vuelve a comprobar contra la **fecha del examen**:
si esa fecha es anterior a cuando el animal cumple la edad mínima, el registro se rechaza.
Si el animal no tiene fecha de nacimiento registrada, tampoco se puede comprobar la edad y
el examen no se puede registrar.

**Qué NO hace:** cumplir la edad mínima no significa que el animal sea apto
reproductivamente — solo habilita que se pueda evaluar. La aptitud real (Apto / No apto /
En observación) la determina quien registra el examen, completando la evaluación física
correspondiente al sexo del animal y, si el resultado es Apto, una observación que
documente esa conclusión.

Mientras estas dos edades no estén configuradas, **no se puede registrar ningún examen
reproductivo** en el sistema (ver "Problemas frecuentes" al final del capítulo).

## Enfermedades

### Para qué sirve

Mantener el catálogo de enfermedades que se usa para clasificar los casos
clínicos y detectar posibles brotes.

### Procedimiento

1. Abre **Sanidad** → **Planes sanitarios** → sub-pestaña **Enfermedades**.
2. Presiona **Nueva enfermedad**.
3. Completa código, nombre y una descripción (signos, transmisión…), y marca
   "Es notificable a sanidad" si corresponde.
4. Presiona **Crear enfermedad**.

Para dejar de usarla en casos nuevos sin borrar el historial, presiona
**Desactivar** en su fila.

### Ejemplo

- Código: ENF-003
- Nombre: Fiebre Aftosa
- Notificable: Sí

### Resultado esperado

La enfermedad queda disponible para vincularla a un **Caso clínico**.

## Jornada sanitaria

### Para qué sirve

Aplicar una actividad del plan (por ejemplo, una vacunación) a un grupo de
animales el mismo día, verificando automáticamente quién puede recibirla y
dejando registro de lo que realmente se aplicó a cada uno.

### Antes de comenzar

Debes tener un plan sanitario **ACTIVO** con al menos una actividad activa
del tipo que vas a aplicar (vacunación, desparasitación, etc.). Sin eso, no
habrá ninguna actividad para elegir al preparar la jornada.

### Procedimiento

1. Abre **Sanidad** → pestaña **Jornadas**.
2. Presiona el botón **+** ("Nueva jornada").
3. Selecciona el tipo de jornada, la fecha de inicio, la propiedad y, si
   querés acotarla, el potrero y/o el lote.
4. Agrega observaciones si corresponde y presiona **Crear jornada**. Queda
   en estado **Borrador** y, si tenés permiso para confirmar jornadas, el
   sistema abre directamente **Preparar jornada**.
5. En **Preparar jornada**, elige la actividad del plan (solo se listan
   actividades activas del tipo elegido) y la fecha de aplicación.
6. Revisa los criterios de elegibilidad aplicados automáticamente
   (categoría, sexo, edad) y las pestañas **Elegibles** / **Excluidos**; en
   "Excluidos" se explica el motivo de cada exclusión. Si la actividad tiene
   varias categorías, aparecen todas separadas por comas (por ejemplo,
   *Categorías: Vaca, Vaquillona*); si no tiene ninguna, dice *Todas*.
7. Marca los animales a incluir, o presiona **Seleccionar todos los
   elegibles**.
8. Si vas a hacer el trabajo en el campo sin llevar la computadora,
   presiona **Exportar planilla** antes de continuar (ver más abajo).
9. Presiona **Continuar a confirmación**.
10. En **Confirmar jornada**, revisa los datos recomendados (medicamento,
    dosis, instrucciones) y ajustá lo que haya sido distinto en la
    práctica: dosis aplicada y motivo del ajuste, producto realmente
    aplicado, vía, lugar, días de retiro de carne y de leche, resultado y
    observaciones.
11. Presiona **Confirmar jornada**.

**Exportar planilla** genera una hoja con los animales que marcaste, la
actividad, el producto y dosis recomendados, y columnas en blanco para
tickear a mano en el campo: **Aplicado**, **Dosis aplicada** y
**Observaciones**. Desde **Ganadero Desktop** se guarda como un archivo
Excel (.xlsx) de verdad, eligiendo dónde con el diálogo de guardar
habitual; desde la versión web descarga un CSV con la misma información
(se abre igual en Excel). De vuelta de la visita, usá esa hoja como
referencia para completar **Confirmar jornada**.

Mientras una jornada esté en **Borrador**, podés usar los íconos de la fila
para **Editar jornada** o **Cancelar jornada** (con motivo).

### Ejemplo

- Tipo: Desparasitación
- Propiedad: Estancia El Roble · Potrero 4
- Fecha: 10/09/2026
- Actividad: Desparasitación trimestral con Ivermectina 1%, dosis por peso
- Resultado: 42 elegibles de 45 (3 excluidos por edad), dosis ajustada según
  el peso vigente de cada animal

### Resultado esperado

La jornada queda **Confirmada**, con un mensaje del tipo "Se registraron 42
aplicaciones", visibles luego en el historial sanitario de cada animal. Ahí
mismo aparece el botón **Agregar otra actividad a esta misma visita**: si
ese día también corresponde, por ejemplo, una desparasitación además de la
vacunación, este botón abre **Nueva jornada** con la fecha, propiedad,
potrero y lote ya completados, y al preparar esa jornada te propone como
punto de partida los mismos animales de la visita anterior que también
sean elegibles para la nueva actividad — no hace falta volver a buscarlos
uno por uno.

### Importante

> Editar una jornada en Borrador reinicia la selección de animales — hay
> que volver a prepararla para que se revalide la elegibilidad con los
> datos nuevos.

> "Agregar otra actividad a esta misma visita" solo precompleta el
> formulario y sugiere animales; sigue siendo una jornada aparte, con su
> propia preparación y confirmación — está pensado para no tener que
> volver al campo un día distinto por cada actividad.

### Ver el detalle de una jornada aplicada

Cuando una jornada queda **Confirmada**, su fila muestra el ícono del ojo
(**Ver detalle de la jornada**). Presiona ese ícono para abrir una ventana
de solo lectura con la fecha, la propiedad, el potrero y el lote de la
jornada, y una tabla con cada animal aplicado: **producto**, **dosis
aplicada** y su unidad, **vía / lugar**, **días de retiro** de carne y de
leche, **resultado**, **observaciones** y **estado** de esa aplicación.

La ventana no permite editar nada: sirve para consultar y auditar lo que
quedó registrado en esa visita.

## Controles individuales

### Para qué sirve

Registrar chequeos clínicos puntuales de un animal específico — al nacer,
por presencia de parásitos externos, o para evaluar su aptitud
reproductiva — que no forman parte de una jornada grupal.

### Antes de comenzar

El animal debe existir en el inventario. Según el control:

- **Control neonatal**: el animal debe estar activo, con fecha de
  nacimiento confirmada (no estimada) y una edad de hasta 7 días.
- **Examen reproductivo**: el animal debe tener fecha de nacimiento
  registrada y estar activo. Además, alguien con permiso de administración
  debe haber cargado antes las edades mínimas en **Sanidad** → **Planes
  sanitarios** → "Edades mínimas para examen reproductivo" (una para
  machos y otra para hembras, en meses); sin eso, el examen no se puede
  guardar.

### Procedimiento

1. Abre **Sanidad** → pestaña **Controles**.
2. Busca y selecciona el animal.
3. Según el control que quieras registrar:
   - **Control neonatal**: presiona **Registrar control neonatal**. Elige
     el momento (Día 0 o Primera semana), la fecha, si el animal quedó
     correctamente calostrado, temperatura, si tiene diarrea, estado del
     ombligo y observaciones. Presiona **Guardar control**.
   - **Control ectoparasitario**: presiona **Registrar control
     ectoparasitario**. Completa los campos según la visita (ver "Campos del
     control ectoparasitario" más abajo) y presiona **Guardar control**.
   - **Examen reproductivo**: presiona **Registrar examen reproductivo**
     y completa los campos según el sexo del animal (ver "Campos del
     examen reproductivo" más abajo). Presiona **Guardar examen**. Si el
     botón está deshabilitado, el animal todavía no alcanza la edad mínima
     configurada y el aviso debajo lo indica.

### Campos del control ectoparasitario

El formulario **Registrar control ectoparasitario** registra el control contra
el animal que tengas seleccionado en **Controles**, o contra un lote completo
si lo abres desde la ficha de ese lote (**Lotes**). El destino no se elige
dentro del formulario: queda fijado por dónde lo abriste, y el título muestra
para quién es.

| Campo | Qué escribir | Ejemplo |
| --- | --- | --- |
| **Tipo** (obligatorio) | El parásito externo que evaluaste: **Garrapata**, **Mosca de los cuernos**, **Tórsalo**, **Piojos** u **Otro**. Viene marcado *Garrapata*. | Garrapata |
| **Nivel de carga** (obligatorio) | La intensidad de la infestación: **Bajo** (neutro), **Medio** (ámbar) o **Alto** (rojo). Viene marcado *Bajo*. | Alto |
| **Fecha** (obligatorio) | El día del control. No puede ser futuro ni anterior al nacimiento o al ingreso del animal. | 15/09/2026 |
| **Tratado en este registro** | Márcalo si en esta misma visita aplicaste un tratamiento. Si no, déjalo sin marcar. | Marcado |
| **Producto** | Nombre comercial del producto aplicado (texto libre, opcional). | Bayticol |
| **Principio activo** | El componente activo del producto (texto libre, opcional). Dispara el aviso de rotación. | Flumetrina |
| **Observaciones** | Qué encontraste y qué hiciste: zonas afectadas, si trataste todo el lote, cuándo revisar de nuevo (opcional). | Garrapatas en ubre y orejas; se bañó el lote. Revisar en 14 días. |

**Producto** y **Principio activo** no son obligatorios, ni siquiera cuando
marcas **Tratado en este registro**: quedan como una nota de lo aplicado.

#### Aviso de rotación del principio activo

Al escribir un **Principio activo**, el sistema lo compara con los de los
**últimos tres** controles del mismo animal o lote. Si coincide (sin distinguir
mayúsculas), muestra un aviso ámbar con esos principios activos para que
consideres rotarlo y evitar que el parásito desarrolle resistencia. Es solo un
aviso: no impide guardar el control.

#### Qué implica una carga Alta

Si registras **Nivel de carga** *Alto* para un animal, el sistema deja un
hallazgo sanitario positivo, que puede disparar una alerta o una actividad
**Por hallazgo** del plan (por ejemplo, una revisión de seguimiento). Para un
lote no se genera ese hallazgo.

#### Ejemplos

**Vaca BOV-0142, garrapata alta:** fecha 15/09/2026; tipo Garrapata; nivel de
carga Alto; **Tratado en este registro** marcado; producto Bayticol; principio
activo Flumetrina; observaciones "Garrapatas en ubre y orejas; se bañó el lote.
Revisar en 14 días.".

**Lote Recría 2026, mosca de los cuernos:** fecha 15/09/2026; tipo Mosca de los
cuernos; nivel de carga Medio; **Tratado en este registro** sin marcar; producto
y principio activo vacíos; observaciones "Hay moscas, pero no se justifica
tratar todavía.".

### Campos del examen reproductivo

El formulario **Registrar examen reproductivo** cambia los campos según
el **sexo del animal**. Arriba muestra la edad mínima configurada para su
sexo y, antes de guardar, el sistema comprueba que el animal tenga fecha
de nacimiento registrada y la haya alcanzado en la fecha del examen. Si el
animal todavía no alcanza esa edad, el botón "Registrar examen reproductivo"
aparece deshabilitado en **Controles**.

#### Datos comunes

| Campo | Qué escribir | Ejemplo |
| --- | --- | --- |
| **Fecha** | Día en que se hizo el examen. No puede ser futuro, ni anterior al nacimiento o al ingreso del animal. | 10/09/2026 |
| **Resultado general** | Conclusión del examen. Opciones: **Apto**, **No apto** o **En observación** (viene marcado *En observación*). | Apto |
| **Veterinario responsable** | Nombre de quien realizó o supervisó el examen (texto libre, opcional). | Dra. Ana Quispe |

#### En un toro (macho)

| Campo | Qué escribir | Ejemplo |
| --- | --- | --- |
| **Circunferencia escrotal (cm)** | Medida de la circunferencia del escroto. Debe ser mayor a cero. | 38.5 |
| **Motilidad espermática (%)** | Porcentaje de espermatozoides móviles, de 0 a 100. | 70 |
| **Morfología (%)** | Porcentaje de espermatozoides con forma normal, de 0 a 100. | 80 |
| **Libido** | Descripción del comportamiento sexual observado. | Normal, monta activa y rápida |
| **Capacidad de servicio** | Estimación de cuántas hembras puede cubrir. | Apto para unas 25 vientres |

#### En una vaquilla o vaca (hembra)

| Campo | Qué escribir | Ejemplo |
| --- | --- | --- |
| **Peso (kg)** | Peso del animal el día del examen. Debe ser mayor a cero. | 320 |
| **Porcentaje de peso adulto (%)** | Qué parte del peso adulto esperado alcanzó, de 0 a 100. | 85 |
| **Condición corporal** | Estado de carnes en escala de 1 (muy flaca) a 5 (muy gorda). | 3.0 |
| **Desarrollo reproductivo** | Descripción del aparato reproductor. | Cuernos uterinos desarrollados, vulva normal |

#### Checklist de enfermedades

Seis listas, una por enfermedad, cada una con **Negativo**, **Positivo**
o **No realizado** (marcado por defecto). Las enfermedades son **IBR**,
**BVD**, **Brucelosis**, **Leptospirosis**, **Tricomoniasis** y
**Campylobacteriosis**. Si no hiciste una prueba, deja **No realizado**;
no se puede repetir la misma enfermedad en el mismo examen.

#### Observaciones

Campo de texto libre para la conclusión de la evaluación y las notas del
veterinario. Es **obligatorio cuando el resultado es Apto**.

> Para guardar **Apto** el sistema exige que **Observaciones** no esté
> vacío y que la evaluación física del sexo esté completa: en machos,
> circunferencia escrotal, motilidad y morfología; en hembras, peso,
> condición corporal y desarrollo reproductivo.

#### Ejemplos

**Toro BOV-0142, Apto:** fecha 10/09/2026; veterinario responsable "Dra.
Ana Quispe"; resultado general Apto; circunferencia escrotal 38.5 cm;
motilidad espermática 70 %; morfología
80 %; libido "Normal, monta activa"; capacidad de servicio "Apto para
unas 25 vientres"; Brucelosis y Leptospirosis en **Negativo** (el resto
**No realizado**); observaciones "Andrológico sin hallazgos. Apto para
servicio.".

**Vaquilla BOV-0210, En observación:** fecha 10/09/2026; resultado
general En observación; peso 290 kg; porcentaje de peso adulto 78 %;
condición corporal 2.5; desarrollo reproductivo "Aún inmaduro; repetir en
60 días"; checklist todo en **No realizado**; observaciones "No alcanza
el peso adulto objetivo; se reprograma la evaluación.".

### Ejemplo

- Animal: BOV-0142
- Control ectoparasitario del 05/09/2026: tipo Garrapata, carga Alta,
  tratado con Amitraz.
- Examen reproductivo del 10/09/2026 (toro BOV-0142): Apto, con la
  evaluación física y el checklist completos (ver "Campos del examen
  reproductivo").

### Resultado esperado

El control queda en el historial sanitario del animal, visible desde esta
misma pantalla la próxima vez que lo busques.

### Importante

> Cumplir la edad mínima configurada para el examen reproductivo no
> acredita por sí sola la aptitud del animal: el resultado depende de la
> evaluación física y del checklist de enfermedades, y una fecha de
> nacimiento estimada tampoco acredita la madurez por sí sola.

## Casos clínicos

### Para qué sirve

Dar seguimiento a un problema de salud de un animal desde que se detecta
hasta que se resuelve.

### Procedimiento

1. Abre **Sanidad** → pestaña **Casos clínicos**.
2. Presiona **Nuevo caso**.
3. Selecciona el animal, la fecha de inicio, la enfermedad (si se conoce, o
   dejalo "Sin identificar"), la severidad (Leve, Moderada, Grave o
   Crítica) y el veterinario responsable si querés dejarlo anotado, y
   describe los síntomas observados.
4. Agrega diagnóstico y observaciones si corresponde, y presiona **Crear
   caso**.
5. Cuando el problema se resuelve, presiona **Cerrar caso** y describe el
   resultado (por ejemplo, "Recuperado, sin complicaciones").

### Ejemplo

- Animal: BOV-0098
- Síntomas: Fiebre, decaimiento, secreción nasal
- Enfermedad: IBR
- Severidad: Grave

### Resultado esperado

El caso aparece en "Casos por atender" del **Resumen** de Sanidad mientras
esté abierto, y deja de aparecer ahí al cerrarlo.

## Tratamientos y aplicaciones

### Para qué sirve

Llevar el protocolo de dosificación de un tratamiento (por ejemplo, tras un
caso clínico) y su cronograma de aplicaciones, incluyendo los días de
retiro de carne y de leche.

### Antes de comenzar

El animal a tratar debe existir en el inventario y estar activo. Si querés
asociar el tratamiento a un caso clínico, ese caso debe estar abierto (no
cerrado ni anulado).

### Procedimiento

1. Abre **Sanidad** → pestaña **Tratamientos**.
2. Presiona **Nuevo tratamiento**.
3. Busca y selecciona el animal. Si el animal tiene casos clínicos abiertos,
   podés asociar el tratamiento a uno de ellos (opcional); el tratamiento no
   puede comenzar antes de la fecha de ese caso.
4. Indica la fecha de inicio y, si querés, la hora de inicio (por defecto
   medianoche). La fecha fin estimada se completa sola con la última dosis
   del protocolo y no puede ser anterior a ella.
5. Completa el diagnóstico, el veterinario responsable (opcional) y las
   observaciones.
6. En "Protocolo de dosificación" completa el producto aplicado (texto
   libre, opcional), dosis, unidad, frecuencia (en horas), duración (en
   días), vía de administración y días de retiro de carne y de leche. Usa
   **Agregar otro protocolo** si el tratamiento combina más de un
   medicamento.
7. Presiona **Crear tratamiento**. Queda en estado **Borrador**.
8. Presiona **Activar** para que el sistema genere el cronograma de
   aplicaciones según el protocolo.
9. Abre **Aplicaciones** para ver las próximas dosis por aplicar. Presiona
   **Aplicar** en la que corresponda, confirma la dosis realmente aplicada
   y observaciones, y presiona **Guardar aplicación**.
10. Si el tratamiento se atrasó, usá **Regenerar cronograma**: conserva las
    dosis ya aplicadas y reprograma las restantes desde el momento en que lo
    presionás (una cada **Frecuencia**), actualizando la fecha fin estimada.
11. Cuando ya no queden aplicaciones pendientes ni atrasadas (todas
    aplicadas o canceladas), presiona **Finalizar**.

Con el botón **Marcar atrasadas**, en la parte superior del listado, podés
actualizar de una vez el estado de todas las aplicaciones vencidas de todos
los tratamientos.

### Los campos del formulario «Nuevo tratamiento»

Los campos marcados como **obligatorio** siempre hay que completarlos; el
resto es opcional.

#### Datos generales

| Campo | Qué escribir |
| --- | --- |
| **Animal** (obligatorio) | Buscá por código o nombre y seleccionalo. Solo aparecen animales activos. Ej.: *BOV-0098*. |
| **Caso clínico** | Si el animal tiene casos clínicos abiertos, podés asociarle el tratamiento a uno de ellos. El tratamiento no puede comenzar antes de la fecha de ese caso. Si lo asociás y luego activás el tratamiento, el caso pasa a **En tratamiento**. |
| **Fecha de inicio** (obligatorio) | El día de la primera dosis. No puede ser una fecha futura. |
| **Hora de inicio** | La hora de la primera dosis, en hora de Bolivia. Si la dejás vacía, se toma la medianoche. Ej.: *08:00*. |
| **Fecha fin estimada** (obligatorio) | El día en que se espera terminar. Se completa sola con la última dosis del protocolo y no puede ser anterior a ella. |
| **Veterinario responsable** | Quién indicó el tratamiento, como texto libre. Ej.: *Dr. Pérez*. |
| **Diagnóstico** | El problema detectado, en tus palabras. Ej.: *Mastitis*. |
| **Observaciones** | Notas adicionales. |

#### Protocolo de dosificación

Podés cargar más de un protocolo si el tratamiento combina medicamentos: usá
**Agregar otro protocolo**. Cada fila lleva estos campos.

| Campo | Qué escribir |
| --- | --- |
| **Dosis** (obligatorio) | La cantidad de cada aplicación. Ej.: *10*. |
| **Unidad de dosis** (obligatorio) | La unidad de esa cantidad. Ej.: *ml*, *cc*. |
| **Producto aplicado** | El nombre comercial del producto, como texto libre. Ej.: *Oxitetraciclina LA*. Es informativo: no descuenta inventario. |
| **Frecuencia (horas)** (obligatorio) | Cada cuántas horas se repite la dosis. Ej.: *24*. |
| **Duración (días)** (obligatorio) | Por cuántos días se aplica. Ej.: *5*. |
| **Vía de administración** | Cómo se aplica. Ej.: *IM*, *M*. |
| **Retiro carne (días)** | Días en que el animal no puede destinarse a carne después de la última dosis. Ej.: *14*. |
| **Retiro leche (días)** | Días en que no se puede vender la leche después de la última dosis. |

### Ejemplo

- Animal: BOV-0098 (siguiendo el caso clínico de Fiebre/IBR del ejemplo
  anterior)
- Caso clínico: Fiebre/IBR
- Inicio: 01/09/2026 a las 08:00
- Fin estimado: 05/09/2026 (se completa solo)
- Protocolo: Oxitetraciclina LA, 10 ml, cada 24 horas, durante 5 días, vía
  intramuscular
- Retiro de carne: 14 días

### Resultado esperado

Al **activar** el tratamiento, el sistema genera el cronograma: una
aplicación en la fecha y hora de inicio y luego una por cada intervalo de
**Frecuencia (horas)**, mientras siga dentro de la **Duración (días)**. Por
cada una programa un aviso 30 minutos antes. Los días de **Retiro** de carne
y de leche quedan vigentes hasta cumplirse.

Si asociaste un caso clínico, este pasa a **En tratamiento**. Cada dosis
programada queda como pendiente, atrasada o aplicada en el historial del
tratamiento. Al finalizarlo, su estado pasa a **Finalizado**.

### Importante

> La **Hora de inicio** marca la primera dosis; de ahí en adelante el
> cronograma avanza por la **Frecuencia**. Si no la completás, la primera
> dosis se programa a medianoche (hora de Bolivia).
>
> La **Fecha fin estimada** no puede ser anterior a la última dosis
> programada: si acortás la duración, el sistema te obliga a ajustarla.
>
> El **Producto aplicado** es informativo y no descuenta inventario, igual
> que en el plan sanitario.
>
> **Regenerar cronograma** no borra el historial: las dosis pendientes o
> atrasadas anteriores quedan como canceladas y las ya aplicadas se
> conservan.

## Calendario y Google Calendar

### Para qué sirve

Ver el estado de sincronización de las actividades sanitarias proyectadas
(próximas jornadas) con un calendario de Google, y reintentar los envíos
que fallaron.

### Antes de comenzar

La cuenta de Google debe estar conectada desde **Mi finca** → **Google
Calendar** — importando el archivo JSON de credenciales OAuth y presionando
**Conectar Google**. Esa conexión solo está disponible desde **Ganadero
Desktop**, no desde la versión web.

Además, ten en cuenta que:

- Solo generan eventos de calendario las actividades del plan con
  modalidad **Por edad**, **Periódica** o **Fecha programada** — las de
  modalidad **Manual** no se sincronizan.
- El sistema solo proyecta actividades dentro de la ventana configurada en
  **Sanidad** → **Planes sanitarios** → "Proyección del calendario
  (meses)"; una actividad prevista más adelante que esa ventana todavía no
  va a aparecer en esta pestaña.

### Procedimiento

1. Abre **Sanidad** → pestaña **Calendario**.
2. Revisa la tabla: actividad, fecha prevista, ubicación (propiedad,
   potrero o lote), cantidad de animales involucrados y estado en Google
   Calendar.
3. Si una fila tiene el ícono de enlace externo, presiona ahí para abrir el
   evento directamente en Google Calendar.
4. Si una fila quedó con error, presiona el ícono de **reintentar**
   (flecha circular) para volver a intentar el envío.

### Ejemplo

- Actividad: Vacunación Fiebre Aftosa
- Fecha: 15/09/2026
- Ubicación: Estancia El Roble · Potrero 2
- Animales: 30
- Estado: SINCRONIZADO

### Resultado esperado

El evento aparece en el Google Calendar de la cuenta conectada, con el
recordatorio propio de Google.

## Problemas frecuentes

### No puedo agregar actividades a un plan

Solo se pueden agregar actividades a planes en **BORRADOR** o **ACTIVO**. Si
el plan está **FINALIZADO** o **ANULADO**, ya no admite cambios: crea un plan
nuevo. Si el plan está en borrador o activo y aun así no ves el botón
**Agregar actividad**, revisa que tengas el permiso para administrar planes
sanitarios.

### El botón «Agregar actividad» está bloqueado

Algún dato no cumple las reglas del formulario. Busca el campo que muestra un
mensaje en rojo debajo y corrígelo (ver **Avisos del formulario** en "Plan
sanitario"). Los casos más comunes: una vía inyectable sin lugar anatómico,
una dosis por peso sin cantidad, una edad objetivo fuera del rango de edad de
los animales elegibles, o una actividad por hallazgo sin ningún hallazgo
marcado.

### La edad objetivo me marca error aunque el rango parece correcto

La edad objetivo y el rango de **A quién aplica** se comparan en días: un mes
cuenta como 30 días y un año como 365, aunque los hayas escrito en unidades
distintas. Por ejemplo, 7 meses son 210 días y no caben en un rango de "hasta
6 meses" (180 días). Revisa la línea que aparece debajo del rango, que indica
cuántos días se guardarán, y ajusta la edad objetivo o el rango.

### Al guardar una edición me pide el motivo del cambio y desde cuándo rige

La actividad ya se usó (tiene fechas en el calendario o aplicaciones) y lo que
cambiaste altera lo que se aplica o a quién, así que el sistema crea una
**versión nueva** en vez de sobrescribir la anterior. Completa **Motivo del
cambio** y **Vigente desde** y vuelve a presionar **Guardar cambios**.

### No puedo presionar "Registrar examen reproductivo"

El animal todavía no alcanza la edad mínima configurada para su sexo. El
aviso debajo del botón indica la edad mínima exigida. Si prefieres otra
edad, cámbiala en **Sanidad** → **Planes sanitarios** → "Edades mínimas
para examen reproductivo".

### No puedo guardar un examen reproductivo

Revisa el mensaje del formulario. Si pide configurar las edades mínimas,
hazlo en **Sanidad** → **Planes sanitarios** → "Edades mínimas para examen
reproductivo". Si indica que el animal no alcanza la edad mínima, la fecha
del examen es anterior a cuando la cumple.

### No puedo presionar "Exportar planilla"

Faltan elegir la actividad y marcar al menos un animal — el botón se
habilita recién con esos dos datos listos.

### Exporté la planilla pero me dio un CSV en vez de un Excel

Estás en la versión web, no en **Ganadero Desktop**. El CSV trae la misma
información y se abre igual en Excel; el .xlsx real solo se genera desde
la aplicación de escritorio.

### Al preparar una jornada, no aparece ninguna actividad para elegir

No existe una actividad **activa** del tipo de jornada que elegiste, o está en
un plan que todavía no está **ACTIVO**. Créala, o activa el plan, en **Planes
sanitarios**.

### Edité una jornada y tengo que volver a elegir los animales

Es intencional: al editar una jornada en Borrador, la selección anterior se
descarta para revalidar la elegibilidad con los datos actualizados.

### No puedo finalizar un tratamiento

Todavía tiene aplicaciones **Pendientes** o **Atrasadas** en
**Aplicaciones**. Registralas con **Aplicar**, o si el protocolo cambió,
usa **Regenerar cronograma** para reemplazarlas.

### No aparece ningún caso clínico para asociar al tratamiento

El campo **Caso clínico** solo lista casos del animal elegido que estén
abiertos. Si el animal no tiene casos abiertos, aparece deshabilitado como
"Sin caso clínico": creá un caso en **Casos clínicos** o dejá el tratamiento
sin asociar.

### La fecha fin estimada no acepta la fecha que quiero

No puede ser anterior a la última dosis programada. Revisá la **Frecuencia
(horas)** y la **Duración (días)** del protocolo: la fecha mínima se completa
sola y se recalcula cuando ajustás esos valores.

### No puedo registrar un control neonatal para un animal

Solo se puede si el animal tiene fecha de nacimiento confirmada (no
estimada) y una edad de hasta 7 días. Fuera de ese período, registra un
**Caso clínico** en su lugar.

### No aparece la opción de conectar Google Calendar

Esa conexión solo está disponible desde **Ganadero Desktop**. Desde la
versión web no se puede conectar ni gestionar la cuenta de Google.

### Sincronicé pero no se generó ningún evento nuevo

Revisa la modalidad de la actividad: las actividades **Manuales** no
generan eventos de calendario. Usa **Por edad**, **Periódica** o **Fecha
programada** si necesitás que se sincronicen.

### Una actividad no aparece en el Calendario aunque está activa

Revisa estas causas, de la más común a la menos común:

- **El plan no está ACTIVO.** Los planes en borrador no generan calendario.
- **Su modalidad es Manual o Por hallazgo.** Esas no generan fechas
  automáticas.
- **Ninguna actividad tiene animales que le correspondan.** Es lo más común
  cuando el calendario queda vacío: por ejemplo, una actividad para
  **Ternero** y **Ternera** no genera nada si en la finca no hay animales de
  esas categorías. Revisa el punto de los animales más abajo.
- **Se creó con una versión anterior de Ganadero.** Las actividades guardadas
  antes de la actualización que genera el calendario al guardar no lo hicieron
  en ese momento. Cierra y vuelve a abrir Ganadero (se genera al iniciar), o
  guarda de nuevo la actividad.
- **Su fecha queda más adelante** que el período de "Proyección del
  calendario (meses)" (**Sanidad** → **Planes sanitarios**). Amplía ese
  período o espera a que la fecha entre en rango.
- **Los animales no cumplen** la categoría, el sexo o el rango de edad de la
  actividad. Además, en una actividad **Por edad** el animal debe tener fecha
  de nacimiento (y no ser una fecha estimada si marcaste excluirlas).
- **El animal ya recibió la actividad** y tiene marcado **Una sola vez en la
  vida del animal**.
- **Su fecha ya había pasado** antes de que crearas la actividad: esas
  fechas no se agendan.

### Finalicé o anulé un plan y desaparecieron fechas del calendario

Es lo esperado: al finalizar o anular un plan se cancelan las fechas
pendientes de sus actividades y sus avisos, y el plan queda solo para
consulta. Lo que ya se aplicó sigue en el historial de cada animal. No se
puede deshacer; si fue un error, crea un plan nuevo con las mismas
actividades.

### Desactivé una actividad y sus fechas desaparecieron del calendario

También es lo esperado: al desactivar una actividad se cancelan sus fechas
pendientes. Si la vuelves a **Activar**, recupera las que todavía no habían
vencido.

### Apliqué una actividad y en el calendario sigue una fecha como vencida

Al confirmar una jornada, el sistema cierra la fecha del calendario más
cercana al día de la aplicación, incluso si ya estaba vencida. Si aun así ves
una fecha vencida, suele ser una de estas:

- **Es una fecha anterior que no se cumplió a tiempo.** Por ejemplo, un baño
  que debía hacerse hace tres meses y se omitió: esa fecha queda vencida como
  registro, aunque hoy sí hayas aplicado la actividad.
- **El animal no estaba en la jornada.** El aviso de una fecha vencida se
  cierra recién cuando todos los animales de ese grupo la recibieron.
- **Es de una versión anterior de la actividad.** Las fechas vencidas de una
  versión anterior quedan como historial.

El historial sanitario de cada animal siempre muestra lo que realmente
recibió.

### Edité un ítem del plan que ya se había usado y no veo el cambio reflejado en jornadas anteriores

Es el comportamiento esperado: el sistema crea una nueva versión del ítem
en lugar de sobrescribir la anterior, para conservar exactamente lo que se
aplicó en cada jornada o tratamiento pasado. La versión nueva se usa desde
la fecha de vigencia que indiques hacia adelante. Las fechas pendientes de
la versión anterior se cancelan y la nueva las vuelve a generar, para que
ningún animal quede con la misma actividad programada dos veces.

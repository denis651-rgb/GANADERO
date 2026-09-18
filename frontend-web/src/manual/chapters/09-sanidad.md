# Sanidad

Gestiona la salud del hato: planes sanitarios, jornadas de vacunación o
desparasitación, controles individuales, casos clínicos, tratamientos y su
sincronización con un calendario externo.

## Plan sanitario

### Para qué sirve

Agrupar las vacunaciones, desparasitaciones y demás actividades sanitarias
que se deben cumplir en la finca, definiendo para cada una a quién aplica,
cuándo corresponde y qué alerta debe generar.

### Antes de comenzar

Si vas a limitar una actividad a una categoría de animal, esa categoría debe
existir ya en **Mi finca**.

### Procedimiento

1. Abre **Sanidad** en el menú lateral y entra a la pestaña **Planes
   sanitarios**.
2. Presiona **Nuevo plan** e indica nombre, fecha de inicio, fecha de fin
   (opcional) y descripción.
3. Presiona **Crear plan**. El plan queda en estado **BORRADOR**.
4. Presiona **Activar** sobre el plan. Solo un plan **ACTIVO** permite
   agregarle actividades.
5. Presiona **Agregar actividad** y completa los datos generales: nombre,
   tipo de actividad (Vacunación, Desparasitación, Vitaminización, Prueba
   diagnóstica, Control ectoparasitario, Vigilancia epidemiológica,
   Tratamiento preventivo u Otra actividad) y clasificación regulatoria
   (Obligatorio SENASAG, Según campaña/riesgo, Recomendado o Configurable).
6. En "Medicamento recomendado" indica el producto y el principio activo
   (es solo informativo, no descuenta ningún inventario) e instrucciones
   para quien la aplique.
7. Define la dosis: tipo de cálculo (Fija por animal, Por peso, Según
   indicación o No aplica) y, si corresponde, cantidad, unidad y peso de
   referencia.
8. Define la vía de administración y el lugar anatómico de aplicación.
9. En "Elegibilidad", limita la actividad por categoría, sexo y rango de
   edad, o déjala sin restricción. Podés marcar "Incluir animales con edad
   desconocida" para no excluirlos por falta de dato.
10. Elige la modalidad de programación (Por edad, Periódica, Fecha
    programada, Por hallazgo o Manual) y completa los campos según la
    modalidad: ventana de anticipación, frecuencia y desde cuándo se
    calcula, fecha exacta, o los hallazgos que la disparan.
11. Configura la alerta: hora prevista de ejecución, días de alerta y
    horarios de aviso (hasta cinco, separados por coma), y marca si es
    obligatoria.
12. Presiona **Agregar actividad**.

### Los campos del formulario «Agregar actividad»

El formulario se divide en siete bloques. Los campos marcados como
**obligatorio** siempre hay que completarlos; el resto es opcional.

#### Datos generales

| Campo | Qué escribir |
| --- | --- |
| **Nombre de la actividad** (obligatorio) | Un nombre corto que identifique la actividad dentro del plan. Ej.: *Vacunación Fiebre Aftosa*. |
| **Tipo de actividad** (obligatorio) | Qué hace la actividad: **Vacunación**, **Desparasitación**, **Vitaminización**, **Prueba diagnóstica**, **Control ectoparasitario**, **Vigilancia epidemiológica**, **Tratamiento preventivo** u **Otra actividad**. |
| **Clasificación regulatoria** (obligatorio) | Por qué existe en el plan: **Obligatorio SENASAG**, **Según campaña/riesgo**, **Recomendado** o **Configurable**. Las obligatorias de SENASAG no se pueden desactivar sin justificar. |
| **Descripción** (opcional) | El objetivo o contexto de la actividad, en tus palabras. |

#### Medicamento recomendado (informativo, no es inventario)

| Campo | Qué escribir |
| --- | --- |
| **Producto recomendado** | Nombre comercial del medicamento o la vacuna sugeridos. Ej.: *Ivermectina 1%*. |
| **Principio activo** | El componente activo del producto. Ej.: *Ivermectina*. |
| **Instrucciones del veterinario** | Indicaciones para quien la aplique. Ej.: *Pesar al animal o usar el último peso medido vigente*. |

Estos datos no descuentan ningún inventario: solo informan a la hora de
preparar la jornada y de armar la planilla de campo.

#### Dosis

Primero elegí el **Tipo de cálculo**:

- **Fija por animal**: todos reciben la misma cantidad.
- **Por peso**: la cantidad depende del peso de cada animal.
- **Según indicación**: la define quien aplica, según la etiqueta o el veterinario.
- **No aplica (sin medicamento)**: para actividades como vigilancia o controles; no se piden más datos.

| Campo | Qué escribir |
| --- | --- |
| **Cantidad** | La dosis en números. Ej.: *5*. Se muestra cuando el cálculo no es "No aplica". |
| **Unidad** | ml, mg, g, tableta, dosis, gota, aplicación, ml por kg, ml por 10 kg, ml por 50 kg, mg por kg u otra. |
| **Detalle de unidad** | Solo si elegiste la unidad "otra": cuál es. |
| **Peso de referencia (kg)** (obligatorio si el cálculo es por peso) | El peso base sobre el que se calcula la dosis. Ej.: *100*. |
| **Dosis mínima** (opcional) | Límite de seguridad hacia abajo. |
| **Dosis máxima** (opcional) | Límite de seguridad hacia arriba. |

#### Vía y lugar de aplicación

| Campo | Qué escribir |
| --- | --- |
| **Vía de administración** | Cómo se aplica: **Subcutánea**, **Intramuscular**, **Intravenosa**, **Oral**, **Tópica**, **Pour-on**, **Intranasal**, otra o no aplica. |
| **Detalle de la vía** | Solo si elegiste "otra": cuál. |
| **Lugar anatómico** | Dónde va la dosis: **Cuello**, **Tabla del cuello**, **Región escapular**, **Lomo**, **Línea dorsal**, **Boca**, **Fosa nasal**, **Todo el cuerpo**, otro o no aplica. |
| **Detalle del lugar** | Solo si elegiste "otro": cuál. |

#### Elegibilidad

| Campo | Qué escribir |
| --- | --- |
| **Categoría** | La categoría de animal a la que aplica. Dejá **Todas** si es para todo el hato. |
| **Sexo aplicable** | **Ambos**, **Macho** o **Hembra**. |
| **Edad de aplicación** | El rango de edad en la unidad que prefieras (**Días**, **Meses** o **Años**): completá **Desde** y **Hasta**, o marcá **Sin límite máximo** para dejar el tope abierto. El campo **Hasta** debe ser igual o mayor que **Desde**. |
| **Incluir animales con edad desconocida** | Marcada, los animales sin fecha de nacimiento entran igual y no se les valida el rango de edad. Sin marcar, quedan excluidos. |

#### Modalidad de programación

Define cuándo corresponde hacer la actividad:

| Modalidad | Qué significa y qué datos pide |
| --- | --- |
| **Por edad** | Se agenda según la edad del animal. Pide **Edad objetivo (días)** (obligatorio), la **Ventana anticipada** y la **Ventana posterior** en días (cuántos antes/después de esa edad se acepta aplicar) y si se quieren **Excluir animales con fecha de nacimiento estimada**. |
| **Periódica** | Se repite cada cierto tiempo. Pide **Frecuencia** (número, ej.: *90*), su **Unidad** (días, semanas, meses o años), **Se calcula desde** (Fecha de ingreso, Fecha de nacimiento, Última aplicación, Fecha inicial del plan o Fecha configurada) y la tolerancia anticipada y posterior en días. |
| **Fecha programada** | Se hace una vez en una fecha y hora fijas. Pide **Fecha y hora programada** (obligatorio) y si es **Una sola vez** (marcado por defecto). |
| **Por hallazgo** | Se activa cuando se detecta un hallazgo (caso clínico abierto, ectoparásitos con carga alta, examen reproductivo no apto o control neonatal con alerta). Pide el **Plazo para resolverlo (días)** y si **Requiere validación veterinaria**. |
| **Manual** | No se agenda sola: la dispara quien registra la jornada. No pide datos extra. |

#### Alertas

| Campo | Qué escribir |
| --- | --- |
| **Hora prevista de ejecución** (obligatorio) | A qué hora del día se hace la actividad, en hora de Bolivia. Por defecto 08:00. |
| **Días de alerta** | Cuántos días antes se avisa. Ej.: *3* = aviso 3 días antes; 0 = sin aviso anticipado. |
| **Horarios de aviso** (obligatorio) | Hasta cinco horas separadas por coma. Ej.: *06:00, 07:00, 08:00*. |
| **Actividad obligatoria** | Marcada si la actividad no se puede saltear sin justificación. |

> Al editar una actividad que ya se usó, el formulario muestra además
> **Motivo del cambio** y **Vigente desde**. Ver "Importante" más abajo.

### Ejemplo

- Plan: Calendario sanitario 2026
- Actividad: Vacunación Fiebre Aftosa
- Clasificación: Obligatorio SENASAG
- Producto recomendado: Vacuna antiaftosa trivalente
- Dosis: Fija por animal, 2 ml
- Vía: Subcutánea · Lugar: Tabla del cuello
- Modalidad: Periódica, cada 4 meses, calculada desde la última aplicación
- Alerta: 3 días antes, avisos a las 06:00 y 07:00

### Resultado esperado

La actividad queda activa dentro del plan. A partir de ahí puede elegirse al
preparar una jornada sanitaria de ese tipo. El sistema proyecta las próximas
fechas y genera **una alerta por fecha programada**, no una por animal: si la
actividad corresponde a 60 animales, verás un solo aviso que indica cuántos
animales alcanza. La prioridad de esa alerta sube a medida que se acerca la
fecha.

### Importante

> Si editás una actividad que ya fue usada en alguna jornada o tratamiento,
> el sistema no sobrescribe los datos anteriores: crea una nueva versión
> (indicá el motivo del cambio). Las versiones anteriores quedan visibles
> en **Versiones**, para saber exactamente qué se aplicó en cada momento.

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
   "Excluidos" se explica el motivo de cada exclusión.
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

El plan debe estar en estado **Activo**. Si está en Borrador, presiona
**Activar** primero.

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

No existe una actividad **activa** del tipo de jornada que elegiste. Creála
primero en **Planes sanitarios**.

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

Puede que su fecha prevista quede más adelante que la ventana configurada
en "Proyección del calendario (meses)" (**Sanidad** → **Planes
sanitarios**). Ampliá esa ventana o esperá a que la fecha entre en rango.

### Edité un ítem del plan que ya se había usado y no veo el cambio reflejado en jornadas anteriores

Es el comportamiento esperado: el sistema crea una nueva versión del ítem
en lugar de sobrescribir la anterior, para conservar exactamente lo que se
aplicó en cada jornada o tratamiento pasado. La versión nueva se usa desde
la fecha de vigencia que indiques hacia adelante.

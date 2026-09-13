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
preparar una jornada sanitaria de ese tipo, y el sistema generará alertas
según lo configurado.

### Importante

> Si editás una actividad que ya fue usada en alguna jornada o tratamiento,
> el sistema no sobrescribe los datos anteriores: crea una nueva versión
> (indicá el motivo del cambio). Las versiones anteriores quedan visibles
> en **Versiones**, para saber exactamente qué se aplicó en cada momento.

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
     ectoparasitario**. Indica el tipo de parásito (Garrapata, Mosca de los
     cuernos, Tórsalo, Piojos u Otro), el nivel de carga (Bajo, Medio o
     Alto), si fue tratado en el momento, y el producto y principio activo
     usados. Presiona **Guardar control**.
   - **Examen reproductivo**: presiona **Registrar examen reproductivo**.
     Indica fecha y resultado general (Apto, No apto o En observación),
     completa los datos físicos según el sexo (en machos: circunferencia
     escrotal, motilidad espermática, morfología, libido y capacidad de
     servicio; en hembras: peso, porcentaje de peso adulto, condición
     corporal y desarrollo reproductivo) y el checklist de enfermedades
     (IBR, BVD, Brucelosis, Leptospirosis, Tricomoniasis y
     Campylobacteriosis). Presiona **Guardar examen**.

### Ejemplo

- Animal: BOV-0142
- Control ectoparasitario del 05/09/2026: tipo Garrapata, carga Alta,
  tratado con Amitraz.

### Resultado esperado

El control queda en el historial sanitario del animal, visible desde esta
misma pantalla la próxima vez que lo busques.

### Importante

> En el control ectoparasitario, si el principio activo coincide con el de
> un registro reciente del mismo animal o lote, el sistema te avisa para
> que consideres rotarlo y evitar que el parásito desarrolle resistencia.

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
   dejalo "Sin identificar") y la severidad (Leve, Moderada, Grave o
   Crítica), y describe los síntomas observados.
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

El animal a tratar debe existir en el inventario.

### Procedimiento

1. Abre **Sanidad** → pestaña **Tratamientos**.
2. Presiona **Nuevo tratamiento**.
3. Selecciona el animal, la fecha de inicio, la fecha fin estimada, el
   diagnóstico y observaciones.
4. En "Protocolo de dosificación" completa dosis, unidad, frecuencia (en
   horas), duración (en días), vía de administración y días de retiro de
   carne y de leche. Usa **Agregar otro protocolo** si el tratamiento
   combina más de un medicamento.
5. Presiona **Crear tratamiento**. Queda en estado **Borrador**.
6. Presiona **Activar** para que el sistema genere el cronograma de
   aplicaciones según el protocolo.
7. Abre **Aplicaciones** para ver las próximas dosis por aplicar. Presiona
   **Aplicar** en la que corresponda, confirma la dosis realmente aplicada
   y observaciones, y presiona **Guardar aplicación**.
8. Si cambió el protocolo o las fechas, usa **Regenerar cronograma** para
   recalcular las aplicaciones pendientes.
9. Cuando ya no queden aplicaciones pendientes ni atrasadas (todas
   aplicadas o canceladas), presiona **Finalizar**.

Con el botón **Marcar atrasadas**, en la parte superior del listado, podés
actualizar de una vez el estado de todas las aplicaciones vencidas de todos
los tratamientos.

### Ejemplo

- Animal: BOV-0098 (siguiendo el caso clínico de Fiebre/IBR del ejemplo
  anterior)
- Medicamento: Oxitetraciclina, 10 ml, cada 24 horas, durante 5 días, vía
  intramuscular
- Retiro de carne: 14 días

### Resultado esperado

Cada dosis programada queda como pendiente, atrasada o aplicada en el
historial del tratamiento. Al finalizarlo, su estado pasa a **Finalizado**.

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

### No puedo guardar un examen reproductivo

Faltan configurar las edades mínimas para machos y hembras en **Sanidad**
→ **Planes sanitarios** → "Edades mínimas para examen reproductivo".

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

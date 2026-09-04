# Guía: cómo llevar el módulo Sanidad al plan sanitario que describiste

Esta guía traduce tu propuesta (SENASAG, brucelosis, rabia, clostridiales, garrapata,
calendario individual por animal, compra en cantidad) a cambios concretos sobre el código
real de `backend/src/main/java/bo/com/ganadero/sanidad`,
`backend/src/main/java/bo/com/ganadero/animales` y `frontend-web/src/features/sanidad`.
No es una reescritura — es una extensión de lo que ya existe. Reorganizada para reflejar
tanto el ciclo de un animal nacido en la finca como el de un lote comprado afuera.

## 1. Diagnóstico: qué ya tenés vs. qué pide el plan

Lo que ya existe hoy te da una base sólida:

- `PlanSanitarioItem` ya soporta categoría, sexo, rango de edad en días, dosis, frecuencia
  y `diasAlerta` — el "motor de reglas" ya existe.
- `AnimalElegibilidad` ya calcula, para una actividad puntual, quién entra y quién no,
  con motivos de exclusión.
- `ProcesarAlertasVacunacionService` y `ProcesarTratamientosVencidosService` ya generan
  alertas cuando algo se vence.
- `OrigenAnimal.COMPRADO`, `TipoMovimiento.INGRESO_COMPRA` y `CUARENTENA`/`RETORNO_CUARENTENA`
  ya existen — el sistema ya sabe distinguir un animal comprado de uno nacido en la finca.
- `categoriaActualId` se asigna siempre a mano (nunca se calcula solo desde la fecha de
  nacimiento), así que un animal sin fecha exacta igual puede clasificarse correctamente.

Lo que falta es lo que convierte "una tabla de vacunas" en "un motor sanitario", y son
seis piezas concretas, que agrupo en tres frentes:

**Frente A — reglas y clasificación**
1. Clasificación regulatoria (obligatorio SENASAG / recomendado / según campaña /
   configurable) — hoy no existe, todo se ve igual.
2. Mensajes de bloqueo explícitos (por qué un macho no puede recibir brucelosis, no solo
   que no aparece en la lista).

**Frente B — el motor de calendario**
3. Calendario proyectado por animal — hoy todo es reactivo (solo avisa de lo ya aplicado).
4. Anclaje distinto para animales sin fecha de nacimiento conocida (comprados).
5. Semántica de alerta distinta para "vencido de verdad" vs. "comprado sin historial
   verificable" — son casos con implicancia diferente y hoy se tratarían igual.

**Frente C — piezas nuevas fuera del ciclo vacuna→refuerzo**
6. Ingreso masivo de animales, historial sanitario declarado al ingreso, cuarentena
   conectada a Sanidad, control neonatal, control ectoparasitario, examen reproductivo,
   estatus sanitario del predio.

## 2. Los cuatro grupos regulatorios, llevados a campos reales

Agregaría un campo nuevo a `PlanSanitarioItem`: `origenRegulatorio`, con estos valores:

| Valor | Uso | Ejemplo de tu lista |
|---|---|---|
| `OBLIGATORIO_SENASAG` | Exigido por norma, sin margen de decisión del productor | Brucelosis 3–8 meses en hembras |
| `CAMPANA_RIESGO` | Depende de resolución/zona/epidemiología, no es fijo en el calendario | Rabia — "según SENASAG determine" |
| `RECOMENDADO_VETERINARIO` | Buena práctica, pero configurable | Clostridial, IBR, BVD, leptospirosis |
| `CONFIGURABLE_ESTABLECIMIENTO` | Lo define cada finca según su realidad | Frecuencia de desparasitación, ectoparasiticidas |

Esto no reemplaza `tipoActividad` (que sigue diciendo *qué* es: vacunación,
desparasitación, etc.) — lo complementa diciendo *por qué existe esa actividad en el
plan*. En la UI se traduce en un chip de color distinto: "obligatorio" (no se puede
desactivar sin justificar) vs. "recomendado" (el productor decide).

Importante, y lo remarcás vos mismo con razón: **aftosa ya no es "vacunar en mayo y
noviembre"**. Bolivia/Santa Cruz están en zona libre sin vacunación (estatus OMSA), así
que la actividad correspondiente no debería ser `VACUNACION` sino un nuevo valor en
`TipoActividadSanitaria`: `VIGILANCIA_EPIDEMIOLOGICA` — notificación y bioseguridad, no
aplicación de dosis. Modelarla como vacuna con frecuencia fija hereda el error de los
calendarios viejos.

## 3. El origen del animal cambia cómo corre el calendario

Esta es la distinción central de toda la guía: **el mismo motor de reglas corre distinto
según si el animal nació en tu finca o entró comprado**, porque cambia el dato de partida.

### 3.1 Animal nacido en la finca

Tenés `fecha_nacimiento` exacta. El calendario ancla cada actividad a
`fecha_nacimiento + edadMinDias` del `PlanSanitarioItem` correspondiente, y como no
existe aplicación previa, cualquier alerta que se genere es inequívoca: si pasó la
ventana y no hay `AplicacionSanitaria`, es porque nadie lo vacunó — el productor tuvo el
animal desde el día cero y es responsable de esa ventana completa.

### 3.2 Animal comprado, con historial declarado

El vendedor entrega certificado de una actividad (ej. brucelosis con fecha). Se carga
como `AplicacionSanitaria` con un campo nuevo `origenRegistro = DECLARADA_PROVEEDOR`
(vs. `APLICADA_FINCA` para las que aplica tu propio equipo). El calendario calcula
`proxima_aplicacion` a partir de esa fecha declarada exactamente igual que si la
hubieras aplicado vos — sigue su ciclo de refuerzo normal, sin alertas falsas al ingreso.

### 3.3 Animal comprado, sin historial y sin fecha de nacimiento conocida

Acá se combinan dos problemas y hay que resolverlos en dos pasos:

**a) Anclaje de edad.** Sin `fecha_nacimiento`, el calendario no puede calcular
"cumple 3 meses el día X". La regla: usar `edad_min_meses` de la `categoria_animal`
asignada + tiempo transcurrido desde `fecha_ingreso` como fecha de referencia
aproximada. No es exacto, pero es suficiente para no perder la ventana de un animal que
entró siendo ya "vaquillona".

**b) Semántica de la alerta — esto es lo que agregamos hoy.** No es lo mismo un animal
que vencido de verdad (nació en tu finca, tuviste la ventana completa y no se aplicó) que
un animal comprado sin historial verificable. Van dos casos distintos según dónde caiga
la edad estimada al momento del ingreso:

- Si la edad estimada **cae dentro** de la ventana de la actividad (ej. entra con ~7
  meses estimados y la ventana de brucelosis es 3–8 meses): se genera un nuevo tipo de
  alerta, `REVISION_SANITARIA_INGRESO` (distinto de `VACUNA_VENCIDA`), con mensaje del
  estilo *"Verificar/aplicar brucelosis — animal comprado sin historial declarado,
  dentro de ventana"*. Es una tarea sanitaria normal, solo que con incertidumbre sobre
  si ya se hizo.
- Si la edad estimada **ya pasó** la ventana (ej. entra con ~14 meses y la ventana
  termina a los 8): no corresponde generar `VACUNA_VENCIDA` ni pedir que se aplique fuera
  de norma — se genera la misma `REVISION_SANITARIA_INGRESO` pero con severidad de
  auditoría, tipo *"Fuera de ventana de brucelosis sin historial verificable — confirmar
  estatus sanitario antes de incorporar al hato reproductivo"*. Es una tarea de
  trazabilidad/compliance de la compra, no una tarea veterinaria de aplicar una dosis.

Esta distinción evita dos errores opuestos: generar pánico de "vencido" sobre animales
que probablemente sí fueron tratados por el vendedor, y — el error contrario — dejar
pasar en silencio animales que entraron al hato sin ninguna garantía sanitaria real.

## 4. Cambios de modelo de datos necesarios

Sobre las tablas que ya existen (`plan_sanitario_item`, `animal`, `categoria_animal`,
`propiedad`, `aplicacion_sanitaria`), estas son las migraciones Flyway nuevas (recordá:
nunca se edita `V1`/`V2`, siempre se agrega `V3__...sql` en adelante):

**a) `plan_sanitario_item`**: columna `origen_regulatorio text` (los 4 valores de la
sección 2) y `especie_aplicable text` (hoy siempre `BOVINO`, previendo el futuro).

**b) `aplicacion_sanitaria`**: columna `origen_registro text` (`APLICADA_FINCA` /
`DECLARADA_PROVEEDOR`), para distinguir lo que vio tu equipo de lo que declaró el
vendedor.

**c) `alerta` / `TipoAlerta`**: nuevo valor `REVISION_SANITARIA_INGRESO`, con una
subclasificación de severidad (dentro de ventana / fuera de ventana) que puede resolverse
con el campo `severidad` que la tabla `alerta` ya tiene.

**d) Nueva tabla `estatus_sanitario_predio`**: una fila por `propiedad_id`, con
`brucelosis_estado` y `tuberculosis_estado` (`SIN_CERTIFICAR`/`EN_SANEAMIENTO`/`LIBRE`),
fecha de última certificación y observaciones.

**e) Nueva tabla `control_neonatal`**: `animal_id`, `fecha_control`, `calostrado`
(`CORRECTO`/`INSUFICIENTE`/`DESCONOCIDO`), `ombligo_desinfectado` (bool),
`ombligo_estado`, `diarrea` (bool), `estado_general`, `lactancia`, `temperatura_c`,
`observaciones`.

**f) Nueva tabla `control_ectoparasitario`**: `animal_id` o `lote_ganadero_id`, `tipo`
(`GARRAPATA`/`MOSCA_CUERNOS`/`TORSALO`/`PIOJOS`/`OTRO`), `nivel_carga`
(`BAJO`/`MEDIO`/`ALTO`), `producto`, `principio_activo`, `fecha`. La regla de "no repetir
siempre el mismo principio activo" se resuelve en el frontend mostrando qué se usó las
últimas 2-3 veces, como advertencia, no como bloqueo.

**g) Nueva tabla `examen_reproductivo`**: `animal_id`, `sexo`, `fecha`, y para toro:
`circunferencia_escrotal_cm`, `motilidad_espermatica`, `morfologia`, `libido`,
`capacidad_servicio`, resultado `APTO`/`NO_APTO`/`OBSERVACION`; para vaquilla: `peso_kg`,
`porcentaje_peso_adulto`, `condicion_corporal`, `desarrollo_reproductivo`, resultado.
Ambos comparten `enfermedades_evaluadas` (IBR, BVD, brucelosis, leptospirosis,
tricomoniasis, campylobacteriosis) como checklist.

**h) Campo nuevo en `animal`**: `destino_productivo`
(`REPRODUCTOR`/`ENGORDE`/`VENTA`/`DESCARTE`). Revisar si conviene reusar `proposito` (que
hoy probablemente signifique carne/leche/doble propósito) o si conviene un campo aparte,
porque un macho puede tener propósito "carne" y destino "reproductor" o "engorde" en
distintas etapas de su vida.

## 5. El motor de calendario proyectado por animal

Esto es lo más importante de toda la propuesta y lo que no existe hoy de ninguna forma.

Hoy: `ProcesarAlertasVacunacionService` solo mira aplicaciones que **ya se hicieron**
(`estado = 'APLICADA'`) y calcula cuándo vence la siguiente. Si un animal nunca tuvo
ninguna aplicación —recién nacido, o comprado sin historial— el sistema nunca genera una
alerta de "deberías vacunarlo": no hay de dónde partir el cálculo.

`ProyectarCalendarioSanitarioService` (nuevo, corre como job diario igual que los otros
dos) hace, para cada animal activo:

1. Determina la fecha de referencia de edad: `fecha_nacimiento` si existe (3.1), o la
   estimación por categoría + `fecha_ingreso` si no (3.3).
2. Recorre los `PlanSanitarioItem` activos del plan `ACTIVO` que apliquen a su
   categoría/sexo/edad — igual que ya hace `AnimalElegibilidad`, pero mirando hacia
   **adelante en el tiempo**, no solo "hoy".
3. Para cada item sin ninguna `AplicacionSanitaria` (de cualquier `origenRegistro`),
   calcula si la ventana ya se cruzó y con qué margen, y decide el tipo de alerta:
   `VACUNA_PROXIMA`/`VACUNA_VENCIDA` si el animal nació en la finca (3.1), o
   `REVISION_SANITARIA_INGRESO` con la severidad correspondiente si es un comprado sin
   historial (3.3b).

Es la pieza que convierte el plan sanitario de una tabla de referencia en un motor real
del hato — y con la distinción de la sección 3 ya no genera falsos positivos sobre
animales comprados con papeles en regla, ni deja pasar en silencio a los que no los
tienen.

## 6. Reglas de bloqueo automático

Con `sexoAplicable` ya en el modelo, la regla se implementa en `AnimalElegibilidad`
como una exclusión explícita, no como una ausencia silenciosa: si un macho aparece en
"Preparar jornada" para una actividad con `sexoAplicable = HEMBRA`, hoy simplemente no
sale en la lista de elegibles. El cambio pedido ("❌ No corresponde — SENASAG no aplica
Cepa 19/RB51 a machos") es una mejora del motivo de exclusión: en vez de un genérico
"sexo no aplicable", citar la regla concreta. La lógica de bloqueo ya existe; falta que
explique el porqué.

## 7. Ingreso masivo, historial declarado y cuarentena (piezas fuera de Sanidad)

Estas tres viven principalmente en el módulo `animales` y `movimientos`, no en
`sanidad`, pero son las que hacen posible todo lo de la sección 3 con datos reales:

**Ingreso masivo.** Hoy `AnimalController` solo expone `POST /animales` de a uno. Para
comprar 40 juntos hace falta un formulario de "ingreso por lote de compra": datos
comunes (raza, propiedad, potrero, proveedor, fecha de ingreso, precio) una sola vez, y
por fila solo lo variable (código, sexo, categoría estimada, peso). Se agrupan en un
`lote_ganadero` de ingreso para manejarlos juntos durante la cuarentena.

**Historial declarado al ingreso.** Un paso opcional al registrar un animal `COMPRADO`
(o en el ingreso masivo) para cargar qué actividades declara el vendedor, con fecha
aproximada — esto crea las `AplicacionSanitaria` con `origenRegistro = DECLARADA_PROVEEDOR`
de la sección 3.2.

**Cuarentena conectada a Sanidad.** `CUARENTENA`/`RETORNO_CUARENTENA` ya existen como
movimientos pero hoy `sanidad` no los referencia en absoluto. Falta: al confirmar
`INGRESO_COMPRA`, sugerir automáticamente el período de cuarentena, y antes de permitir
`RETORNO_CUARENTENA`, exigir o recomendar con alerta una `PRUEBA_DIAGNOSTICA`
(brucelosis/tuberculosis) sobre el lote — la vigilancia epidemiológica al introducir
animales nuevos que pide tu enfoque, en vez de vacunación automática.

Una vez que el lote sale de cuarentena, no necesita ningún manejo especial: se integra a
sus lotes/potreros definitivos y de ahí en adelante el motor de elegibilidad ya lo toma
en cuenta junto con el resto del hato en cualquier jornada futura, sin distinción.

## 8. Fases actualizadas

Con todo lo anterior, este es el orden que recomiendo, pensado para que cada fase sea
entregable por separado y de bajo riesgo:

**Fase 1 — Clasificación regulatoria y corrección de aftosa.** Agregar
`origenRegulatorio` a `PlanSanitarioItem`, agregar `VIGILANCIA_EPIDEMIOLOGICA` a
`TipoActividadSanitaria`, chip visual en la UI de Planes. Bajo riesgo, corrige de
inmediato el error conceptual de aftosa.

**Fase 2 — Ingreso masivo de animales + historial declarado al ingreso.** Vive en
`animales`, es independiente de Sanidad, se puede hacer en paralelo a la Fase 1. Es el
prerequisito de datos para que las fases siguientes funcionen bien con animales
comprados.

**Fase 3 — Motor de calendario proyectado por animal.** La pieza central:
`ProyectarCalendarioSanitarioService`, con el anclaje dual (fecha de nacimiento real vs.
categoría + fecha de ingreso) y la nueva semántica de alerta
`REVISION_SANITARIA_INGRESO` de la sección 3.3. Depende de la Fase 2 para poder probarse
bien con casos de compra.

**Fase 4 — Cuarentena conectada a Sanidad.** Prueba diagnóstica sugerida/obligatoria
antes de `RETORNO_CUARENTENA`. Depende de la Fase 2 (necesita el lote de ingreso ya
armado).

**Fase 5 — Entidades nuevas independientes**: control neonatal, control ectoparasitario,
examen reproductivo — cada una es un módulo chico y autocontenido, se pueden hacer de a
una, sin depender de las fases anteriores.

**Fase 6 — Estatus sanitario del predio.** La tabla `estatus_sanitario_predio` y su
pantalla, la más simple de todas, se puede hacer en cualquier momento.

Te recomiendo arrancar por la Fase 1 y la Fase 2 en paralelo (son independientes entre
sí y de bajo riesgo cada una), y recién con esas dos hechas seguir con la Fase 3, que es
la que realmente necesita los datos de las dos primeras para tener sentido completo.

## 9. Próximo paso

Decime con cuál de las dos fases iniciales (1 o 2) querés arrancar primero y te armo el
prompt específico para esa fase.

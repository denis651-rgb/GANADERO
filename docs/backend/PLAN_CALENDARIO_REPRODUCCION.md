# Plan: sincronizar alertas de reproducción con Google Calendar + días de destete configurables

Este documento es un plan de implementación, no una guía de comportamiento ya construido
(a diferencia de `CONTROL_GESTACIONES.md` o `PLAN_SANITARIO_SANTA_CRUZ.md`). Describe, con
archivos y nombres reales del código actual, cómo llevar a cabo dos pedidos concretos del
usuario:

1. Que las alertas **"Parto próximo"** y **"Destete próximo"** se sincronicen con Google
   Calendar, igual que ya sincronizan las actividades del plan sanitario.
2. Que el número de días hasta el destete (hoy fijo en 210, ~7 meses) sea configurable
   desde la pantalla de Configuración, en vez de estar fijo en el backend.

## 0. Alcance

**Incluido:**
- `TipoAlerta.PARTO_PROXIMO` y `TipoAlerta.DESTETE_PROXIMO` → sincronización con Google
  Calendar (crear el evento, y cancelarlo cuando el parto/destete ya se registra).
- Nuevo campo "Días hasta el destete" en Configuración, reemplazando el valor fijo de
  `ReproduccionCicloService.diasHastaDestete` (hoy `210`, sobreescribible solo por variable
  de entorno `ganadero.reproduccion.dias-hasta-destete`, nunca desde la UI).

**Explícitamente fuera de este plan** (confirmado con el usuario):
- `TipoAlerta.CELO_DETECTADO` no se sincroniza con el calendario en esta fase.
- No se sincronizan otros tipos de alerta (`VACUNA_PROXIMA`, `MOVIMIENTO_PENDIENTE`, etc.)
  — eso ya funciona hoy para sanidad vía `ocurrencia_calendario_sanitario` y no se toca.
- No se agrega una operación `ACTUALIZAR` para las alertas de reproducción en esta primera
  fase (ver sección 2.1, "Qué NO cubre esta fase" para el porqué).

## 1. Diagnóstico exacto del código actual (ya verificado, no es una suposición)

- El motor de sincronización vive en
  `backend/src/main/java/bo/com/ganadero/integraciones/calendario/CalendarioExternoService.java`.
  Las tablas `cola_sincronizacion_calendario` y `correspondencia_calendario_externo`
  (migración `V35__sincronizacion_calendario_externo.sql`) tienen una columna
  `ocurrencia_id text not null references ocurrencia_calendario_sanitario(id)` — **no hay
  forma de encolar nada que no sea una fila de esa tabla** tal como está hoy.
- El proceso que realmente habla con la API de Google vive en
  `electron/src/google-calendar-sync.ts`. Arma el evento (`eventRequest()`) leyendo campos
  puramente de sanidad: `nombreActividad`, `producto`, `dosis`, `via`, `lugarAplicacion`,
  `diasAlerta`, `horariosAviso`.
- `TipoAlerta.PARTO_PROXIMO` se programa en
  `backend/src/main/java/bo/com/ganadero/reproduccion/application/ReproduccionService.java:290-296`,
  al confirmar un diagnóstico de gestación positivo.
- `TipoAlerta.DESTETE_PROXIMO` se programa en
  `backend/src/main/java/bo/com/ganadero/reproduccion/application/ReproduccionCicloService.java:99-101`,
  al registrar la cría de un parto. La fecha se calcula como
  `parto.fechaParto() + diasHastaDestete` (línea 35: `private int diasHastaDestete=210;`,
  línea 41: inyectado por `@Value("${ganadero.reproduccion.dias-hasta-destete:210}")`).
- Ambas alertas ya se **resuelven solas** cuando el hecho real ocurre:
  - Destete: `ReproduccionCicloService.registrarDestete()` línea 138 llama
    `resolver(u,"CRIA",cria.id())`.
  - Parto: revisar en implementación si `registrarParto()` resuelve el `PARTO_PROXIMO` de
    la gestación asociada (si no lo hace todavía, hay que agregarlo — ver sección 2.4).
- La tabla `alerta` (migración `V1__baseline_sqlite.sql:816-845`) **no tiene columna
  `empresa_id`** (app de un solo usuario) — a diferencia de `cola_sincronizacion_calendario`
  y `configuracion_calendario_externo`, que sí la tienen. Hay que tenerlo presente al
  escribir los JOIN: no se filtra `alerta` por empresa, pero sí las tablas de la cola.
- `Configuracion` es una fila única (`backend/.../configuracion/domain/Configuracion.java`),
  ya tiene `diasAlertaDestete` (cuántos días antes avisar) — lo que falta es
  `diasHastaDestete` (la edad de destete en sí).
- Existe ya un puente entre módulos para este tipo de dato:
  `alertas.application.AlertaConfiguracionPort` / `AlertaConfiguracion` (implementado por
  `JdbcAlertaConfiguracion`), que `ReproduccionCicloService` ya usa para leer
  `diasAlertaDestete` sin depender directamente del módulo `configuracion` (límite de
  Spring Modulith). El nuevo campo debe viajar por el mismo puente.

## 2. Parte A — Días hasta el destete configurables

Más simple que la Parte B: es una columna nueva en una tabla que ya existe, sin tocar
llaves foráneas.

### 2.1 Migración

Nuevo archivo `backend/src/main/resources/db/migration/V40__dias_hasta_destete.sql`:

```sql
alter table configuracion add column dias_hasta_destete integer not null default 210;
```

Simple `ADD COLUMN` con default — SQLite lo soporta sin recrear la tabla (a diferencia de
lo que hace falta en la Parte B).

### 2.2 Backend — cadena de configuración

Archivos a tocar, seven en el mismo orden que ya sigue `diasAlertaDestete`:

1. `configuracion/domain/Configuracion.java` — agregar `Integer diasHastaDestete` al record.
2. `configuracion/api/ConfiguracionResponse.java` — agregar `int diasHastaDestete` y pasarlo
   en `from(Configuracion c)`.
3. `configuracion/api/ActualizarConfiguracionRequest.java` — agregar
   `@Min(1) Integer diasHastaDestete` y pasarlo a `ConfiguracionCommand`.
4. `configuracion/application/ConfiguracionCommand.java` — agregar el campo (mismo patrón
   que los `diasXxx` existentes).
5. `configuracion/application/ConfiguracionService.java` — revisar si valida rangos de los
   `diasXxx` existentes; replicar la misma validación para el nuevo campo si aplica.
6. `configuracion/infrastructure/JdbcConfiguracionAdapter.java`:
   - `SELECT`: agregar `dias_hasta_destete`.
   - `update(...)`: agregar `dias_hasta_destete=coalesce(:dhd,dias_hasta_destete)` y el
     parámetro `dhd` en `params()`.
   - `map(...)`: agregar `r.getInt("dias_hasta_destete")` en la posición correcta del record.
7. `alertas/application/AlertaConfiguracion.java` — agregar `int diasHastaDestete` al
   record, y a `valoresPredeterminados()` (usar `210`).
8. `alertas/infrastructure/JdbcAlertaConfiguracion.java` — agregar `dias_hasta_destete` al
   `select` y al `new AlertaConfiguracion(...)`.
9. `reproduccion/application/ReproduccionCicloService.java`:
   - Eliminar el campo fijo `private int diasHastaDestete=210;` y el setter `@Value(...)`
     (o dejarlo únicamente como valor de arranque para instalaciones que todavía no migraron
     la config — a decidir en implementación, pero lo correcto es que la fuente de verdad
     pase a ser la config de empresa).
   - Agregar un método `private int diasHastaDestete(UUID empresaId)` que lea de
     `configuracionAlertas` (mismo patrón exacto que `diasAlertaDestete(UUID empresaId)`,
     líneas 42-46).
   - Usar `diasHastaDestete(u.empresaId())` en las líneas 100-101 en vez del campo fijo.

### 2.3 Frontend

1. `frontend-web/src/features/configuracion/api.ts` línea 11 — agregar
   `diasHastaDestete: number` a la interfaz de configuración.
2. `frontend-web/src/features/propiedades/pages/ConfiguracionGeneralPage.tsx`:
   - Línea ~29, en el `submit`/`FormData`: agregar
     `diasHastaDestete: Number(data.get('diasHastaDestete'))`.
   - Línea ~74, junto al `Field` de "Días de alerta de destete": agregar un `Field` nuevo
     "Días hasta el destete" (`type="number" min="1"`), con un hint explicando que es la
     edad a la que normalmente se desteta la cría (ej. "210 días ≈ 7 meses. Ajusta según tu
     manejo — carne vs. lechería suelen destetar a edades distintas.").

### 2.4 Tests

- Backend: extender el test existente de `ConfiguracionService`/`JdbcConfiguracionAdapter`
  (buscar el archivo real antes de escribir) para cubrir lectura/escritura de
  `diasHastaDestete`, con el mismo patrón que los demás `diasXxx`.
- Backend: en `ReproduccionCicloServiceTest` (si existe con ese nombre — verificar), agregar
  un caso donde se cambia `diasHastaDestete` vía configuración y se confirma que la fecha
  programada de `DESTETE_PROXIMO` cambia en consecuencia (no queda pegada a 210).
- Frontend: si existe `ConfiguracionGeneralPage.test.tsx`, agregar un caso que cambie "Días
  hasta el destete" y confirme que se manda en el `PATCH`.

## 3. Parte B — Sincronizar `PARTO_PROXIMO` y `DESTETE_PROXIMO` con Google Calendar

Esta es la parte grande. Se apoya en generalizar el "origen" de un trabajo de
sincronización, para que pueda ser una ocurrencia sanitaria **o** una alerta.

### 3.1 Diseño: origen genérico en la cola

Hoy: `cola_sincronizacion_calendario.ocurrencia_id` es `not null` y solo apunta a
`ocurrencia_calendario_sanitario`. Igual en `correspondencia_calendario_externo`.

Propuesta:
- Agregar `origen_tipo text not null default 'OCURRENCIA_SANITARIA'` con
  `check (origen_tipo in ('OCURRENCIA_SANITARIA','ALERTA_REPRODUCCION'))`.
- Agregar `alerta_id text references alerta(id)`, nullable.
- Hacer `ocurrencia_id` nullable (ya no `not null`), conservando la referencia a
  `ocurrencia_calendario_sanitario(id)` para cuando `origen_tipo='OCURRENCIA_SANITARIA'`.
- Reemplazar los `unique(ocurrencia_id, proveedor)` por un índice único por expresión que
  cubra ambos orígenes, por ejemplo:
  ```sql
  create unique index uq_correspondencia_origen
      on correspondencia_calendario_externo(origen_tipo, coalesce(ocurrencia_id,''), coalesce(alerta_id,''), proveedor);
  ```
  (SQLite 3.9+ soporta índices únicos por expresión; el proyecto corre 3.46 — validar la
  sintaxis exacta contra una base de prueba durante la implementación, no asumir que
  compila a la primera).

SQLite no permite `alter table ... alter column ... drop not null` ni agregar una
`check` compuesta sin recrear la tabla. Hay que seguir el mismo patrón que ya usa el
proyecto para esto (ver comentario de `V27` sobre `pragma foreign_keys=off/on` alrededor de
una recreación de tabla con datos reales referenciándola): crear tabla nueva con el esquema
final, copiar los datos existentes (con `origen_tipo='OCURRENCIA_SANITARIA'`,
`ocurrencia_id` tal cual, `alerta_id=null`), borrar la vieja, renombrar, recrear índices.
Aplica tanto a `cola_sincronizacion_calendario` como a `correspondencia_calendario_externo`.

Nuevo archivo: `backend/src/main/resources/db/migration/V41__calendario_origen_generico.sql`
(el número exacto depende de si V40 ya se usó para la Parte A — confirmar el último
número real al momento de escribir el archivo, no asumir V40/V41 si mientras tanto se
agregó otra migración).

### 3.2 Qué NO cubre esta fase (para no sobre-construir)

- Solo `operacion` `CREAR` y `CANCELAR` para alertas de reproducción. No hay `ACTUALIZAR`:
  a diferencia de una actividad sanitaria (cuyo `PlanSanitarioItem` puede cambiar de
  producto/dosis mientras está pendiente), una alerta de `PARTO_PROXIMO`/`DESTETE_PROXIMO`
  no tiene campos editables una vez programada — solo puede terminar en "se resolvió" o
  seguir pendiente. Si en el futuro se necesita reflejar un cambio (ej. se corrige la fecha
  probable de parto), se agrega `ACTUALIZAR` entonces, no antes.
- No se toca `listarOcurrencias()` para fusionar ambos orígenes en una sola pantalla en esta
  fase — se deja como una decisión de UI a tomar en el punto 3.5. Lo mínimo indispensable es
  que la sincronización funcione; la visibilidad en el panel "Calendario" es deseable pero
  separable.

### 3.3 Backend — `CalendarioExternoService`

Cambios método por método (todos en
`backend/src/main/java/bo/com/ganadero/integraciones/calendario/CalendarioExternoService.java`):

1. **Nuevo método** `encolarAlertasPendientes(UUID empresaId)`, hermano de
   `encolarOcurrenciasPendientes` (líneas 312-326), pero fuente `alerta`:
   ```sql
   insert into cola_sincronizacion_calendario
       (id, empresa_id, origen_tipo, alerta_id, proveedor, operacion, clave_idempotencia, payload)
   select <uuid-random-sqlite>, :empresa, 'ALERTA_REPRODUCCION', a.id, :proveedor, 'CREAR',
          :proveedor||':CREAR:ALERTA:'||a.id,
          json_object('alertaId', a.id, 'animalId', a.animal_id, 'tipoAlerta', a.tipo,
                      'titulo', a.titulo, 'mensaje', a.mensaje,
                      'fechaProgramada', a.fecha_programada, 'fechaVencimiento', a.fecha_vencimiento)
   from alerta a
   left join correspondencia_calendario_externo c
       on c.alerta_id=a.id and c.proveedor=:proveedor and c.origen_tipo='ALERTA_REPRODUCCION'
   where a.tipo in ('PARTO_PROXIMO','DESTETE_PROXIMO')
     and a.estado in ('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA')
     and c.id is null
   on conflict(clave_idempotencia) do nothing
   ```
   Nota: `alerta.fecha_vencimiento` es la fecha real del evento (el parto/destete);
   `alerta.fecha_programada` es cuándo debería empezar a avisar. La diferencia en días entre
   ambas reemplaza a `diasAlerta` de una actividad sanitaria (ver 3.4, cálculo de
   recordatorios) — no hace falta un campo nuevo, se calcula al vuelo en la consulta o en
   Java al armar el payload.
   Necesita animal_codigo/animal_nombre para el título del evento — hay que hacer join con
   `animal` (`left join animal an on an.id=a.animal_id`) y agregarlos al `json_object`.
2. **Nuevo método** `encolarDesenlacesAlertas(UUID empresaId)`, hermano de
   `encolarDesenlaces` (líneas 329-346): busca alertas con correspondencia activa cuyo
   `estado` ya pasó a `RESUELTA` o `CANCELADA`, y encola `operacion='CANCELAR'`.
3. `guardar(...)`, `confirmarConexion(...)`, `reintentarErrores(...)`: en cada lugar que hoy
   llama `encolarOcurrenciasPendientes(u.empresaId())`, agregar también la llamada a
   `encolarAlertasPendientes(u.empresaId())`.
4. `reclamar(...)`: agregar `encolarDesenlacesAlertas(u.empresaId())` junto a
   `encolarDesenlaces(u.empresaId())`.
5. `prepararSincronizacionManual()`: igual, agregar ambas llamadas nuevas.
6. `trabajos(UUID empresaId, List<String> ids)` (líneas 362-387): hoy hace `join
   ocurrencia_calendario_sanitario o on o.id=q.ocurrencia_id` de forma incondicional — con
   `ocurrencia_id` ahora nullable, ese join debe ser `left join`, y el `json_patch` que arma
   el `payload_completo` debe construirse condicionalmente: si `q.origen_tipo =
   'ALERTA_REPRODUCCION'`, el patch usa los campos que ya vinieron en el `payload` original
   (no hace falta re-consultar nada, a diferencia de sanidad, porque ya se guardó todo lo
   necesario al encolar). Esto probablemente se resuelve mejor en Java que en SQL puro: leer
   el `payload` base, y si `origen_tipo='OCURRENCIA_SANITARIA'` aplicar el enriquecido actual
   con `plan_sanitario_item`, si no, dejarlo tal cual.
7. `completar(...)` (líneas 237-273) y `fallar(...)` (líneas 276-297): usan
   `trabajo.ocurrenciaId()` para el `insert`/`on conflict` de
   `correspondencia_calendario_externo`. Deben pasar a usar el origen genérico
   (`origen_tipo` + `ocurrencia_id`/`alerta_id` según corresponda) en el `on conflict`.
8. `TrabajoSincronizacionCalendario` (el record que representa un trabajo) necesita un campo
   más genérico que `ocurrenciaId` — evaluar renombrarlo a `origenId` + agregar
   `origenTipo`, para que el mapeo a JSON hacia Electron sea uniforme (ver 3.4). Si se
   renombra, actualizar todos los usos dentro de esta clase y el DTO de la API
   (`TrabajoSincronizacionCalendario` en `integraciones/calendario/api/...` si existe un DTO
   separado del record de aplicación — confirmar el nombre exacto del archivo al
   implementar).

### 3.4 Backend — resolver el `PARTO_PROXIMO` cuando el parto se registra

Verificar en implementación si `ReproduccionCicloService.registrarParto(...)` /
`ReproduccionService` ya llama a `motor.resolverPorOrigen(empresaId, "GESTACION",
gestacionId)` (el `origenTipo` usado al programar la alerta, línea ~294-296 de
`ReproduccionService.java`) en el momento en que el parto se confirma. Si no lo hace, hay
que agregarlo — sin esto, `encolarDesenlacesAlertas` nunca encontraría el `PARTO_PROXIMO`
en estado `RESUELTA`/`CANCELADA` y el evento quedaría húerfano en Google Calendar después de
que el parto ya ocurrió.

### 3.5 Electron — `google-calendar-sync.ts`

1. `interface Job`: renombrar `ocurrenciaId` → `origenId`, agregar `origenTipo: 'OCURRENCIA_SANITARIA' | 'ALERTA_REPRODUCCION'`.
2. `interface EventPayload`: agregar campos opcionales `origenTipo`, `animalCodigo`,
   `animalNombre`, `tipoAlerta`, `titulo`, `mensaje`, `fechaProgramada` (para calcular
   `diasAlerta` al vuelo si no viene ya calculado desde el backend — más simple: que el
   backend ya mande `diasAlerta` calculado en el `payload` de la alerta, reusando el mismo
   campo que ya usa `reminderMinutes()`, así esta función no necesita tocarse).
3. `eventRequest(...)`: si `p.origenTipo === 'ALERTA_REPRODUCCION'`, construir:
   - `summary`: `p.titulo` (ej. "Parto próximo").
   - `description`: `p.mensaje` (ej. "Lucero tiene parto estimado para el 22/06/2027").
   - `location`: omitir o dejar vacío (una alerta de reproducción no trae
     propiedad/potrero/lote en su modelo actual).
   - el resto (`start`/`end`/`reminders`/`extendedProperties`) se arma igual, usando
     `p.fechaVencimiento` como fecha del evento — sin cambios en `reminderMinutes()` si el
     backend ya manda `diasAlerta` calculado.
4. `processJob(...)`: cambiar las referencias a `job.ocurrenciaId` por `job.origenId` (el
   `eventId` de Google se sigue derivando igual, solo cambia el nombre del campo fuente).

### 3.6 Frontend — visibilidad (a decidir con el usuario antes de implementar)

Dos caminos, no excluyentes:
- **Mínimo viable**: no tocar `CalendarioExternoPanel.tsx`. La sincronización funciona
  igual, pero el usuario no ve el estado de sync de sus alertas de reproducción en esa
  pantalla — solo lo nota porque el evento aparece en su Google Calendar.
- **Completo**: extender `listarOcurrencias()` (o agregar un método hermano
  `listarAlertasSincronizadas()`) y mostrar ambos orígenes en la misma tabla del panel
  "Calendario", con una columna que distinga "Sanidad" de "Reproducción".

Recomendación: arrancar con el mínimo viable (la sincronización es lo que el usuario pidió
explícitamente), y ofrecer la visibilidad en el panel como una mejora posterior si hace
falta depurar por qué algo no sincronizó.

### 3.7 Tests

- Backend: nuevo test de integración (mismo estilo que
  `AnimalServiceBatchIntegrationTest`, con SQLite real + Flyway) que:
  1. Crea una alerta `PARTO_PROXIMO` directamente en la tabla `alerta`.
  2. Llama `encolarAlertasPendientes` (o el flujo público que la dispara, ej. `guardar(...)`
     con `sincronizacionAutomatica=true`).
  3. Verifica que aparece un trabajo `CREAR` en `cola_sincronizacion_calendario` con
     `origen_tipo='ALERTA_REPRODUCCION'`.
  4. Marca la alerta como `RESUELTA` y llama `encolarDesenlacesAlertas`.
  5. Verifica que se encola un `CANCELAR`.
- Backend: test de que una ocurrencia sanitaria existente sigue sincronizando exactamente
  igual que antes (regresión — no romper lo que ya funciona).
- Electron: extender `google-calendar-sync` (si tiene tests — confirmar) o agregar uno
  nuevo que verifique que `eventRequest()` arma un `summary`/`description` correctos para un
  payload con `origenTipo='ALERTA_REPRODUCCION'`.

## 4. Orden de implementación recomendado

1. Parte A completa primero (más chica, autocontenida, sin riesgo de romper sincronización
   existente). Verificar con tests antes de tocar nada de la Parte B.
2. Migración de la Parte B (3.1) en una base de datos de prueba, confirmando que:
   - Los datos existentes en `cola_sincronizacion_calendario` /
     `correspondencia_calendario_externo` sobreviven la recreación de tabla.
   - Los índices únicos por expresión compilan y funcionan como se espera en SQLite 3.46.
3. Backend de la Parte B (3.3, 3.4), con los tests de 3.7 antes de tocar Electron.
4. Electron (3.5) — requiere reconstruir la app de escritorio para probarse de verdad, igual
   que con el cambio de notificaciones agrupadas ya hecho en esta misma sesión.
5. Decisión de UI (3.6) al final, una vez que el resto ya sincroniza de verdad.

## 5. Checklist de verificación manual post-implementación

- [ ] Cambiar "Días hasta el destete" en Configuración y confirmar que una cría nueva
      recibe una fecha de `DESTETE_PROXIMO` acorde al nuevo valor (no 210 fijo).
- [ ] Con Google Calendar conectado y sincronización automática activada: confirmar una
      gestación con parto próximo real y verificar que aparece un evento en el calendario de
      Google en menos de un ciclo de sincronización (~60s Electron + reclamo inmediato).
- [ ] Registrar el parto correspondiente y confirmar que el evento desaparece (o se marca
      cancelado) de Google Calendar.
- [ ] Registrar una cría y confirmar que aparece un evento "Destete próximo" en Google
      Calendar con la fecha correcta.
- [ ] Registrar el destete de esa cría y confirmar que el evento se retira del calendario.
- [ ] Confirmar que una actividad sanitaria (vacuna, por ejemplo) sigue sincronizando
      exactamente igual que antes de este cambio (no regresión).

## 6. Riesgos y decisiones abiertas

- **Riesgo de la migración de tabla**: recrear `cola_sincronizacion_calendario` y
  `correspondencia_calendario_externo` con datos reales de producción (si el usuario ya usa
  la sincronización con sanidad) requiere probar la migración contra una copia real de su
  base antes de aplicarla en su instalación — no solo contra una base de test vacía.
- **Nombre de campo `origenId` vs. mantener `ocurrenciaId`**: renombrar toca más archivos
  (Java + TypeScript) pero deja el código más claro a largo plazo. Alternativa más barata:
  dejar `ocurrenciaId` como nombre aunque a veces contenga un id de alerta, documentando la
  ambigüedad con un comentario. Recomendación: renombrar, es la opción más honesta.
- **`ACTUALIZAR` para alertas**: si más adelante se quiere reflejar, por ejemplo, una
  corrección de fecha probable de parto, hay que revisar si `Alerta` se actualiza in-place
  (mismo id, `fecha_vencimiento` nueva) o si se cancela una y se crea otra — de eso depende
  si `ACTUALIZAR` tiene sentido o si alcanza con que `encolarDesenlacesAlertas` +
  `encolarAlertasPendientes` combinados generen un CANCELAR seguido de un CREAR.
- **Nombres exactos de archivos a confirmar al implementar** (no verificados en esta pasada
  de investigación, marcarlos explícitamente antes de escribir código):
  - Si existe un DTO de API separado para `TrabajoSincronizacionCalendario` (fuera del
    record de aplicación) que también necesite el campo `origenTipo`.
  - Nombre exacto del test file de `ConfiguracionService`/`JdbcConfiguracionAdapter`, si
    existe.
  - Si `ReproduccionCicloServiceTest` existe con ese nombre exacto.
  - Si `registrarParto()` ya resuelve el `PARTO_PROXIMO` al confirmarse el parto (sección
    3.4) — esto cambia si hace falta un paso adicional o no.

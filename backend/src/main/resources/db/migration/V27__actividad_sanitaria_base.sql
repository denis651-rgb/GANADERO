-- Sanidad genérica y sin inventario de medicamentos: "actividad sanitaria" deja de asumir que
-- todo es VACUNACION. Se amplía plan_sanitario_item con: nombre/descripción propios, medicamento
-- recomendado como texto informativo (nunca inventario), principio activo, dosis estructurada
-- (cantidad/unidad/tipo de cálculo/peso de referencia/mín/máx), vía y lugar de aplicación como
-- catálogos separados y controlados, categorías aplicables (ahora una lista, no una sola), y
-- versionado con vigencia temporal (identidad_logica_id/numero_version/version_anterior_id/
-- vigente_desde/vigente_hasta/motivo_version) — patrón nuevo en este repo, no existía ninguna
-- entidad versionada con vigencia hasta ahora.
--
-- Igual que V3 (mismo problema documentado ahí): `aplicacion_sanitaria.plan_item_id` referencia
-- esta tabla, así que se crea la tabla nueva con nombre temporal, se copian los datos, se
-- suelta la original y recién ahí se renombra — nunca al revés.
--
-- El remapeo de tipo_actividad (VIGILANCIA_EPIDEMIOLOGICA->VIGILANCIA, OTRO->OTRA) y el caso
-- ambiguo CONTROL->OTRA (marcado requiere_revision) se hacen aquí mismo, en el propio INSERT,
-- porque el nuevo CHECK de tipo_actividad ya no acepta los valores viejos: si se difiriera a
-- una migración Java posterior (como V28, que sólo infiere modalidad/modalidad_config), el
-- INSERT de esta migración violaría el CHECK antes de que Java pudiera correr.
--
-- PRAGMA foreign_keys=OFF: a diferencia de cuando V3/V4 recrearon estas tablas (con
-- aplicacion_sanitaria todavía vacía), hoy sí existen jornadas confirmadas reales con filas en
-- aplicacion_sanitaria que referencian plan_sanitario_item/jornada_sanitaria — soltar esas
-- tablas con foreign_keys=on activo falla con SQLITE_CONSTRAINT_FOREIGNKEY (confirmado
-- empíricamente contra una copia de la base real). Se desactiva para esta migración puntual.
pragma foreign_keys = off;

create table plan_sanitario_item_nuevo (
    id text primary key,
    plan_id text not null references plan_sanitario(id),
    identidad_logica_id text not null,
    numero_version integer not null default 1,
    version_anterior_id text references plan_sanitario_item_nuevo(id),
    vigente_desde text not null,
    vigente_hasta text,
    motivo_version text,
    codigo_interno text,
    nombre text not null default '',
    descripcion text,
    tipo_actividad text not null,
    tipo_actividad_legacy text,
    modalidad text not null default 'MANUAL',
    modalidad_config text not null default '{}',
    producto_id text,
    producto_recomendado_texto text,
    principio_activo text,
    instrucciones_veterinario text,
    observaciones text,
    dosis numeric,
    unidad_dosis text,
    dosis_cantidad numeric,
    dosis_unidad text,
    dosis_unidad_detalle text,
    dosis_tipo_calculo text not null default 'NO_APLICA',
    dosis_peso_referencia_kg numeric,
    dosis_minima numeric,
    dosis_maxima numeric,
    via_administracion text,
    via_administracion_codigo text,
    via_administracion_detalle text,
    lugar_aplicacion text,
    lugar_aplicacion_detalle text,
    categoria_animal_id text references categoria_animal(id),
    categorias_aplicables text not null default '[]',
    sexo_aplicable text,
    edad_min_dias integer,
    edad_max_dias integer,
    edad_unidad text not null default 'DIAS',
    permite_edad_desconocida integer not null default 0,
    frecuencia_dias integer,
    dias_alerta integer not null default 0,
    obligatorio integer not null default 0,
    origen_regulatorio text not null default 'CONFIGURABLE_ESTABLECIMIENTO',
    especie_aplicable text not null default 'BOVINO',
    requiere_revision integer not null default 0,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_item_tipo check (tipo_actividad in (
        'VACUNACION','DESPARASITACION','VITAMINIZACION','PRUEBA_DIAGNOSTICA',
        'CONTROL_ECTOPARASITARIO','VIGILANCIA','TRATAMIENTO_PREVENTIVO','OTRA'
    )),
    constraint ck_item_modalidad check (modalidad in (
        'POR_EDAD','PERIODICA','FECHA_PROGRAMADA','POR_HALLAZGO','MANUAL'
    )),
    constraint ck_item_dosis_tipo_calculo check (dosis_tipo_calculo in (
        'FIJA_POR_ANIMAL','POR_PESO','SEGUN_INDICACION','NO_APLICA'
    )),
    constraint ck_item_dosis_unidad check (dosis_unidad is null or dosis_unidad in (
        'ML','MG','G','TABLETA','DOSIS','GOTA','APLICACION','ML_POR_KG','ML_POR_10KG','ML_POR_50KG','MG_POR_KG','OTRA'
    )),
    constraint ck_item_via_codigo check (via_administracion_codigo is null or via_administracion_codigo in (
        'SUBCUTANEA','INTRAMUSCULAR','INTRAVENOSA','ORAL','TOPICA','POUR_ON','INTRANASAL','OTRA','NO_APLICA'
    )),
    constraint ck_item_lugar check (lugar_aplicacion is null or lugar_aplicacion in (
        'CUELLO','TABLA_DEL_CUELLO','REGION_ESCAPULAR','LOMO','LINEA_DORSAL','BOCA','FOSA_NASAL','TODO_EL_CUERPO','OTRO','NO_APLICA'
    )),
    constraint ck_item_edad_unidad check (edad_unidad in ('DIAS','MESES','ANIOS')),
    constraint ck_item_sexo check (sexo_aplicable is null or sexo_aplicable in ('MACHO','HEMBRA')),
    constraint ck_item_dosis check (dosis is null or dosis > 0),
    constraint ck_item_frecuencia check (frecuencia_dias is null or frecuencia_dias > 0),
    constraint ck_item_alerta check (dias_alerta >= 0),
    constraint ck_item_origen_regulatorio check (origen_regulatorio in (
        'OBLIGATORIO_SENASAG','CAMPANA_RIESGO','RECOMENDADO_VETERINARIO','CONFIGURABLE_ESTABLECIMIENTO'
    ))
);

insert into plan_sanitario_item_nuevo (id, plan_id, identidad_logica_id, numero_version, vigente_desde,
    codigo_interno, nombre, tipo_actividad, tipo_actividad_legacy, requiere_revision,
    producto_id, producto_recomendado_texto,
    dosis, unidad_dosis, dosis_cantidad, dosis_tipo_calculo,
    via_administracion, categoria_animal_id, categorias_aplicables, sexo_aplicable,
    edad_min_dias, edad_max_dias, permite_edad_desconocida, frecuencia_dias, dias_alerta, obligatorio,
    origen_regulatorio, especie_aplicable, activo, created_at, created_by, updated_at, updated_by, version)
select id, plan_id, id, 1, created_at,
    null, coalesce(nullif(producto_recomendado_texto,''), tipo_actividad),
    case tipo_actividad
        when 'VIGILANCIA_EPIDEMIOLOGICA' then 'VIGILANCIA'
        when 'OTRO' then 'OTRA'
        when 'CONTROL' then 'OTRA'
        else tipo_actividad
    end,
    tipo_actividad,
    case when tipo_actividad = 'CONTROL' then 1 else 0 end,
    producto_id, producto_recomendado_texto,
    dosis, unidad_dosis, dosis, case when dosis is not null then 'FIJA_POR_ANIMAL' else 'NO_APLICA' end,
    via_administracion, categoria_animal_id,
    case when categoria_animal_id is not null then '["' || categoria_animal_id || '"]' else '[]' end,
    sexo_aplicable,
    edad_min_dias, edad_max_dias, permite_edad_desconocida, frecuencia_dias, dias_alerta, obligatorio,
    origen_regulatorio, especie_aplicable, activo, created_at, created_by, updated_at, updated_by, version
from plan_sanitario_item;

drop table plan_sanitario_item;
alter table plan_sanitario_item_nuevo rename to plan_sanitario_item;

create index idx_plan_items on plan_sanitario_item(plan_id) where activo = 1;
create index idx_plan_items_identidad on plan_sanitario_item(identidad_logica_id);
create index idx_plan_items_vigentes on plan_sanitario_item(plan_id) where vigente_hasta is null;

-- jornada_sanitaria.tipo_jornada usa el mismo universo de tipos; incluida por completitud del
-- mismo recorte de enum (mismo mapeo que arriba). aplicacion_sanitaria.jornada_id y
-- jornada_animal.jornada_id referencian esta tabla, así que se aplica el mismo patrón
-- "crear con nombre temporal, copiar, soltar la original, recién ahí renombrar" documentado en
-- V2__multi_propiedad.sql — renombrar la tabla ORIGINAL primero haría que SQLite reescriba esas
-- referencias hacia el nombre temporal, dejándolas colgantes al soltarlo.
create table jornada_sanitaria_nueva (
    id text primary key,
    tipo_jornada text not null,
    fecha_inicio text not null,
    fecha_fin text,
    propiedad_id text not null,
    potrero_id text references potrero(id),
    lote_ganadero_id text references lote_ganadero(id),
    responsable_id text,
    veterinario_id text,
    estado text not null default 'BORRADOR',
    observaciones text,
    operation_id text unique,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_jornada_estado check (estado in ('BORRADOR','EN_PROCESO','CONFIRMADA','ANULADA')),
    constraint ck_jornada_tipo check (tipo_jornada in (
        'VACUNACION','DESPARASITACION','VITAMINIZACION','PRUEBA_DIAGNOSTICA',
        'CONTROL_ECTOPARASITARIO','VIGILANCIA','TRATAMIENTO_PREVENTIVO','OTRA'
    )),
    constraint ck_jornada_fechas check (fecha_fin is null or fecha_fin >= fecha_inicio)
);

insert into jornada_sanitaria_nueva (id, tipo_jornada, fecha_inicio, fecha_fin, propiedad_id, potrero_id,
    lote_ganadero_id, responsable_id, veterinario_id, estado, observaciones, operation_id,
    created_at, created_by, updated_at, updated_by, version)
select id,
    case tipo_jornada
        when 'VIGILANCIA_EPIDEMIOLOGICA' then 'VIGILANCIA'
        when 'OTRO' then 'OTRA'
        when 'CONTROL' then 'OTRA'
        else tipo_jornada
    end,
    fecha_inicio, fecha_fin, propiedad_id, potrero_id, lote_ganadero_id, responsable_id, veterinario_id,
    estado, observaciones, operation_id, created_at, created_by, updated_at, updated_by, version
from jornada_sanitaria;

drop table jornada_sanitaria;
alter table jornada_sanitaria_nueva rename to jornada_sanitaria;

pragma foreign_keys = on;

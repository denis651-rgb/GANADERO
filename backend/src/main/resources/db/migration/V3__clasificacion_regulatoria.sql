-- Fase 1 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, secciones 2 y 4a):
-- clasificacion regulatoria de actividades sanitarias (OBLIGATORIO_SENASAG / CAMPANA_RIESGO /
-- RECOMENDADO_VETERINARIO / CONFIGURABLE_ESTABLECIMIENTO) y correccion del modelo de aftosa
-- (deja de ser una VACUNACION con frecuencia fija y pasa a ser VIGILANCIA_EPIDEMIOLOGICA).
--
-- `plan_sanitario_item` tiene dos CHECK constraints que hay que tocar (el tipo de actividad,
-- para sumar VIGILANCIA_EPIDEMIOLOGICA, y uno nuevo para origen_regulatorio) y SQLite no
-- permite alterar un CHECK existente ni agregar uno con `alter table`. Se reconstruye la
-- tabla completa (crear con nombre temporal, copiar datos, soltar la original, renombrar) en
-- vez de renombrar la original primero, por la misma razon documentada al inicio de
-- V2__multi_propiedad.sql: `aplicacion_sanitaria.plan_item_id references plan_sanitario_item(id)`
-- quedaria apuntando a la tabla vieja si se la renombra antes de crear la nueva con el nombre
-- definitivo.

create table plan_sanitario_item_nuevo (
    id text primary key,
    plan_id text not null references plan_sanitario(id),
    tipo_actividad text not null,
    producto_id text,
    producto_recomendado_texto text,
    categoria_animal_id text references categoria_animal(id),
    sexo_aplicable text,
    edad_min_dias integer,
    edad_max_dias integer,
    dosis numeric,
    unidad_dosis text,
    frecuencia_dias integer,
    dias_alerta integer not null default 0,
    via_administracion text,
    obligatorio integer not null default 0,
    origen_regulatorio text not null default 'CONFIGURABLE_ESTABLECIMIENTO',
    especie_aplicable text not null default 'BOVINO',
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_item_tipo check (tipo_actividad in (
        'VACUNACION','DESPARASITACION','VITAMINIZACION','CONTROL','PRUEBA_DIAGNOSTICA','OTRO','VIGILANCIA_EPIDEMIOLOGICA'
    )),
    constraint ck_item_sexo check (sexo_aplicable is null or sexo_aplicable in ('MACHO','HEMBRA')),
    constraint ck_item_dosis check (dosis is null or dosis > 0),
    constraint ck_item_frecuencia check (frecuencia_dias is null or frecuencia_dias > 0),
    constraint ck_item_alerta check (dias_alerta >= 0),
    constraint ck_item_origen_regulatorio check (origen_regulatorio in (
        'OBLIGATORIO_SENASAG','CAMPANA_RIESGO','RECOMENDADO_VETERINARIO','CONFIGURABLE_ESTABLECIMIENTO'
    ))
);

-- origen_regulatorio y especie_aplicable no se seleccionan: no hay datos previos de aftosa
-- ni de ninguna clasificacion regulatoria (no existia el concepto), asi que todo lo que ya
-- estaba cargado toma el default declarado arriba ('CONFIGURABLE_ESTABLECIMIENTO' / 'BOVINO').
insert into plan_sanitario_item_nuevo (id, plan_id, tipo_actividad, producto_id, producto_recomendado_texto,
    categoria_animal_id, sexo_aplicable, edad_min_dias, edad_max_dias, dosis, unidad_dosis, frecuencia_dias,
    dias_alerta, via_administracion, obligatorio, activo, created_at, created_by, updated_at, updated_by, version)
select id, plan_id, tipo_actividad, producto_id, producto_recomendado_texto,
    categoria_animal_id, sexo_aplicable, edad_min_dias, edad_max_dias, dosis, unidad_dosis, frecuencia_dias,
    dias_alerta, via_administracion, obligatorio, activo, created_at, created_by, updated_at, updated_by, version
from plan_sanitario_item;

drop table plan_sanitario_item;
alter table plan_sanitario_item_nuevo rename to plan_sanitario_item;

create index idx_plan_items on plan_sanitario_item(plan_id) where activo = 1;

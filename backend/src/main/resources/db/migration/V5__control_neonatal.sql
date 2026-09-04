-- Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.c):
-- checklist de control neonatal para terneros recien nacidos. Independiente del plan
-- sanitario activo en la propiedad: no referencia plan_sanitario_item ni el motor de
-- elegibilidad.
create table control_neonatal (
    id text primary key,
    empresa_id text,
    animal_id text not null references animal(id),
    fecha_control text not null,
    momento text not null,
    calostrado text not null,
    ombligo_desinfectado integer not null default 0,
    ombligo_estado text,
    diarrea integer not null default 0,
    estado_general text,
    lactancia text,
    temperatura_c numeric,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_control_neonatal_momento check (momento in ('DIA_0','PRIMERA_SEMANA')),
    constraint ck_control_neonatal_calostrado check (calostrado in ('CORRECTO','INSUFICIENTE','DESCONOCIDO','NO_APLICA'))
);

create index idx_control_neonatal_animal on control_neonatal(animal_id);

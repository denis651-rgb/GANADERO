-- Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.e):
-- examen reproductivo de toros y vaquillas antes del servicio. El sexo NO se duplica
-- en esta tabla: se deriva de animal.sexo en el momento de la consulta.
create table examen_reproductivo (
    id text primary key,
    empresa_id text,
    animal_id text not null references animal(id),
    fecha text not null,
    resultado text not null,
    veterinario_id text,
    -- Campos de TORO (solo si el animal es macho)
    circunferencia_escrotal_cm numeric,
    motilidad_espermatica_pct numeric,
    morfologia_pct numeric,
    libido text,
    capacidad_servicio text,
    -- Campos de VAQUILLA (solo si el animal es hembra)
    peso_kg numeric,
    porcentaje_peso_adulto numeric,
    condicion_corporal numeric,
    desarrollo_reproductivo text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_examen_repro_resultado check (resultado in ('APTO','NO_APTO','OBSERVACION')),
    constraint ck_examen_repro_motilidad check (motilidad_espermatica_pct is null or motilidad_espermatica_pct between 0 and 100),
    constraint ck_examen_repro_morfologia check (morfologia_pct is null or morfologia_pct between 0 and 100),
    constraint ck_examen_repro_pct_peso_adulto check (porcentaje_peso_adulto is null or porcentaje_peso_adulto between 0 and 100),
    constraint ck_examen_repro_condicion check (condicion_corporal is null or condicion_corporal between 1 and 5)
);

create index idx_examen_repro_animal on examen_reproductivo(animal_id);

create table examen_reproductivo_prueba (
    id text primary key,
    examen_id text not null references examen_reproductivo(id),
    enfermedad text not null,
    resultado text not null,
    constraint ck_examen_prueba_enfermedad check (enfermedad in ('IBR','BVD','BRUCELOSIS','LEPTOSPIROSIS','TRICOMONIASIS','CAMPYLOBACTERIOSIS')),
    constraint ck_examen_prueba_resultado check (resultado in ('NEGATIVO','POSITIVO','NO_REALIZADO')),
    constraint uq_examen_prueba unique (examen_id, enfermedad)
);

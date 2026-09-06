-- Categorías por edad configurables (Mi finca -> Configuración general) con excepciones manuales
-- extensibles (ya no hardcodeadas al código 'BUEY') y orden de evaluación explícito.
alter table categoria_animal add column clasificacion_automatica integer not null default 1;
alter table categoria_animal add column orden_evaluacion integer not null default 0;
update categoria_animal set clasificacion_automatica = 0 where codigo = 'BUEY';

-- Historial inmutable de cambios de categoría: nunca se sobreescribe ni se elimina.
create table historial_categoria_animal (
    id text primary key,
    animal_id text not null references animal(id),
    categoria_anterior_id text references categoria_animal(id),
    categoria_nueva_id text not null references categoria_animal(id),
    fecha_cambio text not null default current_timestamp,
    tipo_cambio text not null,
    motivo text,
    usuario_id text,
    edad_dias integer,
    edad_confirmada integer not null default 0,
    categoria_config_id text references categoria_animal(id),
    created_at text not null default current_timestamp,
    constraint ck_historial_categoria_tipo check (tipo_cambio in ('AUTOMATICO','MANUAL','CORRECCION'))
);

create index idx_historial_categoria_animal on historial_categoria_animal(animal_id, fecha_cambio desc);

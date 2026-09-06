-- Calendario sanitario genérico (sección 19-20): reemplaza, para toda actividad (no sólo
-- VACUNACION), lo que antes vivía implícito dentro de ProyectarCalendarioSanitarioService sin
-- una entidad propia. `ciclo_clave` es la clave anti-duplicado por modalidad: animal+actividad+
-- edad objetivo (POR_EDAD), animal+actividad+número de ciclo (PERIODICA), animal+actividad+fecha
-- programada (FECHA_PROGRAMADA), animal+actividad+hallazgo (POR_HALLAZGO). MANUAL no genera fila.
create table evento_calendario_sanitario (
    id text primary key,
    empresa_id text,
    actividad_id text not null references plan_sanitario_item(id),
    animal_id text not null references animal(id),
    ciclo_clave text not null,
    fecha_prevista text not null,
    ventana_desde text,
    ventana_hasta text,
    estado text not null default 'PROYECTADO',
    origen_modalidad text not null,
    hallazgo_origen_tipo text,
    hallazgo_origen_id text,
    jornada_id text references jornada_sanitaria(id),
    prioridad text not null default 'NORMAL',
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_evento_estado check (estado in (
        'PROYECTADO','PROGRAMADO','EN_PREPARACION','REALIZADO','VENCIDO','OMITIDO','CANCELADO'
    )),
    constraint ck_evento_modalidad check (origen_modalidad in (
        'POR_EDAD','PERIODICA','FECHA_PROGRAMADA','POR_HALLAZGO','MANUAL'
    )),
    constraint uq_evento_ciclo unique (actividad_id, animal_id, ciclo_clave)
);

create unique index uq_evento_hallazgo on evento_calendario_sanitario(actividad_id, hallazgo_origen_tipo, hallazgo_origen_id)
    where hallazgo_origen_id is not null;
create index idx_evento_animal on evento_calendario_sanitario(animal_id, fecha_prevista);
create index idx_evento_pendientes on evento_calendario_sanitario(estado, fecha_prevista)
    where estado in ('PROYECTADO','PROGRAMADO','EN_PREPARACION');

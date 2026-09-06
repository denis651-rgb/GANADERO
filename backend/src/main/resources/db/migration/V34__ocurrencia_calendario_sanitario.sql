-- Fase 1 de la integración con Google Calendar: agrupa los eventos individuales por animal en una
-- sola "ocurrencia operativa" (misma actividad + fecha exacta + propiedad + potrero + lote), para
-- que la futura sincronización externa envíe un evento por grupo y no uno por animal.
-- `ocurrencia_clave` es la clave anti-duplicado: se usa una columna de texto en vez de un unique
-- compuesto porque SQLite trata cada NULL como distinto en un índice único, lo que rompería la
-- deduplicación cuando lote_ganadero_id es nulo (no todo animal pertenece a un lote).
create table ocurrencia_calendario_sanitario (
    id text primary key,
    plan_item_id text not null references plan_sanitario_item(id),
    fecha_prevista text not null,
    propiedad_id text not null references propiedad(id),
    potrero_id text not null references potrero(id),
    lote_ganadero_id text references lote_ganadero(id),
    ocurrencia_clave text not null,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0
);
create unique index uq_ocurrencia_clave on ocurrencia_calendario_sanitario(ocurrencia_clave);
create index idx_ocurrencia_fecha on ocurrencia_calendario_sanitario(fecha_prevista);

-- Nullable a propósito: los eventos generados antes de esta fase (y los de POR_HALLAZGO, que son
-- intrínsecamente individuales) quedan sin ocurrencia asociada.
alter table evento_calendario_sanitario add column ocurrencia_id text
    references ocurrencia_calendario_sanitario(id);

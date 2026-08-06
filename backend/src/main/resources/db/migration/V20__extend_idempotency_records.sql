alter table core.idempotency_records
    add column if not exists payload_hash varchar(64),
    add column if not exists response_code varchar(60),
    add column if not exists correlation_id varchar(64),
    add column if not exists expires_at timestamptz;

create index if not exists idx_idempotency_expires_at on core.idempotency_records(expires_at);

create or replace function core.limpiar_idempotencia_expirada(horas integer default 24)
returns bigint language plpgsql as $$
declare
    eliminados bigint;
begin
    delete from core.idempotency_records
    where expires_at is not null and expires_at < now() - make_interval(hours => horas);
    get diagnostics eliminados = row_count;
    return eliminados;
end $$;

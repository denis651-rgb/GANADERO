alter table alertas.alertas
    add column clave_idempotencia varchar(500);

update alertas.alertas
set clave_idempotencia = 'LEGACY|' || id::text
where clave_idempotencia is null;

alter table alertas.alertas
    alter column clave_idempotencia set not null;

create unique index uq_alerta_clave_idempotencia
    on alertas.alertas (empresa_id, clave_idempotencia);

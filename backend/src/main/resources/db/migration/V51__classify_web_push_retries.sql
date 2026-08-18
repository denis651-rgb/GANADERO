alter table alertas.entregas_notificacion
    add column if not exists reintentable boolean not null default true;

update alertas.entregas_notificacion
set reintentable = false,
    proximo_intento_at = null
where estado = 'ERROR'
  and ultimo_error ~ '(^|:|\\[)(HTTP |WEB_PUSH_HTTP_)(400|401|403)([^0-9]|$)';

drop index if exists alertas.idx_entregas_reintento;

create index idx_entregas_reintento
    on alertas.entregas_notificacion(proximo_intento_at, created_at)
    where estado = 'PENDIENTE' or (estado = 'ERROR' and reintentable);

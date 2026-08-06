alter table auditoria.registros
    add column if not exists dispositivo varchar(200),
    add column if not exists ip varchar(64),
    add column if not exists user_agent varchar(500),
    add column if not exists datos_anteriores jsonb,
    add column if not exists datos_nuevos jsonb,
    add column if not exists entidad_tipo varchar(120);

create index if not exists idx_auditoria_modulo_entidad
    on auditoria.registros (modulo, entidad, entidad_id);
create index if not exists idx_auditoria_usuario
    on auditoria.registros (usuario_id, created_at desc);

alter table seguridad.permisos
    add column modulo varchar(60) not null default 'GENERAL';

update seguridad.permisos
set modulo = 'EMPRESAS'
where codigo in ('EMPRESA_VER', 'EMPRESA_EDITAR', 'CONFIGURACION_EMPRESA_VER', 'CONFIGURACION_EMPRESA_EDITAR');

create index idx_roles_empresa_activo on seguridad.roles (empresa_id, activo);
create index idx_permisos_modulo_activo on seguridad.permisos (modulo, activo);


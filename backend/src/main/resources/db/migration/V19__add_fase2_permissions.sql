insert into seguridad.permisos (id, codigo, nombre, descripcion, modulo, activo)
values
 ('00000000-0000-0000-0001-000000000048','PESAJE_VER','Ver pesajes',null,'PESAJE',true),
 ('00000000-0000-0000-0001-000000000049','PESAJE_REGISTRAR','Registrar pesajes',null,'PESAJE',true),
 ('00000000-0000-0000-0001-000000000050','PESAJE_ANULAR','Anular pesajes',null,'PESAJE',true),
 ('00000000-0000-0000-0001-000000000051','SINC_DISPOSITIVO_REGISTRAR','Registrar dispositivo de sincronización',null,'SINCRONIZACION',true),
 ('00000000-0000-0000-0001-000000000052','SINC_PUSH','Enviar operaciones de sincronización',null,'SINCRONIZACION',true),
 ('00000000-0000-0000-0001-000000000053','SINC_PULL','Recibir cambios de sincronización',null,'SINCRONIZACION',true),
 ('00000000-0000-0000-0001-000000000054','SINC_BOOTSTRAP','Descargar datos iniciales para sincronización',null,'SINCRONIZACION',true),
 ('00000000-0000-0000-0001-000000000055','DOCUMENTO_VER','Ver documentos',null,'ARCHIVOS',true),
 ('00000000-0000-0000-0001-000000000056','DOCUMENTO_SUBIR','Subir documentos',null,'ARCHIVOS',true),
 ('00000000-0000-0000-0001-000000000057','DOCUMENTO_ELIMINAR','Eliminar documentos',null,'ARCHIVOS',true),
 ('00000000-0000-0000-0001-000000000058','DASHBOARD_VER','Ver dashboard',null,'DASHBOARD',true)
on conflict (codigo) do update set nombre = excluded.nombre, descripcion = excluded.descripcion,
    modulo = excluded.modulo, activo = excluded.activo;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r cross join seguridad.permisos p
where r.codigo in ('PROPIETARIO', 'ADMINISTRADOR') and r.empresa_id is null
  and p.codigo in ('PESAJE_VER','PESAJE_REGISTRAR','PESAJE_ANULAR',
                   'SINC_DISPOSITIVO_REGISTRAR','SINC_PUSH','SINC_PULL','SINC_BOOTSTRAP',
                   'DOCUMENTO_VER','DOCUMENTO_SUBIR','DOCUMENTO_ELIMINAR','DASHBOARD_VER')
on conflict do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'PESAJE_VER','PESAJE_REGISTRAR','SINC_DISPOSITIVO_REGISTRAR','SINC_PUSH','SINC_PULL','SINC_BOOTSTRAP',
 'DOCUMENTO_VER','DOCUMENTO_SUBIR'
)
where r.codigo in ('ENCARGADO_CAMPO', 'VETERINARIO') and r.empresa_id is null
on conflict do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'PESAJE_VER','DOCUMENTO_VER','SINC_PULL','DASHBOARD_VER'
)
where r.codigo = 'CONSULTA' and r.empresa_id is null
on conflict do nothing;

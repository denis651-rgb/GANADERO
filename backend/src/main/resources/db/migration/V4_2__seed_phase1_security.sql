insert into seguridad.roles (id, empresa_id, codigo, nombre, descripcion, es_sistema, activo)
values
 ('00000000-0000-0000-0000-000000000001', null, 'PROPIETARIO', 'Propietario', 'Control total de la empresa.', true, true),
 ('00000000-0000-0000-0000-000000000002', null, 'ADMINISTRADOR', 'Administrador', 'Administración operativa de la empresa.', true, true),
 ('00000000-0000-0000-0000-000000000003', null, 'VETERINARIO', 'Veterinario', 'Gestión sanitaria y reproductiva.', true, true),
 ('00000000-0000-0000-0000-000000000004', null, 'ENCARGADO_CAMPO', 'Encargado de campo', 'Operación ganadera de campo.', true, true),
 ('00000000-0000-0000-0000-000000000005', null, 'ENCARGADO_INVENTARIO', 'Encargado de inventario', 'Gestión de existencias e insumos.', true, true),
 ('00000000-0000-0000-0000-000000000006', null, 'CONTABILIDAD', 'Contabilidad', 'Consulta y gestión económica.', true, true),
 ('00000000-0000-0000-0000-000000000007', null, 'CONSULTA', 'Consulta', 'Acceso de solo lectura.', true, true)
on conflict do nothing;

insert into seguridad.permisos (id, codigo, nombre, descripcion, modulo, activo)
values
 ('00000000-0000-0000-0001-000000000001','EMPRESA_VER','Ver empresa',null,'EMPRESAS',true),
 ('00000000-0000-0000-0001-000000000002','EMPRESA_EDITAR','Editar empresa',null,'EMPRESAS',true),
 ('00000000-0000-0000-0001-000000000003','CONFIGURACION_EMPRESA_VER','Ver configuración',null,'EMPRESAS',true),
 ('00000000-0000-0000-0001-000000000004','CONFIGURACION_EMPRESA_EDITAR','Editar configuración',null,'EMPRESAS',true),
 ('00000000-0000-0000-0001-000000000005','USUARIO_VER','Ver usuarios',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000006','USUARIO_CREAR','Crear usuarios',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000007','USUARIO_EDITAR','Editar usuarios',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000008','USUARIO_BLOQUEAR','Bloquear usuarios',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000009','USUARIO_ASIGNAR_ROL','Asignar roles',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000010','ROL_VER','Ver roles',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000011','ROL_CREAR','Crear roles',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000012','ROL_EDITAR','Editar roles',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000013','ROL_ASIGNAR_PERMISOS','Asignar permisos',null,'SEGURIDAD',true),
 ('00000000-0000-0000-0001-000000000014','PROPIEDAD_VER','Ver propiedades',null,'PROPIEDADES',true),
 ('00000000-0000-0000-0001-000000000015','PROPIEDAD_CREAR','Crear propiedades',null,'PROPIEDADES',true),
 ('00000000-0000-0000-0001-000000000016','PROPIEDAD_EDITAR','Editar propiedades',null,'PROPIEDADES',true),
 ('00000000-0000-0000-0001-000000000017','POTRERO_VER','Ver potreros',null,'POTREROS',true),
 ('00000000-0000-0000-0001-000000000018','POTRERO_CREAR','Crear potreros',null,'POTREROS',true),
 ('00000000-0000-0000-0001-000000000019','POTRERO_EDITAR','Editar potreros',null,'POTREROS',true),
 ('00000000-0000-0000-0001-000000000020','ANIMAL_VER','Ver animales',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000021','ANIMAL_CREAR','Crear animales',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000022','ANIMAL_EDITAR','Editar animales',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000023','ANIMAL_CAMBIAR_ESTADO','Cambiar estado animal',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000024','ANIMAL_REGISTRAR_BAJA','Registrar baja animal',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000025','IDENTIFICADOR_VER','Ver identificadores',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000026','IDENTIFICADOR_ASIGNAR','Asignar identificadores',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000027','IDENTIFICADOR_RETIRAR','Retirar identificadores',null,'ANIMALES',true),
 ('00000000-0000-0000-0001-000000000028','LOTE_VER','Ver lotes',null,'LOTES',true),
 ('00000000-0000-0000-0001-000000000029','LOTE_CREAR','Crear lotes',null,'LOTES',true),
 ('00000000-0000-0000-0001-000000000030','LOTE_EDITAR','Editar lotes',null,'LOTES',true),
 ('00000000-0000-0000-0001-000000000031','LOTE_ASIGNAR_ANIMALES','Asignar animales a lotes',null,'LOTES',true),
 ('00000000-0000-0000-0001-000000000032','MOVIMIENTO_VER','Ver movimientos',null,'MOVIMIENTOS',true),
 ('00000000-0000-0000-0001-000000000033','MOVIMIENTO_CREAR','Crear movimientos',null,'MOVIMIENTOS',true),
 ('00000000-0000-0000-0001-000000000034','MOVIMIENTO_CONFIRMAR','Confirmar movimientos',null,'MOVIMIENTOS',true),
 ('00000000-0000-0000-0001-000000000035','MOVIMIENTO_ANULAR','Anular movimientos',null,'MOVIMIENTOS',true),
 ('00000000-0000-0000-0001-000000000036','AUDITORIA_VER','Ver auditoría',null,'AUDITORIA',true)
on conflict (codigo) do update set nombre = excluded.nombre, descripcion = excluded.descripcion,
    modulo = excluded.modulo, activo = excluded.activo;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r cross join seguridad.permisos p
where r.codigo in ('PROPIETARIO', 'ADMINISTRADOR') and r.empresa_id is null
on conflict do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'EMPRESA_VER','PROPIEDAD_VER','POTRERO_VER','POTRERO_CREAR','POTRERO_EDITAR','ANIMAL_VER','ANIMAL_CREAR',
 'ANIMAL_EDITAR','ANIMAL_CAMBIAR_ESTADO','IDENTIFICADOR_VER','IDENTIFICADOR_ASIGNAR','IDENTIFICADOR_RETIRAR',
 'LOTE_VER','LOTE_CREAR','LOTE_EDITAR','LOTE_ASIGNAR_ANIMALES','MOVIMIENTO_VER','MOVIMIENTO_CREAR','MOVIMIENTO_CONFIRMAR'
)
where r.codigo = 'ENCARGADO_CAMPO' and r.empresa_id is null
on conflict do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo like '%_VER'
where r.codigo = 'CONSULTA' and r.empresa_id is null
on conflict do nothing;


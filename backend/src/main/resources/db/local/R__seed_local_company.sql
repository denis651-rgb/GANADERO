insert into core.empresas (
    id, codigo, razon_social, nombre_comercial, zona_horaria, moneda, estado, created_by, updated_by
) values (
    '00000000-0000-0000-0000-000000000001', 'LOCAL-001', 'Ganadería Local', 'GANADERO Local',
    'America/La_Paz', 'BOB', 'ACTIVA', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001'
) on conflict (id) do nothing;

insert into core.configuraciones_empresa (empresa_id, created_by, updated_by)
values ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001')
on conflict (empresa_id) do nothing;

insert into seguridad.perfiles_usuario (id, nombres, apellidos, activo, created_by, updated_by)
values ('00000000-0000-0000-0000-000000000001', 'Usuario', 'Local', true,
        '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

insert into seguridad.miembros_empresa (
    id, empresa_id, usuario_id, cargo, estado, fecha_ingreso, acceso_todas_propiedades, created_by, updated_by
) values (
    '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001', 'Propietario local', 'ACTIVO', current_date, true,
    '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'
) on conflict (id) do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select '00000000-0000-0000-0000-000000000001', id
from seguridad.permisos
where codigo in ('EMPRESA_VER', 'EMPRESA_EDITAR', 'CONFIGURACION_EMPRESA_VER', 'CONFIGURACION_EMPRESA_EDITAR')
on conflict do nothing;

insert into seguridad.usuario_roles (miembro_empresa_id, rol_id)
values ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001')
on conflict do nothing;

insert into core.propiedades (id,empresa_id,codigo,nombre,departamento,municipio,superficie_ha,created_by,updated_by)
values ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
        'HAC-LOCAL','Hacienda local','Santa Cruz','Yapacaní',100,
        '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

insert into campo.sectores (id,empresa_id,propiedad_id,codigo,nombre,created_by,updated_by)
values ('30000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001','SEC-LOCAL','Sector local',
        '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

insert into campo.potreros (id,empresa_id,propiedad_id,sector_id,codigo,nombre,superficie_ha,tipo_pasto_id,
                            capacidad_ua,tiene_agua,estado,created_by,updated_by)
values ('40000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001',
        'POT-LOCAL','Potrero local',20,'10000000-0000-0000-0000-000000000001',15,true,'DISPONIBLE',
        '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

insert into ganado.animales (id,empresa_id,codigo,nombre,sexo,fecha_nacimiento,raza_principal_id,
 categoria_actual_id,proposito,origen,propiedad_actual_id,potrero_actual_id,estado,fecha_ingreso,created_by,updated_by)
values ('70000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
 'A-LOCAL-001','Animal local','HEMBRA','2024-01-15','50000000-0000-0000-0000-000000000001',
 '60000000-0000-0000-0000-000000000003','CARNE','NACIDO',
 '20000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001','ACTIVO',current_date,
 '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

insert into ganado.eventos_animal (id,empresa_id,animal_id,tipo,estado_nuevo,motivo,registrado_por)
values ('80000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
 '70000000-0000-0000-0000-000000000001','NACIMIENTO','ACTIVO','Registro local inicial',
 '00000000-0000-0000-0000-000000000001')
on conflict (id) do nothing;

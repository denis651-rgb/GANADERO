-- Fase 3: permisos de Reproducción, Sanidad y Alertas
insert into seguridad.permisos (id, codigo, nombre, descripcion, modulo, activo)
values
 ('00000000-0000-0000-0001-000000000060','REPRODUCCION_VER','Ver registros de reproducción',null,'REPRODUCCION',true),
 ('00000000-0000-0000-0001-000000000061','REPRODUCCION_REGISTRAR','Registrar eventos reproductivos',null,'REPRODUCCION',true),
 ('00000000-0000-0000-0001-000000000062','REPRODUCCION_EDITAR','Editar registros de reproducción',null,'REPRODUCCION',true),
 ('00000000-0000-0000-0001-000000000063','REPRODUCCION_ANULAR','Anular registros de reproducción',null,'REPRODUCCION',true),
 ('00000000-0000-0000-0001-000000000064','SANIDAD_VER','Ver registros de sanidad',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000065','SANIDAD_PLAN_ADMINISTRAR','Administrar planes sanitarios',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000066','SANIDAD_JORNADA_CREAR','Crear jornadas sanitarias',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000075','SANIDAD_JORNADA_CONFIRMAR','Confirmar jornadas sanitarias',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000067','SANIDAD_APLICAR','Aplicar vacunas y tratamientos',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000068','SANIDAD_CASO_CREAR','Crear casos clínicos',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000069','SANIDAD_TRATAMIENTO_CREAR','Crear tratamientos',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000070','SANIDAD_TRATAMIENTO_APLICAR','Aplicar tratamientos',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000071','SANIDAD_CUARENTENA_GESTIONAR','Gestionar cuarentenas',null,'SANIDAD',true),
 ('00000000-0000-0000-0001-000000000072','ALERTA_VER','Ver alertas',null,'ALERTAS',true),
 ('00000000-0000-0000-0001-000000000073','ALERTA_ATENDER','Atender alertas',null,'ALERTAS',true),
 ('00000000-0000-0000-0001-000000000076','ALERTA_RESOLVER','Resolver alertas',null,'ALERTAS',true),
 ('00000000-0000-0000-0001-000000000074','ALERTA_CONFIGURAR','Configurar reglas de alerta',null,'ALERTAS',true)
on conflict (codigo) do update set nombre = excluded.nombre, descripcion = excluded.descripcion,
    modulo = excluded.modulo, activo = excluded.activo;

-- PROPIETARIO y ADMINISTRADOR: todos los permisos de fase 3
insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r cross join seguridad.permisos p
where r.codigo in ('PROPIETARIO', 'ADMINISTRADOR') and r.empresa_id is null
  and p.codigo in ('REPRODUCCION_VER','REPRODUCCION_REGISTRAR','REPRODUCCION_EDITAR','REPRODUCCION_ANULAR',
                   'SANIDAD_VER','SANIDAD_PLAN_ADMINISTRAR','SANIDAD_JORNADA_CREAR','SANIDAD_JORNADA_CONFIRMAR','SANIDAD_APLICAR',
                   'SANIDAD_CASO_CREAR','SANIDAD_TRATAMIENTO_CREAR','SANIDAD_TRATAMIENTO_APLICAR',
                   'SANIDAD_CUARENTENA_GESTIONAR',
                   'ALERTA_VER','ALERTA_ATENDER','ALERTA_RESOLVER','ALERTA_CONFIGURAR')
on conflict do nothing;

-- VETERINARIO: reproducción completa + sanidad completa + alertas operativas
insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'REPRODUCCION_VER','REPRODUCCION_REGISTRAR','REPRODUCCION_EDITAR','REPRODUCCION_ANULAR',
 'SANIDAD_VER','SANIDAD_PLAN_ADMINISTRAR','SANIDAD_JORNADA_CREAR','SANIDAD_JORNADA_CONFIRMAR','SANIDAD_APLICAR',
 'SANIDAD_CASO_CREAR','SANIDAD_TRATAMIENTO_CREAR','SANIDAD_TRATAMIENTO_APLICAR',
 'SANIDAD_CUARENTENA_GESTIONAR','ALERTA_VER','ALERTA_ATENDER','ALERTA_RESOLVER'
)
where r.codigo = 'VETERINARIO' and r.empresa_id is null
on conflict do nothing;

-- ENCARGADO_CAMPO: registro de celos y jornadas, lectura de sanidad y alertas
insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'REPRODUCCION_VER','REPRODUCCION_REGISTRAR',
 'SANIDAD_VER','SANIDAD_JORNADA_CREAR','SANIDAD_APLICAR','ALERTA_VER'
)
where r.codigo = 'ENCARGADO_CAMPO' and r.empresa_id is null
on conflict do nothing;

-- CONSULTA: solo lectura
insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo in (
 'REPRODUCCION_VER','SANIDAD_VER','ALERTA_VER'
)
where r.codigo = 'CONSULTA' and r.empresa_id is null
on conflict do nothing;

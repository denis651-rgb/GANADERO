-- Etapas 5 y 6: fortalecer movimientos y timeline del animal

-- 1. Reversión controlada de movimientos confirmados
alter table ganado.movimientos add column if not exists observacion varchar(1000);
alter table ganado.movimientos add column if not exists usuario_revierte uuid;
alter table ganado.movimientos add column if not exists fecha_reversion timestamptz;
alter table ganado.movimientos add column if not exists motivo_reversion varchar(1000);
alter table ganado.movimientos add column if not exists movimiento_revertido_id uuid references ganado.movimientos(id);
alter table ganado.movimientos add column if not exists movimiento_reversion_id uuid references ganado.movimientos(id);

create index if not exists idx_movimientos_revertido on ganado.movimientos(movimiento_revertido_id);

alter table ganado.movimientos drop constraint if exists ck_movimiento_estado;
alter table ganado.movimientos add constraint ck_movimiento_estado
    check (estado in ('PENDIENTE','CONFIRMADO','ANULADO','REVERTIDO'));

-- 2. Snapshot de ubicación por animal para poder revertir sin ambigüedad
alter table ganado.movimiento_detalles add column if not exists animal_version_esperada bigint not null default 0;
alter table ganado.movimiento_detalles add column if not exists propiedad_antes uuid references core.propiedades(id);
alter table ganado.movimiento_detalles add column if not exists potrero_antes uuid references campo.potreros(id);
alter table ganado.movimiento_detalles add column if not exists lote_antes uuid references ganado.lotes_ganaderos(id);
alter table ganado.movimiento_detalles add column if not exists propiedad_despues uuid references core.propiedades(id);
alter table ganado.movimiento_detalles add column if not exists potrero_despues uuid references campo.potreros(id);
alter table ganado.movimiento_detalles add column if not exists lote_despues uuid references ganado.lotes_ganaderos(id);
alter table ganado.movimiento_detalles add column if not exists estado_resultado varchar(20);
alter table ganado.movimiento_detalles add column if not exists mensaje_resultado varchar(500);

-- 3. Tipo de timeline para reversiones en la línea de tiempo del animal
alter table ganado.eventos_animal drop constraint if exists ck_evento_animal_tipo;
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO','ACTUALIZACION',
                    'IDENTIFICADOR','GENEALOGIA','LOTE','MOVIMIENTO','CUARENTENA',
                    'LOTE_INGRESO','LOTE_SALIDA','LOTE_CAMBIO','REVERSION'));

-- 4. Permiso para revertir movimientos confirmados
insert into seguridad.permisos (id, codigo, nombre, descripcion, modulo, activo)
values ('00000000-0000-0000-0001-000000000059','MOVIMIENTO_REVERTIR','Revertir movimientos',null,'MOVIMIENTOS',true)
on conflict (codigo) do update set nombre = excluded.nombre, descripcion = excluded.descripcion,
    modulo = excluded.modulo, activo = excluded.activo;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r cross join seguridad.permisos p
where r.codigo in ('PROPIETARIO', 'ADMINISTRADOR') and r.empresa_id is null
  and p.codigo = 'MOVIMIENTO_REVERTIR'
on conflict do nothing;

insert into seguridad.rol_permisos (rol_id, permiso_id)
select r.id, p.id from seguridad.roles r join seguridad.permisos p on p.codigo = 'MOVIMIENTO_REVERTIR'
where r.codigo in ('ENCARGADO_CAMPO', 'VETERINARIO') and r.empresa_id is null
on conflict do nothing;

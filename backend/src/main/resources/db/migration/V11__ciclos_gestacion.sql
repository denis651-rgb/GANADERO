-- No se infieren relaciones entre eventos históricos sin gestación identificada.
create table gestacion_ciclo (
 id text primary key,
 empresa_id text not null,
 animal_id text not null references animal(id),
 servicio_id text references servicio(id),
 diagnostico_id text references diagnostico_gestacion(id),
 antecedentes_desconocidos integer not null check(antecedentes_desconocidos in (0,1)),
 fecha_confirmacion text not null,
 fecha_inicio_estimada text,
 observaciones text,
 estado text not null check(estado in ('ABIERTA','FINALIZADA_PARTO','FINALIZADA_ABORTO')),
 fecha_cierre text,
 evento_id text unique,
 creado_por text not null,
 creado_en text not null,
 cerrado_por text,
 check((estado='ABIERTA' and fecha_cierre is null and evento_id is null) or
       (estado<>'ABIERTA' and fecha_cierre is not null and evento_id is not null)),
 check(fecha_cierre is null or fecha_cierre>=fecha_confirmacion),
 check(fecha_inicio_estimada is null or fecha_inicio_estimada<=fecha_confirmacion),
 check(antecedentes_desconocidos=0 or servicio_id is null)
);
create unique index gestacion_unica_abierta on gestacion_ciclo(empresa_id,animal_id) where estado='ABIERTA';
create unique index gestacion_servicio_unico on gestacion_ciclo(servicio_id) where servicio_id is not null;
create unique index gestacion_diagnostico_unico on gestacion_ciclo(diagnostico_id) where diagnostico_id is not null;
create table gestacion_diagnostico (
 diagnostico_id text primary key references diagnostico_gestacion(id),
 ciclo_id text not null references gestacion_ciclo(id)
);
alter table parto add column ciclo_gestacion_id text references gestacion_ciclo(id);
alter table aborto add column ciclo_gestacion_id text references gestacion_ciclo(id);
create unique index parto_ciclo_unico on parto(ciclo_gestacion_id) where ciclo_gestacion_id is not null;
create unique index aborto_ciclo_unico on aborto(ciclo_gestacion_id) where ciclo_gestacion_id is not null;
create trigger gestacion_cerrada_inmutable before update on gestacion_ciclo
when old.estado <> 'ABIERTA'
begin select raise(abort,'La gestación ya está finalizada.'); end;

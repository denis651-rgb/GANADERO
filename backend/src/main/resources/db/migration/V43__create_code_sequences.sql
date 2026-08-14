create table core.secuencias_codigo (
    empresa_id uuid not null references core.empresas(id),
    tipo_entidad varchar(30) not null,
    ambito_id uuid not null default '00000000-0000-0000-0000-000000000000',
    anio integer not null default 0,
    ultimo_numero bigint not null default 0,
    updated_at timestamptz not null default now(),
    primary key (empresa_id, tipo_entidad, ambito_id, anio),
    constraint ck_secuencia_codigo_tipo check (
        tipo_entidad in ('PROPIEDAD','SECTOR','POTRERO','ANIMAL','LOTE')
    ),
    constraint ck_secuencia_codigo_numero check (ultimo_numero >= 0),
    constraint ck_secuencia_codigo_anio check (anio = 0 or anio between 2000 and 9999)
);

-- Se conserva cada codigo existente. La secuencia comienza en el mayor sufijo numerico encontrado.
insert into core.secuencias_codigo (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
select empresa_id,'PROPIEDAD','00000000-0000-0000-0000-000000000000',0,
       max(case when codigo ~ '[0-9]+$' then substring(codigo from '([0-9]+)$')::bigint else 0 end)
from core.propiedades group by empresa_id
on conflict do nothing;

insert into core.secuencias_codigo (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
select empresa_id,'SECTOR',propiedad_id,0,
       max(case when codigo ~ '[0-9]+$' then substring(codigo from '([0-9]+)$')::bigint else 0 end)
from campo.sectores group by empresa_id,propiedad_id
on conflict do nothing;

insert into core.secuencias_codigo (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
select empresa_id,'POTRERO',propiedad_id,0,
       max(case when codigo ~ '[0-9]+$' then substring(codigo from '([0-9]+)$')::bigint else 0 end)
from campo.potreros group by empresa_id,propiedad_id
on conflict do nothing;

insert into core.secuencias_codigo (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
select empresa_id,'ANIMAL','00000000-0000-0000-0000-000000000000',0,
       max(case when codigo ~ '[0-9]+$' then substring(codigo from '([0-9]+)$')::bigint else 0 end)
from ganado.animales group by empresa_id
on conflict do nothing;

insert into core.secuencias_codigo (empresa_id,tipo_entidad,ambito_id,anio,ultimo_numero)
select empresa_id,'LOTE','00000000-0000-0000-0000-000000000000',extract(year from fecha_apertura)::integer,
       max(case when codigo ~ '[0-9]+$' then substring(codigo from '([0-9]+)$')::bigint else 0 end)
from ganado.lotes_ganaderos group by empresa_id,extract(year from fecha_apertura)::integer
on conflict do nothing;

-- PostgreSQL considera distintos "ANI-001" y "ani-001"; estos indices cierran esa brecha.
create unique index uq_propiedad_empresa_codigo_normalizado
    on core.propiedades (empresa_id,upper(codigo));
create unique index uq_sector_propiedad_codigo_normalizado
    on campo.sectores (propiedad_id,upper(codigo));
create unique index uq_potrero_propiedad_codigo_normalizado
    on campo.potreros (propiedad_id,upper(codigo));
create unique index uq_animal_empresa_codigo_normalizado
    on ganado.animales (empresa_id,upper(codigo));
create unique index uq_lote_empresa_codigo_normalizado
    on ganado.lotes_ganaderos (empresa_id,upper(codigo));

insert into seguridad.permisos (id,codigo,nombre,descripcion,modulo,activo)
values ('00000000-0000-0000-0001-000000000077','CODIGO_MANUAL_ASIGNAR','Asignar codigos manuales',
        'Permite reemplazar la numeracion automatica y modificar codigos existentes.','SISTEMA',true)
on conflict (codigo) do update set nombre=excluded.nombre,descripcion=excluded.descripcion,
    modulo=excluded.modulo,activo=excluded.activo;

insert into seguridad.rol_permisos (rol_id,permiso_id)
select r.id,p.id from seguridad.roles r cross join seguridad.permisos p
where r.codigo in ('PROPIETARIO','ADMINISTRADOR') and r.empresa_id is null
  and p.codigo='CODIGO_MANUAL_ASIGNAR'
on conflict do nothing;

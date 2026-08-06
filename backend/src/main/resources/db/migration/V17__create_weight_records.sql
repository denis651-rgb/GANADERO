create table produccion.pesajes (
    id uuid primary key,
    empresa_id uuid not null references core.empresas(id),
    animal_id uuid not null references ganado.animales(id),
    fecha date not null default current_date,
    peso_kg numeric(10,3) not null,
    tipo varchar(20) not null default 'RUTINA',
    condicion_corporal numeric(4,2),
    bascula varchar(120),
    responsable_id uuid,
    propiedad_id uuid references core.propiedades(id),
    potrero_id uuid references campo.potreros(id),
    lote_id uuid references ganado.lotes_ganaderos(id),
    dispositivo varchar(200),
    cliente_uuid uuid,
    idempotency_key varchar(200),
    estado varchar(20) not null default 'ACTIVO',
    motivo_anulacion varchar(1000),
    anulado_por uuid,
    fecha_anulacion timestamptz,
    observaciones varchar(1000),
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint uq_pesaje_empresa_cliente unique (empresa_id, cliente_uuid),
    constraint ck_pesaje_peso check (peso_kg > 0),
    constraint ck_pesaje_condicion check (condicion_corporal is null or condicion_corporal between 1 and 5),
    constraint ck_pesaje_tipo check (tipo in ('RUTINA','NACIMIENTO','DESTETE','ENTRADA','VENTA','PESADA_ESPECIAL')),
    constraint ck_pesaje_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_pesajes_animal_fecha on produccion.pesajes(empresa_id, animal_id, fecha desc, created_at desc);
create index idx_pesajes_empresa_fecha on produccion.pesajes(empresa_id, fecha desc);
create index idx_pesajes_lote on produccion.pesajes(empresa_id, lote_id, fecha desc);

create view produccion.v_ultimo_peso_animal as
select distinct on (empresa_id, animal_id)
       id, empresa_id, animal_id, fecha, peso_kg, tipo, condicion_corporal,
       bascula, responsable_id, propiedad_id, potrero_id, lote_id, dispositivo, observaciones, created_at
from produccion.pesajes
where estado = 'ACTIVO'
order by empresa_id, animal_id, fecha desc, created_at desc;

create view produccion.v_ganancia_diaria_animal as
select a.id as animal_id, a.empresa_id, a.codigo,
       ultimo.id as pesaje_id, ultimo.fecha as fecha_ultimo_pesaje, ultimo.peso_kg as peso_actual_kg,
       anterior.id as pesaje_anterior_id, anterior.fecha as fecha_pesaje_anterior, anterior.peso_kg as peso_anterior_kg,
       case
           when anterior.id is null then null
           when ultimo.fecha = anterior.fecha then 0
           else round((ultimo.peso_kg - anterior.peso_kg) / nullif((ultimo.fecha - anterior.fecha), 0), 3)
       end as ganancia_diaria_kg
from ganado.animales a
join produccion.v_ultimo_peso_animal ultimo on ultimo.animal_id = a.id
left join lateral (
    select p.id, p.fecha, p.peso_kg
    from produccion.pesajes p
    where p.animal_id = a.id and p.estado = 'ACTIVO'
      and (p.fecha, p.created_at) < (ultimo.fecha, ultimo.created_at)
    order by p.fecha desc, p.created_at desc
    limit 1
) anterior on true;

create view produccion.v_promedio_peso_lote as
select l.id as lote_id, l.empresa_id, l.codigo, l.nombre, l.estado,
       count(distinct u.animal_id) as animales_pesados,
       round(avg(u.peso_kg), 2) as peso_promedio_kg,
       min(u.peso_kg) as peso_minimo_kg,
       max(u.peso_kg) as peso_maximo_kg,
       min(u.fecha) as fecha_primer_pesaje,
       max(u.fecha) as fecha_ultimo_pesaje
from ganado.lotes_ganaderos l
left join ganado.animales a on a.lote_actual_id = l.id and a.empresa_id = l.empresa_id and a.estado = 'ACTIVO'
left join produccion.v_ultimo_peso_animal u on u.animal_id = a.id and u.empresa_id = l.empresa_id
group by l.id, l.empresa_id, l.codigo, l.nombre, l.estado;

create view produccion.v_animales_sin_pesaje as
select a.id, a.empresa_id, a.codigo, a.nombre, a.sexo,
       a.categoria_actual_id, a.propiedad_actual_id, a.potrero_actual_id, a.lote_actual_id,
       u.fecha as ultimo_pesaje, u.peso_kg as peso_ultimo_kg,
       (current_date - coalesce(u.fecha, a.created_at::date)) as dias_sin_pesaje
from ganado.animales a
left join produccion.v_ultimo_peso_animal u on u.animal_id = a.id
where a.estado = 'ACTIVO'
order by dias_sin_pesaje desc;

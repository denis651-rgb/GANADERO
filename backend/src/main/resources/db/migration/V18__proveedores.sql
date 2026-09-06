-- Proveedor formal: antes el dato vivía como texto libre en animal.observaciones
-- (`Proveedor: <nombre>`, ver IngresoLotePage.tsx). Ahora es una entidad propia.
create table proveedor (
    id text primary key,
    nombre text not null,
    telefono text,
    documento text,
    direccion text,
    correo text,
    observaciones text,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0
);

create index idx_proveedor_nombre on proveedor(nombre);
create index idx_proveedor_telefono on proveedor(telefono);
create unique index uq_proveedor_documento on proveedor(documento) where documento is not null and documento <> '';

-- secuencia_codigo: se recrea (SQLite no permite ALTER de CHECK) solo para sumar 'COMPRA'
-- al conjunto ya vigente desde V2 (PROPIEDAD,SECTOR,POTRERO,ANIMAL,LOTE); no se pierden contadores.
create table secuencia_codigo_nueva (
    tipo_entidad text not null,
    ambito_id text not null default '00000000-0000-0000-0000-000000000000',
    anio integer not null default 0,
    ultimo_numero integer not null default 0,
    updated_at text not null default current_timestamp,
    primary key (tipo_entidad, ambito_id, anio),
    constraint ck_secuencia_codigo_tipo check (tipo_entidad in ('PROPIEDAD','SECTOR','POTRERO','ANIMAL','LOTE','COMPRA')),
    constraint ck_secuencia_codigo_numero check (ultimo_numero >= 0)
);
insert into secuencia_codigo_nueva select * from secuencia_codigo;
drop table secuencia_codigo;
alter table secuencia_codigo_nueva rename to secuencia_codigo;

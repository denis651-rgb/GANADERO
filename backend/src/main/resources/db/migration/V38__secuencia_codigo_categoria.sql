-- secuencia_codigo: se recrea (SQLite no permite ALTER de CHECK) solo para sumar 'CATEGORIA'
-- al conjunto ya vigente (PROPIEDAD,SECTOR,POTRERO,ANIMAL,LOTE,COMPRA); no se pierden contadores.
create table secuencia_codigo_nueva (
    tipo_entidad text not null,
    ambito_id text not null default '00000000-0000-0000-0000-000000000000',
    anio integer not null default 0,
    ultimo_numero integer not null default 0,
    updated_at text not null default current_timestamp,
    primary key (tipo_entidad, ambito_id, anio),
    constraint ck_secuencia_codigo_tipo check (tipo_entidad in ('PROPIEDAD','SECTOR','POTRERO','ANIMAL','LOTE','COMPRA','CATEGORIA')),
    constraint ck_secuencia_codigo_numero check (ultimo_numero >= 0)
);
insert into secuencia_codigo_nueva select * from secuencia_codigo;
drop table secuencia_codigo;
alter table secuencia_codigo_nueva rename to secuencia_codigo;

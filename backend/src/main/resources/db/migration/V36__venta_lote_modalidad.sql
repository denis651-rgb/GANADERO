-- Venta por lote: agrupa varias filas de venta (una por animal) bajo un mismo grupo_venta_id,
-- y suma modalidad de precio (EN_PIE = precio fijo por cabeza, CARNEADO = precio por kilo
-- calculado según el peso de salida de cada animal) más el teléfono del comprador.
alter table venta add column telefono_comprador text;
alter table venta add column modalidad text not null default 'EN_PIE' check (modalidad in ('EN_PIE','CARNEADO'));
alter table venta add column precio_unitario numeric;
alter table venta add column grupo_venta_id text;

create index idx_venta_grupo on venta(grupo_venta_id);

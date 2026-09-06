-- Tolerancia (en días) entre la fecha de recepción de una compra y la fecha del pesaje de
-- compra vinculado; mismo estilo que los demás `dias_alerta_*`/`dias_*` ya existentes.
-- Va en `configuracion` (fila única de ajustes globales, ver V2__multi_propiedad.sql) y no en
-- `propiedad`, que desde V2 es el catálogo de parcelas (varias filas), no un settings row.
alter table configuracion add column dias_tolerancia_peso_compra integer not null default 3;

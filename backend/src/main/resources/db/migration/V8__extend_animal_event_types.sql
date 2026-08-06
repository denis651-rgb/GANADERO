alter table ganado.eventos_animal drop constraint ck_evento_animal_tipo;
alter table ganado.eventos_animal add constraint ck_evento_animal_tipo
    check (tipo in ('NACIMIENTO','COMPRA','INGRESO','CAMBIO_ESTADO','ACTUALIZACION'));

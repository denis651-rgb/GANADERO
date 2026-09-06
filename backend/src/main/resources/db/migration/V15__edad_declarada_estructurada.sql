-- La edad aproximada declarada (compra o estimación de campo) deja de vivir concatenada como
-- texto dentro de `observaciones` (ver EstimacionEdadAnimal anterior) y pasa a columnas propias:
-- así se puede mostrar, editar y corregir sin perder el dato original ni mezclarlo con notas libres.
alter table animal add column edad_declarada_valor integer;
alter table animal add column edad_declarada_unidad text
    check (edad_declarada_unidad is null or edad_declarada_unidad in ('DIAS','MESES','ANIOS'));
alter table animal add column fecha_referencia_edad text;
alter table animal add column fuente_edad_declarada text
    check (fuente_edad_declarada is null or fuente_edad_declarada in ('PROVEEDOR','ESTIMACION_CAMPO'));
alter table animal add column observacion_estimacion text;

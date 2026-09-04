-- No reclasificar pesos antiguos sin confirmación: no existe procedencia fiable.
alter table animal add column peso_ingreso_kg numeric check (peso_ingreso_kg is null or peso_ingreso_kg > 0);
alter table animal add column peso_ingreso_estimado integer check (peso_ingreso_estimado is null or peso_ingreso_estimado in (0,1));

-- El flujo de tratamiento libre ya guarda retiro_carne_dias/retiro_leche_dias por detalle
-- (tratamiento_detalle, desde V1) pero nunca calculaba una fecha de fin de retiro al aplicar una
-- dosis real (a diferencia del flujo de jornada, que sí lo hace en aplicacion_sanitaria). Estas
-- columnas permiten que ClinicaService.aplicar() calcule el mismo tipo de restricción desde la
-- ejecución real (sección 24), sin inventar nada para aplicaciones ya registradas antes de esta
-- migración (quedan en null: no hay forma de saber retroactivamente si debieron tener retiro).
alter table aplicacion_tratamiento add column retiro_carne_hasta text;
alter table aplicacion_tratamiento add column retiro_leche_hasta text;

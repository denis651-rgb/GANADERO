-- Para animales nacidos en la finca, el ingreso al hato ocurre al nacer.
-- created_at conserva por separado cuándo se dio de alta el registro en el sistema.
UPDATE animal
SET fecha_ingreso = fecha_nacimiento,
    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ','now'),
    version = version + 1
WHERE origen = 'NACIDO'
  AND fecha_nacimiento IS NOT NULL
  AND (fecha_ingreso IS NULL OR fecha_ingreso <> fecha_nacimiento);

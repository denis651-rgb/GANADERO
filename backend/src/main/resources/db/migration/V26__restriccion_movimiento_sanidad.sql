-- "Mover lote" necesitaba saber si un caso clínico o tratamiento restringe el traslado de un
-- animal. Antes de esta migración esa decisión se inferia solo con un switch en
-- RestriccionSanitariaTrasladoAdapter a partir de severidad/estado: una heurística fuera del
-- dominio sanitario, invisible para quien registra el caso o el tratamiento. Ahora es un campo
-- explícito que el veterinario puede fijar (o dejar en el valor por defecto calculado por
-- ClinicaService al crear el registro, que reproduce exactamente el criterio anterior).
alter table caso_clinico add column restriccion_movimiento text
    check (restriccion_movimiento is null or restriccion_movimiento in ('BLOQUEANTE','ADVERTENCIA','INFORMATIVA'));
alter table tratamiento add column restriccion_movimiento text
    check (restriccion_movimiento is null or restriccion_movimiento in ('BLOQUEANTE','ADVERTENCIA','INFORMATIVA'));

update caso_clinico set restriccion_movimiento = case severidad
    when 'CRITICA' then 'BLOQUEANTE'
    when 'GRAVE' then 'ADVERTENCIA'
    else 'INFORMATIVA'
end where restriccion_movimiento is null;

update tratamiento set restriccion_movimiento = 'ADVERTENCIA' where restriccion_movimiento is null;

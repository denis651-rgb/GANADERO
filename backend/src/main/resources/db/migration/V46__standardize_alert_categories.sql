alter table alertas.preferencias_notificacion
    add column movimientos boolean not null default true,
    add column inventario boolean not null default true,
    add column sistema boolean not null default true;

update alertas.alertas
set tipo = case tipo
    when 'PROXIMO_PARTO' then 'PARTO_PROXIMO'
    when 'DIAGNOSTICO_GESTACION_PENDIENTE' then 'DIAGNOSTICO_PENDIENTE'
    when 'VACUNACION_PROXIMA' then 'VACUNA_PROXIMA'
    when 'TRATAMIENTO_PENDIENTE' then 'TRATAMIENTO_PROXIMO'
    when 'RETIRO_SANITARIO' then case
        when metadata ->> 'tipoRetiro' = 'LECHE' then 'RETIRO_LECHE_VIGENTE'
        else 'RETIRO_CARNE_VIGENTE'
    end
    else tipo
end
where tipo in (
    'PROXIMO_PARTO',
    'DIAGNOSTICO_GESTACION_PENDIENTE',
    'VACUNACION_PROXIMA',
    'TRATAMIENTO_PENDIENTE',
    'RETIRO_SANITARIO'
);

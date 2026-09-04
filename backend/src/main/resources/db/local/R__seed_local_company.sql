insert into sector (id, codigo, nombre)
values ('30000000-0000-0000-0000-000000000001', 'SEC-LOCAL', 'Sector local')
on conflict (id) do nothing;

insert into potrero (id, sector_id, codigo, nombre, superficie_ha, tipo_pasto_id, capacidad_ua, tiene_agua, estado)
values ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
        'POT-LOCAL', 'Potrero local', 20, '10000000-0000-0000-0000-000000000001', 15, 1, 'DISPONIBLE')
on conflict (id) do nothing;

insert into animal (id, codigo, nombre, sexo, fecha_nacimiento, raza_principal_id,
    categoria_actual_id, proposito, origen, potrero_actual_id, estado, fecha_ingreso)
values ('70000000-0000-0000-0000-000000000001', 'A-LOCAL-001', 'Animal local', 'HEMBRA', '2024-01-15',
    '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000003', 'CARNE', 'NACIDO',
    '40000000-0000-0000-0000-000000000001', 'ACTIVO', current_date)
on conflict (id) do nothing;

insert into evento_animal (id, animal_id, tipo, estado_nuevo, motivo)
values ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001',
    'NACIMIENTO_REGISTRADO', 'ACTIVO', 'Registro local inicial')
on conflict (id) do nothing;

alter table ganado.identificadores_animal
    add column payload text;

alter table ganado.identificadores_animal
    add constraint ck_identificador_payload_qr check (
        (tipo = 'QR' and payload is not null)
        or (tipo <> 'QR' and payload is null)
    );

create unique index uq_identificador_qr_activo_animal
    on ganado.identificadores_animal (animal_id)
    where tipo = 'QR' and estado = 'ACTIVO';

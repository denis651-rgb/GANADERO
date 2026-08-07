alter table ganado.identificadores_animal
    add constraint ck_identificador_retiro_consistente check (
        (
            estado = 'ACTIVO'
            and fecha_retiro is null
            and motivo_retiro is null
            and retirado_por is null
        )
        or
        (
            estado = 'RETIRADO'
            and fecha_retiro is not null
            and motivo_retiro is not null
            and retirado_por is not null
        )
    );

create unique index uq_identificador_principal_activo_animal
    on ganado.identificadores_animal (animal_id)
    where principal = true and estado = 'ACTIVO';

create unique index uq_identificador_empresa_tipo_valor_lower
    on ganado.identificadores_animal (empresa_id, tipo, lower(valor));

drop index if exists ganado.idx_identificadores_animal;
create index idx_identificadores_animal
    on ganado.identificadores_animal (empresa_id, animal_id, estado);

create index idx_identificadores_busqueda
    on ganado.identificadores_animal (empresa_id, tipo, valor);

create or replace function sync.registrar_cambio()
returns trigger
language plpgsql
as
$$
declare
    registro jsonb;
    empresa_uuid uuid;
    entidad_uuid uuid;
    dispositivo_uuid uuid;
begin
    if tg_op = 'DELETE' then
        registro := to_jsonb(old);
    else
        registro := to_jsonb(new);
    end if;

    entidad_uuid :=
        nullif(registro ->> 'id', '')::uuid;

    if tg_table_schema = 'core'
       and tg_table_name = 'empresas' then

        empresa_uuid := entidad_uuid;
    else
        empresa_uuid :=
            nullif(registro ->> 'empresa_id', '')::uuid;
    end if;

    begin
        dispositivo_uuid :=
            nullif(
                current_setting(
                    'app.dispositivo_id',
                    true
                ),
                ''
            )::uuid;
    exception
        when others then
            dispositivo_uuid := null;
    end;

    insert into sync.cambios (
        empresa_id,
        tabla,
        entidad_id,
        tipo_cambio,
        datos,
        dispositivo_origen
    )
    values (
        empresa_uuid,
        tg_table_schema || '.' || tg_table_name,
        entidad_uuid,
        tg_op,
        registro,
        dispositivo_uuid
    );

    if tg_op = 'DELETE' then
        return old;
    end if;

    return new;
end;
$$;

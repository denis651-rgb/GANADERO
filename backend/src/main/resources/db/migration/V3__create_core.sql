create table core.empresas (
    id uuid primary key,
    codigo varchar(30) not null unique,
    razon_social varchar(180) not null,
    nombre_comercial varchar(180) not null,
    nit varchar(40),
    telefono varchar(40),
    email varchar(180),
    direccion varchar(300),
    zona_horaria varchar(60) not null default 'America/La_Paz',
    moneda varchar(3) not null default 'BOB',
    estado varchar(20) not null default 'ACTIVA',
    logo_path varchar(500),
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint ck_empresas_estado check (estado in ('ACTIVA', 'INACTIVA')),
    constraint ck_empresas_moneda check (char_length(moneda) = 3)
);

create table core.configuraciones_empresa (
    empresa_id uuid primary key references core.empresas(id),
    unidad_peso varchar(10) not null default 'KG',
    unidad_superficie varchar(10) not null default 'HA',
    moneda varchar(3) not null default 'BOB',
    dias_alerta_preparto integer not null default 15,
    dias_alerta_vacunacion integer not null default 7,
    dias_sin_pesaje integer not null default 30,
    permitir_stock_negativo boolean not null default false,
    requiere_aprobacion_venta boolean not null default false,
    comprimir_imagenes boolean not null default true,
    calidad_imagen integer not null default 80,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    version bigint not null default 0,
    constraint ck_config_unidad_peso check (unidad_peso in ('KG')),
    constraint ck_config_unidad_superficie check (unidad_superficie in ('HA')),
    constraint ck_config_moneda check (char_length(moneda) = 3),
    constraint ck_config_alertas check (
        dias_alerta_preparto >= 0 and dias_alerta_vacunacion >= 0 and dias_sin_pesaje >= 0
    ),
    constraint ck_config_calidad_imagen check (calidad_imagen between 1 and 100)
);


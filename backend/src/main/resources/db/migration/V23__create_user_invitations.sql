create table seguridad.invitaciones_usuario (
    id uuid primary key,
    empresa_id uuid not null
        references core.empresas(id),
    miembro_empresa_id uuid
        references seguridad.miembros_empresa(id),
    usuario_id uuid,
    email varchar(180) not null,
    estado varchar(30) not null,
    fecha_envio timestamptz,
    fecha_vencimiento timestamptz not null,
    fecha_aceptacion timestamptz,
    fecha_cancelacion timestamptz,
    intentos_envio integer not null default 0,
    ultimo_error_codigo varchar(100),
    ultimo_error_mensaje varchar(500),
    invitado_por uuid not null,
    cancelado_por uuid,
    motivo_cancelacion varchar(300),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,

    constraint ck_invitacion_usuario_estado
        check (
            estado in (
                'PENDIENTE',
                'ACEPTADA',
                'VENCIDA',
                'CANCELADA',
                'ERROR_ENVIO'
            )
        ),

    constraint ck_invitacion_usuario_fechas
        check (
            fecha_vencimiento > created_at
        )
);

create unique index uq_invitacion_usuario_activa_email
    on seguridad.invitaciones_usuario (
        empresa_id,
        lower(email)
    )
    where estado in (
        'PENDIENTE',
        'ERROR_ENVIO'
    );

create index idx_invitaciones_empresa_estado
    on seguridad.invitaciones_usuario (
        empresa_id,
        estado,
        created_at desc
    );

create index idx_invitaciones_miembro
    on seguridad.invitaciones_usuario (
        miembro_empresa_id
    );

create index idx_invitaciones_usuario
    on seguridad.invitaciones_usuario (
        usuario_id
    )
    where usuario_id is not null;

create index idx_invitaciones_vencimiento
    on seguridad.invitaciones_usuario (
        fecha_vencimiento
    )
    where estado in (
        'PENDIENTE',
        'ERROR_ENVIO'
    );

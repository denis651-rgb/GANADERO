-- Historial lógico de respaldos de la base SQLite. El archivo físico (.ganadero-backup) vive
-- fuera de la base, en la carpeta de respaldos; esta tabla registra su estado y metadatos para
-- poder listar, aplicar retención y bloquear el borrado de respaldos en uso o sin validar.
create table respaldo_registro (
    nombre_archivo text primary key,
    fecha_creacion text not null,
    tamano_bytes integer not null,
    hash_sha256 text not null,
    version_formato integer not null,
    version_aplicacion text not null,
    version_base_datos text,
    empresa_id text,
    estado text not null,
    integridad text not null default 'DESCONOCIDA',
    ultimo_error text,
    fecha_copia_externa text,
    created_by text,
    created_at text not null default current_timestamp,
    constraint ck_respaldo_estado check (estado in (
        'CREANDO','CREADO_LOCALMENTE','COPIANDO_A_CARPETA_EXTERNA','COPIADO_A_CARPETA_EXTERNA',
        'ERROR_DE_COPIA','INTEGRIDAD_INVALIDA'
    )),
    constraint ck_respaldo_integridad check (integridad in ('DESCONOCIDA','VALIDA','INVALIDA')),
    constraint ck_respaldo_tamano check (tamano_bytes > 0)
);

create index idx_respaldo_fecha on respaldo_registro(fecha_creacion desc);

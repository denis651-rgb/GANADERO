alter table core.configuraciones_empresa
    add column dias_diagnostico_post_servicio integer not null default 30,
    add column dias_gestacion_estimada integer not null default 285;

alter table core.configuraciones_empresa
    add constraint ck_config_reproduccion_alertas check (
        dias_diagnostico_post_servicio > 0
        and dias_gestacion_estimada > 0
    );

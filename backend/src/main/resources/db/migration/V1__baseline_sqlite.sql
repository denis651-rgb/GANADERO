-- Baseline SQLite para Ganadero Desktop.
-- Reemplaza las 51 migraciones historicas de PostgreSQL (multi-tenant, cloud).
-- Nota: activar `PRAGMA foreign_keys = ON;` en el datasource (no aplica dentro de una migracion).
-- Simplificaciones respecto al esquema Postgres original:
--   * Sin esquemas (SQLite no los soporta): nombres de tabla planos.
--   * Sin empresa_id / catalogo de empresas ni propiedades multi-tenant: la app es de una sola
--     finca y un solo usuario local. `propiedad` es una tabla de una sola fila (settings).
--   * uuid -> TEXT, timestamptz -> TEXT (ISO-8601), boolean -> INTEGER (0/1), jsonb -> TEXT.
--   * geography(...) de PostGIS -> TEXT (WKT); el dominio Java ya trataba estas columnas como
--     String (ubicacionWkt / geometriaWkt), no como tipos espaciales nativos.
--   * Columnas *_por / *_by / usuario_id / responsable_id / veterinario_id / tecnico_id quedan
--     como TEXT libres (sin FK) porque ya no existe una tabla de usuarios multiusuario.

-- ============================================================
-- Configuracion local (reemplaza empresas + configuraciones_empresa + propiedades)
-- ============================================================
create table propiedad (
    id text primary key,
    nombre text not null,
    descripcion text,
    departamento text,
    municipio text,
    localidad text,
    direccion_referencia text,
    superficie_ha numeric,
    ubicacion_wkt text,
    limite_geografico_wkt text,
    zona_horaria text not null default 'America/La_Paz',
    moneda text not null default 'BOB',
    unidad_peso text not null default 'KG',
    unidad_superficie text not null default 'HA',
    dias_alerta_preparto integer not null default 15,
    dias_alerta_vacunacion integer not null default 7,
    dias_sin_pesaje integer not null default 30,
    dias_alerta_destete integer not null default 7,
    dias_diagnostico_post_servicio integer not null default 30,
    dias_gestacion_estimada integer not null default 285,
    comprimir_imagenes integer not null default 1,
    calidad_imagen integer not null default 80,
    nombre_usuario text,
    pin_hash text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_propiedad_calidad_imagen check (calidad_imagen between 1 and 100)
);

-- ============================================================
-- Campo: sectores y potreros
-- ============================================================
create table tipo_pasto (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    nombre_cientifico text,
    descripcion text,
    activo integer not null default 1
);

create table sector (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    descripcion text,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0
);

create table potrero (
    id text primary key,
    sector_id text references sector(id),
    codigo text not null unique,
    nombre text not null,
    superficie_ha numeric,
    tipo_pasto_id text references tipo_pasto(id),
    capacidad_ua numeric,
    tiene_agua integer not null default 0,
    estado text not null default 'DISPONIBLE',
    geometria_wkt text,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_potrero_estado check (estado in ('DISPONIBLE','OCUPADO','DESCANSO','MANTENIMIENTO'))
);

create index idx_sectores_activo on sector(activo);
create index idx_potreros_sector on potrero(sector_id);
create index idx_potreros_estado on potrero(estado);

-- ============================================================
-- Ganado: catalogos, animales, timeline
-- ============================================================
create table raza (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    especie text not null default 'BOVINO',
    descripcion text,
    activo integer not null default 1
);

create table categoria_animal (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    sexo_aplicable text not null,
    edad_min_meses integer,
    edad_max_meses integer,
    descripcion text,
    activo integer not null default 1,
    constraint ck_categoria_sexo check (sexo_aplicable in ('MACHO','HEMBRA','AMBOS'))
);

create table animal (
    id text primary key,
    codigo text not null unique,
    nombre text,
    sexo text not null,
    fecha_nacimiento text,
    fecha_nacimiento_estimada integer not null default 0,
    raza_principal_id text not null references raza(id),
    categoria_actual_id text not null references categoria_animal(id),
    color text,
    proposito text not null,
    origen text not null,
    potrero_actual_id text not null references potrero(id),
    lote_actual_id text,
    estado text not null default 'ACTIVO',
    fecha_ingreso text not null,
    precio_adquisicion numeric,
    peso_nacimiento_kg numeric,
    condicion_corporal_actual numeric,
    foto_principal_path text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_animal_sexo check (sexo in ('MACHO','HEMBRA')),
    constraint ck_animal_proposito check (proposito in ('CARNE','LECHE','REPRODUCCION','DOBLE_PROPOSITO')),
    constraint ck_animal_origen check (origen in ('NACIDO','COMPRADO','TRANSFERIDO')),
    constraint ck_animal_estado check (estado in ('ACTIVO','VENDIDO','MUERTO','PERDIDO','TRANSFERIDO','DESCARTADO'))
);

create index idx_animales_estado on animal(estado);
create index idx_animales_ubicacion on animal(potrero_actual_id);
create index idx_animales_categoria on animal(categoria_actual_id);
create index idx_animales_lote on animal(lote_actual_id);

create table identificador_animal (
    id text primary key,
    animal_id text not null references animal(id),
    tipo text not null,
    valor text not null,
    principal integer not null default 0,
    estado text not null default 'ACTIVO',
    payload text,
    fecha_asignacion text not null default current_timestamp,
    fecha_retiro text,
    motivo_retiro text,
    asignado_por text,
    retirado_por text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint uq_identificador_tipo_valor unique (tipo, valor),
    constraint ck_identificador_tipo check (tipo in ('ARETE','QR','RFID','TATUAJE','OTRO')),
    constraint ck_identificador_estado check (estado in ('ACTIVO','RETIRADO')),
    constraint ck_identificador_payload_qr check (
        (tipo = 'QR' and payload is not null) or (tipo <> 'QR' and payload is null)
    )
);

create index idx_identificadores_animal on identificador_animal(animal_id, estado);
create unique index uq_identificador_principal_activo on identificador_animal(animal_id) where principal = 1 and estado = 'ACTIVO';
create unique index uq_identificador_qr_activo on identificador_animal(animal_id) where tipo = 'QR' and estado = 'ACTIVO';

create table parentesco (
    id text primary key,
    animal_id text not null references animal(id),
    tipo_parentesco text not null,
    animal_padre_id text references animal(id),
    nombre_externo text,
    raza_externa_id text references raza(id),
    registro_genealogico text,
    fecha_registro text not null default current_timestamp,
    registrado_por text,
    created_at text not null default current_timestamp,
    constraint uq_parentesco_animal_tipo unique (animal_id, tipo_parentesco),
    constraint ck_parentesco_tipo check (tipo_parentesco in ('MADRE','PADRE')),
    constraint ck_parentesco_origen check (
        animal_padre_id is not null or nombre_externo is not null or raza_externa_id is not null
    ),
    constraint ck_parentesco_autoreferencia check (animal_padre_id is null or animal_padre_id <> animal_id)
);

create index idx_parentescos_animal on parentesco(animal_id);
create index idx_parentescos_padre on parentesco(animal_padre_id);

create table evento_animal (
    id text primary key,
    animal_id text not null references animal(id),
    tipo text not null,
    titulo text,
    descripcion text,
    modulo_origen text,
    registro_origen text,
    dispositivo text,
    metadata text not null default '{}',
    fecha_evento text not null default current_timestamp,
    fecha_tecnica text,
    estado_anterior text,
    estado_nuevo text,
    motivo text,
    usuario_id text,
    dispositivo_id text,
    idempotency_key text,
    registrado_por text,
    created_by text,
    created_at text not null default current_timestamp,
    constraint ck_evento_animal_tipo check (tipo in (
        'ANIMAL_ACTUALIZADO','COMPRA_REGISTRADA','CUARENTENA_FINALIZADA','CUARENTENA_INICIADA',
        'ESTADO_CAMBIADO','FOTO_AGREGADA','FOTO_ELIMINADA','FOTO_PRINCIPAL_CAMBIADA',
        'GENEALOGIA_ACTUALIZADA','GENEALOGIA_REGISTRADA','IDENTIFICADOR_ACTUALIZADO',
        'IDENTIFICADOR_ASIGNADO','IDENTIFICADOR_PRINCIPAL','IDENTIFICADOR_REEMPLAZADO',
        'IDENTIFICADOR_RETIRADO','INGRESO_REGISTRADO','LOTE_ASIGNADO','LOTE_CAMBIADO',
        'LOTE_REMOVIDO','MOVIMIENTO_REGISTRADO','MOVIMIENTO_REVERTIDO','NACIMIENTO_REGISTRADO',
        'ORIGEN_SYNC','PESAJE_ANULADO','PESAJE_REGISTRADO','QR_ASIGNADO','QR_REEMPLAZADO',
        'CELO_DETECTADO','SERVICIO_REGISTRADO','DIAGNOSTICO_GESTACION_REGISTRADO',
        'GESTACION_CONFIRMADA','GESTACION_DESCARTADA','PERDIDA_GESTACION','ABORTO_REGISTRADO',
        'PARTO_REGISTRADO','CRIA_REGISTRADA','DESTETE_REGISTRADO','VACUNACION_APLICADA',
        'JORNADA_SANITARIA_CONFIRMADA','CASO_CLINICO_ABIERTO','CASO_CLINICO_CERRADO',
        'TRATAMIENTO_INICIADO','TRATAMIENTO_APLICADO','TRATAMIENTO_FINALIZADO','VENTA_REGISTRADA'
    ))
);

create unique index uq_eventos_animal_idempotencia on evento_animal(animal_id, idempotency_key) where idempotency_key is not null;
create index idx_eventos_animal_consulta on evento_animal(animal_id, fecha_evento desc);

-- ============================================================
-- Lotes y movimientos
-- ============================================================
create table lote_ganadero (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    descripcion text,
    estado text not null default 'ACTIVO',
    motivo_cierre text,
    fecha_apertura text not null default current_date,
    fecha_cierre text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_lote_estado check (estado in ('ACTIVO','CERRADO')),
    constraint ck_lote_fechas check (fecha_cierre is null or fecha_cierre >= fecha_apertura)
);

create table membresia_lote (
    id text primary key,
    lote_id text not null references lote_ganadero(id),
    animal_id text not null references animal(id),
    fecha_ingreso text not null default current_timestamp,
    fecha_salida text,
    motivo_ingreso text,
    motivo_salida text,
    observacion text,
    modo text not null default 'PARCIAL',
    ingresado_por text,
    salida_por text,
    created_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_membresia_periodo check (fecha_salida is null or fecha_salida >= fecha_ingreso),
    constraint ck_membresia_modo check (modo in ('ATOMICO','PARCIAL'))
);

create index idx_membresias_lote_activas on membresia_lote(lote_id, animal_id) where fecha_salida is null;
create unique index uq_membresia_lote_activa_animal on membresia_lote(animal_id) where fecha_salida is null;
create index idx_membresias_animal on membresia_lote(animal_id, fecha_ingreso desc);
create index idx_lotes_estado on lote_ganadero(estado);

create table movimiento (
    id text primary key,
    tipo text not null,
    estado text not null default 'PENDIENTE',
    fecha_movimiento text not null,
    motivo text,
    origen_potrero_id text references potrero(id),
    origen_lote_id text references lote_ganadero(id),
    destino_potrero_id text references potrero(id),
    destino_lote_id text references lote_ganadero(id),
    usuario_crea text,
    usuario_confirma text,
    usuario_anula text,
    usuario_revierte text,
    fecha_confirmacion text,
    fecha_anulacion text,
    fecha_reversion text,
    motivo_anulacion text,
    motivo_reversion text,
    observacion text,
    movimiento_revertido_id text references movimiento(id),
    movimiento_reversion_id text references movimiento(id),
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_movimiento_tipo check (tipo in (
        'CAMBIO_POTRERO','CAMBIO_LOTE','INGRESO_COMPRA','SALIDA_VENTA','CUARENTENA','RETORNO_CUARENTENA'
    )),
    constraint ck_movimiento_estado check (estado in ('PENDIENTE','CONFIRMADO','ANULADO','REVERTIDO'))
);

create table movimiento_detalle (
    id text primary key,
    movimiento_id text not null references movimiento(id),
    animal_id text not null references animal(id),
    estado_antes text,
    estado_despues text,
    animal_version_esperada integer not null default 0,
    potrero_antes text references potrero(id),
    lote_antes text references lote_ganadero(id),
    potrero_despues text references potrero(id),
    lote_despues text references lote_ganadero(id),
    estado_resultado text,
    mensaje_resultado text,
    constraint uq_movimiento_detalle_animal unique (movimiento_id, animal_id)
);

create index idx_movimientos_estado on movimiento(estado, created_at desc);
create index idx_movimientos_tipo on movimiento(tipo);
create index idx_movimientos_revertido on movimiento(movimiento_revertido_id);
create index idx_movimiento_detalles_animal on movimiento_detalle(animal_id);

-- ============================================================
-- Pesajes
-- ============================================================
create table pesaje (
    id text primary key,
    animal_id text not null references animal(id),
    fecha text not null default current_date,
    peso_kg numeric not null,
    tipo text not null default 'RUTINA',
    condicion_corporal numeric,
    bascula text,
    responsable_id text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    dispositivo text,
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    motivo_anulacion text,
    anulado_por text,
    fecha_anulacion text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_pesaje_peso check (peso_kg > 0),
    constraint ck_pesaje_condicion check (condicion_corporal is null or condicion_corporal between 1 and 5),
    constraint ck_pesaje_tipo check (tipo in ('RUTINA','NACIMIENTO','DESTETE','ENTRADA','VENTA','PESADA_ESPECIAL')),
    constraint ck_pesaje_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_pesajes_animal_fecha on pesaje(animal_id, fecha desc, created_at desc);
create index idx_pesajes_fecha on pesaje(fecha desc);
create index idx_pesajes_lote on pesaje(lote_id, fecha desc);
create index idx_pesajes_potrero on pesaje(potrero_id, fecha desc);
create index idx_pesajes_estado on pesaje(estado, fecha desc);

-- NOTA: las vistas Postgres v_ultimo_peso_animal / v_ganancia_diaria_animal /
-- v_promedio_peso_lote / v_animales_sin_pesaje usaban `DISTINCT ON` y `LATERAL JOIN`,
-- no soportados por SQLite. Quedan pendientes de reescribir (fase siguiente) si el
-- backend las necesita; por ahora esa logica puede resolverse en la capa de aplicacion.

-- ============================================================
-- Reproduccion
-- ============================================================
create table celo (
    id text primary key,
    animal_id text not null references animal(id),
    fecha_deteccion text not null,
    tipo_deteccion text not null,
    intensidad text,
    detectado_por text,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    anulado_at text,
    anulado_by text,
    motivo_anulacion text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_celo_tipo check (tipo_deteccion in ('VISUAL','TORO_MARCADOR','PODOMETRO','SENSOR','OTRO')),
    constraint ck_celo_intensidad check (intensidad is null or intensidad in ('BAJA','MEDIA','ALTA')),
    constraint ck_celo_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_celos_fecha on celo(fecha_deteccion desc, created_at desc);
create index idx_celos_animal_fecha on celo(animal_id, fecha_deteccion desc, created_at desc);

create table servicio (
    id text primary key,
    hembra_id text not null references animal(id),
    celo_id text references celo(id),
    fecha_servicio text not null,
    tipo_servicio text not null,
    macho_id text references animal(id),
    codigo_semen text,
    proveedor_semen text,
    tecnico_id text,
    numero_intento integer not null default 1,
    fecha_diagnostico_recomendada text not null,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'PENDIENTE_DIAGNOSTICO',
    anulado_at text,
    anulado_by text,
    motivo_anulacion text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_servicio_tipo check (tipo_servicio in ('MONTA_NATURAL','INSEMINACION_ARTIFICIAL','TRANSFERENCIA_EMBRIONARIA')),
    constraint ck_servicio_intento check (numero_intento > 0),
    constraint ck_servicio_estado check (estado in ('REGISTRADO','PENDIENTE_DIAGNOSTICO','GESTACION_CONFIRMADA','NO_PRENADA','FINALIZADO','ANULADO'))
);

create index idx_servicios_fecha on servicio(fecha_servicio desc, created_at desc);
create index idx_servicios_animal_fecha on servicio(hembra_id, fecha_servicio desc, created_at desc);
create index idx_servicios_celo on servicio(celo_id);

create table diagnostico_gestacion (
    id text primary key,
    animal_id text not null references animal(id),
    servicio_id text references servicio(id),
    fecha_diagnostico text not null,
    resultado text not null,
    metodo text,
    dias_gestacion_estimados integer,
    fecha_probable_parto text,
    veterinario_id text,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    motivo_anulacion text,
    anulado_por text,
    fecha_anulacion text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_diagnostico_resultado check (resultado in ('POSITIVO','NEGATIVO','DUDOSO','PERDIDA_GESTACION')),
    constraint ck_diagnostico_metodo check (metodo is null or metodo in ('PALPACION','ECOGRAFIA','SANGRE','OTRO')),
    constraint ck_diagnostico_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_diagnosticos_fecha on diagnostico_gestacion(fecha_diagnostico desc, created_at desc);
create index idx_diagnosticos_animal_fecha on diagnostico_gestacion(animal_id, fecha_diagnostico desc, created_at desc);
create index idx_diagnosticos_servicio on diagnostico_gestacion(servicio_id);

create table parto (
    id text primary key,
    madre_id text not null references animal(id),
    servicio_id text references servicio(id),
    diagnostico_gestacion_id text references diagnostico_gestacion(id),
    fecha_parto text not null,
    tipo_parto text not null default 'NORMAL',
    dificultad text not null default 'SIN_ASISTENCIA',
    asistido integer not null default 0,
    responsable_id text,
    resultado_madre text,
    numero_crias integer not null default 1,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    anulado_at text,
    anulado_by text,
    motivo_anulacion text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_parto_tipo check (tipo_parto in ('NORMAL','PREMATURO','DISTOCICO','CESAREA','OTRO')),
    constraint ck_parto_dificultad check (dificultad in ('SIN_ASISTENCIA','ASISTENCIA_LEVE','ASISTENCIA_MODERADA','ASISTENCIA_DIFICIL','CESAREA')),
    constraint ck_parto_crias check (numero_crias >= 1),
    constraint ck_parto_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_partos_fecha on parto(fecha_parto desc, created_at desc);
create index idx_partos_animal_fecha on parto(madre_id, fecha_parto desc, created_at desc);
create index idx_partos_servicio on parto(servicio_id);
create unique index uq_parto_gestacion_activo on parto(diagnostico_gestacion_id) where diagnostico_gestacion_id is not null and estado = 'ACTIVO';

create table cria_parto (
    id text primary key,
    parto_id text not null references parto(id),
    animal_cria_id text references animal(id),
    sexo text not null,
    peso_nacimiento_kg numeric,
    nombre text,
    estado_nacimiento text not null,
    hora_nacimiento text,
    observaciones text,
    cliente_uuid text unique,
    idempotency_key text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint uq_cria_parto_animal unique (parto_id, animal_cria_id),
    constraint ck_cria_sexo check (sexo in ('MACHO','HEMBRA')),
    constraint ck_cria_peso check (peso_nacimiento_kg is null or peso_nacimiento_kg >= 0),
    constraint ck_cria_estado check (estado_nacimiento in ('VIVO','MUERTO','NATIMUERTO'))
);

create index idx_crias_parto on cria_parto(parto_id);
create index idx_crias_animal on cria_parto(animal_cria_id);

create table aborto (
    id text primary key,
    animal_id text not null references animal(id),
    servicio_id text references servicio(id),
    gestacion_id text references diagnostico_gestacion(id),
    fecha_evento text not null,
    edad_gestacional_estimada integer,
    causa text,
    diagnostico text,
    veterinario_id text,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_aborto_estado check (estado in ('ACTIVO','ANULADO'))
);

create index idx_abortos_fecha on aborto(fecha_evento desc, created_at desc);
create index idx_abortos_animal_fecha on aborto(animal_id, fecha_evento desc, created_at desc);
create index idx_abortos_servicio on aborto(servicio_id);
create index idx_abortos_diagnostico on aborto(gestacion_id);

create table destete (
    id text primary key,
    animal_cria_id text not null references animal(id),
    madre_id text not null references animal(id),
    fecha_destete text not null,
    peso_destete_kg numeric,
    tipo_destete text not null,
    motivo text,
    responsable_id text,
    observaciones text,
    potrero_id text references potrero(id),
    lote_id text references lote_ganadero(id),
    cliente_uuid text unique,
    idempotency_key text,
    estado text not null default 'ACTIVO',
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_destete_estado check (estado in ('ACTIVO','ANULADO')),
    constraint ck_destete_tipo check (tipo_destete in ('NORMAL','PRECOZ','TEMPORAL','FORZADO','OTRO')),
    constraint ck_destete_peso check (peso_destete_kg is null or peso_destete_kg > 0)
);

create index idx_destetes_fecha on destete(fecha_destete desc, created_at desc);
create index idx_destetes_animal_fecha on destete(animal_cria_id, fecha_destete desc, created_at desc);

-- ============================================================
-- Sanidad
-- ============================================================
create table enfermedad (
    id text primary key,
    codigo text not null unique,
    nombre text not null,
    descripcion text,
    es_notificable integer not null default 0,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp
);

create table plan_sanitario (
    id text primary key,
    nombre text not null,
    descripcion text,
    fecha_inicio text not null,
    fecha_fin text,
    estado text not null default 'BORRADOR',
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_plan_estado check (estado in ('BORRADOR','ACTIVO','FINALIZADO','ANULADO')),
    constraint ck_plan_fechas check (fecha_fin is null or fecha_fin >= fecha_inicio)
);

create unique index uq_plan_activo on plan_sanitario(estado) where estado = 'ACTIVO';

create table plan_sanitario_item (
    id text primary key,
    plan_id text not null references plan_sanitario(id),
    tipo_actividad text not null,
    producto_id text,
    producto_recomendado_texto text,
    categoria_animal_id text references categoria_animal(id),
    sexo_aplicable text,
    edad_min_dias integer,
    edad_max_dias integer,
    dosis numeric,
    unidad_dosis text,
    frecuencia_dias integer,
    dias_alerta integer not null default 0,
    via_administracion text,
    obligatorio integer not null default 0,
    activo integer not null default 1,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_item_tipo check (tipo_actividad in ('VACUNACION','DESPARASITACION','VITAMINIZACION','CONTROL','PRUEBA_DIAGNOSTICA','OTRO')),
    constraint ck_item_sexo check (sexo_aplicable is null or sexo_aplicable in ('MACHO','HEMBRA')),
    constraint ck_item_dosis check (dosis is null or dosis > 0),
    constraint ck_item_frecuencia check (frecuencia_dias is null or frecuencia_dias > 0),
    constraint ck_item_alerta check (dias_alerta >= 0)
);

create index idx_plan_items on plan_sanitario_item(plan_id) where activo = 1;

create table jornada_sanitaria (
    id text primary key,
    tipo_jornada text not null,
    fecha_inicio text not null,
    fecha_fin text,
    potrero_id text references potrero(id),
    lote_ganadero_id text references lote_ganadero(id),
    responsable_id text,
    veterinario_id text,
    estado text not null default 'BORRADOR',
    observaciones text,
    operation_id text unique,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_jornada_estado check (estado in ('BORRADOR','EN_PROCESO','CONFIRMADA','ANULADA')),
    constraint ck_jornada_tipo check (tipo_jornada in ('VACUNACION','DESPARASITACION','VITAMINIZACION','CONTROL','PRUEBA_DIAGNOSTICA','OTRO')),
    constraint ck_jornada_fechas check (fecha_fin is null or fecha_fin >= fecha_inicio)
);

create table jornada_animal (
    jornada_id text not null references jornada_sanitaria(id) on delete cascade,
    animal_id text not null references animal(id),
    primary key (jornada_id, animal_id)
);

create table aplicacion_sanitaria (
    id text primary key,
    jornada_id text references jornada_sanitaria(id),
    plan_item_id text references plan_sanitario_item(id),
    animal_id text not null references animal(id),
    producto_id text,
    lote_producto_id text,
    dosis numeric,
    unidad_dosis text,
    via_administracion text,
    fecha_aplicacion text not null,
    proxima_aplicacion text,
    retiro_carne_hasta text,
    retiro_leche_hasta text,
    aplicado_por text,
    resultado text,
    observaciones text,
    idempotency_key text not null unique,
    estado text not null default 'APLICADA',
    created_at text not null default current_timestamp,
    created_by text,
    version integer not null default 0,
    constraint uq_aplicacion_jornada_animal unique (jornada_id, animal_id),
    constraint ck_aplicacion_estado check (estado in ('APLICADA','ANULADA')),
    constraint ck_aplicacion_dosis check (dosis is null or dosis > 0)
);

create index idx_aplicaciones_animal_fecha on aplicacion_sanitaria(animal_id, fecha_aplicacion desc);

create table caso_clinico (
    id text primary key,
    animal_id text not null references animal(id),
    fecha_inicio text not null,
    sintomas text not null,
    enfermedad_id text references enfermedad(id),
    diagnostico_texto text,
    severidad text not null,
    estado text not null default 'ABIERTO',
    veterinario_id text,
    fecha_cierre text,
    resultado text,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_caso_severidad check (severidad in ('LEVE','MODERADA','GRAVE','CRITICA')),
    constraint ck_caso_estado check (estado in ('ABIERTO','EN_OBSERVACION','EN_TRATAMIENTO','CERRADO','ANULADO'))
);

create index idx_casos_animal on caso_clinico(animal_id, fecha_inicio desc);

create table tratamiento (
    id text primary key,
    caso_clinico_id text references caso_clinico(id),
    animal_id text not null references animal(id),
    fecha_inicio text not null,
    fecha_fin_estimada text not null,
    fecha_fin_real text,
    diagnostico text,
    veterinario_id text,
    estado text not null default 'BORRADOR',
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_tratamiento_estado check (estado in ('BORRADOR','ACTIVO','FINALIZADO','SUSPENDIDO','ANULADO'))
);

create table tratamiento_detalle (
    id text primary key,
    tratamiento_id text not null references tratamiento(id),
    producto_id text,
    lote_producto_id text,
    dosis numeric not null,
    unidad_dosis text not null,
    frecuencia_horas integer not null,
    duracion_dias integer not null,
    via_administracion text,
    retiro_carne_dias integer not null default 0,
    retiro_leche_dias integer not null default 0,
    created_at text not null default current_timestamp,
    constraint ck_detalle_dosis check (dosis > 0),
    constraint ck_detalle_frecuencia check (frecuencia_horas > 0),
    constraint ck_detalle_duracion check (duracion_dias > 0)
);

create table aplicacion_tratamiento (
    id text primary key,
    tratamiento_detalle_id text not null references tratamiento_detalle(id),
    fecha_programada text not null,
    fecha_aplicada text,
    dosis_programada numeric not null,
    dosis_aplicada numeric,
    aplicado_por text,
    estado text not null default 'PENDIENTE',
    observaciones text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_ap_trat_estado check (estado in ('PENDIENTE','APLICADA','OMITIDA','ATRASADA','CANCELADA'))
);

create unique index uq_ap_tratamiento_programada_activa on aplicacion_tratamiento(tratamiento_detalle_id, fecha_programada) where estado <> 'CANCELADA';
create index idx_ap_trat_pendiente on aplicacion_tratamiento(fecha_programada) where estado in ('PENDIENTE','ATRASADA');

-- ============================================================
-- Alertas y recordatorios (motor local, sin push web)
-- ============================================================
create table alerta (
    id text primary key,
    animal_id text references animal(id),
    tipo text not null,
    titulo text not null,
    mensaje text not null,
    severidad text not null,
    fecha_programada text not null,
    fecha_vencimiento text,
    origen_tipo text not null,
    origen_id text,
    estado text not null default 'PROGRAMADA',
    metadata text not null default '{}',
    clave_idempotencia text not null unique,
    enviada_at text,
    atendida_at text,
    resuelta_at text,
    cancelada_at text,
    atendida_por text,
    resuelta_por text,
    motivo_cancelacion text,
    intentos_envio integer not null default 0,
    ultimo_error text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    constraint ck_alerta_estado check (estado in ('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','RESUELTA','CANCELADA','ERROR')),
    constraint ck_alerta_severidad check (severidad in ('INFO','WARNING','URGENTE','CRITICA'))
);

create unique index uq_alerta_origen_activa on alerta(tipo, origen_tipo, origen_id)
    where origen_id is not null and estado in ('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','ERROR');
create index idx_alertas_programadas on alerta(fecha_programada) where estado = 'PROGRAMADA';
create index idx_alertas_pendientes_programadas on alerta(fecha_programada) where estado in ('PROGRAMADA','PENDIENTE');
create index idx_alertas_estado on alerta(estado, fecha_programada desc);
create index idx_alertas_animal on alerta(animal_id, created_at desc);

create table recordatorio (
    id text primary key,
    creado_por text,
    titulo text not null,
    mensaje text not null,
    severidad text not null,
    animal_id text references animal(id),
    fecha_evento text not null,
    proxima_ejecucion text not null,
    cantidad_notificaciones integer not null,
    intervalo_minutos integer,
    notificaciones_generadas integer not null default 0,
    estado text not null default 'ACTIVO',
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_recordatorio_severidad check (severidad in ('INFO','WARNING','URGENTE','CRITICA')),
    constraint ck_recordatorio_estado check (estado in ('ACTIVO','PAUSADO','COMPLETADO','CANCELADO')),
    constraint ck_recordatorio_cantidad check (cantidad_notificaciones between 1 and 10),
    constraint ck_recordatorio_generadas check (notificaciones_generadas between 0 and cantidad_notificaciones),
    constraint ck_recordatorio_fechas check (proxima_ejecucion <= fecha_evento)
);

create index idx_recordatorios_pendientes on recordatorio(proxima_ejecucion) where estado = 'ACTIVO';
create index idx_recordatorios_animal on recordatorio(animal_id) where animal_id is not null;

-- ============================================================
-- Auditoria (simplificada, sin multi-tenant)
-- ============================================================
create table auditoria_registro (
    id text primary key,
    usuario_id text,
    accion text not null,
    modulo text not null,
    entidad text not null,
    entidad_id text,
    entidad_tipo text,
    correlation_id text,
    resultado text not null,
    datos text not null default '{}',
    datos_anteriores text,
    datos_nuevos text,
    dispositivo text,
    ip text,
    user_agent text,
    created_at text not null default current_timestamp
);

create index idx_auditoria_modulo_entidad on auditoria_registro(modulo, entidad, entidad_id);
create index idx_auditoria_fecha on auditoria_registro(created_at desc);

-- ============================================================
-- Codigos correlativos e idempotencia HTTP
-- ============================================================
create table secuencia_codigo (
    tipo_entidad text not null,
    ambito_id text not null default '00000000-0000-0000-0000-000000000000',
    anio integer not null default 0,
    ultimo_numero integer not null default 0,
    updated_at text not null default current_timestamp,
    primary key (tipo_entidad, ambito_id, anio),
    constraint ck_secuencia_codigo_tipo check (tipo_entidad in ('SECTOR','POTRERO','ANIMAL','LOTE')),
    constraint ck_secuencia_codigo_numero check (ultimo_numero >= 0)
);

create table idempotency_records (
    subject text not null,
    idempotency_key text not null,
    http_method text not null,
    request_path text not null,
    state text not null default 'PROCESSING',
    response_status integer,
    response_content_type text,
    response_body blob,
    payload_hash text,
    response_code text,
    correlation_id text,
    expires_at text,
    created_at text not null default current_timestamp,
    completed_at text,
    primary key (subject, idempotency_key, http_method, request_path),
    constraint ck_idempotency_state check (state in ('PROCESSING', 'COMPLETED'))
);

create index idx_idempotency_expires_at on idempotency_records(expires_at);

-- ============================================================
-- Documentos / imagenes (metadata; el archivo se guarda en disco local)
-- ============================================================
create table documento (
    id text primary key,
    entidad_tipo text not null,
    entidad_id text,
    nombre_original text,
    nombre_almacenado text not null unique,
    mime_type text,
    tamano_bytes integer,
    es_principal integer not null default 0,
    ancho_px integer,
    alto_px integer,
    created_by text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    version integer not null default 0
);

create index idx_documentos_entidad on documento(entidad_tipo, entidad_id);

create table venta (
    id text primary key,
    animal_id text not null references animal(id),
    movimiento_id text references movimiento(id),
    fecha_venta text not null,
    comprador text not null,
    precio numeric not null,
    moneda text not null default 'BOB',
    peso_venta_kg numeric,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    version integer not null default 0,
    constraint ck_venta_precio check (precio > 0)
);

create index idx_venta_animal on venta(animal_id);
create index idx_venta_fecha on venta(fecha_venta);

insert into propiedad (id, nombre) values ('00000000-0000-0000-0000-000000000001', 'Mi finca');

insert into raza (id, codigo, nombre, especie) values
 ('50000000-0000-0000-0000-000000000001','BRAHMAN','Brahman','BOVINO'),
 ('50000000-0000-0000-0000-000000000002','NELORE','Nelore','BOVINO'),
 ('50000000-0000-0000-0000-000000000003','HOLSTEIN','Holstein','BOVINO'),
 ('50000000-0000-0000-0000-000000000004','PARDO_SUIZO','Pardo Suizo','BOVINO'),
 ('50000000-0000-0000-0000-000000000005','MESTIZO','Mestizo','BOVINO');

insert into categoria_animal (id, codigo, nombre, sexo_aplicable, edad_min_meses, edad_max_meses) values
 ('60000000-0000-0000-0000-000000000001','TERNERO','Ternero','MACHO',0,12),
 ('60000000-0000-0000-0000-000000000002','TERNERA','Ternera','HEMBRA',0,12),
 ('60000000-0000-0000-0000-000000000003','VAQUILLA','Vaquilla','HEMBRA',13,35),
 ('60000000-0000-0000-0000-000000000004','NOVILLO','Novillo','MACHO',13,35),
 ('60000000-0000-0000-0000-000000000005','VACA','Vaca','HEMBRA',36,null),
 ('60000000-0000-0000-0000-000000000006','TORO','Toro','MACHO',24,null),
 ('60000000-0000-0000-0000-000000000007','BUEY','Buey','MACHO',24,null);

insert into tipo_pasto (id, codigo, nombre, nombre_cientifico, descripcion) values
 ('10000000-0000-0000-0000-000000000001','BRACHIARIA','Brachiaria','Urochloa spp.','Pasto tropical de uso ganadero.'),
 ('10000000-0000-0000-0000-000000000002','MOMBASA','Mombasa','Megathyrsus maximus','Pasto de alta produccion de biomasa.'),
 ('10000000-0000-0000-0000-000000000003','TANZANIA','Tanzania','Megathyrsus maximus','Pasto tropical para pastoreo rotacional.'),
 ('10000000-0000-0000-0000-000000000004','NATURAL','Pasto natural',null,'Pastura natural sin cultivar.');

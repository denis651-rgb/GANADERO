create table sanidad.jornadas_sanitarias (
 id uuid primary key,empresa_id uuid not null references core.empresas(id),tipo_jornada varchar(30) not null,
 fecha_inicio date not null,fecha_fin date,propiedad_id uuid not null references core.propiedades(id),potrero_id uuid references campo.potreros(id),
 lote_ganadero_id uuid references ganado.lotes_ganaderos(id),responsable_id uuid not null,veterinario_id uuid,estado varchar(20) not null default 'BORRADOR',
 observaciones text,operation_id uuid,created_at timestamptz not null default now(),created_by uuid,updated_at timestamptz not null default now(),updated_by uuid,version bigint not null default 0,
 constraint ck_jornada_estado check(estado in('BORRADOR','EN_PROCESO','CONFIRMADA','ANULADA')),
 constraint ck_jornada_tipo check(tipo_jornada in('VACUNACION','DESPARASITACION','VITAMINIZACION','CONTROL','PRUEBA_DIAGNOSTICA','OTRO')),
 constraint ck_jornada_fechas check(fecha_fin is null or fecha_fin>=fecha_inicio),unique(empresa_id,operation_id)
);
create table sanidad.jornada_animales(jornada_id uuid not null references sanidad.jornadas_sanitarias(id) on delete cascade,
 empresa_id uuid not null references core.empresas(id),animal_id uuid not null references ganado.animales(id),primary key(jornada_id,animal_id));
create table sanidad.aplicaciones_sanitarias (
 id uuid primary key,empresa_id uuid not null references core.empresas(id),jornada_id uuid references sanidad.jornadas_sanitarias(id),
 plan_item_id uuid references sanidad.plan_sanitario_items(id),animal_id uuid not null references ganado.animales(id),producto_id uuid,lote_producto_id uuid,
 dosis numeric(12,3),unidad_dosis varchar(30),via_administracion varchar(60),fecha_aplicacion date not null,proxima_aplicacion date,
 retiro_carne_hasta date,retiro_leche_hasta date,aplicado_por uuid not null,resultado varchar(60),observaciones text,
 idempotency_key varchar(200) not null,estado varchar(20) not null default 'APLICADA',created_at timestamptz not null default now(),created_by uuid,version bigint not null default 0,
 constraint ck_aplicacion_estado check(estado in('APLICADA','ANULADA')),constraint ck_aplicacion_dosis check(dosis is null or dosis>0),
 unique(empresa_id,idempotency_key),unique(jornada_id,animal_id)
);
create index idx_aplicaciones_animal_fecha on sanidad.aplicaciones_sanitarias(empresa_id,animal_id,fecha_aplicacion desc);

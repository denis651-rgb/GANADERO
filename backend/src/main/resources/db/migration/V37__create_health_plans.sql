create table sanidad.enfermedades (
 id uuid primary key, empresa_id uuid references core.empresas(id), codigo varchar(60) not null, nombre varchar(160) not null,
 descripcion text, es_notificable boolean not null default false, activo boolean not null default true,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create unique index uq_enfermedad_sistema_codigo on sanidad.enfermedades(codigo) where empresa_id is null;
create unique index uq_enfermedad_empresa_codigo on sanidad.enfermedades(empresa_id,codigo) where empresa_id is not null;

create table sanidad.planes_sanitarios (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), nombre varchar(160) not null, descripcion text,
 fecha_inicio date not null, fecha_fin date, estado varchar(20) not null default 'BORRADOR',
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(), updated_by uuid,
 version bigint not null default 0,
 constraint ck_plan_estado check(estado in ('BORRADOR','ACTIVO','FINALIZADO','ANULADO')),
 constraint ck_plan_fechas check(fecha_fin is null or fecha_fin >= fecha_inicio)
);
create unique index uq_plan_activo_empresa on sanidad.planes_sanitarios(empresa_id) where estado='ACTIVO';

create table sanidad.plan_sanitario_items (
 id uuid primary key, empresa_id uuid not null references core.empresas(id), plan_id uuid not null references sanidad.planes_sanitarios(id),
 tipo_actividad varchar(30) not null, producto_id uuid, producto_recomendado_texto varchar(300),
 categoria_animal_id uuid references ganado.categorias_animal(id), sexo_aplicable varchar(10), edad_min_dias int,
 edad_max_dias int, dosis numeric(12,3), unidad_dosis varchar(30), frecuencia_dias int, dias_alerta int not null default 0,
 via_administracion varchar(60), obligatorio boolean not null default false, activo boolean not null default true,
 created_at timestamptz not null default now(), created_by uuid, updated_at timestamptz not null default now(), updated_by uuid,
 version bigint not null default 0,
 constraint ck_item_tipo check(tipo_actividad in ('VACUNACION','DESPARASITACION','VITAMINIZACION','CONTROL','PRUEBA_DIAGNOSTICA','OTRO')),
 constraint ck_item_sexo check(sexo_aplicable is null or sexo_aplicable in ('MACHO','HEMBRA')),
 constraint ck_item_edades check((edad_min_dias is null or edad_min_dias>=0) and (edad_max_dias is null or edad_max_dias>=0) and (edad_min_dias is null or edad_max_dias is null or edad_max_dias>=edad_min_dias)),
 constraint ck_item_dosis check(dosis is null or dosis>0), constraint ck_item_frecuencia check(frecuencia_dias is null or frecuencia_dias>0),
 constraint ck_item_alerta check(dias_alerta>=0)
);
create index idx_plan_items on sanidad.plan_sanitario_items(empresa_id,plan_id) where activo;

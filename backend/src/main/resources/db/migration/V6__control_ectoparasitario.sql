-- Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.d):
-- control ectoparasitario. Puede registrarse contra UN animal o UN lote_ganadero
-- completo (nunca ambos, nunca ninguno) — de ahi el check que exige exactamente uno.
create table control_ectoparasitario (
    id text primary key,
    empresa_id text,
    animal_id text references animal(id),
    lote_ganadero_id text references lote_ganadero(id),
    tipo text not null,
    nivel_carga text not null,
    tratado integer not null default 0,
    producto text,
    principio_activo text,
    fecha text not null,
    observaciones text,
    created_at text not null default current_timestamp,
    created_by text,
    updated_at text not null default current_timestamp,
    updated_by text,
    version integer not null default 0,
    constraint ck_control_ecto_destino check (
        (animal_id is not null and lote_ganadero_id is null)
        or (animal_id is null and lote_ganadero_id is not null)
    ),
    constraint ck_control_ecto_tipo check (tipo in ('GARRAPATA','MOSCA_CUERNOS','TORSALO','PIOJOS','OTRO')),
    constraint ck_control_ecto_nivel check (nivel_carga in ('BAJO','MEDIO','ALTO'))
);

create index idx_control_ecto_animal on control_ectoparasitario(animal_id);
create index idx_control_ecto_lote on control_ectoparasitario(lote_ganadero_id);

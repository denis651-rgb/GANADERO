-- Seed manual: 20 toros Nelore de engorde en CERRO VERDE UNO / CORRAL.
-- Requiere que las migraciones (incluida V43) ya hayan sido aplicadas.
-- Es idempotente: la marca SEED:CVU-CORRAL-ENGORDE-2026 evita duplicados.

begin;

do $seed$
declare
    v_propiedad_id uuid;
    v_empresa_id uuid;
    v_propiedad_created_by uuid;
    v_potrero_id uuid;
    v_raza_id uuid;
    v_categoria_id uuid;
    v_actor_id uuid;
    v_animal_id uuid;
    v_codigo varchar(60);
    v_marca varchar(100);
    v_fecha_nacimiento date;
    v_fecha_ingreso date;
    v_numero bigint;
    v_coincidencias integer;
    v_insertados integer := 0;
    v_existentes integer := 0;
    i integer;
begin
    select count(*)
      into v_coincidencias
      from core.propiedades p
     where upper(trim(p.nombre)) = 'CERRO VERDE UNO'
       and p.activo = true;

    if v_coincidencias = 0 then
        raise exception 'No existe una propiedad activa llamada CERRO VERDE UNO';
    elsif v_coincidencias > 1 then
        raise exception 'Hay % propiedades activas llamadas CERRO VERDE UNO; use un nombre univoco antes de ejecutar el seed',
            v_coincidencias;
    end if;

    select p.id, p.empresa_id, p.created_by
      into v_propiedad_id, v_empresa_id, v_propiedad_created_by
      from core.propiedades p
     where upper(trim(p.nombre)) = 'CERRO VERDE UNO'
       and p.activo = true;

    select count(*)
      into v_coincidencias
      from campo.potreros p
     where p.propiedad_id = v_propiedad_id
       and upper(trim(p.nombre)) = 'CORRAL'
       and p.activo = true;

    if v_coincidencias = 0 then
        raise exception 'No existe un potrero activo llamado CORRAL dentro de CERRO VERDE UNO';
    elsif v_coincidencias > 1 then
        raise exception 'Hay % potreros activos llamados CORRAL dentro de CERRO VERDE UNO',
            v_coincidencias;
    end if;

    select p.id
      into v_potrero_id
      from campo.potreros p
     where p.propiedad_id = v_propiedad_id
       and upper(trim(p.nombre)) = 'CORRAL'
       and p.activo = true;

    select r.id
      into v_raza_id
      from ganado.razas r
     where upper(trim(r.codigo)) = 'NELORE'
       and r.activo = true
       and (r.empresa_id = v_empresa_id or r.empresa_id is null)
     order by case when r.empresa_id = v_empresa_id then 0 else 1 end
     limit 1;

    if v_raza_id is null then
        raise exception 'No existe una raza NELORE activa para la empresa de CERRO VERDE UNO';
    end if;

    select c.id
      into v_categoria_id
      from ganado.categorias_animal c
     where upper(trim(c.codigo)) = 'TORO'
       and c.activo = true
       and c.sexo_aplicable in ('MACHO', 'AMBOS')
       and (c.empresa_id = v_empresa_id or c.empresa_id is null)
     order by case when c.empresa_id = v_empresa_id then 0 else 1 end
     limit 1;

    if v_categoria_id is null then
        raise exception 'No existe una categoria TORO activa y aplicable a MACHO';
    end if;

    select me.usuario_id
      into v_actor_id
      from seguridad.miembros_empresa me
     where me.empresa_id = v_empresa_id
       and me.estado = 'ACTIVO'
     order by me.created_at, me.id
     limit 1;

    v_actor_id := coalesce(
        v_actor_id,
        v_propiedad_created_by,
        '00000000-0000-0000-0000-000000000001'::uuid
    );

    for i in 1..20 loop
        v_marca := 'SEED:CVU-CORRAL-ENGORDE-2026-' || lpad(i::text, 2, '0');

        if exists (
            select 1
              from ganado.animales a
             where a.empresa_id = v_empresa_id
               and position(v_marca in coalesce(a.observaciones, '')) > 0
        ) then
            v_existentes := v_existentes + 1;
            continue;
        end if;

        insert into core.secuencias_codigo (
            empresa_id, tipo_entidad, ambito_id, anio, ultimo_numero
        ) values (
            v_empresa_id, 'ANIMAL',
            '00000000-0000-0000-0000-000000000000'::uuid, 0, 1
        )
        on conflict (empresa_id, tipo_entidad, ambito_id, anio)
        do update
           set ultimo_numero = core.secuencias_codigo.ultimo_numero + 1,
               updated_at = now()
        returning ultimo_numero into v_numero;

        v_codigo := 'ANI-' || lpad(v_numero::text, 6, '0');
        v_animal_id := gen_random_uuid();
        v_fecha_nacimiento := date '2023-10-15' + (((i - 1) * 11) % 300);
        v_fecha_ingreso := date '2026-07-15' + ((i - 1) % 10);

        insert into ganado.animales (
            id, empresa_id, codigo, nombre, sexo, fecha_nacimiento,
            fecha_nacimiento_estimada, raza_principal_id, categoria_actual_id,
            color, proposito, origen, propiedad_actual_id, potrero_actual_id,
            estado, fecha_ingreso, precio_adquisicion,
            condicion_corporal_actual, observaciones,
            created_by, updated_by
        ) values (
            v_animal_id,
            v_empresa_id,
            v_codigo,
            'Engorde CVU ' || lpad(i::text, 2, '0'),
            'MACHO',
            v_fecha_nacimiento,
            true,
            v_raza_id,
            v_categoria_id,
            'Blanco ceniza',
            'CARNE',
            'COMPRADO',
            v_propiedad_id,
            v_potrero_id,
            'ACTIVO',
            v_fecha_ingreso,
            5200.00 + (i * 85.00),
            2.80 + ((i % 5) * 0.15),
            v_marca || ' | Toro Nelore comprado para prueba de engorde en corral.',
            v_actor_id,
            v_actor_id
        );

        insert into ganado.eventos_animal (
            id, empresa_id, animal_id, tipo, fecha_evento,
            estado_nuevo, motivo, registrado_por,
            titulo, descripcion, modulo_origen, registro_origen,
            metadata, created_by, fecha_tecnica, usuario_id,
            idempotency_key
        ) values (
            gen_random_uuid(),
            v_empresa_id,
            v_animal_id,
            'COMPRA_REGISTRADA',
            v_fecha_ingreso::timestamptz,
            'ACTIVO',
            'Ingreso creado por seed de toros de engorde',
            v_actor_id,
            'Compra registrada',
            'Ingreso inicial de ' || v_codigo || ' al potrero CORRAL.',
            'ANIMALES',
            v_animal_id,
            jsonb_build_object(
                'seed', 'CVU-CORRAL-ENGORDE-2026',
                'codigo', v_codigo,
                'propiedad', 'CERRO VERDE UNO',
                'potrero', 'CORRAL'
            ),
            v_actor_id,
            v_fecha_ingreso::timestamptz,
            v_actor_id,
            'SEED|CVU-CORRAL-ENGORDE-2026|' || lpad(i::text, 2, '0')
        );

        v_insertados := v_insertados + 1;
    end loop;

    raise notice 'Seed completado: % animales insertados, % ya existentes',
        v_insertados, v_existentes;
end
$seed$;

commit;

-- Resultado esperado: 20 filas en total para esta marca de seed.
select
    a.codigo,
    a.nombre,
    a.sexo,
    a.fecha_nacimiento,
    a.fecha_nacimiento_estimada,
    r.nombre as raza,
    c.nombre as categoria,
    a.proposito,
    a.origen,
    pr.nombre as propiedad,
    po.nombre as potrero,
    a.estado,
    a.fecha_ingreso
from ganado.animales a
join ganado.razas r on r.id = a.raza_principal_id
join ganado.categorias_animal c on c.id = a.categoria_actual_id
join core.propiedades pr on pr.id = a.propiedad_actual_id
join campo.potreros po on po.id = a.potrero_actual_id
where a.observaciones like 'SEED:CVU-CORRAL-ENGORDE-2026-%'
order by a.nombre;

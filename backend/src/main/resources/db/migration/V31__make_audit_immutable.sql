-- Inmutabilidad de auditoría (Etapa 7, tarea 7.4)
-- Los registros de auditoría son de solo lectura: no se pueden editar ni eliminar.
-- La limpieza por retención es una política administrativa separada: TRUNCATE/DROP
-- quedan disponibles únicamente para el administrador de la base de datos.

revoke update, delete on auditoria.registros from public;

create or replace function auditoria.fn_bloquear_modificacion()
returns trigger
language plpgsql
as $$
begin
    raise exception 'Los registros de auditoría son inmutables: no se permite % en auditoria.registros.', tg_op;
end;
$$;

drop trigger if exists trg_auditoria_no_update on auditoria.registros;
create trigger trg_auditoria_no_update
before update on auditoria.registros
for each row execute function auditoria.fn_bloquear_modificacion();

drop trigger if exists trg_auditoria_no_delete on auditoria.registros;
create trigger trg_auditoria_no_delete
before delete on auditoria.registros
for each row execute function auditoria.fn_bloquear_modificacion();

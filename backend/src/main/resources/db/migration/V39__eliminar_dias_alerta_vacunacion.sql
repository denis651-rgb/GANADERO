-- dias_alerta_vacunacion nunca llego a usarse para calcular alertas de vacunacion: esa
-- logica quedo resuelta a nivel de cada plan_sanitario_item.dias_alerta (obligatorio por
-- item desde V27), que permite un plazo de aviso distinto por vacuna/tratamiento. El campo
-- global de configuracion quedo huerfano (se guardaba y se devolvia por la API, pero ningun
-- servicio lo leia). No tiene CHECK constraint asociado, asi que se puede soltar directo.
alter table configuracion drop column dias_alerta_vacunacion;

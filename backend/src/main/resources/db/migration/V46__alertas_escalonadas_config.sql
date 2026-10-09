-- Días configurables para avisos escalonados de preparto (15d, 7d, 1d) y anticipación de vacunas (1d)
alter table configuracion add column dias_alerta_preparto_2 integer not null default 7;
alter table configuracion add column dias_alerta_preparto_3 integer not null default 1;
alter table configuracion add column dias_alerta_vacunacion integer not null default 1;

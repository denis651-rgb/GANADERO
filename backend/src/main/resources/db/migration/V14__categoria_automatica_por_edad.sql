-- La edad no permite distinguir un buey (castrado) de otro macho adulto.
-- Toro comienza al terminar el rango de novillo; Buey permanece como clasificación manual.
update categoria_animal set edad_min_meses = 36 where codigo = 'TORO';

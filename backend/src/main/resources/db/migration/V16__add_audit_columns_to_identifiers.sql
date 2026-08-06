alter table ganado.identificadores_animal
    add column if not exists created_by uuid,
    add column if not exists updated_by uuid;

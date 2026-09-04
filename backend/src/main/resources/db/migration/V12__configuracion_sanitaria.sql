CREATE TABLE configuracion_sanitaria (
    ambito TEXT PRIMARY KEY,
    edad_min_macho_meses INTEGER CHECK (edad_min_macho_meses BETWEEN 1 AND 120),
    edad_min_hembra_meses INTEGER CHECK (edad_min_hembra_meses BETWEEN 1 AND 120),
    version INTEGER NOT NULL DEFAULT 0,
    actualizado_por TEXT,
    actualizado_en TEXT
);

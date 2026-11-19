-- La búsqueda de nombres, categorías e implementos usa comparación sin tildes.
-- Se agrega como migración independiente para no modificar migraciones ya aplicadas.
CREATE EXTENSION IF NOT EXISTS unaccent;

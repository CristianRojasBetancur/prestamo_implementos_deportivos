-- Ejecutar con la misma base y dataset de la prueba de carga:
-- psql "$DB_URL" -v ON_ERROR_STOP=1 -f performance/explain.sql

EXPLAIN (ANALYZE, BUFFERS)
SELECT i.nombre,
       c.id,
       c.nombre,
       COUNT(*) FILTER (WHERE i.estado = 'DISPONIBLE') AS unidades_disponibles,
       COUNT(*) AS unidades_totales
  FROM implemento_deportivo i
  JOIN categoria c ON c.id = i.categoria_id
 WHERE c.activa = true
   AND i.estado <> 'DADO_DE_BAJA'
   AND NOT EXISTS (
       SELECT 1 FROM incidencia inc
        WHERE inc.implemento_id = i.id AND inc.estado = 'REGISTRADA'
   )
 GROUP BY i.nombre, c.id, c.nombre
HAVING COUNT(*) FILTER (WHERE i.estado = 'DISPONIBLE') > 0
 ORDER BY i.nombre, c.nombre, c.id
 LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id, p.codigo, p.estado, p.fecha_prestamo
  FROM prestamo p
 ORDER BY p.fecha_prestamo DESC, p.id ASC
 LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT d.id, d.codigo, d.fecha_devolucion, d.alcance
  FROM devolucion d
 ORDER BY d.fecha_devolucion DESC, d.id ASC
 LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT i.id, i.codigo, i.tipo, i.estado, i.fecha_registro
  FROM incidencia i
 ORDER BY i.fecha_registro DESC, i.id ASC
 LIMIT 20;

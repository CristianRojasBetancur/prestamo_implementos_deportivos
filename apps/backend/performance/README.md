# Evidencia de rendimiento del backend

## Configuración

La medición usa PostgreSQL 17, Java 21, Spring Boot 4.1.2, el perfil `local`,
un pool Hikari máximo de 20 conexiones y 100 usuarios virtuales durante un
minuto. El criterio principal es p95 menor o igual a 1.500 ms por etiqueta de
endpoint y menos de 1 % de respuestas fallidas.

El dataset debe contener, como mínimo, 20 categorías activas, 500 unidades,
5.000 préstamos, 5.000 devoluciones, 2.000 incidencias y usuarios suficientes
para cubrir los roles `ADMIN`, `PRESTADOR` y `USER`. Los tokens de las rutas
protegidas se pasan por variables de entorno; no se almacenan credenciales ni
tokens en el repositorio.

## Ejecución

Con la API levantada y tokens temporales de `ADMIN` y `PRESTADOR` obtenidos por
login:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e ADMIN_TOKEN="$ADMIN_TOKEN" \
  -e PRESTADOR_TOKEN="$PRESTADOR_TOKEN" \
  -e DURATION=1m \
  performance/critical-api.js
```

La consulta pública de disponibilidad también puede ejecutarse de forma aislada:

```bash
k6 run -e BASE_URL=http://localhost:8080 performance/availability.js
```

Los planes de PostgreSQL se revisan con:

```bash
psql "$DB_URL" -v ON_ERROR_STOP=1 -f performance/explain.sql
```

Los resultados se deben registrar en la especificación SDD del issue junto con
la versión del dataset, configuración de máquina, tasa de errores y cualquier
desviación del umbral.

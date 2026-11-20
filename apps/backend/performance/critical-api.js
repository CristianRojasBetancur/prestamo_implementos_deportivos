import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const adminToken = __ENV.ADMIN_TOKEN || '';
const prestadorToken = __ENV.PRESTADOR_TOKEN || '';

export const options = {
  scenarios: {
    rutas_criticas: {
      executor: 'constant-vus',
      vus: 100,
      duration: __ENV.DURATION || '1m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'http_req_duration{endpoint:availability}': ['p(95)<1500'],
    'http_req_duration{endpoint:inventory}': ['p(95)<1500'],
    'http_req_duration{endpoint:loans}': ['p(95)<1500'],
    'http_req_duration{endpoint:returns}': ['p(95)<1500'],
    'http_req_duration{endpoint:incidents}': ['p(95)<1500'],
    http_req_failed: ['rate<0.01'],
  },
};

const auth = (token) => token ? { headers: { Authorization: `Bearer ${token}` } } : {};

export default function () {
  const rutas = [
    { endpoint: 'availability', url: `${baseUrl}/api/v1/availability?pagina=0&tamano=20`, auth: false },
    { endpoint: 'inventory', url: `${baseUrl}/api/v1/inventory/units?pagina=0&tamano=20`, token: adminToken },
    { endpoint: 'loans', url: `${baseUrl}/api/v1/prestamos?pagina=0&tamano=20`, token: prestadorToken },
    { endpoint: 'returns', url: `${baseUrl}/api/v1/devoluciones?pagina=0&tamano=20`, token: prestadorToken },
    { endpoint: 'incidents', url: `${baseUrl}/api/v1/incidencias?pagina=0&tamano=20`, token: prestadorToken },
  ];
  const ruta = rutas[__ITER % rutas.length];
  const respuesta = http.get(ruta.url, {
    ...auth(ruta.token),
    tags: { endpoint: ruta.endpoint },
  });
  check(respuesta, {
    [`${ruta.endpoint} responde con éxito`]: (r) => r.status >= 200 && r.status < 300,
  });
}

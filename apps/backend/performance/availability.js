import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    consulta_disponibilidad: {
      executor: 'constant-vus',
      vus: 100,
      duration: '1m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const respuesta = http.get(
    `${baseUrl}/api/v1/availability?pagina=0&tamano=20`,
    { tags: { endpoint: 'availability' } },
  );

  check(respuesta, {
    'availability responde 200': (r) => r.status === 200,
    'availability entrega una página': (r) => {
      try {
        const body = r.json();
        return Array.isArray(body.contenido)
          && Number.isInteger(body.totalElementos)
          && Number.isInteger(body.totalPaginas);
      } catch (_) {
        return false;
      }
    },
  });
}

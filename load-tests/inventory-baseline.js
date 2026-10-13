import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    inventory_baseline: {
      executor: 'constant-vus',
      vus: 10,
      duration: '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const page = Math.floor(Math.random() * 20);
  const res = http.get(`http://localhost:8081/api/inventory?warehouseCode=WH-1&page=${page}&size=50`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has content': (r) => r.body && r.body.length > 0,
  });

  sleep(0.1);
}

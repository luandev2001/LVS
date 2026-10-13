import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const MODE = __ENV.MODE || 'ASYNC'; // ASYNC | SEQUENTIAL
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || '30s';
const PAGES = Number(__ENV.PAGES || 4);
const SIZE = Number(__ENV.SIZE || 50);

export const options = {
  scenarios: {
    inventory_async: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const path = MODE === 'SEQUENTIAL'
    ? '/api/inventory/async/sequential'
    : '/api/inventory/async';
  const res = http.get(`${BASE}${path}?warehouseCode=WH-1&pages=${PAGES}&size=${SIZE}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'has content': (r) => r.status !== 200 || JSON.parse(r.body).content !== undefined,
  });
  sleep(0.1);
}

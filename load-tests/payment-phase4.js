import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    payments: { executor: 'constant-vus', vus: 5, duration: '30s' },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';
const MODES = ['SUCCESS', 'FAILURE', 'TIMEOUT', 'DELAYED_SUCCESS', 'DUPLICATE_SUCCESS', 'INVALID_SIGNATURE'];

export default function () {
  const mode = MODES[Math.floor(Math.random() * MODES.length)];
  const key = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const url = `${BASE_URL}/api/payments?orderReference=ORDER-${key}&amount=100.00&currency=VND&mode=${mode}`;
  const res = http.post(url, null, { headers: { 'Idempotency-Key': key } });
  check(res, { 'created': r => r.status === 201 });
  sleep(1);
}

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const VUS = Number(__ENV.VUS || 100);
const DURATION = __ENV.DURATION || '30s';
const PAGES = Number(__ENV.PAGES || 8);
const SIZE = Number(__ENV.SIZE || 50);

export const options = {
  scenarios: {
    overload: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
};

export default function () {
  const res = http.get(`${BASE}/api/inventory/async?warehouseCode=WH-1&pages=${PAGES}&size=${SIZE}`);
  check(res, {
    'accepted or explicitly backpressured': (r) => r.status === 200 || r.status === 429,
  });
  sleep(0.05);
}

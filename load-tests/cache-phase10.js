import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const WAREHOUSE = __ENV.WAREHOUSE || 'WH-1';
const PAGE = __ENV.PAGE || '0';
const SIZE = __ENV.SIZE || '50';
const PROTECTION = __ENV.PROTECTION || 'LOCKED';
const SCENARIO = __ENV.SCENARIO || 'warm';

export const options = {
  scenarios: {
    cache: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  if (SCENARIO === 'stampede' && __VU === 1 && __ITER === 0) {
    http.post(`${BASE_URL}/api/inventory/cache/invalidate?warehouseCode=${WAREHOUSE}`);
    sleep(0.2);
  }

  const url = `${BASE_URL}/api/inventory/cached?warehouseCode=${WAREHOUSE}&page=${PAGE}&size=${SIZE}&protection=${PROTECTION}`;
  const response = http.get(url);
  check(response, { 'cache endpoint is 200': r => r.status === 200 });
  sleep(0.1);
}

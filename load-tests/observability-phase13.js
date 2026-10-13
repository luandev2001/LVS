import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const WAREHOUSE = __ENV.WAREHOUSE || 'WH-1';
export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '60s',
  thresholds: { http_req_failed: ['rate<0.05'] },
};
export default function () {
  const path = (__ITER % 3 === 0) ? '/api/inventory?warehouseCode=' + WAREHOUSE + '&page=0&size=50'
    : '/api/inventory/cached?warehouseCode=' + WAREHOUSE + '&page=0&size=50';
  const res = http.get(BASE + path, { tags: { workload: 'observability-lab' } });
  check(res, { 'status is 200': r => r.status === 200 });
  sleep(0.1);
}

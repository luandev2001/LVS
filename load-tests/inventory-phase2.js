import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    baseline: { executor: 'constant-vus', vus: 200, duration: '60s' },
  },
};

export default function () {
  const page = Math.floor(Math.random() * 100);
  const base = __ENV.BASE_URL || 'http://localhost:8081';
  const res = http.get(`${base}/api/inventory/cached?warehouseCode=WH-1&page=${page}&size=50`);
  check(res, { 'optimized list is 200': (r) => r.status === 200 });
}

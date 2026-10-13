import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 10, duration: '60s' };

export default function () {
  const base = __ENV.BASE_URL || 'http://localhost:8081';
  const res = http.get(`${base}/api/inventory/keyset?warehouseCode=WH-1&size=50`);
  check(res, { 'keyset list is 200': (r) => r.status === 200 });
}

import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 20, duration: '30s' };

export default function () {
  const strategy = __ENV.STRATEGY || 'none';
  const inventoryId = Number(__ENV.INVENTORY_ID || 1);
  const base = __ENV.BASE_URL || 'http://localhost:8081';
  const res = http.post(`${base}/api/inventory/${inventoryId}/reserve/${strategy}?amount=1`);
  check(res, { 'reservation response is expected': (r) => [204, 409, 500].includes(r.status) });
}

import http from 'k6/http';
import { check, sleep } from 'k6';
const targets = [
  ['inventory', __ENV.INVENTORY_URL || 'http://localhost:8081/actuator/health'],
  ['payment', __ENV.PAYMENT_URL || 'http://localhost:8084/actuator/health'],
  ['accounting', __ENV.ACCOUNTING_URL || 'http://localhost:8085/actuator/health'],
];
export const options = { vus: 1, iterations: 1, thresholds: { http_req_failed: ['rate==0'] } };
export default function () {
  for (const [name, url] of targets) {
    const r = http.get(url, { tags: { operation: `health_${name}` } });
    check(r, { [`${name} actuator healthy`]: x => x.status === 200 });
  }
  sleep(0.1);
}

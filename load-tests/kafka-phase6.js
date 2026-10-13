import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 5, duration: '30s' };
const base = __ENV.BASE_URL || 'http://localhost:8084';

export default function () {
  const key = `k6-${__VU}-${__ITER}`;
  const create = http.post(`${base}/api/payments?orderReference=${key}&amount=100.00&currency=VND&mode=SUCCESS`, null,
    { headers: { 'Idempotency-Key': key } });
  check(create, { 'payment created': r => r.status === 201 });
  if (create.status === 201) {
    const id = JSON.parse(create.body).id;
    const duplicate = http.post(`${base}/api/payments/${id}/events/duplicate`);
    check(duplicate, { 'duplicate event accepted': r => r.status === 202 });
  }
  sleep(1);
}

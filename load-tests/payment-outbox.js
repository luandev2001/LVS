import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';

export default function () {
  const idempotencyKey = `outbox-${__VU}-${__ITER}-${Date.now()}`;
  const url = `${BASE_URL}/api/payments?orderReference=OUTBOX-${idempotencyKey}&amount=100&currency=VND&mode=SUCCESS`;
  const res = http.post(url, null, {
    headers: { 'Idempotency-Key': idempotencyKey },
  });

  check(res, {
    'payment created': (r) => r.status === 201,
  });

  sleep(0.05);
}

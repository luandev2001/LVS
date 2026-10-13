import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '30s',
};

const base = __ENV.BASE_URL || 'http://localhost:8084';
const key = __ENV.IDEMPOTENCY_KEY || 'phase5-shared-key';

export default function () {
  const res = http.post(
    `${base}/api/payments?orderReference=ORDER-IDEMP-1&amount=100.00&currency=VND&mode=SUCCESS`,
    null,
    { headers: { 'Idempotency-Key': key } }
  );
  check(res, { 'created or replayed': r => r.status === 201 || r.status === 200 });
}

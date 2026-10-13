import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: Number(__ENV.VUS || 10), duration: __ENV.DURATION || '15s' };
const base = __ENV.BASE_URL || 'http://localhost:8084';
const paymentId = __ENV.PAYMENT_ID;
const gatewayReference = __ENV.GATEWAY_REFERENCE;
const signature = __ENV.SIGNATURE;

export default function () {
  if (!paymentId || !gatewayReference || !signature) return;
  const res = http.post(`${base}/api/payments/${paymentId}/webhook?gatewayReference=${gatewayReference}&amount=100.00&currency=VND&success=true`, null,
    { headers: { 'X-Payment-Signature': signature } });
  check(res, { 'webhook handled': r => r.status === 200 });
}

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomUUID } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = { vus: 5, duration: '30s' };
const paymentBase = __ENV.PAYMENT_URL || 'http://localhost:8084';
const accountingBase = __ENV.ACCOUNTING_URL || 'http://localhost:8085';

export default function () {
  const key = randomUUID();
  const payment = http.post(`${paymentBase}/api/payments?orderReference=OBS-${key}&amount=100.00&currency=USD&mode=SUCCESS`, null, {
    headers: { 'Idempotency-Key': key },
  });
  check(payment, { 'payment created': (r) => r.status === 201 });
  sleep(0.5);
  const journal = http.get(`${accountingBase}/api/accounting/journal-entries?limit=20`);
  check(journal, { 'accounting reachable': (r) => r.status === 200 });
}

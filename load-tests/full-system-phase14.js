import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const INVENTORY = __ENV.INVENTORY_URL || 'http://localhost:8081';
const PAYMENT = __ENV.PAYMENT_URL || 'http://localhost:8084';
const ACCOUNTING = __ENV.ACCOUNTING_URL || 'http://localhost:8085';
const MODE = (__ENV.MODE || 'MIXED').toUpperCase();
const WAREHOUSE = __ENV.WAREHOUSE || 'WH-1';
const failures = new Counter('lab_check_failures');
const paymentDuration = new Trend('payment_create_duration', true);

export const options = {
  scenarios: {
    full_system: {
      executor: 'ramping-vus',
      startVUs: Number(__ENV.START_VUS || 1),
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.TARGET_VUS || 20) },
        { duration: __ENV.STEADY || '2m', target: Number(__ENV.TARGET_VUS || 20) },
        { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2500'],
    lab_check_failures: ['count==0'],
  },
};

function get(url, name) {
  const r = http.get(url, { tags: { operation: name, mode: MODE } });
  const ok = check(r, { [`${name}: status 2xx`]: x => x.status >= 200 && x.status < 300 });
  if (!ok) failures.add(1, { operation: name, status: String(r.status) });
  return r;
}

function inventoryRead() {
  const useCache = __ITER % 2 === 0;
  const endpoint = useCache ? '/api/inventory/cached' : '/api/inventory/optimized';
  get(`${INVENTORY}${endpoint}?warehouseCode=${WAREHOUSE}&page=0&size=50`, useCache ? 'inventory_cached' : 'inventory_optimized');
}

function paymentCreate() {
  const key = `phase14-${__VU}-${__ITER}-${Date.now()}`;
  const url = `${PAYMENT}/api/payments?orderReference=${encodeURIComponent(key)}&amount=100&currency=VND&mode=SUCCESS`;
  const response = http.post(url, null, { headers: { 'Idempotency-Key': key }, tags: { operation: 'payment_create', mode: MODE } });
  paymentDuration.add(response.timings.duration);
  const ok = check(response, { 'payment create accepted': r => r.status >= 200 && r.status < 300 });
  if (!ok) failures.add(1, { operation: 'payment_create', status: String(response.status) });
}

export default function () {
  if (MODE === 'INVENTORY') inventoryRead();
  else if (MODE === 'PAYMENT') paymentCreate();
  else if (MODE === 'ACCOUNTING') get(`${ACCOUNTING}/api/accounting/journal-entries`, 'accounting_journal_entries');
  else {
    const choice = __ITER % 10;
    if (choice < 7) inventoryRead();
    else if (choice < 9) paymentCreate();
    else get(`${ACCOUNTING}/api/accounting/journal-entries`, 'accounting_journal_entries');
  }
  sleep(Number(__ENV.THINK_TIME_SECONDS || 0.1));
}

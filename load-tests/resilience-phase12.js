import http from 'k6/http';
import { check, sleep } from 'k6';

const base = __ENV.BASE_URL || 'http://localhost:8081';
export const options = {
  scenarios: {
    resilience: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 30),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {}, // establish baseline before setting SLOs
};
export default function () {
  const fail = __ENV.FAIL || 'false';
  const delayMs = __ENV.DELAY_MS || '100';
  const retries = __ENV.RETRIES || '0';
  const res = http.get(`${base}/api/resilience-lab/dependency?fail=${fail}&delayMs=${delayMs}&retries=${retries}`);
  check(res, { 'response is handled': r => [200, 429, 502, 503].includes(r.status) });
  sleep(0.05);
}

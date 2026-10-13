import http from 'k6/http'; import { check, sleep } from 'k6';
export const options={vus:5,duration:'30s'};
export default function(){const key=`saga-${__VU}-${__ITER}-${Date.now()}`;const r=http.post(`http://localhost:8084/api/payments?orderReference=SAGA-${key}&amount=100&currency=USD&mode=SUCCESS&inventoryId=1&quantity=1&sagaFailureMode=NONE`,null,{headers:{'Idempotency-Key':key}});check(r,{'payment accepted':x=>x.status===201});sleep(1);}

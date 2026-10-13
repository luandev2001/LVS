# PAY-003 — Callback Validation

## Security experiment

For a webhook to be accepted, the service requires a valid HMAC signature and the gateway reference belonging to the current payment attempt.

## Negative cases

1. invalid signature
2. wrong payment ID
3. gateway reference from an older attempt
4. callback for a refunded payment
5. callback for an invalid state

## Expected behavior

Invalid signature/reference must not mutate payment state.

## Results

[TO BE MEASURED]

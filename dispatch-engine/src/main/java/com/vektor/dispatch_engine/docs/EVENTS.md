# Vektor Event Contracts

## Inbound: `delivery-updates`

Telemetry from driver devices. JSON, UTF-8. Key = `driverId` (string) —
required for per-driver ordering; unkeyed messages break ordering guarantees.

### Schema

| Field        | Type          | Required | Notes                                              |
|--------------|---------------|----------|----------------------------------------------------|
| `eventId`    | UUID string   | yes      | Producer-generated. THE idempotency key.           |
| `driverId`   | string (2–64) | yes      | Also used as the Kafka message key.                |
| `status`     | enum          | yes      | `EN_ROUTE` \| `DELIVERED` \| `FAILED` \| `CANCELLED` |
| `lat`        | number        | no       | −90..90                                            |
| `lng`        | number        | no       | −180..180                                          |
| `distanceKm` | number ≥ 0    | yes*     | Distance for this leg; feeds per-km payout rate.   |
| `occurredAt` | ISO-8601 UTC  | yes      | Client-side event time. Settlement keys on this.   |

Unknown fields are ignored (forward-compatible: producers may add fields
without a consumer redeploy).

### Example

```json
{
  "eventId": "47755c67-c91c-4ad2-a539-f02460c798aa",
  "driverId": "R-101",
  "status": "DELIVERED",
  "lat": 6.8118, "lng": 79.8659,
  "distanceKm": 2.35,
  "occurredAt": "2026-07-12T01:13:23.383583+00:00"
}
```

### Delivery semantics (what producers must know)

- **At-least-once + idempotent write = effectively-once.** Retries with the
  same `eventId` are safe and encouraged; duplicates are detected by a
  database unique constraint and skipped. Never reuse an `eventId` for a
  different event.
- **Malformed messages** (invalid JSON, unknown `status`, type mismatches)
  are retried 3× then quarantined byte-for-byte on `delivery-updates-dlt`,
  same partition as the source. They are NOT silently dropped — inspect the
  DLT for your producer's rejects.
- Only `DELIVERED` events are payable. Other statuses are recorded but do
  not affect settlement.

## Outbound: `payout-completed` (planned)

Emitted when a payout transitions to PAID. Schema TBD in the
payout-completed milestone; consumers should expect
`{driverId, totalAmount, bankReferenceId, paidAt}`.
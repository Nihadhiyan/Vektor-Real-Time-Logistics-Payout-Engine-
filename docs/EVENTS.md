# Vektor Event Contracts

## Inbound Topic: `delivery-updates`

Telemetry from driver devices or logistics edge servers. Formatted as UTF-8 JSON.
* **Message Key**: `driverId` (`string`) — Required to guarantee strict per-driver ordering across Kafka partitions. Unkeyed messages break ordering guarantees.

### Schema

| Field | Type | Required | Notes |
| :--- | :--- | :--- | :--- |
| `eventId` | UUID string | yes | Producer-generated. **THE idempotency key** (`uq_delivery_events_event_id`). |
| `driverId` | string (2–64) | yes | Also used as the Kafka message key for partition routing. |
| `status` | enum | yes | `EN_ROUTE` \| `DELIVERED` \| `FAILED` \| `CANCELLED` |
| `lat` | number | no | −90..90 |
| `lng` | number | no | −180..180 |
| `distanceKm` | number ≥ 0 | yes* | Distance for this leg; feeds per-km payout rate card calculation. |
| `occurredAt` | ISO-8601 UTC | yes | Client-side event timestamp. Payout settlement cutoff keys on this field. |

Unknown fields are ignored (forward-compatible: producers may add fields without requiring a consumer redeploy).

### Example Payload

```json
{
  "eventId": "47755c67-c91c-4ad2-a539-f02460c798aa",
  "driverId": "R-101",
  "status": "DELIVERED",
  "lat": 6.8118,
  "lng": 79.8659,
  "distanceKm": 2.35,
  "occurredAt": "2026-07-12T01:13:23.383583+00:00"
}
```

### Delivery Semantics & Producer Guidelines

- **At-Least-Once + Idempotent Write = Effectively-Once**: Retries with the exact same `eventId` are safe and encouraged. Duplicates are detected by PostgreSQL unique constraints (`uq_delivery_events_event_id`) and silently skipped (`DataIntegrityViolationException`). Never reuse an `eventId` for a different event.
- **Malformed Messages (Poison Pills)**: Invalid JSON, unknown `status` enums, or type mismatches are retried 3× and then quarantined byte-for-byte on `delivery-updates-dlt` (`ErrorHandlingDeserializer` with `ByteArraySerializer`) on the same partition as the source topic. They are NOT silently dropped — inspect the DLT for producer errors.
- Only `DELIVERED` status events are eligible for financial payout calculations. Other statuses (`EN_ROUTE`, `CANCELLED`) are recorded in telemetry but skipped during rate card settlement.

---

## Outbound Topic: `payout-results`

Emitted by `OutboxRecordProcessor` whenever an outbox transfer attempt transitions to either `PAID` or `FAILED` via downstream banking rails (`BankGatewayService`).

* **Message Key**: `driverId` (`string`) — Ensures settlement events for the same driver arrive in sequential order.

### Schema (`PayoutCompletedEventPayload`)

| Field | Type | Required | Notes |
| :--- | :--- | :--- | :--- |
| `outboxId` | UUID string | yes | The unique ID of the staged outbox record (`payout_outbox`). |
| `driverId` | string | yes | Driver receiving the settlement funds. |
| `amount` | number (decimal) | yes | Total payout amount calculated by Spring Batch. |
| `bankReference` | string \| null | no | Bank reference ID returned upon successful transfer (`BANK-REF-XXXX`). Null if failed. |
| `status` | enum | yes | `PAID` \| `FAILED` |
| `processedAt` | ISO-8601 UTC | yes | Timestamp when the transfer was finalized by the outbox processor. |

### Example Payload

```json
{
  "outboxId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
  "driverId": "R-101",
  "amount": 142.50,
  "bankReference": "BANK-REF-F81D4FAE",
  "status": "PAID",
  "processedAt": "2026-07-12T10:01:15.123Z"
}
```

### Consumer Guidelines

- Downstream notification services, accounting general ledgers, or driver push-notification microservices should subscribe to `payout-results` using consumer groups.
- If `status == "FAILED"`, the record may either be scheduled for manual admin review (`payout_admin` role via REST) or automatically retried by the outbox sweeper if the failure was due to temporary downstream circuit breaker openings (`FAILED_SYSTEM_DOWN`).

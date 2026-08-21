# ADR-0017: V2 Web recording uses protected direct upload

Status: Accepted

## Context

The older API shell described a JSON reservation followed by a presigned upload,
while the accepted V2 API design requires `POST /api/v1/audio/uploads` to return a
validated `READY` user audio asset. V2-T13 needs a complete Web Push-to-Talk flow,
but there is not yet a production object-storage signing service or upload-completion
worker in the modular monolith.

## Decision

V2 Web recordings use authenticated `multipart/form-data` direct upload at
`POST /api/v1/audio/uploads`. The API enforces owner, MIME type, size, duration,
server-computed SHA-256 and idempotency before returning a `READY` asset. The
object is written through `PrivateAudioObjectStorage`; no public or permanent URL
is returned, and the database stores metadata rather than audio bytes.

The initial adapter writes into a configurable private storage root. Production
must mount that root as durable private object storage. A future S3-compatible
adapter can replace it without changing the use case, domain model or HTTP
contract. Presigned multipart upload remains an optional scale-out optimization,
not a second contract for V2 clients.

## Consequences

- The first request body passes through the application server and is therefore
  limited to 50 MB / 10 minutes at both HTTP and business-validation layers.
- Object writes happen before metadata insertion; database failure triggers a
  compensating delete, and concurrent idempotent insertion returns the winner.
- `PROCESS_ONLY` recordings have a durable 24-hour upper-bound deadline and are
  deleted immediately after successful transcription when possible; a periodic
  retention sweep retries failed deletes and enforces all configured deadlines.
- The storage root must never be served as static content or exposed by a URL.

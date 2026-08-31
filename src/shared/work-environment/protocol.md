# Minis Desktop Companion Protocol

Mobile clients discover and control a desktop companion so the agent can
run shell work on a real computer instead of the on-device Linux sandbox.

## Discovery

Bonjour / mDNS service type:

```
_minis-desktop._tcp
```

TXT records:

| key | meaning |
|---|---|
| `id` | stable device id (uuid) |
| `name` | hostname |
| `os` | `mac` / `linux` / `windows` |
| `ver` | companion version |
| `pair` | `1` if this companion already has at least one paired token |

Default port: `18765`.

## HTTP

All JSON. CORS is open for local previews.

### `GET /v1/hello`

```json
{ "id": "...", "name": "office.local", "os": "mac", "version": "1.0", "allowDiscover": true }
```

### `POST /v1/pair`

```json
{ "clientId": "ios-...", "clientName": "iPhone" }
```

Response:

```json
{ "token": "…", "deviceId": "…", "name": "office.local", "os": "mac" }
```

Rejected with `403` when discovery is disabled.

### `GET /v1/status`

Header: `Authorization: Bearer <token>`

```json
{ "ok": true, "name": "office.local", "os": "mac", "paired": true }
```

### `POST /v1/exec`

Header: `Authorization: Bearer <token>`

```json
{ "command": "uname -a", "timeout": 60 }
```

Response:

```json
{ "stdout": "…", "stderr": "…", "exitCode": 0 }
```

### `POST /v1/unpair`

Header: `Authorization: Bearer <token>` — forget that token.

## Routing

When the work environment is **cloud**, `shell_execute` uses the on-device
Alpine sandbox (iSH / PRoot).

When it is **desktop** and a paired companion is online, `shell_execute` is
forwarded to `POST /v1/exec`. File tools stay on-device.

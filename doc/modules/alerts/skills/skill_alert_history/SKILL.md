---
name: skill_alert_history
description: A skill to display the authenticated user's personal alert history, paginated and optionally filtered by template.
license: GPL-3.0
metadata:
  author: "disk91"
  version: "1.0.0"
---

# Alert History

## Overview

This skill allows a user to browse their personal alert history: every alert in which the backend
recorded a delivery attempt for that user. The list is ordered newest first, paginated, and
optionally filtered by alert template identifier(s).

**Privacy guarantee** — the API never exposes other users' data. Sensitive alert fields
(`id`, `publicAccessId`, `targetedGroups`) are stripped from every response. The `sent` field
contains only the requesting user's own delivery record.

## When to use this skill

When you need a "My alerts" page or drawer where a user can see which alerts were sent to them,
when they were sent, and on which channels the delivery was attempted.

---

## Expected behavior

### Alert list

- Display alerts in a table or card list, newest first.
- Each row shows: alert name / template reference, event time, delivery status, delivery channel.
- Pagination controls: previous / next page, page size selector (suggested values: 10, 20, 50).
- An optional template filter (multi-select or tag input) narrows the results.

### Delivery status

Derive the status from the `sent` object:

| Condition | Status label |
|---|---|
| `sent` is null or empty | Pending / not delivered |
| At least one state with `sent = true` and `ack = true` | Acknowledged |
| At least one state with `sent = true` | Sent |
| All states have `sent = false` | Failed |

Display the channel (medium) next to the status badge.

### Empty state

When the API returns `204`, show a neutral empty-state message ("No alerts in your history").

---

## API Endpoint

### `GET /alerts/1.0/history` — Paginated alert history

Returns the authenticated user's alert history, ordered by event time descending.

**Auth**: `ROLE_LOGIN_COMPLETE` — standard authenticated user, no special role required.

**Query parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `page` | integer | No | `0` | 0-based page number |
| `size` | integer | No | `20` | Records per page, 1–100 |
| `templateId` | string (repeatable) | No | — | Filter to one or more alert template IDs; repeat the parameter for multiple values |

**Example requests:**
```
GET /alerts/1.0/history?page=0&size=20
GET /alerts/1.0/history?page=1&size=10&templateId=ABCDEF&templateId=GHIJKL
```

**Response `200`:**
```json
{
  "total": 142,
  "page": 0,
  "size": 20,
  "alerts": [
    {
      "alertId": "alert-temperature-high-123456",
      "alertDefRef": "dev-123456",
      "alertTemplateId": "ABCDEF",
      "deviceId": "123456",
      "state": "ENDED",
      "requestMs": 1749600000000,
      "fireMs": 1749600001000,
      "expirationMs": 0,
      "error": "",
      "sent": {
        "userLogin": "a3f2b1c9d4e5f6a7",
        "state": [
          {
            "medium": "EMAIL",
            "sent": true,
            "sentMs": 1749600002000,
            "ack": false,
            "ackMs": 0,
            "error": ""
          }
        ]
      }
    }
  ]
}
```

**Response `204`:** no body — no alerts found for this user.

**Response `400`:** invalid `page` or `size` parameter.

---

## Alert state values

| State | Meaning |
|---|---|
| `PENDING` | Detected, queued for processing |
| `PENDING_QUEUE` | In the in-memory processing queue |
| `FIRED` | Processed (legacy) |
| `RUNNING` | Active, waiting for a stop condition |
| `ENDING` | Stop condition received, queued for close notification |
| `ENDING_QUEUE` | In the in-memory queue for close processing |
| `ENDED` | Fully processed and closed |

---

## AlertMedium values

`EMAIL`, `SMS`, `PUSH`, `WHATSAPP`, `WEBHOOK`, `TOPIC`, `POPUP`, `DEFAULT`

---

## Front-end implementation notes

### Pagination state

```ts
let page = 0
let size = 20
let total = 0
let alerts: AlertHistoryEntry[] = []
let templateIdFilter: string[] = []
```

Reload the list whenever `page`, `size`, or `templateIdFilter` changes.

### Template filter

The `templateId` parameter is repeatable. Build the query string as:
```ts
const params = new URLSearchParams()
params.set('page', String(page))
params.set('size', String(size))
templateIdFilter.forEach(id => params.append('templateId', id))
fetch(`/alerts/1.0/history?${params}`)
```

### Deriving delivery status from `sent`

```ts
function deliveryStatus(sent: AlertSentEntry | null): string {
  if (!sent || !sent.state?.length) return 'pending'
  if (sent.state.some(s => s.sent && s.ack)) return 'acknowledged'
  if (sent.state.some(s => s.sent)) return 'sent'
  return 'failed'
}
```

### Relative / absolute timestamps

Use `requestMs` as the primary event timestamp. Display it as a formatted local date-time.
`fireMs` (when the notification actually fired) can be shown in a tooltip for precision.

### i18n

Use a dedicated translations file (e.g. `alerts.json`) shared with other alert skills.

---

## i18n keys

```
"alerts-history-title": "My alerts",
"alerts-history-empty": "No alerts in your history",
"alerts-history-status-pending": "Pending",
"alerts-history-status-sent": "Sent",
"alerts-history-status-acknowledged": "Acknowledged",
"alerts-history-status-failed": "Failed",
"alerts-history-filter-template": "Filter by template",
"alerts-history-col-alert": "Alert",
"alerts-history-col-date": "Date",
"alerts-history-col-channel": "Channel",
"alerts-history-col-status": "Status",
"alerts-history-invalid-page-size": "Page size must be between 1 and 100",
"alerts-history-invalid-page": "Page number must be 0 or greater",
```

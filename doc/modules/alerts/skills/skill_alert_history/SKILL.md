---
name: skill_alert_history
description: A skill to display alert history — personal view for regular users, full platform view for ROLE_GOD_ADMIN.
license: GPL-3.0
metadata:
  author: "disk91"
  version: "1.1.0"
---

# Alert History

## Overview

This skill allows a user to browse alert history, paginated and optionally filtered by template identifier(s),
ordered by event time descending (newest first).

The API behaviour differs by role:

| Role | Alerts returned | `sent` field | `targetedGroups` |
|---|---|---|---|
| Regular user | Only alerts where the user has a delivery record | The user's own `AlertSentEntry` (array of one) | Empty array |
| `ROLE_GOD_ADMIN` | **All** alerts on the platform | Full delivery list for all users | Full group list |

`id` and `publicAccessId` are **never** returned regardless of role.

## When to use this skill

- **Regular user** — a "My alerts" page showing which alerts were sent to them, when, and on which channel.
- **Admin** — a platform-wide alert monitoring view showing all alerts with full delivery details and target groups.

---

## Expected behavior

### Alert table layout

Render alerts as a table. Each alert occupies **one primary row**, with an optional **secondary row** for
the error message, and an **expandable sub-table** for delivery details.

#### Primary row columns

| Column | Field | Notes |
|---|---|---|
| Source | `alertDefRef` | Source module reference |
| Template | `alertTemplateId` | Short template identifier |
| Device | `deviceId` | May be empty |
| State | `state` | Badge, color-coded (see states below) |
| Event time | `requestMs` | Formatted local date-time |
| Fire time | `fireMs` | Formatted local date-time; hide or grey-out when `fireMs = 0` |
| Delivery | derived from `sent` | Status badge + medium icon (see delivery status below) |

For `ROLE_GOD_ADMIN` only, add two extra columns:
- **Groups** — comma-separated list from `targetedGroups`
- **Recipients** — count of entries in `sent` (e.g. "3 users")

#### Secondary row (error)

When `error` is non-empty, render a second row spanning all columns beneath the primary row.
Display the error text in a muted style (e.g. italic, red or amber tint). Do not show the secondary
row when `error` is null or an empty string.

#### Expandable delivery sub-table

Clicking anywhere on a primary row toggles an inline sub-table below it (and below the secondary error
row when present). The sub-table shows the `sent` array details:

| Sub-column | Field | Notes |
|---|---|---|
| User | `userLogin` | Hidden for regular users (always their own login) |
| Medium | `medium` | Channel icon + label |
| Sent | `sent` | Boolean badge |
| Sent at | `sentMs` | Formatted time; hidden when `sentMs = 0` |
| Ack | `ack` | Boolean badge |
| Ack at | `ackMs` | Formatted time; hidden when `ackMs = 0` |
| Error | `error` | Shown only when non-empty |

For a regular user `sent` has at most one entry, so the sub-table has at most one data row.
For `ROLE_GOD_ADMIN` the sub-table has one row per targeted user.

Hide the **User** sub-column for regular users (the login is always their own and adds no information).

#### Pagination controls

Place pagination below the table: previous / next page buttons, current page indicator, and a page
size selector (suggested values: 10, 20, 50).

#### Template filter

Place an optional multi-select or tag input above the table to filter by one or more `alertTemplateId`
values. Changing the filter resets `page` to `0`.

### Delivery status

Derive the status badge shown in the primary row from the `sent` array:

| Condition | Status label |
|---|---|
| `sent` is empty | Pending |
| At least one entry with `sent = true` and `ack = true` | Acknowledged |
| At least one entry with `sent = true` | Sent |
| All entries have `sent = false` | Failed |

### Admin vs user view

When the authenticated user has `ROLE_GOD_ADMIN`:
- Show the **Groups** and **Recipients** extra columns in the primary row.
- Show the **User** sub-column in the delivery sub-table.
- `total` reflects all alerts on the platform.

When the user is a regular user:
- `targetedGroups` is always an empty array — do not render the Groups column.
- `sent` contains at most one entry — the User sub-column is hidden.
- `total` reflects only alerts where the user has a delivery record.

### Empty state

When `total = 0`, show a neutral empty-state message inside the table area. The API always returns
`200` with an empty list — there is no `204`.

---

## API Endpoint

### `GET /alerts/1.0/history` — Paginated alert history

Returns alert history ordered by event time descending. The scope (own vs all) is resolved server-side
from the authenticated user's role — no extra parameter is needed.

**Auth**: `ROLE_LOGIN_COMPLETE` — any fully authenticated user.

**Query parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `page` | integer | No | `0` | 0-based page number |
| `size` | integer | No | `20` | Records per page, 1–100 |
| `templateId` | string (repeatable) | No | — | Filter to one or more alertTemplateId values; repeat for multiple |

**Example requests:**
```
GET /alerts/1.0/history?page=0&size=20
GET /alerts/1.0/history?page=1&size=10&templateId=ABCDEF&templateId=GHIJKL
```

**Response `200` — regular user:**
```json
{
  "total": 42,
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
      "targetedGroups": [],
      "sent": [
        {
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
      ]
    }
  ]
}
```

**Response `200` — ROLE_GOD_ADMIN (same structure, full data):**
```json
{
  "total": 314,
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
      "targetedGroups": ["grpA", "grpB"],
      "sent": [
        {
          "userLogin": "a3f2b1c9d4e5f6a7",
          "state": [{ "medium": "EMAIL", "sent": true, "sentMs": 1749600002000, "ack": false, "ackMs": 0, "error": "" }]
        },
        {
          "userLogin": "b7e1c2d3f4a5b6c7",
          "state": [{ "medium": "PUSH", "sent": true, "sentMs": 1749600003000, "ack": true, "ackMs": 1749600004000, "error": "" }]
        }
      ]
    }
  ]
}
```

**Response `400`:** invalid `page` or `size` parameter, or requesting user not found.

---

## Alert state values

| State | Meaning |
|---|---|
| `PENDING` | Detected, queued for processing |
| `PENDING_QUEUE` | In the in-memory processing queue |
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

```ts
const params = new URLSearchParams()
params.set('page', String(page))
params.set('size', String(size))
templateIdFilter.forEach(id => params.append('templateId', id))
fetch(`/alerts/1.0/history?${params}`)
```

### Deriving delivery status

For a regular user, `sent` has at most one entry. For admin, iterate all entries.

```ts
function deliveryStatus(sent: AlertSentEntry[]): string {
  if (!sent?.length) return 'pending'
  const all = sent.flatMap(e => e.state ?? [])
  if (all.some(s => s.sent && s.ack)) return 'acknowledged'
  if (all.some(s => s.sent)) return 'sent'
  return 'failed'
}
```

### Admin: showing targetedGroups

Only render the `targetedGroups` column / section when the local user state indicates admin.
The field is always present in the response but is an empty array for non-admin users, so it is
safe to derive the decision from `targetedGroups.length > 0` as a fallback.

### Timestamps

Use `requestMs` as the primary event timestamp (formatted local date-time).
`fireMs` (actual notification time) can be shown in a tooltip.

### i18n

Use a dedicated translations file (e.g. `alerts.json`) shared with other alert skills.

---

## i18n keys

```
"alerts-history-title": "My alerts",
"alerts-history-title-admin": "All alerts",
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
"alerts-history-col-groups": "Target groups",
"alerts-history-col-recipients": "Recipients",
"alerts-history-invalid-page-size": "Page size must be between 1 and 100",
"alerts-history-invalid-page": "Page number must be 0 or greater",
```

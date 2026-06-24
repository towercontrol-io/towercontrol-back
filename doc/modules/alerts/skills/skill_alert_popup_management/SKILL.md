---
name: skill_alert_popup_management
description: A skill to display in-app popup notifications (bell icon, badge, overlay list, toaster) for the authenticated user.
license: GPL-3.0
metadata:
  author: "disk91"
  version: "1.1.0"
---

# Alert Popup Management

## Overview

This skill covers the in-app popup notification feature. It is split into two independent circuits that share the same
underlying data but serve different UX goals:

| Circuit | Purpose | API | State impact |
|---|---|---|---|
| **Bell / history** | Badge counter + overlay list of recent notifications | `GET /count`, `GET /`, `PUT /viewed` | Marks entries as viewed |
| **Toaster / live** | Real-time toaster for newly arrived alerts | `GET /new?since=<ts>` | None — read-only |

The two circuits are completely independent: the toaster polling does not affect the badge or the history list, and
opening the history overlay does not affect the toaster's `since` pointer.

---

## Circuit 1 — Bell / history

### Bell icon and badge

- A bell icon lives in the upper-right corner of the application shell, persistent across pages.
- A red badge shows the **count of unread popup notifications**.
- The badge is hidden when the count is zero.
- Poll `GET /alerts/1.0/popup/count` every 30 seconds to refresh the badge.

### Overlay list

- Clicking the bell opens a slide-in overlay on the right side of the screen.
- On open, call `GET /alerts/1.0/popup` to load the history (up to `alerts.popup.max.displayed` entries:
  all unread ones + read ones within the last `alerts.popup.max.displayed.ms` window, newest first).
- Entries are stacked vertically, separated by a thin divider.
- Each entry shows: criticality badge (color-coded), message text, relative timestamp (e.g. "2 hours ago").
- **Immediately after the overlay opens**, call `PUT /alerts/1.0/popup/viewed` to mark all entries as viewed
  and clear the badge. Update `unreadCount` to 0 locally as an optimistic update.

---

## Circuit 2 — Toaster / live

### Design principle

The toaster mechanism is driven by a dedicated endpoint that accepts a client-managed timestamp (`since`).
The server returns every popup whose `timeMs > since`. The response includes the full popup data
(message, criticality, timeMs), so the toaster can render immediately **without a second API call**.

This endpoint has **no side effects**: it never touches `viewedMs`, never changes the badge count, and
does not interfere with the history overlay.

### Client-side `since` pointer

- Initialise `since` to `Date.now()` (the page-load timestamp) when the component mounts.
- After each non-empty response, update `since` to the highest `timeMs` value received.
- After an empty response, keep `since` unchanged.

### Polling flow

1. Every N seconds (recommended: 30 s), call `GET /alerts/1.0/popup/new?since=<since>`.
2. If the response list is non-empty:
   - Show a toaster for each entry (or only the most recent one if UX prefers a single toaster).
   - Update `since` to `Math.max(...entries.map(e => e.timeMs))`.
   - Increment the local `unreadCount` by the number of new entries (optimistic badge update, avoids
     an extra call to `/count`).
3. If the response is empty, do nothing.

---

## Criticality color mapping

| Criticality | Color suggestion |
|---|---|
| `INFO` | Blue |
| `WARNING` | Amber / orange |
| `DANGER` | Red |
| `DEFAULT` | Grey |

---

## API Endpoints

All endpoints require a valid Bearer token in the `Authorization` header and `ROLE_LOGIN_COMPLETE`.

---

### `GET /alerts/1.0/popup` — History list (bell circuit)

Returns up to `alerts.popup.max.displayed` entries for the authenticated user:
all unread ones plus read ones from the last `alerts.popup.max.displayed.ms` milliseconds,
ordered newest first.

**Response `200`:**
```json
[
  {
    "alertId": "alert-temperature-high-123456",
    "message": "Temperature exceeded threshold on sensor A3",
    "criticality": "WARNING",
    "timeMs": 1749600000000
  }
]
```

Returns `200` with an empty array when there are no entries.

---

### `GET /alerts/1.0/popup/count` — Unread count / badge (bell circuit)

Returns the number of unread popup notifications for the authenticated user.

**Response `200`:**
```json
{
  "unreadCount": 3
}
```

---

### `PUT /alerts/1.0/popup/viewed` — Mark all as viewed (bell circuit)

Marks all unread popup notifications as viewed. Call this immediately when the overlay opens.

**Response `200`:** empty body.

---

### `GET /alerts/1.0/popup/new?since=<ts>` — New arrivals since timestamp (toaster circuit)

Returns all popup notifications with `timeMs > since`, ordered oldest first.
The response is self-contained: all data needed to render the toaster is included.
**No server state is modified.**

**Query parameter:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `since` | long (ms) | Yes | Exclusive lower bound; use `Date.now()` on mount, then the highest `timeMs` seen |

**Response `200`:**
```json
[
  {
    "alertId": "alert-temperature-high-123456",
    "message": "Temperature exceeded threshold on sensor A3",
    "criticality": "WARNING",
    "timeMs": 1749600000000
  }
]
```

Returns `200` with an empty array when no new entries exist since `since`.

**Error responses:**
- `400` — `since` parameter missing or not a valid long.

---

## Front-end state

```ts
// Bell circuit
let unreadCount = 0           // drives the badge
let popups: AlertPopup[] = [] // loaded on overlay open
let overlayOpen = false

// Toaster circuit  (independent)
let since = Date.now()        // client-managed pointer, never sent to the bell endpoints
```

---

## Front-end implementation notes

### Two independent polling intervals

Run two separate `setInterval` calls (or composables):

1. **Bell badge** — calls `GET /count` every 30 s to keep the badge accurate while the overlay is closed.
   Stop or pause this interval while the overlay is open (badge is already cleared).
2. **Toaster** — calls `GET /new?since=<since>` every 30 s regardless of overlay state.

### Overlay open sequence

```
overlayOpen = true
→ GET /alerts/1.0/popup           (load history)
→ PUT /alerts/1.0/popup/viewed    (mark viewed, no await needed)
→ unreadCount = 0                 (optimistic)
```

### Toaster on new entry

```
response = GET /alerts/1.0/popup/new?since=<since>
if response.length > 0:
  show toaster(response[response.length - 1])   // most recent
  since = max(response.map(e => e.timeMs))
  unreadCount += response.length                // optimistic badge increment
```

### Relative timestamps

Use `Intl.RelativeTimeFormat` or an equivalent date library. Recompute every minute while the overlay is open.

### i18n

Use a dedicated translations file (e.g. `alerts.json`) separate from the common translations file.

---

## i18n keys

```
"alerts-popup-no-notifications": "No notifications",
"alerts-popup-mark-viewed": "Mark all as read",
"alerts-popup-just-now": "Just now",
"alerts-popup-criticality-info": "Info",
"alerts-popup-criticality-warning": "Warning",
"alerts-popup-criticality-danger": "Danger",
```

---
name: skill_alert_popup_management
description: A skill to display in-app popup notifications (bell icon, badge, overlay list) for the authenticated user.
license: GPL-3.0
metadata:
  author: "disk91"
  version: "1.0.0"
---

# Alert Popup Management

## Overview

This skill covers the in-app popup notification feature: the bell icon with an unread badge, the slide-in overlay listing 
recent notifications, and the toaster for newly arriving alerts.

Popups are created by the backend when an alert is delivered via the `POPUP` medium. The front-end polls (or subscribes) 
to detect new entries and displays them in real time.

## When to use this skill

When you need to implement the notification bell component in the application shell, including:
- the badge counter driven by unread popup count
- the overlay list shown when the user clicks the bell
- the toaster for newly arrived alerts
- the "mark as viewed" action that clears the badge

---

## Expected behavior

### Bell icon and badge

- A bell icon is placed in the upper-right corner of the application shell (persistent across pages).
- A red badge overlaying the bell shows the **count of unread popups**.
- The badge is hidden when the count is zero.
- On mount and periodically, the front-end calls `GET /alerts/1.0/popup/count` to refresh the badge.

### Overlay list

- Clicking the bell opens an overlay panel on the right side of the screen.
- The overlay calls `GET /alerts/1.0/popup` to load up to 10 entries (unread + last two days).
- Entries are stacked vertically, newest first, separated by a thin divider.
- Each entry shows: criticality badge (color-coded), message text, relative time (e.g. "2 hours ago").
- Unread entries (viewedMs = 0) are visually highlighted (e.g. bold text or colored left border).
- When the overlay opens, `PUT /alerts/1.0/popup/viewed` is called to mark all entries as viewed and clear the badge.

### Toaster for new alerts

- A background mechanism (polling `GET /alerts/1.0/popup/count` every 30 seconds, or SSE/WebSocket) detects new unread popups.
- When the count increases, a toaster notification appears at the top right with the message from the newest unread entry.
- The toaster auto-dismisses after a few seconds.
- The badge is updated immediately.

### Criticality color mapping

| Criticality | Color suggestion |
|---|---|
| `INFO` | Blue |
| `WARNING` | Amber / orange |
| `DANGER` | Red |
| `DEFAULT` | Grey |

---

## API Endpoints

All endpoints require a valid Bearer token in the `Authorization` header and `ROLE_LOGIN_COMPLETE`.

### `GET /alerts/1.0/popup` — Get popup list

Returns up to 10 popup notifications for the authenticated user: all unread entries plus read entries from the last two days, ordered by time descending.

**Response `200`:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "alertId": "alert-temperature-high-123456",
    "message": "Temperature exceeded threshold on sensor A3",
    "criticality": "WARNING",
    "timeMs": 1749600000000,
    "viewedMs": 0
  }
]
```

- `viewedMs = 0` means the entry has not been viewed yet.
- Returns **`204`** (no body) when there are no entries to show.

---

### `GET /alerts/1.0/popup/count` — Get unread count (badge)

Returns the count of unread popup notifications for the authenticated user.

**Response `200`:**
```json
{
  "unreadCount": 3
}
```

---

### `PUT /alerts/1.0/popup/viewed` — Mark all as viewed

Marks all unread popup notifications as viewed. Call this when the overlay is opened to clear the badge.

**Response `200`:** empty body on success.

---

## Front-end implementation notes

### Polling strategy

Poll `GET /alerts/1.0/popup/count` every 30 seconds. Compare the returned `unreadCount` with the last known count. If it has increased, fetch `GET /alerts/1.0/popup` to get the new entries and show a toaster for the most recent unread one.

### State

- `unreadCount: number` — drives the badge display.
- `popups: AlertPopup[]` — the list displayed in the overlay.
- `overlayOpen: boolean` — controls overlay visibility.

### Mark-as-viewed timing

Call `PUT /alerts/1.0/popup/viewed` immediately when the overlay opens (before the user scrolls). This clears the server-side unread state. Set `unreadCount` to 0 locally as an optimistic update.

### Relative timestamps

Use a lightweight relative-time formatter (e.g. `Intl.RelativeTimeFormat` or a date library) to display "2 hours ago" instead of raw timestamps. Recompute on a 1-minute interval while the overlay is open.

### i18n

Use a dedicated translations file (e.g. `alerts.json`) separate from the common translations.

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

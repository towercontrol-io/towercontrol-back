---
name: skill_alert_template_management
description: A skill to manage alert templates (create, update, delete, list) as an identified user with the appropriate role.
license: GPL-3.0
metadata:
  author: "disk91"
  version: "1.2.0"
---

# Alert Template Management

## Overview

This skill allows you to build a front-end page for creating, updating, deleting and listing alert templates.
Alert templates define the notification messages (multi-locale, multi-medium) sent when an alert fires.
The write endpoints are restricted to users with `ROLE_ALERTS_ADMIN` or `ROLE_ALERTS_TEMPLATE`.
The list endpoint is available to any authenticated user.

## When to use this skill

When you need a UI for administrators or authorized users to manage the library of alert templates available on the platform.

## Roles

| Role | Permissions |
|---|---|
| `ROLE_ALERTS_ADMIN` | Create / update / delete **any** template, including global ones |
| `ROLE_ALERTS_TEMPLATE` | Create / update / delete **only their own** non-global templates |

A **global** template is visible to all connected users regardless of who created it.
Only `ROLE_ALERTS_ADMIN` can mark a template as global.

---

## Expected behavior

### List the Alert Templates for edition and deletion

A user views a table listing templates they have access to. Each row shows the name, behavior, a **Global** badge when applicable, and the number of configured locales. Each row has **Edit** and **Delete** buttons; **Delete** is hidden when the user is neither the owner nor an admin.

### Create / Edit form — overall layout

The form is organized into four sections rendered as distinct cards:

1. **General information** — name, description, behavior, duration, preferred channels, global toggle
2. **Parameters** — ordered list of parameter slots injected as `{1}`, `{2}`, … in messages
3. **Open messages** — at least one locale required
4. **Close messages** — optional, visible only when behavior is `FIRE_TO_END` or `FIRE_UNTIL`

---

## Detailed UX guidelines

### Section 1 — General information

- **Name**: text input, required, max 100 characters.
- **Description**: text input, optional, max 500 characters.
- **Behavior**: select/dropdown with four options (`FIRE_FORGET`, `FIRE_TO_END`, `FIRE_UNTIL`, `SILENT`). Required.
- **Duration**: numeric input, visible **only** when behavior is `FIRE_TO_END` or `FIRE_UNTIL`.
    - **Store and transmit duration as milliseconds** in the API (`durationMs`), but **display and accept the value in minutes** in the UI. Convert on load (`÷ 60000`) and on save (`× 60000`).
- **Global toggle**: visible **only** for users with `ROLE_ALERTS_ADMIN`.
- **Preferred channels** (ordered):
    - The channel order is meaningful — it expresses priority. Render as a vertical ordered list with up/down reorder controls and a remove button per item.
    - Above the list, provide a select/dropdown (filtered to channels not yet added) and an **Add** button. After adding, advance the selector to the next available option so the user can add another channel immediately.
    - Show an empty-state message when no channel has been added.

### Section 2 — Parameters

- Render as an ordered list of rows. Each row has: a type selector, an optional value field (shown only for `CUSTOM_PARAM` and `ALERT_LINK`), up/down reorder buttons, and a remove button.
- An **Add parameter** button appends a new row.
- Position in the list determines the injection token: first row → `{1}`, second → `{2}`, etc.
- Show an empty-state message when no parameter has been defined.

### Section 3 & 4 — Open and Close message sections

Both sections share the same structure. The differences are: Open is always visible and requires at least one locale; Close is hidden when behavior is `FIRE_FORGET` or `SILENT`.

#### Language management — placement and initialization

- Place the **language selector** (dropdown + Add button) in the **section header**, not in the body. This keeps the body focused on content.
- The language dropdown must **only show locales not yet added** (filtered list). After a locale is added, advance the selector to the next available option.
- When no locale has been added yet, display an empty-state hint in the body pointing the user to the header selector.
- When a new locale is added (and it is not the first), **initialize its medium list** from the first existing locale: copy the same channels with empty messages, so the user only needs to fill in the text.

#### Language tabs

- Render locales as **tabs** using a proper tab component from the UI library (e.g., `UTabs` in Nuxt UI). Do not build custom pill buttons.
- Each tab label shows the locale code and a **close (×) button** to remove that locale.
    - Use the tab component's trailing-slot API (or equivalent) to embed the × button inside the tab trigger. Use `stopPropagation` on the × click so it does not trigger tab selection.
- Track the active tab by **locale value (string)**, not by array index. This avoids stale index references after add/remove.
- When a locale is added, make that new tab active.
- When the active locale is removed, activate the previous locale (or the first one if it was already the first). When all locales are removed, clear the active locale.

#### Channel (medium) selection

- Display all available channels as **inline toggle buttons** — one button per channel, all always visible.
- A channel is **selected** (solid / filled style) when it has been added to the current locale; **unselected** (outline / ghost style) otherwise.
- Clicking a selected button removes that channel and its message from the locale. Clicking an unselected button adds it with an empty message.
- Do **not** use a dropdown + Add button for channel selection; the toggle-button row gives immediate visual feedback on what is and is not active.

#### Medium message card

Each enabled channel renders a card containing:
1. A **header row** with:
    - A badge showing the channel name.
    - **Parameter shortcut buttons**: if the template has parameters defined, show a small clickable button per parameter (`{1}`, `{2}`, …) in monospace style. Clicking one inserts the token at the current cursor position in the textarea below.
        - Track the cursor position by listening to `blur`, `mouseup`, and `keyup` events on the textarea. Store the `selectionStart`/`selectionEnd` pair. On insert, splice the token into the message string at the stored position and advance the stored cursor past the inserted text.
        - Only show parameter buttons when at least one parameter has been defined in Section 2.
    - A **remove button** (trash icon), right-aligned via `margin-left: auto` / `ml-auto`, to remove this channel.
2. A **message textarea** — support Markdown syntax (monospace font is recommended). Use `{1}`, `{2}`, … placeholders for injected parameters.

---

## API Endpoints

All endpoints require a valid Bearer token in the `Authorization` header.

### `POST /alerts/1.0/template` — Create or update a template

- **Auth**: `ROLE_ALERTS_ADMIN` or `ROLE_ALERTS_TEMPLATE`
- When the body contains a `shortId` field → **update**; when `shortId` is absent → **creation**.
- Returns **`201`** on creation, **`200`** on update.

**Request body:**
```json
{
  "shortId": "ABCDEF",
  "name": "High temperature alert",
  "description": "Fired when a sensor exceeds the threshold",
  "global": false,
  "parameters": [
    { "type": "DEVICE_NAME", "param": "" },
    { "type": "CUSTOM_PARAM", "param": "temperature" }
  ],
  "open": [
    {
      "locale": "en",
      "mediums": [
        { "medium": "EMAIL", "message": "**{1}** reported a temperature of **{2}°C**." },
        { "medium": "PUSH",  "message": "{1}: temperature {2}°C" }
      ]
    },
    {
      "locale": "fr",
      "mediums": [
        { "medium": "EMAIL", "message": "**{1}** a signalé une température de **{2}°C**." },
        { "medium": "PUSH",  "message": "{1} : température {2}°C" }
      ]
    }
  ],
  "close": [],
  "behavior": "FIRE_FORGET",
  "preferred": ["PUSH", "EMAIL"],
  "durationMs": 0
}
```

**`shortId`** is optional — omit it to create a new template. On creation the server generates and returns a unique 6-letter identifier.

**Response `201` / `200`:**
```json
{
  "shortId": "ABCDEF",
  "name": "High temperature alert",
  "description": "Fired when a sensor exceeds the threshold",
  "owner": "a3f2b1c9d4e5f6a7",
  "global": false,
  "parameters": [ "..." ],
  "open": [ "..." ],
  "close": [],
  "behavior": "FIRE_FORGET",
  "preferred": ["PUSH", "EMAIL"],
  "durationMs": 0
}
```

**Error responses:**
- `400` — validation failure (name missing or too long, behavior unknown, no open message, etc.)
- `403` — insufficient role, or trying to modify a template owned by another user

---

### `DELETE /alerts/1.0/template/{shortId}` — Delete a template

- **Auth**: `ROLE_ALERTS_ADMIN` or `ROLE_ALERTS_TEMPLATE`
- `ROLE_ALERTS_ADMIN` may delete any template.
- `ROLE_ALERTS_TEMPLATE` may only delete their own templates.
- Returns **`200`** on success, **`403`** when not found or not authorized.

---

### `GET /alerts/1.0/template` — List templates

- **Auth**: any `ROLE_LOGIN_COMPLETE` user
- Returns templates **created by the requesting user** and **all global templates**.
- Optional query parameter `search` applies a case-insensitive name filter.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `search` | string | No | Case-insensitive partial match on the template name |

**Response `200`:**
```json
{
  "templates": [
    {
      "shortId": "ABCDEF",
      "name": "High temperature alert",
      "owner": "a3f2b1c9d4e5f6a7",
      "global": false,
      "behavior": "FIRE_FORGET",
      "durationMs": 0
    }
  ],
  "total": 1
}
```

Returns **`204`** (no body) when no templates are found.

---

## Reference: enum values

### AlertBehavior

| Value | Meaning |
|---|---|
| `FIRE_FORGET` | Fire once and terminate immediately; the next trigger creates a new alarm |
| `FIRE_TO_END` | Fire and stay active until explicitly cancelled or expired |
| `FIRE_UNTIL` | Fire and stay active until cancelled or `durationMs` elapses |
| `SILENT` | No notification sent; only an audit log entry is written |

### AlertMedium

`EMAIL`, `SMS`, `PUSH`, `WHATSAPP`, `WEBHOOK`, `TOPIC`, `DEFAULT`

- Individual mediums (`EMAIL`, `SMS`, `PUSH`, `WHATSAPP`) are delivered per user.
- Collective mediums (`WEBHOOK`, `TOPIC`) use group-level settings.

### AlertParameter types

| Type | Description | `param` field |
|---|---|---|
| `DEVICE_ID` | Technical device identifier | — |
| `DEVICE_NAME` | Human-readable device name | — |
| `GROUP_NAME` | Name of the associated group | — |
| `USER_FIRSTNAME` | First name of the recipient | — |
| `USER_LASTNAME` | Last name of the recipient | — |
| `USER_GENDER` | Gender of the recipient | — |
| `ALERT_TIME` | Alert time, e.g. `18:45` | — |
| `ALERT_DATE_TIME` | Full UTC date-time, e.g. `2026-06-08 18:45` | — |
| `CUSTOM_PARAM` | Custom parameter | parameter name (lowercase) |
| `SERVICE_NAME` | Platform service name from configuration | — |
| `SERVICE_HOME` | Platform home URL from configuration | — |
| `ALERT_LINK` | Deep link URL | URL template with `{aid}`, `{did}`, `{pubID}` |

Parameters are injected into message templates as `{1}`, `{2}`, … matching their position in the `parameters` array.

---

## Front-end implementation notes

### Form state

- `name`, `description`, `behavior`, `global`: plain strings / boolean.
- `durationMin`: integer (minutes) in the UI only — convert to/from `durationMs` (milliseconds) at API boundary.
- `parameters`: ordered array of `{ type, param }`.
- `preferred`: ordered array of `AlertMedium` strings.
- `open` / `close`: array of `{ locale: string, mediums: { medium: string, message: string }[] }`.
- Active locale per section: **a string (the locale code)**, not an index. Derive the index when needed with a `findIndex` call.

### Locale selector filtering

Always filter the locale dropdown to exclude already-added locales. After any add or remove, reset the selector to the first still-available option so it never shows a locale that is already present.

### Tab component integration

Use the UI library's native tab component bound to the active locale string. When the library expects a numeric index or a separate model, adapt with a computed that converts between locale string and index. The tab trigger should embed the close button via a trailing slot, with event propagation stopped so the close click does not also select the tab.

### Channel toggle logic

For each channel, compute `isSelected = locale.mediums.some(m => m.medium === channel)`. A single toggle handler:
- if selected → splice the medium out of the array by its index.
- if not selected → push a new `{ medium, message: '' }` entry.

No separate "selected medium" state variable is needed.

### Parameter insertion into textarea

Maintain a cursor-position store keyed by a unique string per textarea (e.g., `{section}_{locale}_{medium}`). Update on `blur`, `mouseup`, and `keyup` using `selectionStart`/`selectionEnd` from the event target. On parameter-button click, splice the token into the message string at the stored position, then advance the stored cursor past the inserted token. Avoid framework-specific element refs if possible; the native event target from the textarea's DOM events is sufficient.

### i18n

Use a **dedicated translations file** for this feature (e.g., `alerts.json`), separate from the application's common translations file. Register it in the i18n configuration alongside other feature-specific files.

### String concatenation in templates

When building strings that look like `{n}` inside framework template syntax (e.g., Vue, Angular, JSX), **avoid ES6 template literals** — use string concatenation instead (`'{' + (n + 1) + '}'`). Template literals with `${…}` may be misinterpreted by the framework's template parser.

---

## Validation rules (client-side)

| Field | Rule |
|---|---|
| `name` | Required, max 100 characters |
| `description` | Optional, max 500 characters |
| `behavior` | Must be one of the four `AlertBehavior` values |
| `open` | At least one locale required |
| each locale in `open` | Must have at least one medium with a non-empty message |

---

## i18n keys

```
"alerts-template-name-required": "The template name is required",
"alerts-template-name-too-long": "The template name must not exceed 100 characters",
"alerts-template-description-too-long": "The template description must not exceed 500 characters",
"alerts-template-behavior-required": "A valid behavior must be selected",
"alerts-template-open-required": "At least one open message locale is required",
"alerts-template-locale-required": "Each message block must specify a locale",
"alerts-template-medium-required": "Each locale must contain at least one medium and its message",
"alerts-template-message-required": "The message text must not be empty",
"alerts-template-global-forbidden": "Only administrators can create or update global templates",
"alerts-template-update-forbidden": "You can only modify templates that you own",
"alerts-template-delete-forbidden": "You can only delete templates that you own",
"alerts-template-not-found": "The requested alert template was not found",
"alerts-template-deleted": "The alert template has been deleted",
"alerts-template-shortid-generation-failed": "Unable to generate a unique short identifier for the template, please retry",
```

# Implementation reference

- [types implementation](https://raw.githubusercontent.com/towercontrol-io/towercontrol-front/refs/heads/main/app/types/alerts.ts)
- [api implementation](https://raw.githubusercontent.com/towercontrol-io/towercontrol-front/refs/heads/main/app/plugins/api.backend.alerts.ts)


---
name: skill_alert_template_management
description: A skill to manage alert templates (create, update, delete, list) as an identified user with the appropriate role.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
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

## API Endpoints

All endpoints require a valid Bearer token in the `Authorization` header.

### `POST /alerts/1.0/template` — Create or update a template

- **Auth**: `ROLE_ALERTS_ADMIN` or `ROLE_ALERTS_TEMPLATE`
- When the body contains an `id` field → **update**; when `id` is absent → **creation**.
- Returns **`201`** on creation, **`200`** on update.

**Request body:**
```json
{
  "id": "6660a1b2c3d4e5f600000001",
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

**`id`** is optional — omit it to create a new template.

**Response `201` / `200`:**
```json
{
  "id": "6660a1b2c3d4e5f600000001",
  "name": "High temperature alert",
  "description": "Fired when a sensor exceeds the threshold",
  "owner": "a3f2b1c9d4e5f6a7",
  "global": false,
  "parameters": [ ... ],
  "open": [ ... ],
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

### `DELETE /alerts/1.0/template/{id}` — Delete a template

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
      "id": "6660a1b2c3d4e5f600000001",
      "name": "High temperature alert",
      "owner": "a3f2b1c9d4e5f6a7",
      "global": false,
      "behavior": "FIRE_FORGET",
      "durationMs": 0,
      ...
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

## Front-end workflow

### List page

1. Call `GET /alerts/1.0/template` (with optional `?search=…`) to load the list.
2. Display `name`, `behavior`, a **Global** badge when `global === true`, and the number of locales configured.
3. Show **Edit** and **Delete** buttons on each row; hide **Delete** when the user is not the owner and not admin.

### Create / Edit form

1. **Header fields**: `name` (required, max 100), `description` (optional, max 500), `behavior` (select, required), `durationMs` (number, shown only for `FIRE_TO_END` / `FIRE_UNTIL`), `global` toggle (shown only for admin), `preferred` multi-select.
2. **Parameters section**: ordered list of `{ type, param }` entries. The user adds parameters in the desired order; the position maps to `{1}`, `{2}`, … in the message body.
3. **Open messages section**: at least one locale required; each locale must have at least one medium with a non-empty message.
4. **Close messages section**: optional, shown only when `behavior` is `FIRE_TO_END` or `FIRE_UNTIL`.
5. On submit, send `POST /alerts/1.0/template` with `id` included for update, absent for creation.

### Delete confirmation

Show a modal confirmation dialog before calling `DELETE /alerts/1.0/template/{id}`.

---

## Authentication requirements

- Write endpoints require `ROLE_ALERTS_ADMIN` or `ROLE_ALERTS_TEMPLATE`.
- The list endpoint requires any authenticated user (`ROLE_LOGIN_COMPLETE`).
- Include the Bearer token in every request: `Authorization: Bearer <token>`.
- On `403`, display an error and do not retry.

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
```


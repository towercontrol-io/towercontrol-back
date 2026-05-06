---
name: skill_audit_list
description: A skill to implement an audit log consultation screen with search, filtering and paginated table display. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Audit Log Consultation using the IoT Tower Control backend

## Overview
This skill allows you to implement an audit log consultation screen. It provides a search bar, optional date range filters,
a paginated table of log entries ordered from most recent to oldest, and handles the two levels of access rights:
- **ROLE_AUDIT_RD**: reads logs with sensitive parameters obfuscated as `***`
- **ROLE_AUDIT_RD_CLEAR**: reads logs with sensitive parameters decrypted and displayed in clear text

## When to use this skill
When you've been asked to build an audit log viewer, a security event log screen, or any page that lists and searches
through the audit trail of the application.

## Prerequisites
- The user must be authenticated with a complete session (`ROLE_LOGIN_COMPLETE`)
- The user must have at least one of the roles: `ROLE_AUDIT_RD` or `ROLE_AUDIT_RD_CLEAR`
- You must check the user's roles before rendering the page and redirect to an error page if neither role is present

## Principles of the audit log list screen

### Search field
There is a single free-text search field. When submitted, its value must be **Base64-encoded** before being sent
as the `search` query parameter. The search is a case-insensitive partial match applied simultaneously on:
- the **service** name (e.g. `USER`, `DEVICE`)
- the **action** name (e.g. `USER_CREATE`, `DEVICE_UPDATE`)
- the **owner** field (login hash of the user who triggered the action)

An empty search field means no text filter: all entries are returned.

### Date range filter
Two optional date pickers allow filtering by time range:
- **From** (`startMs`): start date — sent as milliseconds since epoch (Unix timestamp × 1000). `0` means no lower bound.
- **To** (`endMs`): end date — sent as milliseconds since epoch. `0` means no upper bound.

When both dates are set, the backend validates that `startMs < endMs` and returns HTTP 400 otherwise.

### Pagination
- `page`: 0-based page number (default `0`)
- `pageSize`: number of entries per page (default `50`, maximum `200`)

The response contains:
- `total`: total number of entries matching the current filters
- `totalPages`: total number of pages available
- `pageSize`: number of entries per page (echoed back)

### Log parameter rendering
Each log entry has a `logStr` field which is the human-readable log message.
- For users with **ROLE_AUDIT_RD**: sensitive parts of the log are already obfuscated as `***` by the backend
- For users with **ROLE_AUDIT_RD_CLEAR**: sensitive parts are decrypted and inlined — the string is displayed as-is
- No client-side processing is needed: the backend handles the resolution transparently

### No-database status
When the backend is not configured with a database audit backend, the response will have:
- `status` = `"audit-log-non-database"`
- `logs` = empty array
- `total` = 0

In that case, display an informational message to the user using the i18n key `audit-log-non-database`.

---

## API Endpoint

### `GET /audit/1.0/logs/search`
Retrieve a paginated list of audit log entries with optional filters.

**Authentication**: Required — `ROLE_LOGIN_COMPLETE` + (`ROLE_AUDIT_RD` or `ROLE_AUDIT_RD_CLEAR`)

**Query parameters**:

| Parameter  | Type   | Required | Default | Description |
|------------|--------|----------|---------|-------------|
| `search`   | string | No       | —       | Free-text filter on service/action/owner — **must be Base64-encoded** |
| `startMs`  | long   | No       | `0`     | Lower date bound, ms since epoch (`0` = no bound) |
| `endMs`    | long   | No       | `0`     | Upper date bound, ms since epoch (`0` = no bound) |
| `page`     | int    | No       | `0`     | 0-based page number |
| `pageSize` | int    | No       | `50`    | Entries per page, max `200` |

**Example request** (search for "USER", page 0, 50 entries per page):
```
GET /audit/1.0/logs/search?search=VVNFUA==&page=0&pageSize=50
Authorization: Bearer <jwt_token>
```
(`VVNFUA==` is the Base64 encoding of `USER`)

**Success response — HTTP 200**:
```json
{
  "total": 142,
  "pageSize": 50,
  "totalPages": 3,
  "status": "ok",
  "logs": [
    {
      "actionMs": 1746518400000,
      "service": "USER",
      "action": "USER_CREATE",
      "owner": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
      "logStr": "A new user *** has been created by ***",
      "linkChain": true
    }
  ]
}
```

**Error responses**:
- HTTP `400` — invalid date range or malformed Base64 search parameter
  ```json
  { "status": "BADREQUEST", "status_code": 400, "message": "audit-search-invalid-date-range" }
  ```
- HTTP `403` — user not authenticated or missing required role

---

## Screen layout

### Overall structure
```
┌──────────────────────────────────────────────────────┐
│  🔍 Search bar          [From date]  [To date]  [🔎] │
├──────────────────────────────────────────────────────┤
│  Showing X–Y of TOTAL entries                        │
├────────────┬────────────┬──────────┬─────────────────┤
│  Date/Time │  Service   │  Action  │  Log message    │
├────────────┼────────────┼──────────┼─────────────────┤
│  ...       │  ...       │  ...     │  ...            │
├──────────────────────────────────────────────────────┤
│  [< Prev]      Page X of Y        [Next >]           │
└──────────────────────────────────────────────────────┘
```

### Search bar behaviour
- Single text input, placeholder: `Search by service, action or owner…`
- When the user submits (button click or Enter key), encode the input value with Base64 before calling the API
- Reset `page` to `0` on every new search or filter change
- The search input and date pickers should be part of the same form; all filters are applied together

### Date pickers
- Use a date-time picker component that returns a timestamp in milliseconds
- Display the selected dates in a human-readable local format (e.g. `YYYY-MM-DD HH:mm`)
- When empty, send `0` for the corresponding parameter
- Show a validation error when `startMs > endMs` before submitting (client-side guard)

### Results table columns

| Column     | Source field | Format | Notes |
|------------|-------------|--------|-------|
| Date/Time  | `actionMs`  | Local datetime (`YYYY-MM-DD HH:mm:ss`) | Sort: most recent first (enforced by backend) |
| Service    | `service`   | Plain text, badge/tag style | e.g. `USER`, `DEVICE` |
| Action     | `action`    | Plain text, monospace or badge | e.g. `USER_CREATE` |
| Owner      | `owner`     | Truncated hash with tooltip showing full value | Login hash |
| Log        | `logStr`    | Plain text — `***` for obfuscated params, clear text for ROLE_AUDIT_RD_CLEAR | May be long: allow text wrap |

### Pagination controls
- Display the current page number and total pages: `Page X of Y`
- Display the count range: `Showing X–Y of TOTAL entries`
- Previous / Next buttons — disable Previous on page 0, disable Next on last page
- Optional: direct page number input
- A page size selector (options: `25`, `50`, `100`, `200`) — changing it resets to page 0

### Empty state
- When `total = 0` and `status = "ok"`: display a neutral message — _"No audit log entries match your search criteria"_
- When `status = "audit-log-non-database"`: display a warning banner — i18n key `audit-log-non-database`

### Error state
- HTTP 400 with `audit-search-invalid-date-range`: display an inline validation error near the date pickers
- HTTP 400 with `audit-search-invalid-base64-param`: this should never happen if the front-end Base64-encodes correctly — display a generic error
- HTTP 403: redirect to an access-denied page

---

## Base64 encoding helper (TypeScript)

```typescript
/**
 * Encode a search string to Base64 for use as API query parameter.
 * Returns undefined when the input is empty or blank (no filter).
 */
function encodeSearchParam(value: string | null | undefined): string | undefined {
  if (!value || value.trim() === '') return undefined
  return btoa(unescape(encodeURIComponent(value.trim())))
}
```

> **Note**: Use `btoa(unescape(encodeURIComponent(value)))` to correctly handle non-ASCII characters.
> On the URL, pass the result as-is — no additional URL encoding is needed for standard Base64 characters.

---

## API call example (TypeScript / Nuxt.js)

API data strcuture

```typescript
/**
 * Audit Log trace
 */
export interface AuditResponse {
    /**
     * Timestamp of the audit log, ms since EPOCH
     * @example 178667672
     */
    actionMs: number;

    /**
     * Service Name
     * @example USER
     */
    service: string;

    /**
     * Action Name
     * @example USER_CREATE
     */
    action: string;

    /**
     * Owner
     * @example system
     */
    owner: string;

    /**
     * Log String
     * @example A new user xxxx has been created
     */
    logStr: string;

    /**
     * When true chained signature verification is valid (not yet implemented, always true)
     * @example true
     */
    linkChain: boolean;
}

/**
 * Audit log search response
 */
export interface AuditSearchResponse {
    /**
     * Total number of entries matching the search criteria
     * @example 150
     */
    total: number;

    /**
     * Number of elements per page returned
     * @example 50
     */
    pageSize: number;

    /**
     * Total number of pages available for the current search criteria
     * @example 3
     */
    totalPages: number;

    /**
     * Status of the search. 'ok' on success, 'audit-log-non-database' when no database backend is configured.
     * @example ok
     */
    status: string;

    /**
     * List of audit log entries for the requested page, ordered from most recent to oldest
     */
    logs: AuditResponse[];
}
```

Api Call example

```typescript
// Set the routes
const auditModuleSearchGet: string = '/audit/1.0/logs/search';

/**
 * List Audit logs with pagination and search
 */
auditModuleListAndSearch: async (
        search : string, // search string, will be base64 encoded
        startMs : number, // start date in ms
        endMs : number, // end date in ms
        page : number, // what result page to show
        pageSize : number, // how many result per page
): Promise<{ success?: AuditSearchResponse; error?: ActionResult | { message: string } }> => {
  try {
    const qs = new URLSearchParams();
    if (search.length > 0) qs.set('search', btoa(search));
    if (startMs !== 0)     qs.set('startMs', String(startMs));
    if (endMs !== 0)       qs.set('endMs',   String(endMs));
    if (page !== 0)        qs.set('page',     String(page));
    if (pageSize !== 0)    qs.set('pageSize', String(pageSize));
    const params = qs.size > 0 ? `?${qs.toString()}` : '';
    const response = await apiCallwithTimeout<AuditSearchResponse>(
            'GET',
            auditModuleSearchGet+params,
            undefined,
            false
    );
    return { success: response }
  } catch (error : any) {
    return { error };
  }
}

```

### Full implementation example

- [Data Structure](https://github.com/disk91/IoTowerFront/blob/main/app/types/audit.ts)
- [API call implementation](https://github.com/disk91/IoTowerFront/blob/main/app/plugins/api.backend.audit.ts)
- [Screen implementation](https://github.com/disk91/IoTowerFront/blob/main/app/pages/front/private/audit.vue)

---

## i18n keys used by this screen

```
"audit-log-non-database"            : "Audit logs are not stored in a database backend, search is unavailable",
"audit-search-invalid-date-range"   : "The start date must be before the end date",
"audit-search-invalid-base64-param" : "The search filter parameter is not valid Base64 encoded",
"audit-list-no-results"             : "No audit log entries match your search criteria",
"audit-list-showing"                : "Showing {from}–{to} of {total} entries",
"audit-list-page-of"                : "Page {current} of {total}",
"audit-list-search-placeholder"     : "Search by service, action or owner…",
"audit-list-col-datetime"           : "Date / Time",
"audit-list-col-service"            : "Service",
"audit-list-col-action"             : "Action",
"audit-list-col-owner"              : "Owner",
"audit-list-col-log"                : "Log message",
```

---

## Security considerations
- Always check roles (`ROLE_AUDIT_RD` or `ROLE_AUDIT_RD_CLEAR`) before rendering the page
- Never attempt to decode or decrypt the `***` obfuscated values on the client side
- The `owner` field is a login hash — never display it as a human-readable email without a dedicated lookup
- The `linkChain` field indicates whether the backend signature chain is valid (reserved for future use — always `true`)


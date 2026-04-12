---
name: skill_file_admin
description: A skill to search, view, update and delete any file in the system as an administrator. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Administer files as ROLE_FILES_ADMIN

## Overview
This skill allows you to integrate an admin file management panel in a front-end application. It is based on the
IoT Tower Control backend. The skill covers three closely related interactions for an authenticated administrator
holding the `ROLE_FILES_ADMIN` role:

1. **Search / List** all files in the system (paginated, with sorting and a text search filter).
2. **Update** the description, access type, short name and/or access key of any file.
3. **Delete** any file regardless of who owns it.

## When to use this skill
When you have been asked to build an administration screen where operators can browse, search, moderate or delete
any file uploaded to the platform, irrespective of the owner.

## How to use this skill
This skill helps to generate working code for the admin file management page. It contains examples in
TypeScript, made for Nuxt.js 4, but the code can be adapted to other frameworks.

The file [file_admin.md](assets/file_admin.md) contains the step-by-step workflow with all data
structures and API call examples. The list of [i18n keys](assets/i18n.md) is also provided.

## Principles of the admin file management page

### File search / list
The admin list view shows all files owned by any user:
- Files are returned as a paginated result (default page size: 50, maximum: 250).
- Default sort is by creation date, most recent first (`sort=CREATED`).
  An alternative sort by most downloaded (`sort=ACCESS`) is also available.
- An optional search string performs a case-insensitive `LIKE` filter on the owner login,
  the original filename and the description.
- Each entry displays: original filename, owner login, MIME category, size, access type, access count
  and creation date.
- A thumbnail preview is shown when `thumbnailUniqueName` is not null (IMAGE files only).
- The `accessKey` field is always visible to the admin.

### Update any file
The admin can update the following fields of any file regardless of ownership:
- `accessType` (`PUBLIC`, `CONNECTED` or `PRIVATE`) — required.
- `description` — optional, pass `null` or an empty string to clear it.
- `withShortName` — optional boolean:
  - `true` = generate a 6-character short name if none is assigned yet.
  - `false` = remove the current short name.
  - omit = leave the short name unchanged.
- `withAccessKey` — optional boolean:
  - `true` = generate/regenerate a 16-character access key.
  - `false` = remove the current access key.
  - omit = leave the access key unchanged.

### Delete any file
The admin can permanently delete any file regardless of ownership.
The backend removes both the database record and the physical file (plus its thumbnail) from disk.

## Workflow

The file [file_admin.md](assets/file_admin.md) contains the step-by-step workflow with all data
structures and API call examples. The list of [i18n keys](assets/i18n.md) is also provided.

### Step 1 — Load the paginated file list
- The front-end makes a `GET` request to `/files/1.0/admin/list`.
- Optional query parameters: `page` (default 0), `size` (default 50, max 250),
  `sort` (`CREATED` or `ACCESS`), `search` (free-text filter).
- The expected response is `200` with a `FileAdminListResponseItf` JSON object:
  ```json
  {
    "total": 142,
    "page": 0,
    "size": 50,
    "files": [ /* array of FileUploadResponseItf */ ]
  }
  ```
- When `files` is empty, display an empty-state message.
- Pagination controls are rendered using `total`, `page` and `size`.

### Step 2 — Select a file
- The operator clicks on a file entry to open its detail/edit panel.
- Use `shortName` (if present) or `uniqueName` from the list to build download and thumbnail URLs:
  - Download: `{BACKEND_API_BASE}/files/1.0/{fileRef}/full`
  - Thumbnail: `{BACKEND_API_BASE}/files/1.0/{fileRef}/thumbnail`
- `{fileRef}` is the file's `shortName` when present, otherwise its `uniqueName`.
- The admin always receives the `accessKey` value (if set) and can build shareable URLs
  by appending `?key=<accessKey>` to any download URL.

### Step 3 — Update metadata (optional)
- The operator changes the description, access type, short name or access key.
- The front-end makes a `PUT` request to `/files/1.0/admin/{fileRef}` with the update body.
- The expected response is `200` with the updated `FileUploadResponseItf`.
- The list entry is refreshed in place.
- Full request/response structures are detailed in [file_admin.md](assets/file_admin.md).

### Step 4 — Delete a file (optional)
- The operator clicks the delete button after confirming the action.
- The front-end makes a `DELETE` request to `/files/1.0/admin/{fileRef}`.
- The expected response is `200`.
- The file is removed from the current page; if the page becomes empty, navigate to the previous page.

### Step 5 — Handle errors
- `400`: Invalid `sort` value or malformed request body.
- `403`: Not authenticated, or missing `ROLE_FILES_ADMIN`.
- `50x`: Server error → display a generic error message.
- The complete list of i18n keys is available in [i18n.md](assets/i18n.md).

## API endpoints used

The API endpoints are relative to an API base URL provided via an environment variable.
In a Nuxt.js implementation, this variable is defined in the `nuxt.config.ts` file as follows:

```typescript
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      BACKEND_API_BASE: 'http://localhost:8091',  // Backend API base URL
    }
  },
})
```

### `GET /files/1.0/admin/list`
Return a paginated list of all files in the system.
- Requires `ROLE_FILES_ADMIN`.
- Query parameters:

| Parameter | Type    | Default   | Description                                                                   |
|-----------|---------|-----------|-------------------------------------------------------------------------------|
| `page`    | integer | `0`       | 0-based page index                                                            |
| `size`    | integer | `50`      | Page size (1–250; values outside this range are clamped)                      |
| `sort`    | string  | `CREATED` | `CREATED` (newest first) or `ACCESS` (most accessed first)                   |
| `search`  | string  | *(none)*  | Case-insensitive LIKE filter on owner login, original filename and description |

- Returns `200` with `FileAdminListResponseItf`.
- Returns `400` when `sort` is unrecognised.

### `PUT /files/1.0/admin/{fileId}`
Update the metadata of any file, bypassing ownership checks.
- Requires `ROLE_FILES_ADMIN`.
- `{fileId}` accepts either the `uniqueName` or the 6-character short name.
- Request body: same `FileUpdateBody` as the owner update endpoint.
- Returns `200` with the updated `FileUploadResponseItf`, or `403` when the file does not exist.
- Full request/response structures are detailed in [file_admin.md](assets/file_admin.md).

### `DELETE /files/1.0/admin/{fileId}`
Permanently delete any file, bypassing ownership checks.
- Requires `ROLE_FILES_ADMIN`.
- `{fileId}` accepts either the `uniqueName` or the 6-character short name.
- Returns `200` on success, `403` when the file does not exist.

## Response data structure — `FileAdminListResponseItf`
```json
{
  "total":  142,
  "page":   0,
  "size":   50,
  "files":  [
    {
      "uniqueName":           "550e8400-e29b-41d4-a716-446655440000-1712345678901.jpg",
      "originalName":         "photo.jpg",
      "description":          "Profile picture",
      "mimeCategory":         "IMAGE",
      "mimeType":             "image/jpeg",
      "size":                 204800,
      "ownerId":              "a3f2b1c9d4e5f6a7",
      "accessType":           "PRIVATE",
      "accessCount":          7,
      "createdAt":            1712345678901,
      "updatedAt":            1712345678901,
      "thumbnailUniqueName":  "550e8400-e29b-41d4-a716-446655440000-1712345678901-thumb.jpg",
      "shortName":            "aB3xYz",
      "accessKey":            "b4f2a8c1d7e3f9b5"
    }
  ]
}
```

## Authentication requirements
All admin endpoints require:
- A valid authenticated session / Bearer token.
- The `ROLE_FILES_ADMIN` role assigned to the authenticated user.

If authentication fails or the role is missing:
- HTTP `403` (Forbidden) is returned.
- The user should be redirected to the login page or shown an access-denied screen.
- Display the i18n key `files-admin-access-denied` to the user.

---
name: skill_file_list
description: A skill to list, view and manage the files owned by an identified user. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# List and manage files as an identified user

## Overview
This skill allows you to integrate a file management view in a front-end application. It is based on the
IoT Tower Control backend. The skill covers four closely related interactions for an authenticated user:

1. **List** the user's own files (sorted by upload date, most recent first).
2. **View** the metadata of a single file and build a download/thumbnail URL.
3. **Update** the description or access type of a file.
4. **Delete** a file and its thumbnail.

## When to use this skill
When you have been asked to build a file library, a media gallery or any screen where users can browse,
preview and manage files they have previously uploaded.

## How to use this skill
This skill helps to generate working code for the file management page. It contains examples in
TypeScript, made for Nuxt.js 4, but the code can be adapted to other frameworks.

## Principles of the file management page

### File list
The list view shows all files owned by the authenticated user:
- Files are sorted by creation date, most recent first.
- Each entry displays: original filename, MIME category, size, access type and creation date.
- A thumbnail preview is shown when `thumbnailUniqueName` is not null (IMAGE files only).
- When the list is empty, an empty-state message is displayed.

### File detail / preview
When the user selects a file:
- Metadata is loaded from `GET /files/1.0/{fileRef}/info`.
- A direct download link is built as `{BACKEND_API_BASE}/files/1.0/{fileRef}/full`.
- A thumbnail preview is built as `{BACKEND_API_BASE}/files/1.0/{fileRef}/thumbnail` when available.
- `{fileRef}` is the file's `shortName` when present, otherwise its `uniqueName`.
- For `PUBLIC` files these URLs can be embedded directly in HTML; for `CONNECTED` / `PRIVATE` files
  the Bearer token must be included when fetching programmatically.

### Update metadata
The owner can update the following fields of an existing file:
- `accessType` (`PUBLIC`, `CONNECTED` or `PRIVATE`) — required.
- `description` — optional, pass `null` or an empty string to clear it.
- `withShortName` — optional boolean:
  - `true` = generate a 6-character short name if none is assigned yet.
  - `false` = remove the current short name.
  - omit = leave the short name unchanged.
Upgrading to `PUBLIC` or `CONNECTED` requires the role `ROLE_FILE_WRITE`.

### Delete a file
The owner (or an administrator) can permanently delete a file.
The backend removes both the database record and the physical file (plus its thumbnail) from disk.

## Workflow

The file [file_list.md](assets/file_list.md) contains the step-by-step workflow with all data
structures and API call examples. The list of [i18n keys](assets/i18n.md) is also provided.

### Step 1 — Load the file list
- The front-end makes a `GET` request to `/files/1.0/list`.
- The expected response is `200` with a JSON array of `FileUploadResponseItf`.
- When the response is `204`, the list is empty: display an empty-state message.

### Step 2 — Select a file
- The user clicks on a file entry to open its detail/preview panel.
- Use `shortName` (if present) or `uniqueName` from the list to build download and thumbnail URLs directly —
  no extra API call is needed unless fresh metadata is required
  (in which case call `GET /files/1.0/{fileRef}/info`).

### Step 3 — Update metadata (optional)
- The user changes the description, the access type or the short name assignment in the detail panel.
- Send `withShortName: true` to generate a short name, `false` to remove it, or omit to leave it unchanged.
- The front-end makes a `PUT` request to `/files/1.0/{fileRef}` with the update body.
- The expected response is `200` with the updated `FileUploadResponseItf`.
- The file list entry is refreshed in place.

### Step 4 — Delete a file (optional)
- The user clicks the delete button after confirming the action.
- The front-end makes a `DELETE` request to `/files/1.0/{fileRef}`.
- The expected response is `200`.
- The file is removed from the list.

### Step 5 — Handle errors
- `403`: Not authenticated, or trying to modify/delete a file the user does not own.
- `400`: Invalid `accessType` value in the update body.
- `204`: Empty file list (not an error — show empty state).
- `50x`: Server error → display a generic error message.

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

### `GET /files/1.0/list`
List all files owned by the authenticated user, sorted by creation date descending.
- Requires authentication (Bearer token).
- Returns `200` with an array of `FileUploadResponseItf`, the list can be empty.
- Full data structures are detailed in [file_list.md](assets/file_list.md).

### `GET /files/1.0/{fileId}/info`
Get the metadata of a single file without serving its binary content.
- Requires authentication for `CONNECTED` and `PRIVATE` files; open for `PUBLIC` files.
- Returns `200` with a `FileUploadResponseItf`, or `403` when access is denied.
- Full data structures are detailed in [file_list.md](assets/file_list.md).

### `PUT /files/1.0/{fileRef}`
Update the `description`, `accessType` and/or `shortName` of an existing file.
- Requires authentication (Bearer token).
- Only the owner or `ROLE_FILE_ADMIN` can call this endpoint.
- Upgrading to `PUBLIC` or `CONNECTED` additionally requires `ROLE_FILE_WRITE`.
- Pass `withShortName: true` to create a short name, `false` to remove it, or omit to leave unchanged.
- Returns `200` with the updated `FileUploadResponseItf`.
- Full request/response structures are detailed in [file_list.md](assets/file_list.md).

### `DELETE /files/1.0/{fileId}`
Delete a file and its thumbnail permanently.
- Requires authentication (Bearer token).
- Only the owner or `ROLE_FILE_ADMIN` can call this endpoint.
- Returns `200` on success, `403` when access is denied.

## Authentication requirements
All endpoints require:
- A valid authenticated session/token.
- The Bearer token must be included in every request header.
- `PUT` additionally requires `ROLE_FILE_WRITE` when upgrading the access level to `PUBLIC` or `CONNECTED`.

If authentication fails:
- HTTP `403` (Forbidden) is returned.
- The user should be redirected to the login page.
- An appropriate error message should be displayed using i18n keys.


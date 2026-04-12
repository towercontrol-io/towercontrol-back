---
name: skill_file_upload
description: A skill to upload a file from a front-end application, as an identified user. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Upload a file as an identified user

## Overview
This skill allows you to integrate a file upload feature in a front-end application. It is based on the
IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms.
The backend handles MIME type detection, image resizing, thumbnail generation, quota enforcement and
integrity signature computation transparently. The front-end only needs to send the file and a few
metadata fields.

## When to use this skill
When you have been asked to add a file upload capability to the application — for example to let users
attach profile pictures, documents or any binary assets that should be stored and later retrieved by URL.

## How to use this skill
This skill helps to generate working code for a file upload form or drag-and-drop zone. It contains
examples in TypeScript, made for Nuxt.js 4, but the code can be adapted to other frameworks.

## Principles of file upload

### Access types
Every uploaded file must be assigned one of three access levels:
- `PRIVATE` — accessible only by the owner or an administrator. No special role required to upload.
- `CONNECTED` — accessible by any authenticated user. Requires the role `ROLE_FILES_WRITE`.
- `PUBLIC` — accessible by anyone, including unauthenticated visitors. Requires the role `ROLE_FILES_WRITE`.

The access type is chosen at upload time and can be changed later via the update endpoint.

### Accepted inputs
| Field           | Type    | Required | Description                                                                               |
|-----------------|---------|----------|-------------------------------------------------------------------------------------------|
| `file`          | binary  | yes      | The file to upload (any MIME type)                                                        |
| `accessType`    | string  | yes      | `PUBLIC`, `CONNECTED` or `PRIVATE`                                                        |
| `description`   | string  | no       | Optional human-readable label displayed alongside the file                                |
| `filename`      | string  | no       | Optional original filename                                                                |
| `withShortName` | boolean | no       | When `true`, the backend generates a unique 6-character short name for the file (default: `false`) |
| `withAccessKey` | boolean | no       | When `true`, the backend generates a 16-character access key enabling unauthenticated access (default: `false`) |

### Short name
The `shortName` is an optional 6-character alias (`[a-zA-Z0-9]`) that can be used in place of the
`uniqueName` in any file API URL. It is useful when embedding file links in QR codes, dashboards or
short URLs. Pass `withShortName=true` at upload time to have one generated automatically.
The short name is returned in the `shortName` field of the response (null when not requested).

### Access key
The `accessKey` is an optional 16-character token (`[a-z0-9]`) that can be appended as a `?key=` query
parameter to any file URL (download, thumbnail, info). When provided:
- `CONNECTED` files become accessible without logging in.
- `PRIVATE` files become accessible to anyone who holds the key.

Pass `withAccessKey=true` at upload time to have one generated. The key is returned in the `accessKey`
field of the response **only to the file owner or an administrator**; keep it as a shared secret.

### Quota limits
Uploads are rejected before any disk write when:
- The single file exceeds the maximum allowed file size (HTTP `400`).
- The user already owns the maximum number of files (HTTP `429`).
- Adding the new file would exceed the user's total storage quota (HTTP `429`).

### Image processing
When the uploaded file is an image (JPEG, PNG, GIF, WEBP…), the backend automatically:
1. Resizes the image proportionally if its largest dimension exceeds the configured maximum.
2. Generates a thumbnail whose largest dimension is bounded by the configured thumbnail size.

Both the resized image and the thumbnail are stored on disk and referenced in the returned metadata.
The `thumbnailUniqueName` field is populated only for images; it is `null` for other file types.

## Workflow of the file upload

The file [file_upload.md](assets/file_upload.md) contains the step-by-step workflow with all data
structures and API call examples. The list of [i18n keys](assets/i18n.md) is also provided.

### Step 1 — Build the multipart request
- The user selects a file (via a file input or a drag-and-drop zone).
- The front-end builds a `FormData` object with three fields: `file`, `accessType`, and optionally `description`.
- Basic client-side validation should check that a file is selected and that `accessType` is one of the three allowed values.

### Step 2 — Submit the upload
- The front-end posts the `FormData` to `/files/1.0/upload` with a `POST` request.
- The `Content-Type` header must be `multipart/form-data` (set automatically by the browser / fetch API when using `FormData`).
- The Bearer token must be included in the `Authorization` header.
- The expected response is `201` with the file metadata record in the body.

### Step 3 — Display confirmation
- When the upload succeeds, show the user a confirmation message.
- Use `uniqueName` (or `shortName` if one was assigned) to build download/thumbnail URLs:
  - Download: `/files/1.0/{uniqueName}/full`
  - Thumbnail: `/files/1.0/{uniqueName}/thumbnail` (only when `thumbnailUniqueName` is present)
- If `shortName` is returned in the response, it can be used as a compact alternative to `uniqueName` in any URL.

### Step 4 — Handle errors
- `400`: File is empty, MIME type is invalid, or a quota limit has been exceeded by size → display the i18n error message.
- `403`: The user is not authenticated or lacks `ROLE_FILES_WRITE` for PUBLIC/CONNECTED files.
- `429`: The user has reached their file count or total storage quota.
- `50x`: Server error → display a generic error message.

## API endpoint used

The API endpoint is relative to an API base URL that is provided via an environment variable.
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

### `POST /files/1.0/upload`
Upload a new file.
- Requires authentication (Bearer token).
- Requires `ROLE_FILES_WRITE` when `accessType` is `PUBLIC` or `CONNECTED`.
- The request body must be `multipart/form-data`.
- Returns `201` with a `FileUploadResponseItf` body on success.
- Full request/response structures are detailed in [file_upload.md](assets/file_upload.md).

## Authentication requirements
- The user must be authenticated (valid session/token).
- The Bearer token must be included in the `Authorization` header.
- Creating `PUBLIC` or `CONNECTED` files additionally requires the role `ROLE_FILES_WRITE`.

If authentication fails:
- HTTP `403` (Forbidden) is returned.
- The user should be redirected to the login page.
- An appropriate error message should be displayed using i18n keys.


## File Management module

This module is responsible for managing file uploads and downloads. It provides the following features:

- File upload via API with access rights management
- Local filesystem storage with configurable storage path
- MIME type detection and categorization (image, PDF, text, generic)
- Automatic image resizing to a configurable maximum resolution
- Automatic thumbnail generation for images
- Per-user storage quota enforcement (file count and total size)
- Per-file maximum size enforcement
- File integrity verification through cryptographic signature
- Access statistics (read count per file)
- 3-level directory tree storage for filesystem scalability

Recently used files are managed in a cache to limit database access, especially for files that are part of items such 
as documentation or as backgrounds on dashboards that could be displayed very frequently.

## File access rights

Each uploaded file is assigned an access type that controls who can retrieve it:

- `PUBLIC` - the file can be accessed by anyone, including unauthenticated requests
- `CONNECTED` - the file can only be accessed by authenticated (logged-in) users
- `PRIVATE` - the file can only be accessed by its owner or an administrator & support admin

The access type is set at upload time and can be updated by the owner or an administrator.

## File data structure

Each file is represented as a database record with the following structure:

```json
{
  "id": "string",                  // unique technical identifier (UUID)
  "uniqueName": "string",          // generated unique filename used for physical storage
  "originalName": "string",        // original filename provided at upload time
  "description": "string",         // optional human-readable description
  "mimeCategory": "enum",          // detected MIME category: IMAGE, PDF, TEXT, GENERIC
  "mimeType": "string",            // full detected MIME type (e.g. image/png)
  "size": "number",                // file size in bytes
  "ownerId": "string",             // login of the user who uploaded the file
  "accessType": "enum",            // access control: PUBLIC, CONNECTED, PRIVATE
  "accessCount": "number",         // number of times the file has been successfully downloaded
  "createdAt": "date",             // upload timestamp in MS since epoch
  "updatedAt": "date",             // last metadata update timestamp in MS since epoch
  "signature": "string",           // SHA-256 hex digest of the file content, computed at upload
  "noSignatureCheck": "boolean",   // when true, integrity verification is skipped on download; only an administrator can set this flag
  "thumbnailUniqueName": "string", // unique filename of the generated thumbnail (images only, null otherwise)
  "thumbnailSignature": "string",  // SHA-256 hex digest of the thumbnail content (images only, null otherwise)
  "shortName": "string",           // optional 6-character short alias ([a-zA-Z0-9]), null when not assigned
  "accessKey": "string",           // optional 16-character key ([a-z0-9]) enabling unauthenticated access to CONNECTED/PRIVATE files; null when disabled

  // --- in-memory only, never persisted ---
  "lastSignatureCheck": "long"     // timestamp (MS since epoch) of the last successful signature verification; held in the server-side file cache only
}
```

## Short name

By default each file is identified by its `uniqueName`, a long generated filename such as
`550e8400-e29b-41d4-a716-446655440000-1712345678901.jpg`. For use cases where a compact, human-friendly
identifier is needed (e.g. embedded in a URL, a QR code or a dashboard background), a **short name** can be
requested instead.

### Characteristics

- The short name is a **6-character** string drawn from the alphabet `[a-zA-Z0-9]` (62 characters), giving
  approximately 56 billion unique combinations.
- It is generated with a random generator.
- Uniqueness is verified in the database before the candidate is retained; up to 20 attempts are made before
  the operation is rejected with an error.
- A short name is **never assigned automatically**; it must be explicitly requested by the caller.
- A short name can be assigned at upload time (via the `withShortName=true` upload parameter) or at any later
  point through the update endpoint (via `withShortName: true` in the request body).
- A short name can be removed by passing `withShortName: false` in an update body.
- Once a short name is assigned it remains until it is explicitly removed; no expiry applies.

### Using a short name in API requests

Any API endpoint that accepts a `fileId` path variable (download, thumbnail, info, update, delete) performs
automatic detection:

- When the value is **exactly 6 characters** long it is treated as a **short name**.
- When it is longer it is treated as a **uniqueName**.

### Short name index in cache

The file cache maintains an in-memory `ConcurrentHashMap<String, String>` that maps each known short name to its
corresponding `uniqueName`. This index allows short-name lookups to be resolved to a cache key without a database
round-trip. The index is:

- populated lazily when a file with a short name is first loaded into the cache;
- cleaned up atomically when a file is flushed from the cache (e.g. on update or deletion);
- fully transparent to callers – the `getFileByShortName` method resolves via the index and falls back to the
  database on a miss.

On application restart the index is empty; the first short-name access after restart always triggers a database
lookup, after which the entry is registered in the index for subsequent requests.

## Access key

The **access key** is an optional mechanism that enables unauthenticated (or non-owner) access to `CONNECTED`
and `PRIVATE` files without requiring a login session. It is designed for use cases such as embedded previews,
shared links, QR codes or any context where the recipient may not have an account.

### Characteristics

- The access key is a **16-character** string drawn from the alphabet `[a-z0-9]` (36 characters), giving
  approximately 7.96 × 10²⁴ unique combinations (~82 bits of security).
- It is generated with a cryptographically secure random generator.
- An access key is **never assigned automatically**; it must be explicitly requested by the caller.
- An access key can be assigned at upload time (via the `withAccessKey=true` upload parameter) or later
  through the update endpoint (via `withAccessKey: true` in the request body).
- Passing `withAccessKey: true` in an update body **generates or regenerates** the key (existing key is replaced).
- An access key can be removed by passing `withAccessKey: false` in an update body.
- Once a key is assigned it remains active until explicitly removed or regenerated.

### Using an access key in API requests

The key is passed as a query parameter named `key` on the following endpoints:

```
GET /files/1.0/{fileId}/full?key=<accessKey>
GET /files/1.0/{fileId}/thumbnail?key=<accessKey>
```

When a valid key is provided, the normal authentication and ownership checks are bypassed:
- A `CONNECTED` file becomes accessible to unauthenticated callers.
- A `PRIVATE` file becomes accessible to any caller (authenticated or not) who holds the key.

`PUBLIC` files do not need a key (they are already freely accessible).

### Security considerations

- The access key is returned in the API response **only to the file owner or a ROLE_FILES_ADMIN**.
  Other callers (e.g. CONNECTED users accessing the info endpoint) will not see the key value.
- If a key is compromised, the owner can regenerate it (old key immediately becomes invalid) or remove it.
- The key should be treated as a shared secret and transmitted only over HTTPS.

## Unique filename generation

To guarantee uniqueness and avoid collisions, the physical filename stored on disk is generated as follows:

```
{id}-{epoch_ms_upload}.{original_extension}
```

- `id` is the UUID assigned to the file record
- `epoch_ms_upload` is the upload timestamp in milliseconds since epoch
- `original_extension` is the lowercase extension extracted from the original filename

Example: `550e8400-e29b-41d4-a716-446655440000-1712345678901.jpg`

This combination guarantees uniqueness because the UUID is unique by construction and the timestamp
disambiguates any theoretical race condition. The same scheme is used for thumbnails with a `-thumb` suffix
inserted before the extension:

```
{id}-{epoch_ms_upload}-thumb.{original_extension}
```

Example: `550e8400-e29b-41d4-a716-446655440000-1712345678901-thumb.jpg`

## Storage directory structure

Files are stored in a 3-level directory hierarchy under the root storage path defined by `files.storage.path`:

```
{files.storage.path}/
  {id[0]}/                  <- first character of the file id
    {id[1]}/                <- second character of the file id
      {uniqueName}          <- physical file
      {thumbnailUniqueName} <- thumbnail file (images only)
```

Example for id `550e8400-...`:

```
/var/iotower/files/
  5/
    5/
      550e8400-e29b-41d4-a716-446655440000-1712345678901.jpg
      550e8400-e29b-41d4-a716-446655440000-1712345678901-thumb.jpg
```

This structure spreads files across up to 256 leaf directories (16x16), preventing filesystem performance
degradation caused by too many entries in a single directory.

## MIME type detection and categorization

At upload time, the module inspects the file content (not just the extension) to determine the MIME type.
The detected MIME type is then mapped to one of four internal categories:

| Category  | Examples                                                      |
|-----------|---------------------------------------------------------------|
| `IMAGE`   | image/jpeg, image/png, image/gif, image/webp, image/svg+xml   |
| `PDF`     | application/pdf                                               |
| `TEXT`    | text/plain, text/csv, text/html, text/xml, application/json   |
| `GENERIC` | Everything else (zip, binary, office documents, etc.)         |

Only files whose detected category is `IMAGE` are eligible for resizing and thumbnail generation.

## Image processing

When an uploaded file is categorized as `IMAGE`, two automatic operations are triggered.

### Maximum size enforcement

If either the width or the height of the uploaded image exceeds `files.image.max.pixels` (taking the maximum
of width and height), the image is proportionally resized so that its largest dimension equals exactly
`files.image.max.pixels`. The aspect ratio is always preserved. The resized version replaces the original
before being written to disk.

### Thumbnail generation

A thumbnail is unconditionally generated for every image. Its largest dimension is bounded by
`files.image.thumbnail.pixels`. The thumbnail is stored alongside the original file in the same directory
with its own unique name and its own SHA-256 signature. Both names and signatures are recorded in the
file database record.

## Integrity signature

At upload time, a SHA-256 digest is computed over the final binary content of the file (after any resizing).
This digest is stored in the `signature` field of the database record. At every download, the digest of the
served content is recomputed and compared to the stored value. If they differ, the download is rejected and
an error is logged, as this indicates the file has been tampered with on disk.

The same mechanism applies independently to thumbnails through the `thumbnailSignature` field.
The key used for signing is defined in the configuration file  `files.signature.key`.

### Signature check interval

To avoid unnecessary CPU consumption for files that are accessed very frequently, the signature is not
re-verified on every single download. Instead, a minimum interval is enforced between two consecutive
verifications of the same file. This interval is defined by the `files.signature.check.interval.ms`
configuration parameter. The timestamp of the last successful verification is kept in memory (in the
server-side file cache) through the `lastSignatureCheck` field. This value is never persisted to the
database; it is reset to zero whenever the application restarts, which causes the first access after
a restart to always trigger a verification.

### Disabling signature verification

The `noSignatureCheck` flag can be set on a file to entirely skip integrity verification on download.
This is intended for low-risk files that are accessed at very high frequency, where the CPU cost of
repeated verification is not acceptable. Only an administrator (`ROLE_FILES_ADMIN`) is allowed to set
or clear this flag. Setting it on a file is recorded in the audit log.

## Quota management

Quotas are enforced at upload time before writing anything to disk:

- `files.quota.max.files.per.user` - maximum number of files a single user can own simultaneously.
  When this limit is reached, any new upload from that user is rejected with HTTP 429.
- `files.quota.max.total.bytes.per.user` - maximum cumulative size (in bytes) of all files owned by a
  user. When adding the new file would exceed this limit, the upload is rejected with HTTP 429.
- `files.quota.max.file.bytes` - absolute maximum size in bytes for a single uploaded file, regardless
  of the user quota. A file exceeding this limit is rejected immediately with HTTP 400.

## Configuration properties

The file service configuration file is named `files.properties` and is located in the `configuration`
directory at the root of the Java execution.

### List of properties

- `files.storage.path` : absolute path on the local filesystem where uploaded files are stored

- `files.image.max.pixels` : maximum allowed dimension (width or height, whichever is larger) in pixels
  for uploaded images; images exceeding this value are automatically resized to fit

- `files.image.thumbnail.pixels` : maximum dimension for generated image thumbnails in pixels

- `files.quota.max.files.per.user` : maximum number of files a single user may own; 0 means no limit

- `files.quota.max.total.bytes.per.user` : maximum cumulative file storage in bytes per user; 0 means no limit

- `files.quota.max.file.bytes` : maximum size in bytes for a single uploaded file; 0 means no limit

- `files.signature.secret` : secret key used for computing file integrity signatures; should be a long random string

- `files.signature.check.interval.ms` : minimum time in milliseconds between two consecutive signature verifications
  for the same file; once a file has been verified, it will not be re-verified until this interval has elapsed;
  a value of 0 disables the interval and enforces verification on every download


## API overview

### Upload a file

```
POST /files/1.0/upload
Content-Type: multipart/form-data
```

Accepts a `multipart/form-data` request with binary file and metadata as separate form fields:

| Field           | Type    | Required | Description                                                                  |
|-----------------|---------|----------|------------------------------------------------------------------------------|
| `file`          | binary  | yes      | The file to upload (sent as a `@RequestPart`)                                |
| `accessType`    | string  | yes      | Access control: `PUBLIC`, `CONNECTED` or `PRIVATE`                           |
| `description`   | string  | no       | Optional human-readable description of the file                              |
| `fileName`      | string  | no       | Optional original file name                                                  |
| `withShortName` | boolean | no       | When `true`, a unique 6-character short name is generated (default: `false`) |
| `withAccessKey` | boolean | no       | When `true`, a 16-character access key is generated enabling unauthenticated access (default: `false`) |

Example (curl):
```bash
curl -X POST https://host/files/1.0/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/photo.jpg" \
  -F "accessType=PRIVATE" \
  -F "description=My profile picture" \
  -F "fileName=photo.jpg" \
  -F "withShortName=true"
```

Returns the created file metadata record on success (HTTP 201), including the `shortName` field when generated.
Returns HTTP 400 on format or quota violation, HTTP 403 when the role is insufficient, HTTP 429 when user quota
is exceeded.

First, we will verify that the user has not reached their quotas in terms of maximum file size or the
number of stored files. When adding a file, you need to identify the file type. If it is an image, you
must check its size and resize it if necessary. You also need to create the corresponding channel.
An ID will be generated for the file so it can be saved, and the structure that will be stored in the
database will be completed. The signature is, of course, calculated at that point to validate the file’s
integrity later.

### Download a file

```
GET /files/1.0/{fileId}/full
```

Returns the binary content of the file with the appropriate Content-Type header and the original filename
in the Content-Disposition header. The `{fileId}` path variable accepts either the `uniqueName` or a
6-character short name. The access check is performed against the caller's authentication state
and the file's `accessType`. When the file has an `accessKey` configured, appending `?key=<accessKey>` to
the URL grants access without authentication (CONNECTED and PRIVATE files). Returns HTTP 403 when the file
does not exist or access is denied. The `accessCount` counter is incremented on every successful download.


### Download a thumbnail

```
GET /files/1.0/{fileId}/thumbnail
```

Returns the thumbnail image for the given file. The `{fileId}` path variable accepts either the `uniqueName`
or a 6-character short name. When the file has an `accessKey` configured, appending `?key=<accessKey>` grants
unauthenticated access. Returns HTTP 403 when the file has no thumbnail (not an image) or does not exist.
The `accessCount` counter of the original file is not incremented when downloading the thumbnail.

### Get file metadata

```
GET /files/1.0/{fileId}/info
```

Returns the file metadata record (including `shortName` when assigned) without serving the binary content.
The `{fileId}` path variable accepts either the `uniqueName` or a 6-character short name.
Access check is the same as for the download.

### Update file metadata

```
PUT /files/1.0/{fileId}
```

Allows the owner or an administrator to update the `description`, `accessType` and short name assignment.
The `{fileId}` path variable accepts either the `uniqueName` or a 6-character short name.

Request body fields:

| Field           | Type    | Required | Description                                                                                     |
|-----------------|---------|----------|-------------------------------------------------------------------------------------------------|
| `accessType`    | string  | yes      | New access type: `PUBLIC`, `CONNECTED` or `PRIVATE`                                             |
| `description`   | string  | no       | New description; pass null or empty to clear it                                                 |
| `withShortName` | boolean | no       | `true` = generate a short name if none exists; `false` = remove the current short name; `null`/omit = no change |
| `withAccessKey` | boolean | no       | `true` = generate/regenerate the access key; `false` = remove the access key; `null`/omit = no change |

### Delete a file

```
DELETE /files/1.0/{fileId}
```

Deletes the file record from the database and the physical file (and its thumbnail if any) from disk.
The `{fileId}` path variable accepts either the `uniqueName` or a 6-character short name.
Only the owner or an administrator can delete a file.

### List owned files

```
GET /files/1.0/list
```

Returns the list of files owned by the authenticated user, sorted by creation date descending.

## Roles

Access to files does not require any specific role for read or write access. However, it is possible 
to create private files without any role, but the `ROLE_FILES_WRITE` role will be required to create files that are 
public or connected. `ROLE_FILES_ADMIN` allows accessing and managing any file regardless of ownership and access type; typically assigned to administrators

## Admin API

The admin API is reserved to users holding the `ROLE_FILES_ADMIN` role. All endpoints are located under
`/files/1.0/admin/`.

### Paginated file search (admin)

```
GET /files/1.0/admin/list
```

Returns a paginated list of **all** files in the system (not filtered by ownership). Supports:

| Query parameter | Type    | Default   | Description                                                                      |
|-----------------|---------|-----------|----------------------------------------------------------------------------------|
| `page`          | integer | `0`       | 0-based page index                                                               |
| `size`          | integer | `50`      | Page size (1 to 250; values outside this range are silently clamped)             |
| `sort`          | string  | `CREATED` | Sort order: `CREATED` (newest first) or `ACCESS` (most accessed first)           |
| `search`        | string  | *(none)*  | Optional case-insensitive LIKE filter applied on owner login, filename and description |

Response body (`FileAdminListResponseItf`):

```json
{
  "total": 142,
  "page": 0,
  "size": 50,
  "files": [ /* array of FileUploadResponseItf */ ]
}
```

Returns HTTP 400 when the `sort` value is unrecognised, HTTP 403 when the role is insufficient.

### Update any file (admin)

```
PUT /files/1.0/admin/{fileId}
```

Same contract as the owner `PUT /files/1.0/{fileId}` endpoint but bypasses ownership restrictions.
Accepts the same `FileUpdateBody` request body. Returns the updated `FileUploadResponseItf`.

### Delete any file (admin)

```
DELETE /files/1.0/admin/{fileId}
```

Same contract as the owner `DELETE /files/1.0/{fileId}` endpoint but bypasses ownership restrictions.
Returns HTTP 200 on success.

> For both admin update and admin delete, the `{fileId}` path variable accepts either the `uniqueName`
> or the 6-character short name.

## Traceability

The following events are written to the audit log:

- File upload (owner, file id, size, access type)
- File deletion (caller, file id)
- Metadata update (caller, file id, changed fields)
- Integrity check failure (file id, expected signature, actual signature)
- Quota rejection (caller, reason)
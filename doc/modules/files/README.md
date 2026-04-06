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
  "thumbnailUniqueName": "string", // unique filename of the generated thumbnail (images only, null otherwise)
  "thumbnailSignature": "string"   // SHA-256 hex digest of the thumbnail content (images only, null otherwise)
}
```

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


## API overview

### Upload a file

```
POST /files/1.0/upload
```

Accepts a multipart/form-data request containing:
- the binary file part
- optional `description` field
- mandatory `accessType` field (`PUBLIC`, `CONNECTED`, `PRIVATE`)

Returns the created file record on success (HTTP 201). Returns HTTP 400 on format or quota violation,
HTTP 429 when user quota is exceeded.

First, we will verify that the user has not reached their quotas in terms of maximum file size or the 
number of stored files. When adding a file, you need to identify the file type. If it is an image, you 
must check its size and resize it if necessary. You also need to create the corresponding channel. 
An ID will be generated for the file so it can be saved, and the structure that will be stored in the 
database will be completed. The signature is, of course, calculated at that point to validate the file’s 
integrity later.

### Download a file

```
GET /files/1.0/{fileId}
```

Returns the binary content of the file with the appropriate Content-Type header and the original filename
in the Content-Disposition header. The access check is performed against the caller's authentication state
and the file's `accessType`. Returns HTTP 403 when the file does not exist, HTTP 403 when access is denied.
The `accessCount` counter is incremented on every successful download.


### Download a thumbnail

```
GET /files/1.0/{fileId}/thumbnail
```

Returns the thumbnail image for the given file. Returns HTTP 403 when the file has no thumbnail (not an image) or does 
not exist. As with the original file, permissions are checked to ensure that access is allowed. A 403 response will be 
returned if the permissions are not valid. The `accessCount` counter of the original file is not incremented when 
downloading the thumbnail.

### Get file metadata

```
GET /files/1.0/{fileId}/info
```

Returns the file metadata record without serving the binary content. Access check is the same as for the download.

### Update file metadata

```
PUT /files/1.0/{fileId}
```

Allows the owner or an administrator to update the `description` and `accessType` fields.

### Delete a file

```
DELETE /files/1.0/{fileId}
```

Deletes the file record from the database and the physical file (and its thumbnail if any) from disk.
Only the owner or an administrator can delete a file.

### List owned files

```
GET /files/1.0/list
```

Returns the list of files owned by the authenticated user, sorted by creation date descending.

## Roles

Access to files does not require any specific role for read or write access. However, it is possible 
to create private files without any role, but the `ROLE_FILE_WRITE` role will be required to create files that are 
public or connected. `ROLE_FILE_ADMIN` allows accessing and managing any file regardless of ownership and access type; typically assigned to administrators

## Traceability

The following events are written to the audit log:

- File upload (owner, file id, size, access type)
- File deletion (caller, file id)
- Metadata update (caller, file id, changed fields)
- Integrity check failure (file id, expected signature, actual signature)
- Quota rejection (caller, reason)
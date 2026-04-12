## Implementation guide for the file upload feature

## Step by step communication with the backend

### Step 1 — Build the FormData

Create a `FormData` object and append the upload fields.
`accessType` must be one of `PUBLIC`, `CONNECTED` or `PRIVATE`.
`description` is optional.
`withShortName` is optional — set it to `'true'` to request a 6-character short name alias.

```typescript
const formData = new FormData();
formData.append('file', selectedFile);           // File object from <input type="file"> or drag-and-drop
formData.append('accessType', 'PRIVATE');         // or 'CONNECTED' / 'PUBLIC'
formData.append('description', 'My document');   // optional
formData.append('filename', 'mydocument.pdf');   // optional
formData.append('withShortName', 'true');         // optional — request a 6-char short name
formData.append('withAccessKey', 'true');         // optional — request a 16-char unauthenticated access key
```

### Step 2 — Submit the upload to the backend

`POST` the `FormData` to `/files/1.0/upload`.
Do **not** set the `Content-Type` header manually — the browser sets it automatically with the correct
multipart boundary when using `FormData`.

#### Data structures

```typescript
/** Possible file access levels */
export type FileAccessType = 'PUBLIC' | 'CONNECTED' | 'PRIVATE';

/** Possible MIME categories returned by the backend */
export type FileMimeCategory = 'IMAGE' | 'PDF' | 'TEXT' | 'GENERIC';

/** Metadata returned after a successful upload (also used for info and list endpoints) */
export interface FileUploadResponseItf {
    /** Unique technical identifier (UUID) */
    id: string;

    /** Generated unique filename used for physical storage */
    uniqueName: string;

    /** Original filename provided at upload time */
    originalName: string;

    /** Optional human-readable description */
    description?: string;

    /** Detected MIME category */
    mimeCategory: FileMimeCategory;

    /** Full detected MIME type (e.g. "image/jpeg") */
    mimeType: string;

    /** File size in bytes after any resizing */
    size: number;

    /** Login hash of the user who uploaded the file */
    ownerId: string;

    /** Access control type */
    accessType: FileAccessType;

    /** Number of times the file has been successfully downloaded */
    accessCount: number;

    /** Upload timestamp in milliseconds since epoch */
    createdAt: number;

    /** Last metadata update timestamp in milliseconds since epoch */
    updatedAt: number;

    /**
     * Unique filename of the generated thumbnail.
     * Only present for IMAGE files; null otherwise.
     */
    thumbnailUniqueName?: string;

    /**
     * Optional 6-character short name alias ([a-zA-Z0-9]).
     * Only present when withShortName=true was passed at upload time.
     * Can be used in place of uniqueName in any file API URL.
     */
    shortName?: string;

    /**
     * Optional 16-character access key ([a-z0-9]).
     * Only returned to the file owner or an administrator.
     * Append as ?key=<value> to any file URL to grant unauthenticated access to CONNECTED/PRIVATE files.
     */
    accessKey?: string;
}

export interface ActionResult {
    /** Result status string */
    status: string;

    /** HTTP status code */
    status_code: number;

    /** i18n-ready message key */
    message: string;
}
```

#### Example API call (TypeScript / Nuxt.js 4)

```typescript
const filesModuleUploadPost: string = '/files/1.0/upload';

/**
 * Upload a new file (private API — authenticated user)
 * @param file          - File object to upload
 * @param accessType    - Access level: PUBLIC, CONNECTED or PRIVATE
 * @param description   - Optional description
 * @param withShortName - When true, a 6-character short name is generated
 * @param withAccessKey - When true, a 16-character access key is generated
 */
filesModuleUpload: async (
    file: File,
    accessType: FileAccessType,
    description?: string,
    withShortName?: boolean,
    withAccessKey?: boolean
): Promise<{ success?: FileUploadResponseItf; error?: ActionResult | { message: string } }> => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('accessType', accessType);
        if (description) formData.append('description', description);
        if (withShortName) formData.append('withShortName', 'true');
        if (withAccessKey) formData.append('withAccessKey', 'true');

        const response = await apiCallMultipartWithTimeout<FileUploadResponseItf>(
            'POST',
            filesModuleUploadPost,
            formData
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Response from the API

| HTTP code | Meaning                                                     |
|-----------|-------------------------------------------------------------|
| `201`     | Upload succeeded — body contains `FileUploadResponseItf`    |
| `400`     | Empty file, invalid `accessType`, file too large, I/O error |
| `403`     | Not authenticated or missing `ROLE_FILES_WRITE`             |
| `429`     | User has reached file count or total storage quota          |
| `50x`     | Server-side error                                           |

On error, the body contains an `ActionResult` with an i18n key in the `message` field.

### Step 3 — Build download and thumbnail URLs

After a successful upload, build file URLs from the `uniqueName` (or the `shortName` if one was generated):

```typescript
const config = useRuntimeConfig();
const base = config.public.BACKEND_API_BASE;

// Prefer the short name when available (compact URL), fall back to uniqueName
const fileRef = uploadedFile.shortName ?? uploadedFile.uniqueName;

// Direct file download URL
const downloadUrl  = `${base}/files/1.0/${fileRef}/full`;

// Thumbnail URL (only valid when thumbnailUniqueName is present)
const thumbnailUrl = uploadedFile.thumbnailUniqueName
    ? `${base}/files/1.0/${fileRef}/thumbnail`
    : null;

// When an accessKey was generated, append it to enable unauthenticated sharing
const sharedDownloadUrl = uploadedFile.accessKey
    ? `${base}/files/1.0/${fileRef}/full?key=${uploadedFile.accessKey}`
    : downloadUrl;
```

For `PUBLIC` files, these URLs can be embedded directly in `<img>`, `<a>` or other HTML elements
without an `Authorization` header.
For `CONNECTED` and `PRIVATE` files, the Bearer token must be included when fetching the URL
programmatically (e.g. via `fetch`).

### Step 4 — Display confirmation or error

On success:
- Show a success toast / notification using the i18n key `file-upload-success`.
- Optionally display the thumbnail using the `thumbnailUrl` built above.
- Refresh the file list if one is visible on the page.

On error:
- Read the `message` field from the `ActionResult` response body.
- Display the translated message using your i18n library.
- The list of i18n keys is available in [i18n.md](i18n.md).


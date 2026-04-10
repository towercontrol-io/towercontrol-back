## Implementation guide for the file list and management feature

## Data structures

```typescript
/** Possible file access levels */
export type FileAccessType = 'PUBLIC' | 'CONNECTED' | 'PRIVATE';

/** Possible MIME categories returned by the backend */
export type FileMimeCategory = 'IMAGE' | 'PDF' | 'TEXT' | 'GENERIC';

/** Metadata of a stored file — returned by list, info and update endpoints */
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
     * Present only when explicitly requested at upload or update time.
     * Can be used in place of uniqueName in any file API URL.
     */
    shortName?: string;
}

/** Body sent to PUT /files/1.0/{fileId} */
export interface FileUpdateBody {
    /** New access level — mandatory */
    accessType: FileAccessType;

    /** New description — optional, send empty string to clear */
    description?: string;

    /**
     * Short name management:
     * - true  = generate a short name if none is assigned yet
     * - false = remove the existing short name
     * - omit  = leave the short name unchanged
     */
    withShortName?: boolean;
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

## Step by step communication with the backend

### Step 1 — Load the file list

`GET /files/1.0/list` — returns the full list of files owned by the authenticated user.

```typescript
const filesModuleListGet: string = '/files/1.0/list';

/**
 * List files owned by the authenticated user
 */
filesModuleList: async (): Promise<{
    success?: FileUploadResponseItf[];
    empty?: boolean;
    error?: ActionResult | { message: string }
}> => {
    try {
        // 204 means the list is empty — handle before parsing JSON
        const response = await apiCallwithTimeout<FileUploadResponseItf[]>(
            'GET',
            filesModuleListGet,
            null,
            false
        );
        return { success: response };
    } catch (error: any) {
        if (error?.status_code === 204) return { empty: true, success: [] };
        return { error };
    }
},
```

#### Response

| HTTP code | Meaning                                               |
|-----------|-------------------------------------------------------|
| `200`     | Array of `FileUploadResponseItf` (may be empty array) |
| `403`     | Not authenticated                                     |

### Step 2 — Build download and thumbnail URLs

Once you have a `FileUploadResponseItf`, build the URLs from `uniqueName` (or `shortName` when present):

```typescript
const config = useRuntimeConfig();
const base = config.public.BACKEND_API_BASE;

function getFileRef(file: FileUploadResponseItf): string {
    // Prefer the short name (compact URL) when available
    return file.shortName ?? file.uniqueName;
}

function getDownloadUrl(file: FileUploadResponseItf): string {
    return `${base}/files/1.0/${getFileRef(file)}/full`;
}

function getThumbnailUrl(file: FileUploadResponseItf): string | null {
    return file.thumbnailUniqueName ? `${base}/files/1.0/${getFileRef(file)}/thumbnail` : null;
}
```

For `PUBLIC` files, these URLs can be used directly in `<img src>` or `<a href>` without headers.
For `CONNECTED` and `PRIVATE` files, always fetch programmatically with the Bearer token:

```typescript
async function fetchPrivateFile(fileRef: string): Promise<Blob> {
    const config = useRuntimeConfig();
    const url = `${config.public.BACKEND_API_BASE}/files/1.0/${fileRef}/full`;
    const token = useAuthStore().token; // adapt to your auth implementation
    const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` }
    });
    if (!response.ok) throw new Error(`file-not-found`);
    return response.blob();
}
```

### Step 3 — Get fresh metadata for a single file (optional)

`GET /files/1.0/{fileId}/info` — useful when you need up-to-date metadata without re-fetching the list.

```typescript
/**
 * Get metadata of a single file
 */
filesModuleInfo: async (fileId: string): Promise<{
    success?: FileUploadResponseItf;
    error?: ActionResult | { message: string }
}> => {
    try {
        const response = await apiCallwithTimeout<FileUploadResponseItf>(
            'GET',
            `/files/1.0/${fileId}/info`,
            null,
            false
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

### Step 4 — Update file metadata

`PUT /files/1.0/{fileId}` — update `accessType` and/or `description`.

```typescript
/**
 * Update the description and/or access type of a file
 */
filesModuleUpdate: async (
    fileId: string,
    body: FileUpdateBody
): Promise<{ success?: FileUploadResponseItf; error?: ActionResult | { message: string } }> => {
    try {
        const response = await apiCallwithTimeout<FileUploadResponseItf>(
            'PUT',
            `/files/1.0/${fileId}`,
            body,
            false
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Response

| HTTP code | Meaning                                                    |
|-----------|------------------------------------------------------------|
| `200`     | Update succeeded — body contains updated `FileUploadResponseItf` |
| `400`     | Invalid `accessType` value                                 |
| `403`     | Not authenticated, not the owner, or missing `ROLE_FILE_WRITE` |

### Step 5 — Delete a file

`DELETE /files/1.0/{fileId}` — permanently removes the file and its thumbnail.

```typescript
/**
 * Delete a file
 */
filesModuleDelete: async (
    fileId: string
): Promise<{ success?: boolean; error?: ActionResult | { message: string } }> => {
    try {
        await apiCallwithTimeout<void>(
            'DELETE',
            `/files/1.0/${fileId}`,
            null,
            false
        );
        return { success: true };
    } catch (error: any) {
        return { error };
    }
},
```

#### Response

| HTTP code | Meaning                                            |
|-----------|----------------------------------------------------|
| `200`     | File deleted successfully                          |
| `403`     | Not authenticated or not the owner                 |

### Step 6 — Display errors

On any error, read the `message` field from the `ActionResult` response body and display the
translated message using your i18n library.
The complete list of i18n keys is available in [i18n.md](i18n.md).


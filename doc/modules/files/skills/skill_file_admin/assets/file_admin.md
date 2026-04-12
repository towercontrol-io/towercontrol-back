## Implementation guide for the admin file management feature

## Data structures

```typescript
/** Possible file access levels */
export type FileAccessType = 'PUBLIC' | 'CONNECTED' | 'PRIVATE';

/** Possible MIME categories returned by the backend */
export type FileMimeCategory = 'IMAGE' | 'PDF' | 'TEXT' | 'GENERIC';

/** Metadata of a stored file — returned by list, info, update and admin endpoints */
export interface FileUploadResponseItf {
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
     * Can be used in place of uniqueName in any file API URL.
     */
    shortName?: string;

    /**
     * Optional 16-character access key ([a-z0-9]).
     * Always visible to the admin.
     * Append as ?key=<value> to any file URL to grant unauthenticated access to CONNECTED/PRIVATE files.
     */
    accessKey?: string;
}

/** Paginated response returned by GET /files/1.0/admin/list */
export interface FileAdminListResponseItf {
    /** Total number of files matching the search criteria */
    total: number;

    /** Current page index (0-based) */
    page: number;

    /** Number of records per page */
    size: number;

    /** Files on this page */
    files: FileUploadResponseItf[];
}

/** Sort options for the admin list endpoint */
export type FileAdminSortOrder = 'CREATED' | 'ACCESS';

/** Body sent to PUT /files/1.0/admin/{fileId} */
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

    /**
     * Access key management:
     * - true  = generate (or regenerate) a 16-character access key
     * - false = remove the existing access key
     * - omit  = leave unchanged
     */
    withAccessKey?: boolean;
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

### Step 1 — Search / load the paginated file list

`GET /files/1.0/admin/list` — returns a paginated list of all files in the system.

All query parameters are optional.

| Parameter | Type    | Default   | Description                                                                   |
|-----------|---------|-----------|-------------------------------------------------------------------------------|
| `page`    | integer | `0`       | 0-based page index                                                            |
| `size`    | integer | `50`      | Page size (1–250)                                                             |
| `sort`    | string  | `CREATED` | `CREATED` (newest first) or `ACCESS` (most accessed first)                   |
| `search`  | string  | *(none)*  | Case-insensitive LIKE filter on owner login, original filename and description |

```typescript
const filesAdminListGet: string = '/files/1.0/admin/list';

/**
 * Load a page of files as an admin, with optional search and sort
 */
filesAdminList: async (params?: {
    page?: number;
    size?: number;
    sort?: FileAdminSortOrder;
    search?: string;
}): Promise<{
    success?: FileAdminListResponseItf;
    error?: ActionResult | { message: string };
}> => {
    try {
        // Build query string from provided parameters
        const query = new URLSearchParams();
        if (params?.page !== undefined)  query.set('page',   String(params.page));
        if (params?.size !== undefined)  query.set('size',   String(params.size));
        if (params?.sort)                query.set('sort',   params.sort);
        if (params?.search?.trim())      query.set('search', params.search.trim());

        const url = query.toString()
            ? `${filesAdminListGet}?${query}`
            : filesAdminListGet;

        const response = await apiCallwithTimeout<FileAdminListResponseItf>(
            'GET',
            url,
            null,
            false
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Response

| HTTP code | Meaning                                                        |
|-----------|----------------------------------------------------------------|
| `200`     | `FileAdminListResponseItf` (files array may be empty)          |
| `400`     | Invalid `sort` value                                           |
| `403`     | Not authenticated or missing `ROLE_FILE_ADMIN`                 |

### Step 2 — Build download and thumbnail URLs

Once you have a `FileUploadResponseItf`, build the URLs using `shortName` (preferred) or `uniqueName`:

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
    return file.thumbnailUniqueName
        ? `${base}/files/1.0/${getFileRef(file)}/thumbnail`
        : null;
}

/**
 * Build a shareable unauthenticated URL when an access key is present.
 * Works for CONNECTED and PRIVATE files.
 */
function getShareableUrl(file: FileUploadResponseItf): string | null {
    return file.accessKey
        ? `${getDownloadUrl(file)}?key=${file.accessKey}`
        : null;
}
```

As an admin, the `accessKey` field is always returned and can be used to build shareable links or
to revoke/regenerate keys.

### Step 3 — Update any file metadata

`PUT /files/1.0/admin/{fileId}` — update any file, bypassing ownership checks.

```typescript
/**
 * Admin update of any file's metadata
 */
filesAdminUpdate: async (
    fileRef: string,
    body: FileUpdateBody
): Promise<{ success?: FileUploadResponseItf; error?: ActionResult | { message: string } }> => {
    try {
        const response = await apiCallwithTimeout<FileUploadResponseItf>(
            'PUT',
            `/files/1.0/admin/${fileRef}`,
            body,
            false
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Request body examples

Change access type only:
```json
{ "accessType": "PRIVATE" }
```

Generate a short name and remove the access key:
```json
{
    "accessType":    "PUBLIC",
    "description":   "Reviewed by admin",
    "withShortName": true,
    "withAccessKey": false
}
```

#### Response

| HTTP code | Meaning                                                          |
|-----------|------------------------------------------------------------------|
| `200`     | Update succeeded — body contains updated `FileUploadResponseItf` |
| `400`     | Invalid `accessType` value                                       |
| `403`     | Not authenticated, file not found, or missing `ROLE_FILE_ADMIN`  |

### Step 4 — Delete any file

`DELETE /files/1.0/admin/{fileId}` — permanently removes the file and its thumbnail.

```typescript
/**
 * Admin deletion of any file regardless of ownership
 */
filesAdminDelete: async (
    fileRef: string
): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
    try {
        const response = await apiCallwithTimeout<ActionResult>(
            'DELETE',
            `/files/1.0/admin/${fileRef}`,
            null,
            false
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Response

| HTTP code | Meaning                                                  |
|-----------|----------------------------------------------------------|
| `200`     | File deleted successfully                                |
| `403`     | Not authenticated, file not found, or missing `ROLE_FILE_ADMIN` |

### Step 5 — Handle errors

On any error, read the `message` field from the `ActionResult` response body and display the
translated message using your i18n library.
The complete list of i18n keys is available in [i18n.md](i18n.md).

```typescript
function handleApiError(error: ActionResult | { message: string }): string {
    // Translate the i18n message key using your i18n library (e.g. useI18n().t)
    const { t } = useI18n();
    return t(error.message) ?? t('file-admin-generic-error');
}
```

### Step 6 — Pagination helper

```typescript
/**
 * Navigate to a specific page while preserving current search/sort state.
 * @param currentParams - current query state (sort, search)
 * @param page          - target page index (0-based)
 * @param size          - current page size
 */
async function loadPage(
    currentParams: { sort?: FileAdminSortOrder; search?: string },
    page: number,
    size: number
): Promise<FileAdminListResponseItf | null> {
    const result = await filesAdminList({ ...currentParams, page, size });
    if (result.error) {
        console.error(handleApiError(result.error));
        return null;
    }
    return result.success ?? null;
}
```


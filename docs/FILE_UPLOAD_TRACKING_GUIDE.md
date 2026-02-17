# File Upload Tracking System Guide

## 📋 Overview

This comprehensive file upload tracking system monitors all file uploads to S3 through presigned URLs. It provides complete visibility into:

- **Batch Operations**: Track groups of files uploaded together
- **Individual Files**: Monitor each file's upload status
- **User Activity**: Know who uploaded what and when
- **Success/Failure Tracking**: Detailed error reporting for failed uploads
- **Historical Data**: Query upload history with powerful filters
- **Statistics Dashboard**: Overview of upload performance

## 🗄️ Database Schema

### Table: `file_upload_batch`
Tracks batch-level upload operations.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key (auto-increment) |
| `batchId` | VARCHAR(64) | Unique batch identifier (UUID) |
| `userId` | BIGINT | User ID who initiated the upload |
| `userEmail` | VARCHAR(100) | User email at upload time |
| `userName` | VARCHAR(256) | User name at upload time |
| `userRole` | VARCHAR(256) | User role at upload time |
| `folderType` | VARCHAR(50) | Folder type (dataset, benchmark-eval, etc.) |
| `folderPath` | VARCHAR(500) | S3 folder path for this batch |
| `totalFiles` | INT | Total number of files in batch |
| `successfulFiles` | INT | Number of successfully uploaded files |
| `failedFiles` | INT | Number of failed uploads |
| `pendingFiles` | INT | Number of pending uploads |
| `batchStatus` | VARCHAR(20) | Batch status (see below) |
| `expirationTime` | BIGINT | Presigned URL expiration (milliseconds) |
| `requestTimezone` | VARCHAR(50) | Client timezone at request time |
| `requestIp` | VARCHAR(50) | Client IP address |
| `userAgent` | VARCHAR(500) | Client user agent |
| `notes` | TEXT | Additional notes or metadata |
| `createTime` | DATETIME | Batch creation time (UTC) |
| `updateTime` | DATETIME | Last update time (UTC) |
| `completionTime` | DATETIME | Batch completion time (UTC) |
| `isDelete` | TINYINT | Soft delete flag |

**Batch Status Values:**
- `PENDING`: URLs generated, no uploads started yet
- `IN_PROGRESS`: Some files uploaded, others pending
- `COMPLETED`: All files uploaded successfully
- `FAILED`: All files failed to upload
- `PARTIAL_SUCCESS`: Some files succeeded, some failed

**Indexes:**
- `UNIQUE idx_batchId` on `batchId`
- `idx_userId` on `userId`
- `idx_createTime` on `createTime`
- `idx_batchStatus` on `batchStatus`
- `idx_folderType` on `folderType`
- `idx_userEmail` on `userEmail`

### Table: `file_upload_record`
Tracks individual file upload operations.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key (auto-increment) |
| `batchId` | VARCHAR(64) | Associated batch ID (FK) |
| `userId` | BIGINT | User ID who initiated the upload |
| `originalFileName` | VARCHAR(500) | Original file name from user |
| `sanitizedFileName` | VARCHAR(500) | Sanitized file name (S3-safe) |
| `s3Key` | VARCHAR(1000) | Full S3 object key (path + filename) |
| `s3Bucket` | VARCHAR(255) | S3 bucket name |
| `fileSize` | BIGINT | File size in bytes (after upload) |
| `contentType` | VARCHAR(100) | MIME content type |
| `presignedUrl` | TEXT | Generated presigned URL (reference) |
| `uploadStatus` | VARCHAR(20) | Upload status (see below) |
| `uploadMethod` | VARCHAR(50) | Upload method (PRESIGNED_URL, DIRECT, etc.) |
| `errorMessage` | TEXT | Error message if upload failed |
| `errorCode` | VARCHAR(100) | Error code if upload failed |
| `httpStatusCode` | INT | HTTP status code from S3 response |
| `retryCount` | INT | Number of retry attempts |
| `urlExpirationTime` | DATETIME | Presigned URL expiration time |
| `requestTime` | DATETIME | Presigned URL request time (UTC) |
| `uploadStartTime` | DATETIME | Upload start time (UTC) |
| `uploadCompletionTime` | DATETIME | Upload completion time (UTC) |
| `updateTime` | DATETIME | Last update time (UTC) |
| `clientTimezone` | VARCHAR(50) | Client timezone |
| `metadata` | TEXT | Additional metadata (JSON) |
| `isDelete` | TINYINT | Soft delete flag |

**Upload Status Values:**
- `PENDING`: Presigned URL generated, not uploaded yet
- `SUCCESS`: Upload completed successfully
- `FAILED`: Upload failed
- `EXPIRED`: Presigned URL expired before upload

**Indexes:**
- `idx_batchId` on `batchId`
- `idx_userId` on `userId`
- `idx_uploadStatus` on `uploadStatus`
- `idx_requestTime` on `requestTime`
- `idx_s3Key` on `s3Key(255)`
- `idx_originalFileName` on `originalFileName(255)`

## 🔄 Upload Flow

### Step 1: Frontend Requests Presigned URLs

**Endpoint:** `POST /api/s3/folder/presigned-upload-url/benchmark-eval/batch`

**Request:**
```json
{
  "fileNames": [
    "experiment_results.csv",
    "model_predictions.json",
    "evaluation_report.pdf"
  ],
  "expirationTime": 3600000,
  "clientTimezone": "America/New_York"
}
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "batchId": "550e8400-e29b-41d4-a716-446655440000",
    "entries": [
      {
        "fileName": "experiment_results.csv",
        "sanitizedFileName": "experiment_results.csv",
        "presignedUrl": "https://s3.amazonaws.com/bucket/..."
      },
      {
        "fileName": "model_predictions.json",
        "sanitizedFileName": "model_predictions.json",
        "presignedUrl": "https://s3.amazonaws.com/bucket/..."
      },
      {
        "fileName": "evaluation_report.pdf",
        "sanitizedFileName": "evaluation_report.pdf",
        "presignedUrl": "https://s3.amazonaws.com/bucket/..."
      }
    ]
  },
  "message": "ok"
}
```

**What Happens in Backend:**
1. Validates user is logged in (Sa-Token)
2. Generates unique `batchId` (UUID)
3. Creates folder path: `labOS/benchmark-eval/{userId}/`
4. Sanitizes each file name for S3 safety
5. Generates presigned URLs for each file
6. **Saves batch record** to `file_upload_batch` table
7. **Saves file records** to `file_upload_record` table (status: PENDING)
8. Returns batchId and presigned URLs to frontend

### Step 2: Frontend Uploads Files to S3

Frontend uses the presigned URLs to upload files directly to S3:

```javascript
// For each file
const response = await fetch(presignedUrl, {
  method: 'PUT',
  body: file,
  headers: {
    'Content-Type': file.type
  }
});
```

### Step 3: Frontend Reports Upload Status

After each upload completes (or fails), frontend calls:

**Endpoint:** `POST /api/upload-tracking/update-status`

**Success Example:**
```json
{
  "batchId": "550e8400-e29b-41d4-a716-446655440000",
  "sanitizedFileName": "experiment_results.csv",
  "uploadStatus": "SUCCESS",
  "fileSize": 1048576,
  "httpStatusCode": 200,
  "clientTimezone": "America/New_York"
}
```

**Failure Example:**
```json
{
  "batchId": "550e8400-e29b-41d4-a716-446655440000",
  "sanitizedFileName": "model_predictions.json",
  "uploadStatus": "FAILED",
  "errorMessage": "Network timeout",
  "errorCode": "NETWORK_ERROR",
  "httpStatusCode": 408,
  "clientTimezone": "America/New_York"
}
```

**What Happens in Backend:**
1. Validates user is logged in
2. Updates the specific file record in `file_upload_record`
   - Sets `uploadStatus` to SUCCESS or FAILED
   - Records `fileSize` (if success)
   - Records `errorMessage` and `errorCode` (if failed)
   - Sets `uploadCompletionTime` to current time
3. Recalculates batch statistics:
   - Counts successful, failed, and pending files
   - Updates `file_upload_batch` counts
4. Updates batch status:
   - All successful → `COMPLETED`
   - All failed → `FAILED`
   - Mixed → `PARTIAL_SUCCESS`
   - Still pending → `IN_PROGRESS`
5. If all files done, sets `completionTime`

## 📊 Query APIs

### 1. Get My Upload History (Paginated)

**Endpoint:** `POST /api/upload-tracking/my-uploads`

**Request:**
```json
{
  "current": 1,
  "pageSize": 10,
  "sortField": "createTime",
  "sortOrder": "descend",
  "batchStatus": "COMPLETED",
  "folderType": "benchmark-eval",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "keyword": "experiment"
}
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "batchId": "550e8400-e29b-41d4-a716-446655440000",
        "userName": "John Doe",
        "userEmail": "john@example.com",
        "folderType": "benchmark-eval",
        "folderPath": "labOS/benchmark-eval/123/",
        "totalFiles": 3,
        "successfulFiles": 3,
        "failedFiles": 0,
        "pendingFiles": 0,
        "batchStatus": "COMPLETED",
        "successRate": 100.0,
        "createTime": "2024-12-24T10:30:00Z",
        "completionTime": "2024-12-24T10:35:00Z",
        "totalDurationSeconds": 300
      }
    ],
    "total": 42,
    "size": 10,
    "current": 1,
    "pages": 5
  },
  "message": "ok"
}
```

**Query Parameters:**
- `current`: Page number (1-based)
- `pageSize`: Items per page
- `sortField`: Field to sort by
- `sortOrder`: "ascend" or "descend"
- `batchStatus`: Filter by status (optional)
- `folderType`: Filter by folder type (optional)
- `startDate`: Filter by date range (optional)
- `endDate`: Filter by date range (optional)
- `keyword`: Search in file names (optional)

### 2. Get Batch Detail

**Endpoint:** `GET /api/upload-tracking/batch/{batchId}`

**Response:**
```json
{
  "code": 0,
  "data": {
    "batchId": "550e8400-e29b-41d4-a716-446655440000",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "folderType": "benchmark-eval",
    "folderPath": "labOS/benchmark-eval/123/",
    "totalFiles": 3,
    "successfulFiles": 2,
    "failedFiles": 1,
    "pendingFiles": 0,
    "batchStatus": "PARTIAL_SUCCESS",
    "successRate": 66.67,
    "createTime": "2024-12-24T10:30:00Z",
    "completionTime": "2024-12-24T10:35:00Z",
    "totalDurationSeconds": 300,
    "fileRecords": [
      {
        "id": 1,
        "originalFileName": "experiment_results.csv",
        "sanitizedFileName": "experiment_results.csv",
        "s3Key": "labOS/benchmark-eval/123/experiment_results.csv",
        "fileSize": 1048576,
        "fileSizeFormatted": "1.00 MB",
        "contentType": "text/csv",
        "uploadStatus": "SUCCESS",
        "errorMessage": null,
        "requestTime": "2024-12-24T10:30:00Z",
        "uploadCompletionTime": "2024-12-24T10:32:00Z",
        "durationSeconds": 120,
        "retryCount": 0
      },
      {
        "id": 2,
        "originalFileName": "model_predictions.json",
        "sanitizedFileName": "model_predictions.json",
        "s3Key": "labOS/benchmark-eval/123/model_predictions.json",
        "fileSize": null,
        "fileSizeFormatted": null,
        "contentType": "application/json",
        "uploadStatus": "FAILED",
        "errorMessage": "Network timeout",
        "requestTime": "2024-12-24T10:30:00Z",
        "uploadCompletionTime": "2024-12-24T10:33:00Z",
        "durationSeconds": 180,
        "retryCount": 1
      },
      {
        "id": 3,
        "originalFileName": "evaluation_report.pdf",
        "sanitizedFileName": "evaluation_report.pdf",
        "s3Key": "labOS/benchmark-eval/123/evaluation_report.pdf",
        "fileSize": 2097152,
        "fileSizeFormatted": "2.00 MB",
        "contentType": "application/pdf",
        "uploadStatus": "SUCCESS",
        "errorMessage": null,
        "requestTime": "2024-12-24T10:30:00Z",
        "uploadCompletionTime": "2024-12-24T10:35:00Z",
        "durationSeconds": 300,
        "retryCount": 0
      }
    ]
  },
  "message": "ok"
}
```

### 3. Get Upload Statistics (Dashboard)

**Endpoint:** `GET /api/upload-tracking/statistics`

**Response:**
```json
{
  "code": 0,
  "data": {
    "totalBatches": 42,
    "totalFiles": 256,
    "successfulUploads": 240,
    "failedUploads": 10,
    "pendingUploads": 6,
    "overallSuccessRate": 93.75,
    "totalStorageBytes": 10737418240,
    "totalStorageFormatted": "10.00 GB",
    "datasetFilesCount": 128,
    "benchmarkEvalFilesCount": 128
  },
  "message": "ok"
}
```

## 🎯 Use Cases

### Use Case 1: Monitor Active Uploads
Query batches with status `IN_PROGRESS` to see which uploads are currently happening.

```json
POST /api/upload-tracking/my-uploads
{
  "current": 1,
  "pageSize": 20,
  "batchStatus": "IN_PROGRESS"
}
```

### Use Case 2: Debug Failed Uploads
Find all failed batches and inspect error messages:

```json
POST /api/upload-tracking/my-uploads
{
  "current": 1,
  "pageSize": 20,
  "batchStatus": "FAILED"
}
```

Then get details:
```
GET /api/upload-tracking/batch/{batchId}
```

### Use Case 3: Track Upload Performance
View statistics dashboard to see overall success rates and storage usage:

```
GET /api/upload-tracking/statistics
```

### Use Case 4: Search Files by Name
Find all uploads containing "experiment":

```json
POST /api/upload-tracking/my-uploads
{
  "current": 1,
  "pageSize": 20,
  "keyword": "experiment"
}
```

### Use Case 5: Audit Trail
View all uploads by a user in a date range:

```json
POST /api/upload-tracking/my-uploads
{
  "current": 1,
  "pageSize": 100,
  "startDate": "2024-12-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z"
}
```

## 🔍 Timezone Handling

All timestamps in the database are stored in **UTC** (`DATETIME` type in MySQL).

### Request Timezone
When making requests, you can optionally include `clientTimezone`:

```json
{
  "fileNames": ["test.csv"],
  "clientTimezone": "America/New_York"
}
```

This is stored in the database for reference and logging purposes. The backend stores:
- `requestTimezone` in batch table
- `clientTimezone` in file record table

### Response Timestamps
All timestamps in API responses are in **UTC ISO 8601 format**:
```
"createTime": "2024-12-24T10:30:00Z"
```

Frontend should convert to user's local timezone for display.

## 🛡️ Security

### Authentication
All endpoints require Sa-Token authentication:
- User must be logged in
- Token must be valid
- User can only access their own records

### Authorization
- Users can only query their own upload history
- Batch detail endpoint verifies user owns the batch
- Admins could be given access to all records (future enhancement)

### Data Privacy
- Presigned URLs are stored but expire after defined time
- Sensitive user information (email) is included for audit purposes
- IP addresses and user agents are recorded for security tracking

## 📈 Performance Considerations

### Indexes
All critical query paths are indexed:
- `userId` for user-specific queries
- `batchId` for batch lookups
- `createTime` for time-based queries
- `batchStatus` for status filtering
- `folderType` for type filtering

### Pagination
Always use pagination for list queries:
- Default page size: 10
- Maximum recommended: 100
- Large result sets are automatically paginated

### Query Optimization
- Batch queries don't load file records by default
- File records only loaded in detail view
- Statistics use aggregation queries
- Soft deletes (`isDelete`) are filtered automatically

## 🔧 Maintenance

### Cleanup Old Records
Consider implementing periodic cleanup of:
- Expired pending uploads (presigned URLs expired)
- Old completed batches (after 90+ days)
- Failed uploads with no retry (after 30 days)

### Monitoring
Monitor these metrics:
- Pending upload count (should decrease over time)
- Failed upload rate (should be low)
- Batch completion time (should be reasonable)
- Storage growth rate

## 📝 Example Frontend Implementation

```javascript
class UploadTracker {
  async uploadFiles(files) {
    // Step 1: Get presigned URLs
    const response = await fetch('/api/s3/folder/presigned-upload-url/benchmark-eval/batch', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({
        fileNames: files.map(f => f.name),
        clientTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone
      })
    });
    
    const { data } = await response.json();
    const { batchId, entries } = data;
    
    // Step 2: Upload each file
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const entry = entries[i];
      
      try {
        // Upload to S3
        await fetch(entry.presignedUrl, {
          method: 'PUT',
          body: file,
          headers: {
            'Content-Type': file.type
          }
        });
        
        // Report success
        await this.reportStatus(batchId, entry.sanitizedFileName, {
          uploadStatus: 'SUCCESS',
          fileSize: file.size,
          httpStatusCode: 200
        });
        
      } catch (error) {
        // Report failure
        await this.reportStatus(batchId, entry.sanitizedFileName, {
          uploadStatus: 'FAILED',
          errorMessage: error.message,
          errorCode: 'UPLOAD_ERROR',
          httpStatusCode: 500
        });
      }
    }
    
    return batchId;
  }
  
  async reportStatus(batchId, sanitizedFileName, status) {
    await fetch('/api/upload-tracking/update-status', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({
        batchId,
        sanitizedFileName,
        clientTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        ...status
      })
    });
  }
  
  async getUploadHistory(page = 1, filters = {}) {
    const response = await fetch('/api/upload-tracking/my-uploads', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({
        current: page,
        pageSize: 20,
        ...filters
      })
    });
    
    return await response.json();
  }
  
  async getStatistics() {
    const response = await fetch('/api/upload-tracking/statistics', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });
    
    return await response.json();
  }
}
```

## 🎉 Summary

This tracking system provides:

✅ **Complete Visibility**: Track every upload from request to completion  
✅ **Batch Management**: Group related files together  
✅ **Error Tracking**: Detailed failure information for debugging  
✅ **Historical Queries**: Powerful filtering and pagination  
✅ **Statistics Dashboard**: Overview of upload performance  
✅ **Timezone Support**: Proper handling of time zones  
✅ **Security**: Authentication and authorization built-in  
✅ **Performance**: Optimized with indexes and pagination  

The system is production-ready and provides everything needed to monitor and manage file uploads at scale! 🚀


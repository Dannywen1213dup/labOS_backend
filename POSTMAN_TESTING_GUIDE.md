# Postman 测试指南 - AWS S3 文件夹管理接口

本指南介绍如何使用 Postman 测试新的 AWS S3 文件夹管理接口。

## 基础配置

### 服务器地址
- **Base URL**: `http://localhost:8101/api`

### 请求头设置
所有请求都需要设置：
- `Content-Type`: `application/json`

---

## 接口测试

### 1. 创建上传文件夹

**接口说明**：根据用户 UUID 创建上传文件夹，自动管理文件夹结构。

**请求方式**：`POST`

**URL**：`http://localhost:8101/api/s3/folder/create`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "uuid": "user123"
}
```

**响应示例**（成功）：
```json
{
  "code": 0,
  "data": {
    "folderPath": "labOS/user123/11092025/1/",
    "uuid": "user123",
    "date": "11092025",
    "count": 1
  },
  "message": ""
}
```

**说明**：
- 文件夹结构：`labOS/{uuid}/{MMDDYYYY}/{count}/`
- 如果当天第一次上传，`count` 为 1
- 如果当天已有上传，`count` 会自动递增
- 系统会自动创建不存在的 UUID 文件夹和日期文件夹

**测试步骤**：
1. 在 Postman 中创建新请求
2. 选择 POST 方法
3. 输入 URL：`http://localhost:8101/api/s3/folder/create`
4. 在 Headers 标签页添加：`Content-Type: application/json`
5. 在 Body 标签页选择 raw，选择 JSON 格式
6. 输入请求体（修改 uuid 为你的测试 UUID）
7. 点击 Send 发送请求
8. 查看返回的文件夹路径信息

---

### 2. 查询上传进度

**接口说明**：查询指定文件夹中已上传的文件数量和文件列表。

**请求方式**：`POST`

**URL**：`http://localhost:8101/api/s3/folder/progress`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "folderPath": "labOS/user123/11092025/1"
}
```

**响应示例**（成功）：
```json
{
  "code": 0,
  "data": {
    "folderPath": "labOS/user123/11092025/1/",
    "fileCount": 3,
    "files": [
      "labOS/user123/11092025/1/file1.pdf",
      "labOS/user123/11092025/1/file2.jpg",
      "labOS/user123/11092025/1/file3.txt"
    ]
  },
  "message": ""
}
```

**说明**：
- `fileCount`：文件夹中的文件数量
- `files`：文件列表（包含完整路径）
- 如果文件夹不存在，返回错误信息

**测试步骤**：
1. 创建新的 POST 请求
2. URL：`http://localhost:8101/api/s3/folder/progress`
3. Headers：`Content-Type: application/json`
4. Body：使用上一步创建文件夹返回的 `folderPath`
5. 点击 Send
6. 查看文件数量和文件列表

---

### 3. 下载文件夹（获取 ZIP 下载链接）

**接口说明**：获取文件夹的 ZIP 打包下载链接（预签名 URL，有效期 1 小时）。

**请求方式**：`POST`

**URL**：`http://localhost:8101/api/s3/folder/download`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "folderPath": "labOS/user123/11092025/1"
}
```

**响应示例**（成功）：
```json
{
  "code": 0,
  "data": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/downloads/labOS_user123_11092025_1_.zip?AWSAccessKeyId=AKIAQM4CE4G62VMHPYEA&Expires=1699564800&Signature=xxxxx",
  "message": ""
}
```

**说明**：
- 返回的 URL 是预签名的临时链接，有效期 1 小时
- 浏览器可以直接访问此 URL 下载 ZIP 文件
- ZIP 文件包含文件夹下的所有文件
- 如果文件夹不存在，返回错误信息

**测试步骤**：
1. 创建新的 POST 请求
2. URL：`http://localhost:8101/api/s3/folder/download`
3. Headers：`Content-Type: application/json`
4. Body：使用要下载的文件夹路径
5. 点击 Send
6. 复制响应中的 URL
7. 在浏览器中打开该 URL，即可下载 ZIP 文件

---

### 4. 删除文件夹

**接口说明**：删除指定文件夹及其所有内容。

**请求方式**：`POST`

**URL**：`http://localhost:8101/api/s3/folder/delete`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "folderPath": "labOS/user123/11092025/1"
}
```

**响应示例**（成功）：
```json
{
  "code": 0,
  "data": true,
  "message": ""
}
```

**说明**：
- 此操作会删除文件夹及其所有文件
- 删除操作不可逆，请谨慎使用
- 如果文件夹不存在，返回错误信息

**测试步骤**：
1. 创建新的 POST 请求
2. URL：`http://localhost:8101/api/s3/folder/delete`
3. Headers：`Content-Type: application/json`
4. Body：使用要删除的文件夹路径
5. 点击 Send
6. 查看返回结果（true 表示删除成功）

---

### 5. 批量上传文件（新增）

**接口说明**：批量上传多个文件到指定文件夹，支持自动创建文件夹或上传到已存在的文件夹。

**请求方式**：`POST`

**URL**：`http://localhost:8101/api/s3/folder/batch-upload`

**请求头**：
```
Content-Type: multipart/form-data
```

**请求参数**：
- `files` (文件数组，必须): 要上传的多个文件
- `uuid` (字符串，必须): 用户 UUID
- `folderPath` (字符串，可选): 目标文件夹路径

**场景 1：自动创建新文件夹上传**

在 Postman 中：
1. 选择 Body 标签页
2. 选择 `form-data`
3. 添加参数：
   - Key: `files`, Type: File，选择多个文件（可点击 "Select Files" 多次选择）
   - Key: `uuid`, Type: Text, Value: `user123`

**场景 2：上传到指定文件夹**

在 Postman 中：
1. 选择 Body 标签页
2. 选择 `form-data`
3. 添加参数：
   - Key: `files`, Type: File，选择多个文件
   - Key: `uuid`, Type: Text, Value: `user123`
   - Key: `folderPath`, Type: Text, Value: `labOS/user123/11092025/3`

**响应示例**（成功）：
```json
{
  "code": 0,
  "data": {
    "folderPath": "labOS/user123/11092025/3/",
    "successCount": 3,
    "failCount": 0,
    "successFiles": [
      "labOS/user123/11092025/3/document1.pdf",
      "labOS/user123/11092025/3/image1.jpg",
      "labOS/user123/11092025/3/data.xlsx"
    ],
    "failedFiles": [],
    "folderInfo": {
      "folderPath": "labOS/user123/11092025/3/",
      "uuid": "user123",
      "date": "11092025",
      "count": 3
    }
  },
  "message": ""
}
```

**响应示例**（部分失败）：
```json
{
  "code": 0,
  "data": {
    "folderPath": "labOS/user123/11092025/3/",
    "successCount": 2,
    "failCount": 1,
    "successFiles": [
      "labOS/user123/11092025/3/document1.pdf",
      "labOS/user123/11092025/3/image1.jpg"
    ],
    "failedFiles": [
      "corrupted.zip (上传失败: Invalid file format)"
    ],
    "folderInfo": {
      "folderPath": "labOS/user123/11092025/3/",
      "uuid": "user123",
      "date": "11092025",
      "count": 3
    }
  },
  "message": ""
}
```

**说明**：
- 支持同时上传多个文件
- 如果不提供 `folderPath`，系统会自动创建新的文件夹（次数自动递增）
- 如果提供 `folderPath`，会上传到指定的文件夹（如果文件夹不存在会自动创建）
- 返回每个文件的上传状态（成功/失败）
- 即使部分文件上传失败，也会返回成功上传的文件信息

**测试步骤**：
1. 在 Postman 中创建新请求
2. 选择 POST 方法
3. 输入 URL：`http://localhost:8101/api/s3/folder/batch-upload`
4. 在 Body 标签页选择 `form-data`
5. 添加 `files` 参数（类型选择 File），可以多次添加 `files` 选择多个文件
6. 添加 `uuid` 参数（类型 Text），输入用户 UUID
7. （可选）添加 `folderPath` 参数指定目标文件夹
8. 点击 Send 发送请求
9. 查看返回结果，确认文件上传状态

**重要提示**：
- 在 Postman 中，要上传多个文件，需要多次添加名为 `files` 的参数
- 每次点击 "Select Files" 可以选择一个或多个文件
- 文件会保持原始文件名上传到 S3

---

## 错误代码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40400 | 请求数据不存在（文件夹不存在）|
| 50000 | 系统内部异常 |
| 50001 | 操作失败 |

**错误响应示例**：
```json
{
  "code": 40400,
  "data": null,
  "message": "文件夹不存在"
}
```

---

## 完整测试流程

### 测试场景 1：使用批量上传接口（推荐）

1. **批量上传文件（自动创建文件夹）**
   ```
   POST /api/s3/folder/batch-upload
   Form-data:
   - files: [选择多个文件]
   - uuid: testuser001
   预期结果: 自动创建 labOS/testuser001/11092025/1/ 并上传文件
   ```

2. **查询上传进度**
   ```
   POST /api/s3/folder/progress
   Body: {"folderPath": "labOS/testuser001/11092025/1"}
   预期结果: 返回文件数量和文件列表
   ```

3. **下载文件夹**
   ```
   POST /api/s3/folder/download
   Body: {"folderPath": "labOS/testuser001/11092025/1"}
   预期结果: 返回下载 URL
   ```

### 测试场景 2：首次上传流程（手动创建文件夹）

1. **创建文件夹**
   ```
   POST /api/s3/folder/create
   Body: {"uuid": "testuser001"}
   预期结果: 返回 labOS/testuser001/11092025/1/
   ```

2. **批量上传文件到指定文件夹**
   ```
   POST /api/s3/folder/batch-upload
   Form-data:
   - files: [选择多个文件]
   - uuid: testuser001
   - folderPath: labOS/testuser001/11092025/1
   预期结果: 文件上传到指定文件夹
   ```

3. **查询上传进度**
   ```
   POST /api/s3/folder/progress
   Body: {"folderPath": "labOS/testuser001/11092025/1"}
   预期结果: 返回文件数量和文件列表
   ```

4. **下载文件夹**
   ```
   POST /api/s3/folder/download
   Body: {"folderPath": "labOS/testuser001/11092025/1"}
   预期结果: 返回下载 URL
   ```

5. **测试下载**
   - 复制返回的 URL 到浏览器，验证能否下载 ZIP 文件

### 测试场景 2：同一天多次上传

1. **第一次创建**
   ```
   POST /api/s3/folder/create
   Body: {"uuid": "testuser002"}
   预期结果: count = 1
   ```

2. **第二次创建**
   ```
   POST /api/s3/folder/create
   Body: {"uuid": "testuser002"}
   预期结果: count = 2
   ```

3. **第三次创建**
   ```
   POST /api/s3/folder/create
   Body: {"uuid": "testuser002"}
   预期结果: count = 3
   ```

### 测试场景 3：错误处理

1. **查询不存在的文件夹**
   ```
   POST /api/s3/folder/progress
   Body: {"folderPath": "labOS/nonexistent/11092025/1"}
   预期结果: 返回 40400 错误
   ```

2. **下载不存在的文件夹**
   ```
   POST /api/s3/folder/download
   Body: {"folderPath": "labOS/nonexistent/11092025/1"}
   预期结果: 返回 40400 错误
   ```

3. **删除不存在的文件夹**
   ```
   POST /api/s3/folder/delete
   Body: {"folderPath": "labOS/nonexistent/11092025/1"}
   预期结果: 返回 40400 错误
   ```

4. **使用错误的文件夹格式**
   ```
   POST /api/s3/folder/progress
   Body: {"folderPath": "wrongformat/path"}
   预期结果: 返回 40000 参数错误
   ```

---

## Postman Collection 导入

您可以创建一个 Postman Collection 包含所有接口：

1. 在 Postman 中点击 "Import"
2. 选择 "Raw text"
3. 复制下面的 JSON 配置
4. 点击 "Import"

```json
{
  "info": {
    "name": "AWS S3 Folder Management API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "创建上传文件夹",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"uuid\": \"testuser001\"\n}"
        },
        "url": {
          "raw": "http://localhost:8101/api/s3/folder/create",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8101",
          "path": ["api", "s3", "folder", "create"]
        }
      }
    },
    {
      "name": "查询上传进度",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"folderPath\": \"labOS/testuser001/11092025/1\"\n}"
        },
        "url": {
          "raw": "http://localhost:8101/api/s3/folder/progress",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8101",
          "path": ["api", "s3", "folder", "progress"]
        }
      }
    },
    {
      "name": "下载文件夹",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"folderPath\": \"labOS/testuser001/11092025/1\"\n}"
        },
        "url": {
          "raw": "http://localhost:8101/api/s3/folder/download",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8101",
          "path": ["api", "s3", "folder", "download"]
        }
      }
    },
    {
      "name": "删除文件夹",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"folderPath\": \"labOS/testuser001/11092025/1\"\n}"
        },
        "url": {
          "raw": "http://localhost:8101/api/s3/folder/delete",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8101",
          "path": ["api", "s3", "folder", "delete"]
        }
      }
    }
  ]
}
```

---

## 注意事项

1. **启动应用前**：确保已正确配置 AWS S3 凭证（application.yml）
2. **网络访问**：确保服务器能够访问 AWS S3 服务
3. **权限配置**：AWS IAM 用户需要有 S3 的读写权限
4. **预签名 URL**：下载链接有效期为 1 小时，过期后需要重新获取
5. **文件夹格式**：必须严格按照 `labOS/{uuid}/{MMDDYYYY}/{count}` 格式
6. **日期格式**：日期格式为 MMDDYYYY（月日年），例如 11092025 表示 2025年11月9日

---

## 常见问题

### Q: 如何测试文件上传？
A: 您可以先使用 AWS S3 Console 手动上传文件到创建的文件夹，然后使用进度查询接口验证文件是否上传成功。

### Q: 下载链接打不开？
A: 检查：
1. 链接是否已过期（有效期 1 小时）
2. AWS 凭证是否正确
3. Bucket 权限配置是否正确

### Q: 文件夹路径格式要求？
A: 严格格式：`labOS/{uuid}/{MMDDYYYY}/{count}`
- 不需要开头的斜杠
- 可以带或不带结尾的斜杠
- 日期必须是 8 位数字（MMDDYYYY）
- count 必须是数字

### Q: 如何验证删除成功？
A: 删除后，再次查询该文件夹的进度，应该返回 "文件夹不存在" 的错误。

---

## 技术支持

如有问题，请联系开发团队或查看项目文档。

**作者**: Yifan Wen  
**网站**: https://www.ai4labos.com/


# Postman 测试指南 - 认证系统 V2.0

## 📋 目录

1. [环境配置](#环境配置)
2. [测试流程](#测试流程)
3. [接口详细测试](#接口详细测试)
4. [常见问题](#常见问题)

---

## 环境配置

### 1. 创建 Postman 环境

点击 Postman 右上角的 "Environments" → "+" 创建新环境，命名为 `labOS-Local`

### 2. 配置环境变量

| 变量名 | 初始值 | 当前值 | 说明 |
|--------|--------|--------|------|
| `baseUrl` | `http://localhost:8101/api` | - | API 基础地址 |
| `satoken` | (留空) | (自动填充) | 登录后的 Token |
| `userId` | (留空) | (自动填充) | 用户 ID |
| `userEmail` | `test@example.com` | - | 测试邮箱 |
| `verificationCode` | (留空) | (手动填充) | 注册验证码 |
| `resetToken` | (留空) | (手动填充) | 重置密码 Token |

### 3. 保存环境

点击 "Save" 保存环境，并在右上角选择 `labOS-Local` 环境

---

## 测试流程

### 流程 A：新用户注册并上传文件

```
1. 发送验证码 → 查收邮件/日志
2. 注册用户 → 自动登录获取 Token
3. 生成上传 URL → 上传文件
4. 退出登录
```

### 流程 B：已有用户登录并上传文件

```
1. 用户登录 → 获取 Token
2. 生成上传 URL → 上传文件
3. 退出登录
```

### 流程 C：忘记密码并重置

```
1. 请求重置密码 → 查收邮件/日志
2. 重置密码 → 所有会话登出
3. 使用新密码登录
```

---

## 接口详细测试

### 1️⃣ 发送注册验证码

**接口信息**
- **名称**: Send Registration Code
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/send-code`
- **需要 Token**: ❌ 否

**Headers**
```
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "email": "{{userEmail}}"
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": {
    "email": "test@example.com",
    "message": "Verification code has been sent. Please check your email."
  },
  "message": "ok"
}
```

**失败响应 - 邮箱已注册**
```json
{
  "code": 40000,
  "data": null,
  "message": "Email is already registered. Please login or reset your password."
}
```

**Tests 脚本** (自动保存响应)
```javascript
// 测试响应状态
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 测试响应格式
pm.test("Response has correct format", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("code");
    pm.expect(jsonData).to.have.property("message");
});

// 如果成功，打印提示
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ 验证码已发送！请检查邮箱或服务器日志");
        console.log("📧 邮箱:", jsonData.data.email);
    }
}
```

**操作步骤**
1. 在 Postman 创建新请求，命名为 "1. Send Registration Code"
2. 设置为 POST 方法
3. URL 填入: `{{baseUrl}}/auth/send-code`
4. Headers 添加: `Content-Type: application/json`
5. Body 选择 "raw" 和 "JSON"，填入上面的请求体
6. 切换到 "Tests" 标签，粘贴上面的脚本
7. 点击 "Send" 发送请求
8. **查看服务器日志获取验证码**，搜索: `Verification code generated for registration`
9. 复制验证码到环境变量 `verificationCode` 中

---

### 2️⃣ 注册新用户

**接口信息**
- **名称**: Register New User
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/register`
- **需要 Token**: ❌ 否

**Headers**
```
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "email": "{{userEmail}}",
  "password": "password123",
  "confirmPassword": "password123",
  "code": "{{verificationCode}}",
  "firstName": "John",
  "lastName": "Doe",
  "legalAccepted": true
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isLogin": true,
    "loginId": "1751234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1751234567890,
      "userName": "John Doe",
      "userRole": "user",
      "createTime": "2024-01-01T10:00:00",
      "updateTime": "2024-01-01T10:00:00"
    }
  },
  "message": "ok"
}
```

**失败响应 - 验证码错误**
```json
{
  "code": 40000,
  "data": null,
  "message": "Verification code is incorrect"
}
```

**失败响应 - 密码不一致**
```json
{
  "code": 40000,
  "data": null,
  "message": "Passwords do not match"
}
```

**Tests 脚本** (自动保存 Token)
```javascript
// 测试响应状态
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 自动保存 Token 到环境变量
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0 && jsonData.data.tokenValue) {
        // 保存 Token
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
        
        console.log("✅ 注册成功！已自动登录");
        console.log("🎫 Token:", jsonData.data.tokenValue);
        console.log("👤 用户 ID:", jsonData.data.loginId);
        console.log("📝 用户名:", jsonData.data.userProfile.userName);
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "2. Register New User"
2. 设置为 POST 方法
3. URL: `{{baseUrl}}/auth/register`
4. Headers 添加: `Content-Type: application/json`
5. Body 填入上面的请求体（确保 `verificationCode` 变量已设置）
6. Tests 标签粘贴脚本
7. 点击 "Send"
8. ✅ 检查 Console 输出的 Token 信息
9. ✅ 检查环境变量是否已自动保存 `satoken` 和 `userId`

---

### 3️⃣ 用户登录

**接口信息**
- **名称**: User Login
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/login`
- **需要 Token**: ❌ 否

**Headers**
```
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "email": "{{userEmail}}",
  "password": "password123"
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isLogin": true,
    "loginId": "1751234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1751234567890,
      "userName": "John Doe",
      "userRole": "user",
      "createTime": "2024-01-01T10:00:00",
      "updateTime": "2024-01-01T10:00:00"
    }
  },
  "message": "ok"
}
```

**失败响应 - 邮箱或密码错误**
```json
{
  "code": 40000,
  "data": null,
  "message": "Email or password is incorrect"
}
```

**⚠️ 安全特性**: 无论邮箱是否存在，还是密码错误，都返回相同的错误信息，防止用户枚举攻击。

**Tests 脚本** (自动保存 Token)
```javascript
// 测试响应状态
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 自动保存 Token
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0 && jsonData.data.tokenValue) {
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
        
        console.log("✅ 登录成功！");
        console.log("🎫 Token:", jsonData.data.tokenValue);
        console.log("👤 用户 ID:", jsonData.data.loginId);
        console.log("📝 用户名:", jsonData.data.userProfile.userName);
        console.log("🔐 角色:", jsonData.data.userProfile.userRole);
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "3. User Login"
2. POST 方法，URL: `{{baseUrl}}/auth/login`
3. Headers: `Content-Type: application/json`
4. Body 填入请求体
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. ✅ 确认 Token 已自动保存

---

### 4️⃣ 发送密码重置验证码

**接口信息**
- **名称**: Send Password Reset Code
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/forgot-password/send-code`
- **需要 Token**: ❌ 否

**Headers**
```
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "email": "{{userEmail}}"
}
```

**响应** (200 OK - 总是返回成功)
```json
{
  "code": 0,
  "data": {
    "message": "If the account exists, we have sent a password reset email to your address."
  },
  "message": "ok"
}
```

**⚠️ 安全特性**: 无论邮箱是否存在，都返回相同的成功信息，防止用户枚举攻击。

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

console.log("✅ 请求已发送");
console.log("📧 如果邮箱存在，重置码已发送");
console.log("🔍 请检查邮箱或服务器日志获取重置码");
console.log("⏰ 重置码有效期: 30 分钟");
```

**操作步骤**
1. 创建新请求，命名为 "4. Send Password Reset Code"
2. POST 方法，URL: `{{baseUrl}}/auth/forgot-password/send-code`
3. Headers: `Content-Type: application/json`
4. Body 填入请求体
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. **查看服务器日志获取重置码**，搜索: `Password reset token generated for`
8. 复制重置码到环境变量 `resetToken` 中

---

### 5️⃣ 重置密码

**接口信息**
- **名称**: Reset Password
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/forgot-password/reset`
- **需要 Token**: ❌ 否

**Headers**
```
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "email": "{{userEmail}}",
  "token": "{{resetToken}}",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": {
    "message": "Password has been reset successfully. Please login with your new password."
  },
  "message": "ok"
}
```

**失败响应 - Token 错误**
```json
{
  "code": 40000,
  "data": null,
  "message": "Reset token is incorrect"
}
```

**失败响应 - 密码不一致**
```json
{
  "code": 40000,
  "data": null,
  "message": "Passwords do not match"
}
```

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ 密码重置成功！");
        console.log("🔒 所有登录会话已被注销");
        console.log("🔑 请使用新密码重新登录");
        
        // 清除旧的 Token
        pm.environment.set("satoken", "");
        pm.environment.set("userId", "");
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "5. Reset Password"
2. POST 方法，URL: `{{baseUrl}}/auth/forgot-password/reset`
3. Headers: `Content-Type: application/json`
4. Body 填入请求体（确保 `resetToken` 变量已设置）
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. ✅ 密码重置成功后，使用新密码登录（接口 3）

---

### 6️⃣ 用户登出

**接口信息**
- **名称**: User Logout
- **方法**: `POST`
- **URL**: `{{baseUrl}}/auth/logout`
- **需要 Token**: ✅ 是

**Headers**
```
satoken: {{satoken}}
```

**Request Body**
无需请求体

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**失败响应 - 未登录**
```json
{
  "code": 40101,
  "data": null,
  "message": "Please login first: ..."
}
```

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ 登出成功！");
        
        // 清除环境变量中的 Token
        pm.environment.set("satoken", "");
        pm.environment.set("userId", "");
        console.log("🧹 Token 已清除");
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "6. User Logout"
2. POST 方法，URL: `{{baseUrl}}/auth/logout`
3. Headers: `satoken: {{satoken}}`
4. 无需 Body
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. ✅ 确认 Token 已清除

---

### 7️⃣ 生成数据集上传 URL

**接口信息**
- **名称**: Generate Dataset Upload URL
- **方法**: `POST`
- **URL**: `{{baseUrl}}/s3/folder/presigned-upload-url/dataset`
- **需要 Token**: ✅ 是

**Headers**
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "fileName": "my-dataset.csv",
  "expirationTime": 3600000
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/datasets/1751234567890/my-dataset.csv?X-Amz-Algorithm=...",
  "message": "ok"
}
```

**失败响应 - 未登录**
```json
{
  "code": 40101,
  "data": null,
  "message": "Please login first: ..."
}
```

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ Presigned URL 生成成功！");
        console.log("🔗 URL:", jsonData.data);
        console.log("⏰ 有效期: 1 小时");
        console.log("📁 存储路径: labOS/datasets/{{userId}}/my-dataset.csv");
        
        // 保存 URL 到环境变量（可选）
        pm.environment.set("presignedUrl", jsonData.data);
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "7. Generate Dataset Upload URL"
2. POST 方法，URL: `{{baseUrl}}/s3/folder/presigned-upload-url/dataset`
3. Headers 添加:
   - `satoken: {{satoken}}`
   - `Content-Type: application/json`
4. Body 填入请求体
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. ✅ 复制响应中的 URL
8. **继续下一步：使用 URL 上传文件**

---

### 8️⃣ 上传文件到 S3

**接口信息**
- **名称**: Upload File to S3
- **方法**: `PUT`
- **URL**: `{上一步获取的 Presigned URL}`
- **需要 Token**: ❌ 否（URL 中已包含授权信息）

**Headers**
```
Content-Type: application/octet-stream
```

**Request Body**
- 选择 "binary"
- 点击 "Select File" 选择要上传的文件

**成功响应** (200 OK)
```
(S3 返回空响应体，HTTP 200 表示成功)
```

**Tests 脚本**
```javascript
pm.test("Upload successful", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    console.log("✅ 文件上传成功！");
    console.log("📦 文件已存储到 S3");
}
```

**操作步骤**
1. 创建新请求，命名为 "8. Upload File to S3"
2. **重要**: 设置为 `PUT` 方法（不是 POST）
3. URL: 粘贴上一步获取的完整 Presigned URL
4. Headers: `Content-Type: application/octet-stream`
5. Body 选择 "binary"，点击 "Select File" 选择文件
6. Tests 标签粘贴脚本
7. 点击 "Send"
8. ✅ 确认返回 200 状态码

**也可以使用 curl 命令**:
```bash
curl -X PUT \
  "{粘贴 Presigned URL}" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/path/to/your/file.csv
```

---

### 9️⃣ 生成基准评估上传 URL

**接口信息**
- **名称**: Generate Benchmark Eval Upload URL
- **方法**: `POST`
- **URL**: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval`
- **需要 Token**: ✅ 是

**Headers**
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "fileName": "evaluation-result.json",
  "expirationTime": 3600000
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/evaluation-result.json?X-Amz-Algorithm=...",
  "message": "ok"
}
```

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ Presigned URL 生成成功！");
        console.log("🔗 URL:", jsonData.data);
        console.log("📁 存储路径: labOS/benchmark-eval/{{userId}}/evaluation-result.json");
        pm.environment.set("presignedUrl", jsonData.data);
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "9. Generate Benchmark Eval Upload URL"
2. POST 方法，URL: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval`
3. Headers 添加:
   - `satoken: {{satoken}}`
   - `Content-Type: application/json`
4. Body 填入请求体
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. 使用返回的 URL 上传文件（参考步骤 8）

---

### 🔟 批量生成上传 URL

**接口信息**
- **名称**: Batch Generate Upload URLs
- **方法**: `POST`
- **URL**: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval/batch`
- **需要 Token**: ✅ 是

**Headers**
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body** (raw JSON)
```json
{
  "fileNames": [
    "file1.json",
    "file2.json",
    "file3.json"
  ],
  "expirationTime": 3600000
}
```

**成功响应** (200 OK)
```json
{
  "code": 0,
  "data": {
    "entries": [
      {
        "fileName": "file1.json",
        "sanitizedFileName": "file1.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file1.json?X-Amz-Algorithm=..."
      },
      {
        "fileName": "file2.json",
        "sanitizedFileName": "file2.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file2.json?X-Amz-Algorithm=..."
      },
      {
        "fileName": "file3.json",
        "sanitizedFileName": "file3.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file3.json?X-Amz-Algorithm=..."
      }
    ]
  },
  "message": "ok"
}
```

**Tests 脚本**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0) {
        console.log("✅ 批量 URL 生成成功！");
        console.log("📦 生成了 " + jsonData.data.entries.length + " 个上传 URL");
        
        jsonData.data.entries.forEach(function(entry, index) {
            console.log("\n文件 " + (index + 1) + ":");
            console.log("  📄 文件名:", entry.fileName);
            console.log("  🔗 URL:", entry.presignedUrl.substring(0, 80) + "...");
        });
    }
}
```

**操作步骤**
1. 创建新请求，命名为 "10. Batch Generate Upload URLs"
2. POST 方法，URL: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval/batch`
3. Headers 添加:
   - `satoken: {{satoken}}`
   - `Content-Type: application/json`
4. Body 填入请求体（可以修改文件名列表）
5. Tests 标签粘贴脚本
6. 点击 "Send"
7. 使用返回的多个 URL 分别上传对应的文件

---

## 常见问题

### ❓ Q1: 如何获取验证码？

**A**: 有两种方式

**方式 1: 查看邮箱**（推荐）
- 检查注册邮箱的收件箱
- 查看来自 labOS 的邮件
- 邮件主题: "labOS - Email Verification Code" 或 "labOS - Password Reset Code"

**方式 2: 查看服务器日志**（开发环境）
```bash
# 注册验证码
搜索: "Verification code generated for registration"

# 密码重置验证码
搜索: "Password reset token generated for"
```

---

### ❓ Q2: Token 如何自动保存？

**A**: 在 Tests 脚本中添加以下代码

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0 && jsonData.data.tokenValue) {
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
    }
}
```

这段代码会自动将 Token 保存到环境变量中。

---

### ❓ Q3: 如何在所有请求中自动添加 Token？

**A**: 使用 Pre-request Script

在 Collection 或 Folder 级别添加 Pre-request Script:

```javascript
// 自动添加 Token 到 Header
var token = pm.environment.get("satoken");
if (token && token !== "") {
    pm.request.headers.add({
        key: "satoken",
        value: token
    });
}
```

---

### ❓ Q4: 验证码过期了怎么办？

**A**: 重新发送验证码

**注册验证码**: 重新调用 "1. Send Registration Code" 接口  
**重置密码验证码**: 重新调用 "4. Send Password Reset Code" 接口

**有效期**:
- 注册验证码: 5 分钟
- 重置密码验证码: 30 分钟

---

### ❓ Q5: 为什么登录失败总是返回相同的错误？

**A**: 这是安全特性，防止用户枚举攻击

无论是：
- ❌ 邮箱不存在
- ❌ 密码错误

都会返回相同的错误信息:
```json
{
  "code": 40000,
  "message": "Email or password is incorrect"
}
```

这样攻击者无法通过错误信息判断邮箱是否已注册。

---

### ❓ Q6: 如何测试文件上传？

**A**: 分两步进行

**第 1 步**: 生成 Presigned URL
- 调用接口 7、9 或 10
- 复制响应中的 URL

**第 2 步**: 使用 URL 上传文件
- 创建新的 PUT 请求
- URL 填入上一步的 Presigned URL
- Body 选择 "binary"
- 选择要上传的文件
- 点击 Send

---

### ❓ Q7: Presigned URL 有效期是多久？

**A**: 默认 1 小时

可以在请求中修改 `expirationTime` 参数（单位：毫秒）:

```json
{
  "fileName": "my-file.csv",
  "expirationTime": 7200000  // 2 小时
}
```

---

### ❓ Q8: 上传文件存储在哪里？

**A**: 根据文件类型存储在不同路径

**数据集文件**:
```
Bucket: labosfrontdemo1
路径: labOS/datasets/{你的用户ID}/{文件名}
示例: labOS/datasets/1751234567890/my-dataset.csv
```

**基准评估文件**:
```
Bucket: labosfrontdemo1
路径: labOS/benchmark-eval/{你的用户ID}/{文件名}
示例: labOS/benchmark-eval/1751234567890/evaluation-result.json
```

---

### ❓ Q9: 如何调试接口？

**A**: 使用 Postman Console

1. 打开 Postman 底部的 "Console"
2. 所有 Tests 脚本中的 `console.log()` 都会输出到这里
3. 可以看到详细的请求和响应信息

---

### ❓ Q10: 密码重置后需要重新登录吗？

**A**: 是的，必须重新登录

密码重置成功后：
- ✅ 所有旧的登录会话会被自动注销
- ✅ 环境变量中的 Token 会被清除
- ✅ 需要使用新密码重新登录（调用接口 3）

---

## 测试场景

### 场景 1: 完整的新用户注册流程

```
1. 发送验证码 (接口 1)
2. 查看邮箱/日志获取验证码
3. 注册用户 (接口 2)
4. 自动获得 Token
5. 生成上传 URL (接口 7)
6. 上传文件 (接口 8)
7. 登出 (接口 6)
```

---

### 场景 2: 老用户登录并上传文件

```
1. 登录 (接口 3)
2. 获得 Token
3. 生成上传 URL (接口 9)
4. 上传文件 (接口 8)
5. 登出 (接口 6)
```

---

### 场景 3: 忘记密码并重置

```
1. 请求重置密码 (接口 4)
2. 查看邮箱/日志获取重置码
3. 重置密码 (接口 5)
4. 使用新密码登录 (接口 3)
```

---

### 场景 4: 批量上传文件

```
1. 登录 (接口 3)
2. 批量生成 URL (接口 10)
3. 使用每个 URL 分别上传文件 (接口 8)
```

---

## 快速开始

### 步骤 1: 导入 Postman Collection

你可以创建一个新的 Collection，命名为 "labOS Authentication V2.0"，然后添加上述所有接口。

### 步骤 2: 配置环境变量

创建环境 `labOS-Local`，添加变量:
- `baseUrl`: `http://localhost:8101/api`
- `userEmail`: `test@example.com`
- 其他变量留空（会自动填充）

### 步骤 3: 按顺序测试

建议按照以下顺序测试：

**首次测试（注册新用户）**:
```
接口 1 → 接口 2 → 接口 7 → 接口 8 → 接口 6
```

**测试登录**:
```
接口 3 → 接口 9 → 接口 8 → 接口 6
```

**测试密码重置**:
```
接口 4 → 接口 5 → 接口 3
```

---

## 检查清单

### ✅ 环境配置
- [ ] 创建了 Postman 环境
- [ ] 配置了所有必需的环境变量
- [ ] 选择了正确的环境

### ✅ 接口测试
- [ ] 所有 10 个接口都创建了
- [ ] Headers 配置正确
- [ ] Body 配置正确
- [ ] Tests 脚本已添加

### ✅ 功能测试
- [ ] 新用户可以注册
- [ ] 已注册用户可以登录
- [ ] 可以重置密码
- [ ] 可以生成上传 URL
- [ ] 可以上传文件
- [ ] 可以登出

### ✅ 安全测试
- [ ] 未登录用户不能访问需要认证的接口
- [ ] 过期 Token 被拒绝
- [ ] 登录错误信息是通用的
- [ ] 密码重置后所有会话被注销

---

## 技巧和最佳实践

### 💡 技巧 1: 使用 Collection Runner

批量运行所有接口:
1. 点击 Collection 右键
2. 选择 "Run collection"
3. 按顺序执行所有接口

### 💡 技巧 2: 保存常用的测试数据

在环境变量中保存:
- 测试用的邮箱
- 测试用的密码
- 常用的文件名

### 💡 技巧 3: 使用 Pre-request Script

在 Collection 级别添加 Pre-request Script，自动添加 Token:

```javascript
var token = pm.environment.get("satoken");
if (token && token !== "") {
    pm.request.headers.upsert({
        key: "satoken",
        value: token
    });
}
```

### 💡 技巧 4: 查看详细日志

开启 Postman Console (View → Show Postman Console) 查看所有请求详情。

---

## 总结

这个 Postman 测试指南涵盖了:
- ✅ 10 个核心接口
- ✅ 完整的测试流程
- ✅ 自动化脚本
- ✅ 错误处理
- ✅ 最佳实践

**记住**: 
- 📧 验证码在邮件或日志中
- 🎫 Token 会自动保存
- 🔒 使用 HTTPS 保护敏感数据
- ⏰ 注意验证码过期时间

---

**最后更新**: 2024-12-06  
**版本**: 2.0.0

**祝测试顺利！🚀**


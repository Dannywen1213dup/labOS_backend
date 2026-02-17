# Sa-Token 系统测试指南

## 目录
1. [系统概述](#系统概述)
2. [前置准备](#前置准备)
3. [测试流程](#测试流程)
4. [API 接口说明](#api-接口说明)
5. [常见问题](#常见问题)

---

## 系统概述

本项目已集成 **Sa-Token** 认证框架，实现了完整的用户认证和授权功能。

### 主要功能
- ✅ 用户注册（邮箱验证）
- ✅ 用户登录（邮箱 + 密码）
- ✅ Token 自动管理
- ✅ Session 用户信息存储
- ✅ 接口权限控制
- ✅ 用户角色管理（普通用户/管理员）

### 技术栈
- **Sa-Token**: 1.37.0
- **Redis**: Session 存储
- **Spring Boot**: 2.7.2

---

## 前置准备

### 1. 环境要求
- JDK 1.8+
- MySQL 数据库
- Redis 服务

### 2. 配置检查

确保 `application.yml` 中配置正确：

```yaml
# Redis 配置
spring:
  redis:
    host: localhost
    port: 6379
    database: 0

# Sa-Token 配置
sa-token:
  token-name: satoken
  timeout: 2592000  # 30天
  is-concurrent: true
  is-share: true
```

### 3. 数据库准备

确保 `user` 表包含以下字段：
- `id` (Long): 主键
- `email` (String): 邮箱（唯一）
- `user_password` (String): 密码（加密）
- `first_name` (String): 名字
- `last_name` (String): 姓氏
- `user_name` (String): 用户名
- `user_role` (String): 角色（user/admin）
- `status` (String): 状态（UNVERIFIED/ACTIVE/DISABLED）

---

## 测试流程

### 测试工具推荐
- **Postman** (推荐)
- **Apifox**
- **curl 命令行**

### 基础 URL
```
http://localhost:8101/api
```

---

## API 接口说明

### 1. 检查邮箱是否存在

**接口**: `POST /api/auth/check-email`

**用途**: 判断邮箱是否已注册

**请求示例**:
```json
{
  "email": "test@example.com"
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "exists": false
  },
  "message": "ok"
}
```

---

### 2. 用户注册（初始化）

**接口**: `POST /api/auth/register/init`

**用途**: 创建新用户并发送验证码

**请求示例**:
```json
{
  "email": "newuser@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe",
  "legalAccepted": true
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "email": "newuser@example.com"
  },
  "message": "ok"
}
```

**注意事项**:
- 系统会生成 6 位数字验证码
- 验证码会打印在控制台日志中（开发环境）
- 验证码有效期：5 分钟

---

### 3. 验证邮箱并完成注册

**接口**: `POST /api/auth/register/verify`

**用途**: 验证邮箱验证码，激活账户并返回 Token

**请求示例**:
```json
{
  "email": "newuser@example.com",
  "code": "123456"
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "abc123def456...",
    "isLogin": true,
    "loginId": "1234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1234567890,
      "userName": "John Doe",
      "userRole": "user",
      "userAvatar": null,
      "userProfile": null,
      "createTime": "2025-11-25T10:00:00",
      "updateTime": "2025-11-25T10:00:00"
    }
  },
  "message": "ok"
}
```

**重要**: 保存 `tokenValue`，后续所有需要认证的请求都需要携带此 Token。

---

### 4. 用户登录

**接口**: `POST /api/auth/login`

**用途**: 已注册用户登录获取 Token

**请求示例**:
```json
{
  "email": "newuser@example.com",
  "password": "Password123!"
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "xyz789uvw456...",
    "isLogin": true,
    "loginId": "1234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1234567890,
      "userName": "John Doe",
      "userRole": "user"
    }
  },
  "message": "ok"
}
```

---

### 5. 用户登出

**接口**: `POST /api/auth/logout`

**请求头**:
```
satoken: xyz789uvw456...
```

**响应示例**:
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

### 6. 测试受保护的接口（重点）

#### 示例：批量生成预签名上传 URL

**接口**: `POST /api/s3/folder/presigned-upload-url/benchmark-eval/batch`

**请求头** (必须):
```
satoken: xyz789uvw456...
Content-Type: application/json
```

**请求示例**:
```json
{
  "fileNames": [
    "benchmark_result_1.csv",
    "benchmark_result_2.json",
    "evaluation_data.xlsx"
  ],
  "expirationTime": 3600000
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "entries": [
      {
        "fileName": "benchmark_result_1.csv",
        "sanitizedFileName": "benchmark_result_1.csv",
        "presignedUrl": "https://s3.amazonaws.com/..."
      },
      {
        "fileName": "benchmark_result_2.json",
        "sanitizedFileName": "benchmark_result_2.json",
        "presignedUrl": "https://s3.amazonaws.com/..."
      }
    ]
  },
  "message": "ok"
}
```

**服务器日志输出**:
```
=== Batch Benchmark-Eval Upload Request ===
User ID: 1234567890
User Name: John Doe
User Email: newuser@example.com
User Role: user
File Count: 3
==========================================
```

**错误场景测试**:

❌ **未携带 Token**:
```json
{
  "code": 40100,
  "data": null,
  "message": "Please login first: User not logged in"
}
```

❌ **Token 过期**:
```json
{
  "code": 40100,
  "data": null,
  "message": "Please login first: Token has expired"
}
```

❌ **无效 Token**:
```json
{
  "code": 40100,
  "data": null,
  "message": "Please login first: Invalid token"
}
```

---

## 使用 Postman 测试完整流程

### Step 1: 注册新用户

1. 创建 POST 请求: `http://localhost:8101/api/auth/register/init`
2. Body (JSON):
```json
{
  "email": "test@labos.com",
  "password": "Test123!",
  "firstName": "Test",
  "lastName": "User",
  "legalAccepted": true
}
```
3. 点击 **Send**
4. 查看控制台日志，找到验证码（例如：`123456`）

### Step 2: 验证邮箱

1. 创建 POST 请求: `http://localhost:8101/api/auth/register/verify`
2. Body (JSON):
```json
{
  "email": "test@labos.com",
  "code": "123456"
}
```
3. 点击 **Send**
4. 复制响应中的 `tokenValue`

### Step 3: 测试受保护接口

1. 创建 POST 请求: `http://localhost:8101/api/s3/folder/presigned-upload-url/benchmark-eval/batch`
2. **Headers** 添加:
   - Key: `satoken`
   - Value: `<刚才复制的 tokenValue>`
3. Body (JSON):
```json
{
  "fileNames": ["test1.csv", "test2.json"]
}
```
4. 点击 **Send**
5. 查看服务器日志，应该能看到用户信息打印

### Step 4: 测试登出

1. 创建 POST 请求: `http://localhost:8101/api/auth/logout`
2. **Headers** 添加:
   - Key: `satoken`
   - Value: `<tokenValue>`
3. 点击 **Send**

### Step 5: 验证 Token 失效

1. 再次调用 Step 3 的接口
2. 应该收到 `40100` 错误：`Please login first`

---

## 使用 curl 命令测试

### 1. 注册用户
```bash
curl -X POST http://localhost:8101/api/auth/register/init \
  -H "Content-Type: application/json" \
  -d '{
    "email": "curl@example.com",
    "password": "Curl123!",
    "firstName": "Curl",
    "lastName": "Test",
    "legalAccepted": true
  }'
```

### 2. 验证邮箱（替换 123456 为实际验证码）
```bash
curl -X POST http://localhost:8101/api/auth/register/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "curl@example.com",
    "code": "123456"
  }'
```

### 3. 测试受保护接口（替换 YOUR_TOKEN 为实际 token）
```bash
curl -X POST http://localhost:8101/api/s3/folder/presigned-upload-url/benchmark-eval/batch \
  -H "Content-Type: application/json" \
  -H "satoken: YOUR_TOKEN" \
  -d '{
    "fileNames": ["test1.csv", "test2.json"]
  }'
```

---

## Sa-Token 工具类使用示例

在您的 Controller 中，可以这样使用 Sa-Token 工具类：

```java
import com.labOS.backend.satoken.SaTokenUtil;
import com.labOS.backend.model.vo.LoginUserVO;

@RestController
@RequestMapping("/api/example")
public class ExampleController {
    
    @PostMapping("/protected")
    public BaseResponse<String> protectedEndpoint() {
        // 1. 检查是否登录（未登录会抛出异常）
        SaTokenUtil.checkLogin();
        
        // 2. 获取当前登录用户信息
        LoginUserVO user = SaTokenUtil.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        // 3. 获取用户 ID
        Long userId = SaTokenUtil.getUserId();
        
        // 4. 获取用户名
        String userName = SaTokenUtil.getUserName();
        
        // 5. 获取用户角色
        String userRole = SaTokenUtil.getUserRole();
        
        // 6. 判断是否为管理员
        boolean isAdmin = SaTokenUtil.isAdmin();
        
        log.info("User {} ({}) accessed protected endpoint", 
                userName, userId);
        
        return ResultUtils.success("Access granted");
    }
}
```

---

## 常见问题

### Q1: Token 应该放在哪里？
**A**: Token 应该放在 HTTP 请求头中，键名为 `satoken`。

**示例**:
```
Headers:
  satoken: abc123def456...
  Content-Type: application/json
```

### Q2: 如何查看验证码？
**A**: 在开发环境下，验证码会打印在控制台日志中：
```
Verification code for test@example.com: 123456
```

生产环境需要配置邮件服务发送验证码。

### Q3: Token 有效期是多久？
**A**: 默认 30 天（2592000 秒），可在 `application.yml` 中修改：
```yaml
sa-token:
  timeout: 2592000  # 秒
```

### Q4: 如何测试 Token 过期？
**A**: 两种方法：
1. 修改配置文件将 `timeout` 改为较小值（如 60 秒）
2. 调用 `StpUtil.logout()` 手动清除 Token

### Q5: 忘记密码怎么办？
**A**: 当前版本暂不支持忘记密码功能，需要：
1. 直接在数据库中修改密码（记得用 MD5 + SALT 加密）
2. 或者重新注册新账号

### Q6: 如何添加不需要认证的接口？
**A**: 在 `application.yml` 中添加：
```yaml
labos:
  sa-token-not-filter-url:
    - /api/public/**
    - /api/test/**
```

或在 `SaTokenConfigure.java` 的 `excludePaths` 数组中添加。

### Q7: 同一账号可以多处登录吗？
**A**: 可以。当前配置为：
```yaml
sa-token:
  is-concurrent: true  # 允许并发登录
  is-share: true       # 共享同一个 Token
```

如果想限制单点登录，改为：
```yaml
sa-token:
  is-concurrent: false  # 不允许并发登录
```

### Q8: Redis 连接失败怎么办？
**A**: 检查：
1. Redis 服务是否启动：`redis-cli ping`
2. 配置文件中 Redis 地址是否正确
3. 防火墙是否开放 6379 端口

### Q9: 如何在日志中查看用户信息？
**A**: 在 Controller 中使用：
```java
LoginUserVO user = SaTokenUtil.getUser();
log.info("User: {}, Email: {}", user.getUserName(), user.getEmail());
```

服务器日志会输出：
```
User: John Doe, Email: john@example.com
```

### Q10: 测试环境如何快速创建测试用户？
**A**: 使用 SQL 直接插入（密码：`Test123!`）：
```sql
INSERT INTO user (id, email, user_password, first_name, last_name, user_name, user_role, status, legal_accepted)
VALUES (
  123456789,
  'test@example.com',
  'b8c37e33defde51cf91e1e03e51657da',  -- MD5(SALT + 'Test123!')
  'Test',
  'User',
  'Test User',
  'user',
  'ACTIVE',
  1
);
```

---

## 测试检查清单

完成以下测试项，确保系统正常：

- [ ] **注册流程**: 能够成功注册新用户
- [ ] **验证码**: 控制台能看到验证码输出
- [ ] **邮箱验证**: 使用验证码能激活账户
- [ ] **登录**: 能够成功登录并获取 Token
- [ ] **Token 携带**: 携带 Token 能访问受保护接口
- [ ] **Token 校验**: 不带 Token 会返回 40100 错误
- [ ] **用户信息**: 服务器日志能正确打印用户信息
- [ ] **登出**: 登出后 Token 失效
- [ ] **角色验证**: 普通用户和管理员权限区分正常
- [ ] **Session 持久化**: 重启服务后 Token 仍然有效（Redis 正常运行）

---

## 技术支持

如有问题，请联系：
- **开发者**: Yifan Wen
- **项目地址**: https://github.com/Dannywen1213dup
- **官网**: https://www.ai4labos.com/

---

**祝测试顺利！** 🎉


# Sa-Token 快速开始指南

## 🚀 快速测试流程（5分钟）

### 1️⃣ 启动项目

```bash
cd /path/to/springboot-init-master
./mvnw spring-boot:run
```

确保 MySQL 和 Redis 都在运行。

---

### 2️⃣ 使用 Postman 测试

#### 步骤 1: 注册新用户

**请求**: `POST http://localhost:8101/api/auth/register/init`

**Body (JSON)**:
```json
{
  "email": "test@example.com",
  "password": "Test123!",
  "firstName": "Test",
  "lastName": "User",
  "legalAccepted": true
}
```

**预期响应**:
```json
{
  "code": 0,
  "data": {
    "email": "test@example.com"
  },
  "message": "ok"
}
```

**重要**: 查看控制台日志，找到验证码！
```
Verification code for test@example.com: 123456
```

---

#### 步骤 2: 验证邮箱并获取 Token

**请求**: `POST http://localhost:8101/api/auth/register/verify`

**Body (JSON)**:
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**预期响应**:
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "abc123def456xyz789...",
    "isLogin": true,
    "loginId": "1234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1234567890,
      "userName": "Test User",
      "userRole": "user"
    }
  },
  "message": "ok"
}
```

**重要**: 复制 `tokenValue`！

---

#### 步骤 3: 测试受保护的接口

**请求**: `POST http://localhost:8101/api/s3/folder/presigned-upload-url/benchmark-eval/batch`

**Headers**:
```
satoken: abc123def456xyz789...
Content-Type: application/json
```

**Body (JSON)**:
```json
{
  "fileNames": ["test1.csv", "test2.json", "test3.xlsx"]
}
```

**预期响应**:
```json
{
  "code": 0,
  "data": {
    "entries": [
      {
        "fileName": "test1.csv",
        "sanitizedFileName": "test1.csv",
        "presignedUrl": "https://s3.amazonaws.com/..."
      }
    ]
  },
  "message": "ok"
}
```

**查看服务器日志**:
```
=== Batch Benchmark-Eval Upload Request ===
User ID: 1234567890
User Name: Test User
User Email: test@example.com
User Role: user
File Count: 3
==========================================
```

✅ **成功！您可以看到用户的完整信息（ID、用户名、邮箱、角色）！**

---

## 🔑 核心功能验证

### ✅ Token 认证
- 未登录访问受保护接口 → 返回 `40100` 错误
- 携带 Token 访问 → 正常返回数据

### ✅ 用户信息获取
在 Controller 中可以直接使用：

```java
// 验证登录
SaTokenUtil.checkLogin();

// 获取用户信息
LoginUserVO user = SaTokenUtil.getUser();

// 打印日志
log.info("User: {}, Email: {}", user.getUserName(), user.getEmail());
```

### ✅ 自动拦截
所有不在白名单中的接口都会自动验证 Token：
- `/api/auth/**` → 放行（登录注册）
- `/api/s3/**` → 需要 Token
- `/swagger-ui/**` → 放行（API 文档）

---

## 📝 在您的 Controller 中使用

```java
@RestController
@RequestMapping("/api/your-module")
public class YourController {
    
    @PostMapping("/your-api")
    public BaseResponse<String> yourApi() {
        // 1. 验证登录
        SaTokenUtil.checkLogin();
        
        // 2. 获取用户信息
        LoginUserVO user = SaTokenUtil.getUser();
        Long userId = user.getId();
        String userName = user.getUserName();
        String userRole = user.getUserRole();
        
        // 3. 如果需要邮箱，从数据库查询
        User userEntity = userService.getById(userId);
        String email = userEntity.getEmail();
        
        // 4. 打印日志
        log.info("API called by user: {}, email: {}", userName, email);
        
        // 5. 您的业务逻辑
        // ...
        
        return ResultUtils.success("Success");
    }
}
```

---

## 🛡️ 安全特性

### 1. 自动 Token 管理
- ✅ Token 有效期：30天
- ✅ 自动过期检查
- ✅ Session 持久化（Redis）

### 2. 统一异常处理
- ❌ 未登录 → `40100: Please login first`
- ❌ Token 过期 → `40100: Token has expired`
- ❌ 无效 Token → `40100: Invalid token`

### 3. 用户信息安全
- ✅ 密码加密存储（MD5 + SALT）
- ✅ Session 中只存储脱敏信息
- ✅ 邮箱验证机制

---

## 📊 测试检查清单

完成以下测试项：

- [x] 注册新用户
- [x] 验证邮箱获取 Token
- [x] 携带 Token 访问受保护接口
- [x] 不带 Token 访问被拒绝
- [x] 服务器日志正确打印用户信息
- [x] 登出后 Token 失效

---

## ❓ 常见问题

### Q: 如何查看验证码？
**A**: 开发环境下，验证码会打印在控制台日志中：
```
Verification code for xxx@example.com: 123456
```

### Q: Token 放在哪里？
**A**: 放在 HTTP 请求头中：
```
Headers:
  satoken: your_token_value_here
```

### Q: 如何添加不需要认证的接口？
**A**: 在 `application.yml` 中添加：
```yaml
labos:
  sa-token-not-filter-url:
    - /api/public/**
```

### Q: 如何获取当前登录用户的邮箱？
**A**: 
```java
LoginUserVO user = SaTokenUtil.getUser();
User userEntity = userService.getById(user.getId());
String email = userEntity.getEmail();
```

---

## 📚 完整文档

- **详细测试指南**: `SATOKEN_TESTING_GUIDE.md`
- **实现总结**: `SATOKEN_IMPLEMENTATION_SUMMARY.md`

---

## 🎉 完成！

现在您的系统已经成功集成 Sa-Token，具备完整的用户认证和授权功能！

**祝开发顺利！** 🚀


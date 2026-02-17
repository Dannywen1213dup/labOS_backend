头像上传测试流程
================

本文档演示如何用 curl 测试头像上传流程。

前置条件
--------
- API 基础地址：http://localhost:8101（按需修改）
- 已有可登录的账号（email + password）
- 本地有一张图片文件：`./avatar.jpg`
- Sa-Token 头名称为 `satoken`

步骤 0 - 登录（获取 token）
--------------------------
请求：
```bash
curl -s -X POST "http://localhost:8101/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "your_password"
  }'
```

期望响应（示例）：
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "xxxxx",
    "isLogin": true,
    "loginId": "123"
  }
}
```

保存 token：
```bash
TOKEN="xxxxx"
```

步骤 1 - 获取头像上传的预签名 URL
----------------------------------
请求：
```bash
curl -s -X POST "http://localhost:8101/user/avatar/presigned" \
  -H "Content-Type: application/json" \
  -H "satoken: $TOKEN" \
  -d '{
    "fileName": "avatar.jpg",
    "expirationTime": 3600000
  }'
```

期望响应（示例）：
```json
{
  "code": 0,
  "data": {
    "uploadUrl": "https://...presigned...",
    "avatarKey": "20250123T120305Z.jpg",
    "avatarUrl": "https://<cloudfront>/avatars/123/20250123T120305Z.jpg"
  }
}
```

保存 uploadUrl 和 avatarKey：
```bash
UPLOAD_URL="https://...presigned..."
AVATAR_KEY="20250123T120305Z.jpg"
```

步骤 2 - 使用预签名 URL 直接上传到 S3
--------------------------------------
请求：
```bash
curl -X PUT "$UPLOAD_URL" \
  -H "Content-Type: image/jpeg" \
  --data-binary "@./avatar.jpg"
```

期望响应：
- HTTP 200 或 204 且无响应体（上传成功）

步骤 3 - 确认上传并更新头像
----------------------------
请求：
```bash
curl -s -X POST "http://localhost:8101/user/avatar/confirm" \
  -H "Content-Type: application/json" \
  -H "satoken: $TOKEN" \
  -d "{
    \"avatarKey\": \"$AVATAR_KEY\"
  }"
```

期望响应（示例）：
```json
{
  "code": 0,
  "data": "https://<cloudfront>/avatars/123/20250123T120305Z.jpg"
}
```

步骤 4 - 验证头像 URL
----------------------
- 用浏览器打开返回的 URL，或用 curl：
```bash
curl -I "https://<cloudfront>/avatars/123/20250123T120305Z.jpg"
```

备注 / 排查
-----------
- 需要后端配置 `cloudfront-domain` 才能返回有效的头像 URL。
- 步骤 2 如果返回 403，通常是预签名 URL 过期或 PUT 请求头不一致。
- 步骤 4 如果返回 403，说明 CloudFront 或权限配置有问题。
- 数据库存储：
  - userAvatarKey：文件名（如 `20250123T120305Z.jpg`）
  - userAvatar：CloudFront 基础 URL

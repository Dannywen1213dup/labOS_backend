# Configuration Files Setup Guide

## ⚠️ Important Security Notice

The actual configuration files (`application.yml`, `application-test.yml`, `application-prod.yml`) are **ignored by Git** because they contain sensitive information (database passwords, AWS keys, etc.).

## 📋 Setup Instructions

### For New Developers

1. **Copy the example files** to create your configuration files:

```bash
# Main configuration file
cp src/main/resources/application.yml.example src/main/resources/application.yml

# Test environment configuration
cp src/main/resources/application-test.yml.example src/main/resources/application-test.yml

# Production environment configuration (if needed)
cp src/main/resources/application-prod.yml.example src/main/resources/application-prod.yml
```

2. **Fill in the actual values** in the copied files:
   - Replace `YOUR_DATABASE_PASSWORD` with your database password
   - Replace `YOUR_AWS_ACCESS_KEY` and `YOUR_AWS_SECRET_KEY` with your AWS credentials
   - Replace `YOUR_S3_BUCKET_NAME` with your S3 bucket name
   - Replace all other placeholder values

### Using Environment Variables

For sensitive values, you can use environment variables instead of hardcoding:

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:defaultPassword}
  
aws:
  s3:
    accessKey: ${AWS_ACCESS_KEY:}
    secretKey: ${AWS_SECRET_KEY:}
```

Set environment variables:
```bash
export DB_PASSWORD=your_password
export AWS_ACCESS_KEY=your_access_key
export AWS_SECRET_KEY=your_secret_key
```

## 🔒 Security Best Practices

1. **Never commit** actual configuration files to Git
2. **Use environment variables** for production deployments
3. **Use different credentials** for dev, test, and production environments
4. **Rotate credentials** regularly
5. **Keep example files updated** when adding new configuration options

## 📁 Files in This Directory

- `application.yml.example` - Main configuration template
- `application-test.yml.example` - Test environment configuration template
- `application-prod.yml.example` - Production environment configuration template
- `application.yml` - **Your actual config (gitignored)**
- `application-test.yml` - **Your actual test config (gitignored)**
- `application-prod.yml` - **Your actual prod config (gitignored)**

---

**Last Updated**: 2025-12-01


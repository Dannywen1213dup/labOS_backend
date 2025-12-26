# Database initialization
# @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
# @from <a href="https://www.ai4labos.com/">ai4labOS</a>

-- Create database
CREATE DATABASE IF NOT EXISTS my_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use database
USE my_db;

-- ============================================
-- User table (with authentication fields)
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`             BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userAccount`    VARCHAR(256)                           NULL COMMENT 'Account (legacy, can be email for compatibility)',
    `email`          VARCHAR(100)                           NOT NULL COMMENT 'Email address (unique, primary login method)',
    `userPassword`   VARCHAR(512)                           NOT NULL COMMENT 'Password (hashed with MD5 and salt)',
    `firstName`      VARCHAR(50)                            NULL COMMENT 'First name',
    `lastName`       VARCHAR(50)                            NULL COMMENT 'Last name',
    `legalAccepted`  TINYINT(1)    DEFAULT 0                NOT NULL COMMENT 'Legal terms accepted (0: no, 1: yes)',
    `status`         VARCHAR(20)   DEFAULT 'ACTIVE'         NOT NULL COMMENT 'User status: ACTIVE, DISABLED',
    `unionId`        VARCHAR(256)                           NULL COMMENT 'WeChat Open Platform id',
    `mpOpenId`       VARCHAR(256)                           NULL COMMENT 'Official account openId',
    `userName`       VARCHAR(256)                           NULL COMMENT 'User nickname',
    `userAvatar`     VARCHAR(1024)                          NULL COMMENT 'User avatar',
    `userAvatarKey`  VARCHAR(1000)                          NULL COMMENT 'User avatar S3 key (for deletion / replacement)',
    `userProfile`    VARCHAR(512)                           NULL COMMENT 'User profile',
    `userRole`       VARCHAR(256)  DEFAULT 'user'           NOT NULL COMMENT 'User role: user/admin/ban',
    `createTime`     DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Create time',
    `updateTime`     DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `isDelete`       TINYINT       DEFAULT 0                 NOT NULL COMMENT 'Is deleted',
    INDEX `idx_unionId` (`unionId`),
    UNIQUE INDEX `idx_email` (`email`),
    INDEX `idx_status` (`status`)
) COMMENT 'User' COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- Post table
-- ============================================
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `title`      VARCHAR(512)                       NULL COMMENT 'Title',
    `content`    TEXT                               NULL COMMENT 'Content',
    `tags`       VARCHAR(1024)                      NULL COMMENT 'Tag list (JSON array)',
    `thumbNum`   INT      DEFAULT 0                 NOT NULL COMMENT 'Thumb count',
    `favourNum`  INT      DEFAULT 0                 NOT NULL COMMENT 'Favour count',
    `userId`     BIGINT                             NOT NULL COMMENT 'Creator user id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Create time',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `isDelete`   TINYINT  DEFAULT 0                 NOT NULL COMMENT 'Is deleted',
    INDEX `idx_userId` (`userId`)
) COMMENT 'Post' COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- Post thumb table (hard delete)
-- ============================================
DROP TABLE IF EXISTS `post_thumb`;
CREATE TABLE `post_thumb`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `postId`     BIGINT                             NOT NULL COMMENT 'Post id',
    `userId`     BIGINT                             NOT NULL COMMENT 'Creator user id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Create time',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`)
) COMMENT 'Post thumb' COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- Post favour table (hard delete)
-- ============================================
DROP TABLE IF EXISTS `post_favour`;
CREATE TABLE `post_favour`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `postId`     BIGINT                             NOT NULL COMMENT 'Post id',
    `userId`     BIGINT                             NOT NULL COMMENT 'Creator user id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Create time',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`)
) COMMENT 'Post favour' COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- File Upload Batch table
-- Tracks batch upload operations for comprehensive monitoring
-- ============================================
DROP TABLE IF EXISTS `file_upload_batch`;
CREATE TABLE `file_upload_batch`
(
    `id`                BIGINT AUTO_INCREMENT COMMENT 'Primary key' PRIMARY KEY,
    `batchId`           VARCHAR(64)                        NOT NULL COMMENT 'Unique batch identifier (UUID)',
    `userId`            BIGINT                             NOT NULL COMMENT 'User ID who initiated the upload',
    `userEmail`         VARCHAR(100)                       NULL COMMENT 'User email at the time of upload',
    `userName`          VARCHAR(256)                       NULL COMMENT 'User name at the time of upload',
    `userRole`          VARCHAR(256)                       NULL COMMENT 'User role at the time of upload',
    `folderType`        VARCHAR(50)                        NOT NULL COMMENT 'Folder type: dataset, benchmark-eval, etc.',
    `folderPath`        VARCHAR(500)                       NOT NULL COMMENT 'S3 folder path for this batch',
    `totalFiles`        INT           DEFAULT 0            NOT NULL COMMENT 'Total number of files in this batch',
    `successfulFiles`   INT           DEFAULT 0            NOT NULL COMMENT 'Number of successfully uploaded files',
    `failedFiles`       INT           DEFAULT 0            NOT NULL COMMENT 'Number of failed uploads',
    `pendingFiles`      INT           DEFAULT 0            NOT NULL COMMENT 'Number of pending uploads',
    `batchStatus`       VARCHAR(20)   DEFAULT 'PENDING'    NOT NULL COMMENT 'Batch status: PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS',
    `expirationTime`    BIGINT                             NULL COMMENT 'Presigned URL expiration time in milliseconds',
    `requestTimezone`   VARCHAR(50)   DEFAULT 'UTC'        NULL COMMENT 'Client timezone at request time',
    `requestIp`         VARCHAR(50)                        NULL COMMENT 'Client IP address',
    `userAgent`         VARCHAR(500)                       NULL COMMENT 'Client user agent',
    `notes`             TEXT                               NULL COMMENT 'Additional notes or metadata',
    `createTime`        DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Batch creation time (UTC)',
    `updateTime`        DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time (UTC)',
    `completionTime`    DATETIME                           NULL COMMENT 'Batch completion time (UTC)',
    `isDelete`          TINYINT       DEFAULT 0            NOT NULL COMMENT 'Is deleted',
    UNIQUE INDEX `idx_batchId` (`batchId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    INDEX `idx_batchStatus` (`batchStatus`),
    INDEX `idx_folderType` (`folderType`),
    INDEX `idx_userEmail` (`userEmail`)
) COMMENT 'File Upload Batch Tracking' COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- File Upload Record table
-- Tracks individual file upload operations with detailed metadata
-- ============================================
DROP TABLE IF EXISTS `file_upload_record`;
CREATE TABLE `file_upload_record`
(
    `id`                    BIGINT AUTO_INCREMENT COMMENT 'Primary key' PRIMARY KEY,
    `batchId`               VARCHAR(64)                        NOT NULL COMMENT 'Associated batch ID (FK to file_upload_batch)',
    `userId`                BIGINT                             NOT NULL COMMENT 'User ID who initiated the upload',
    `originalFileName`      VARCHAR(500)                       NOT NULL COMMENT 'Original file name provided by user',
    `sanitizedFileName`     VARCHAR(500)                       NOT NULL COMMENT 'Sanitized file name (safe for S3)',
    `s3Key`                 VARCHAR(1000)                      NOT NULL COMMENT 'Full S3 object key (path + filename)',
    `s3Bucket`              VARCHAR(255)                       NOT NULL COMMENT 'S3 bucket name',
    `fileSize`              BIGINT                             NULL COMMENT 'File size in bytes (if known)',
    `contentType`           VARCHAR(100)                       NULL COMMENT 'MIME content type',
    `presignedUrl`          TEXT                               NULL COMMENT 'Generated presigned URL (for reference)',
    `uploadStatus`          VARCHAR(20)   DEFAULT 'PENDING'    NOT NULL COMMENT 'Upload status: PENDING, SUCCESS, FAILED, EXPIRED',
    `uploadMethod`          VARCHAR(50)   DEFAULT 'PRESIGNED_URL' NOT NULL COMMENT 'Upload method: PRESIGNED_URL, DIRECT, etc.',
    `errorMessage`          TEXT                               NULL COMMENT 'Error message if upload failed',
    `errorCode`             VARCHAR(100)                       NULL COMMENT 'Error code if upload failed',
    `httpStatusCode`        INT                                NULL COMMENT 'HTTP status code from S3 response',
    `retryCount`            INT           DEFAULT 0            NOT NULL COMMENT 'Number of retry attempts',
    `urlExpirationTime`     DATETIME                           NULL COMMENT 'Presigned URL expiration time',
    `requestTime`           DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Presigned URL request time (UTC)',
    `uploadStartTime`       DATETIME                           NULL COMMENT 'Upload start time (UTC)',
    `uploadCompletionTime`  DATETIME                           NULL COMMENT 'Upload completion time (UTC)',
    `updateTime`            DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time (UTC)',
    `clientTimezone`        VARCHAR(50)   DEFAULT 'UTC'        NULL COMMENT 'Client timezone',
    `metadata`              TEXT                               NULL COMMENT 'Additional metadata (JSON format)',
    `isDelete`              TINYINT       DEFAULT 0            NOT NULL COMMENT 'Is deleted',
    INDEX `idx_batchId` (`batchId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_uploadStatus` (`uploadStatus`),
    INDEX `idx_requestTime` (`requestTime`),
    INDEX `idx_s3Key` (`s3Key`(255)),
    INDEX `idx_originalFileName` (`originalFileName`(255))
) COMMENT 'File Upload Record Tracking' COLLATE = utf8mb4_unicode_ci;

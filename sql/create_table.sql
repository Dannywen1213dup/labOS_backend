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

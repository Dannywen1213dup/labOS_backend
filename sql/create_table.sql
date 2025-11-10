# Database initialization
# @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
# @from <a href="https://www.ai4labos.com/">ai4labOS</a>

-- Create database
create database if not exists my_db;

-- Use database
use my_db;

-- User table
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment 'Account',
    userPassword varchar(512)                           not null comment 'Password',
    unionId      varchar(256)                           null comment 'WeChat Open Platform id',
    mpOpenId     varchar(256)                           null comment 'Official account openId',
    userName     varchar(256)                           null comment 'User nickname',
    userAvatar   varchar(1024)                          null comment 'User avatar',
    userProfile  varchar(512)                           null comment 'User profile',
    userRole     varchar(256) default 'user'            not null comment 'User role: user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment 'Create time',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    isDelete     tinyint      default 0                 not null comment 'Is deleted',
    index idx_unionId (unionId)
) comment 'User' collate = utf8mb4_unicode_ci;

-- Post table
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment 'Title',
    content    text                               null comment 'Content',
    tags       varchar(1024)                      null comment 'Tag list (JSON array)',
    thumbNum   int      default 0                 not null comment 'Thumb count',
    favourNum  int      default 0                 not null comment 'Favour count',
    userId     bigint                             not null comment 'Creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'Create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    isDelete   tinyint  default 0                 not null comment 'Is deleted',
    index idx_userId (userId)
) comment 'Post' collate = utf8mb4_unicode_ci;

-- Post thumb table (hard delete)
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment 'Post id',
    userId     bigint                             not null comment 'Creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'Create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    index idx_postId (postId),
    index idx_userId (userId)
) comment 'Post thumb';

-- Post favour table (hard delete)
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment 'Post id',
    userId     bigint                             not null comment 'Creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'Create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    index idx_postId (postId),
    index idx_userId (userId)
) comment 'Post favour';

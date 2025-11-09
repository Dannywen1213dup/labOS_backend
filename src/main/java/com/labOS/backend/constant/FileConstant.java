package com.labOS.backend.constant;

/**
 * 文件常量
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface FileConstant {

    /**
     * AWS S3 存储主机地址
     * S3 bucket URL format: https://{bucket-name}.s3.{region}.amazonaws.com/
     */
    String S3_HOST = "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/";
    
    /**
     * 预签名 URL 有效期（1小时）
     */
    long PRESIGNED_URL_EXPIRATION = 60 * 60 * 1000;
}

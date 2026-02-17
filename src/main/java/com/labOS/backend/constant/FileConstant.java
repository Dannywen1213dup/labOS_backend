package com.labOS.backend.constant;

/**
 * file constant
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface FileConstant {

    /**
     * AWS S3 bucket address
     * S3 bucket URL format: https://{bucket-name}.s3.{region}.amazonaws.com/
     */
    String S3_HOST = "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/";
    
    /**
     *   URL expiration time（eg 1 hour）
     */
    long PRESIGNED_URL_EXPIRATION = 60 * 60 * 1000;
}

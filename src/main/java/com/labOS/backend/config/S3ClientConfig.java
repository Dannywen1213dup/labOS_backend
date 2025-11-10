package com.labOS.backend.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 Client Configuration
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3ClientConfig {

    /**
     * AWS Access Key
     */
    private String accessKey;

    /**
     * AWS Secret Key
     */
    private String secretKey;

    /**
     * AWS Region
     */
    private String region;

    /**
     * S3 Bucket Name
     */
    private String bucket;

    @Bean
    public AmazonS3 amazonS3Client() {
        // Initialize AWS credentials
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        // Create S3 client
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(region)
                .build();
    }
}


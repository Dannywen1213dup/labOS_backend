package com.labOS.backend.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 Client Configuration
 * Supports both AWS S3 and MinIO (S3-compatible storage)
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

    /**
     * Custom endpoint URL (for MinIO or other S3-compatible services)
     * If set, will use this endpoint instead of AWS S3
     * This is the internal endpoint used by the backend to communicate with S3/MinIO
     */
    private String endpoint;

    /**
     * Public endpoint URL for presigned URLs (for browser access)
     * If not set, will use the internal endpoint
     * This should be the host-accessible URL (e.g., http://localhost:9000 for MinIO)
     */
    private String publicEndpoint;

    @Bean
    public AmazonS3 amazonS3Client() {
        // Validate configuration
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new IllegalStateException("AWS S3 access key and secret key must be configured");
        }
        if (StringUtils.isBlank(bucket)) {
            throw new IllegalStateException("AWS S3 bucket name must be configured");
        }
        
        // Initialize AWS credentials
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        
        // If custom endpoint is provided (e.g., MinIO), use it
        if (StringUtils.isNotBlank(endpoint)) {
            // For MinIO and S3-compatible services, use path-style access
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                   .withPathStyleAccessEnabled(true);
        } else {
            // Use standard AWS S3
            builder.withRegion(region);
        }
        
        return builder.build();
    }

    /**
     * Create a separate S3 client for generating presigned URLs with public endpoint
     * This ensures the signature is valid for the public endpoint hostname
     * Only used when USE_LOCAL_MINIO is true
     */
    public AmazonS3 createPresignedUrlClient() {
        // Check if we should use public endpoint for presigned URLs
        String useLocalMinIO = System.getenv("USE_LOCAL_MINIO");
        if (useLocalMinIO == null || !useLocalMinIO.equalsIgnoreCase("true")) {
            // Not using local MinIO, return the standard client
            return amazonS3Client();
        }

        // If public endpoint is not configured, use standard client
        if (StringUtils.isBlank(publicEndpoint)) {
            return amazonS3Client();
        }

        // Create a client with public endpoint for presigned URL generation
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        
        // Use public endpoint for presigned URLs
        builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(publicEndpoint, region))
               .withPathStyleAccessEnabled(true);
        
        return builder.build();
    }
}


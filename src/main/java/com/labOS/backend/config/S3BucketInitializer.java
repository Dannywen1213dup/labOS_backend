package com.labOS.backend.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

/**
 * Initialize S3 bucket on application startup
 * Creates the bucket if it doesn't exist and configures CORS (for MinIO development)
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Component
@Slf4j
public class S3BucketInitializer implements CommandLineRunner {

    @Autowired
    private S3ClientConfig s3ClientConfig;

    @Autowired
    private AmazonS3 amazonS3;

    @Override
    public void run(String... args) {
        String bucketName = s3ClientConfig.getBucket();
        
        if (bucketName == null || bucketName.isEmpty()) {
            log.error("S3 bucket name is not configured!");
            return;
        }

        try {
            // Check if bucket exists
            if (amazonS3.doesBucketExistV2(bucketName)) {
                log.info("S3 bucket '{}' already exists", bucketName);
            } else {
                // Create bucket if it doesn't exist
                log.info("Creating S3 bucket '{}'...", bucketName);
                Bucket bucket = amazonS3.createBucket(bucketName);
                log.info("Successfully created S3 bucket '{}'", bucket.getName());
            }

            // Configure CORS if using local MinIO
            String useLocalMinIO = System.getenv("USE_LOCAL_MINIO");
            if ("true".equalsIgnoreCase(useLocalMinIO) && StringUtils.isNotBlank(s3ClientConfig.getEndpoint())) {
                try {
                    configureCORS(bucketName);
                } catch (Exception e) {
                    log.warn("Failed to configure CORS for bucket '{}': {}. You may need to configure CORS manually via MinIO console.", 
                            bucketName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            // Don't throw exception - allow application to continue
            // Bucket might be created manually or might not be needed immediately
        }
    }

    private void configureCORS(String bucketName) {
        log.info("Configuring CORS for bucket '{}'...", bucketName);
        
        CORSRule.AllowedMethods[] allowedMethods = {
            CORSRule.AllowedMethods.GET,
            CORSRule.AllowedMethods.PUT,
            CORSRule.AllowedMethods.POST,
            CORSRule.AllowedMethods.DELETE,
            CORSRule.AllowedMethods.HEAD
        };
        
        CORSRule corsRule = new CORSRule()
                .withAllowedOrigins(Collections.singletonList("*"))
                .withAllowedMethods(Arrays.asList(allowedMethods))
                .withAllowedHeaders(Collections.singletonList("*"))
                .withExposedHeaders(Collections.singletonList("ETag"))
                .withMaxAgeSeconds(3000);

        BucketCrossOriginConfiguration corsConfig = new BucketCrossOriginConfiguration(
                Collections.singletonList(corsRule));

        amazonS3.setBucketCrossOriginConfiguration(bucketName, corsConfig);
        log.info("Successfully configured CORS for bucket '{}'", bucketName);
    }
}


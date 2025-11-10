package com.labOS.backend.manager;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.labOS.backend.config.S3ClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * S3 Object Storage Operations
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Component
@Slf4j
public class S3Manager {

    @Resource
    private S3ClientConfig s3ClientConfig;

    @Resource
    private AmazonS3 amazonS3;

    private static final String PROJECT_PREFIX = "labOS";

    /**
     * Upload object to S3
     *
     * @param key  unique key
     * @param file file to upload
     * @return PutObjectResult
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3ClientConfig.getBucket(), key, file);
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * Upload object to S3 using input stream
     *
     * @param key         unique key
     * @param inputStream input stream
     * @param metadata    object metadata
     * @return PutObjectResult
     */
    public PutObjectResult putObject(String key, InputStream inputStream, ObjectMetadata metadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3ClientConfig.getBucket(), key, inputStream, metadata);
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * Check if folder exists
     *
     * @param folderPath folder path
     * @return true if exists
     */
    public boolean doesFolderExist(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3ClientConfig.getBucket())
                .withPrefix(folderPath)
                .withMaxKeys(1);
        ListObjectsV2Result result = amazonS3.listObjectsV2(request);
        return !result.getObjectSummaries().isEmpty();
    }

    /**
     * Create folder (by creating an empty object)
     *
     * @param folderPath folder path
     */
    public void createFolder(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3ClientConfig.getBucket(), folderPath, emptyContent, metadata);
        amazonS3.putObject(putObjectRequest);
        log.info("Created folder: {}", folderPath);
    }

    /**
     * Get or create upload folder path and return the count
     * Folder structure: labOS/{uuid}/{MMDDYYYY}/{count}/
     *
     * @param uuid user UUID
     * @return complete folder path
     */
    public String getOrCreateUploadFolder(String uuid) {
        // Build base path
        String basePath = PROJECT_PREFIX + "/" + uuid + "/";
        
        // Get today's date (MMDDYYYY format)
        LocalDate today = LocalDate.now();
        String dateFolder = today.format(DateTimeFormatter.ofPattern("MMddyyyy"));
        String datePath = basePath + dateFolder + "/";

        // Check if UUID folder exists, create if not
        if (!doesFolderExist(basePath)) {
            createFolder(basePath);
            log.info("Created UUID folder: {}", basePath);
        }

        // Check if date folder exists, create if not
        if (!doesFolderExist(datePath)) {
            createFolder(datePath);
            log.info("Created date folder: {}", datePath);
        }

        // Find the maximum count for today
        int maxCount = getMaxCountForDate(datePath);
        int nextCount = maxCount + 1;

        // Create new count folder
        String countPath = datePath + nextCount + "/";
        createFolder(countPath);
        log.info("Created count folder: {}", countPath);

        return countPath;
    }

    /**
     * Get the maximum count under the specified date folder
     *
     * @param datePath date folder path
     * @return maximum count
     */
    private int getMaxCountForDate(String datePath) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3ClientConfig.getBucket())
                .withPrefix(datePath)
                .withDelimiter("/");

        ListObjectsV2Result result = amazonS3.listObjectsV2(request);
        int maxCount = 0;

        for (String prefix : result.getCommonPrefixes()) {
            String folder = prefix.replace(datePath, "").replace("/", "");
            try {
                int count = Integer.parseInt(folder);
                if (count > maxCount) {
                    maxCount = count;
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric folders
            }
        }

        return maxCount;
    }

    /**
     * Delete folder and all its contents
     *
     * @param folderPath folder path
     */
    public void deleteFolder(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        // List all objects
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3ClientConfig.getBucket())
                .withPrefix(folderPath);

        ListObjectsV2Result result;
        do {
            result = amazonS3.listObjectsV2(request);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                amazonS3.deleteObject(s3ClientConfig.getBucket(), objectSummary.getKey());
                log.info("Deleted object: {}", objectSummary.getKey());
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        log.info("Deleted folder: {}", folderPath);
    }

    /**
     * Download all files in folder and package as ZIP
     *
     * @param folderPath folder path
     * @return ZIP file as temporary file
     * @throws IOException IO exception
     */
    public File downloadFolderAsZip(String folderPath) throws IOException {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        // Create temporary ZIP file
        File zipFile = File.createTempFile("download-", ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // List all files in folder
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(s3ClientConfig.getBucket())
                    .withPrefix(folderPath);

            ListObjectsV2Result result;
            do {
                result = amazonS3.listObjectsV2(request);
                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    String key = objectSummary.getKey();
                    
                    // Skip folder marker objects
                    if (key.endsWith("/")) {
                        continue;
                    }

                    // Download file
                    S3Object s3Object = amazonS3.getObject(s3ClientConfig.getBucket(), key);
                    
                    // Get relative path as file name in ZIP
                    String fileName = key.replace(folderPath, "");
                    
                    // Add to ZIP
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);
                    
                    try (InputStream is = s3Object.getObjectContent()) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                    
                    zos.closeEntry();
                    log.info("Added to ZIP: {}", fileName);
                }
                request.setContinuationToken(result.getNextContinuationToken());
            } while (result.isTruncated());
        }

        return zipFile;
    }

    /**
     * Generate presigned URL
     *
     * @param key            object key
     * @param expirationTime expiration time in milliseconds
     * @return presigned URL
     */
    public String generatePresignedUrl(String key, long expirationTime) {
        Date expiration = new Date(System.currentTimeMillis() + expirationTime);
        URL url = amazonS3.generatePresignedUrl(s3ClientConfig.getBucket(), key, expiration);
        return url.toString();
    }

    /**
     * Get upload progress (return file count in folder)
     *
     * @param folderPath folder path
     * @return file count
     */
    public int getUploadProgress(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3ClientConfig.getBucket())
                .withPrefix(folderPath);

        int fileCount = 0;
        ListObjectsV2Result result;
        do {
            result = amazonS3.listObjectsV2(request);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                // Only count files, not folders
                if (!objectSummary.getKey().endsWith("/")) {
                    fileCount++;
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return fileCount;
    }

    /**
     * List all files in folder
     *
     * @param folderPath folder path
     * @return list of file keys
     */
    public List<String> listFiles(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        List<String> files = new ArrayList<>();
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3ClientConfig.getBucket())
                .withPrefix(folderPath);

        ListObjectsV2Result result;
        do {
            result = amazonS3.listObjectsV2(request);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                // Only add files, not folders
                if (!key.endsWith("/")) {
                    files.add(key);
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return files;
    }
}

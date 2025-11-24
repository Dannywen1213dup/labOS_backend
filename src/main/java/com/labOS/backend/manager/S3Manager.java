package com.labOS.backend.manager;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.labOS.backend.config.S3ClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
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

    // Safe characters pattern: alphanumeric, space, underscore, dot, asterisk, single quote, parentheses, dash, forward slash
    private static final Pattern SAFE_CHARACTERS = Pattern.compile("[^0-9a-zA-Z! _\\.\\*'\\(\\)\\-\\/]");

    // Dangerous SQL keywords and patterns (case-insensitive)
    private static final Pattern[] DANGEROUS_SQL_PATTERNS = {
        Pattern.compile("(?i)DROP\\s+TABLE"),
        Pattern.compile("(?i)DROP\\s+DATABASE"),
        Pattern.compile("(?i)DELETE\\s+FROM"),
        Pattern.compile("(?i)TRUNCATE\\s+TABLE"),
        Pattern.compile("(?i)ALTER\\s+TABLE"),
        Pattern.compile("(?i)CREATE\\s+TABLE"),
        Pattern.compile("(?i)INSERT\\s+INTO"),
        Pattern.compile("(?i)UPDATE\\s+.*SET"),
        Pattern.compile("(?i)WHERE\\s+.*SELECT"),
        Pattern.compile("(?i)SELECT\\s+.*FROM"),
        Pattern.compile("(?i)UNION\\s+SELECT"),
        Pattern.compile("(?i)EXEC\\s*\\("),
        Pattern.compile("(?i)EXECUTE\\s*\\("),
        Pattern.compile("(?i)<SCRIPT"),
        Pattern.compile("(?i)JAVASCRIPT:"),
        Pattern.compile("(?i)ONLOAD\\s*="),
        Pattern.compile("(?i)ONERROR\\s*="),
        Pattern.compile("';\\s*--"),
        Pattern.compile("';\\s*#"),
        Pattern.compile("(?i)OR\\s+1\\s*=\\s*1"),
        Pattern.compile("(?i)OR\\s+'1'\\s*=\\s*'1'")
    };

    // Latin character map for replacement
    private static final Map<String, String> LATIN_CHAR_MAP = createLatinCharMap();

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
     * Get or create dataset folder for user
     * Folder structure: labOS/datasets/{userId}/
     *
     * @param userId user ID
     * @return complete folder path
     */
    public String getOrCreateDatasetFolder(String userId) {
        String folderPath = PROJECT_PREFIX + "/datasets/" + userId + "/";
        if (!doesFolderExist(folderPath)) {
            createFolder(folderPath);
            log.info("Created dataset folder: {}", folderPath);
        }
        return folderPath;
    }

    /**
     * Get or create benchmark-eval folder for user
     * Folder structure: labOS/benchmark-eval/{userId}/
     *
     * @param userId user ID
     * @return complete folder path
     */
    public String getOrCreateBenchmarkEvalFolder(String userId) {
        String folderPath = PROJECT_PREFIX + "/benchmark-eval/" + userId + "/";
        if (!doesFolderExist(folderPath)) {
            createFolder(folderPath);
            log.info("Created benchmark-eval folder: {}", folderPath);
        }
        return folderPath;
    }

    /**
     * Get or create upload folder path and return the count
     * Folder structure: labOS/{uuid}/{MMDDYYYY}/{count}/
     * 
     * // KEEP FOR NOW/REFERENCE
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
     * Generate presigned URL (for GET/download operations)
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
     * Generate presigned URL for PUT/upload operations
     *
     * @param key            object key (full path including folder and filename)
     * @param expirationTime expiration time in milliseconds
     * @return presigned URL for upload
     */
    public String generatePresignedUploadUrl(String key, long expirationTime) {
        Date expiration = new Date(System.currentTimeMillis() + expirationTime);
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                s3ClientConfig.getBucket(), key, HttpMethod.PUT);
        generatePresignedUrlRequest.setExpiration(expiration);
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    /**
     * Configure CORS on the S3 bucket to allow uploads from frontend origins
     * This should be called once during setup or can be done manually via AWS Console
     *
     * @param allowedOrigins list of allowed origins (e.g., http://localhost:8080, https://example.com)
     */
    public void configureBucketCors(List<String> allowedOrigins) {
        try {
            BucketCrossOriginConfiguration configuration = new BucketCrossOriginConfiguration();
            List<CORSRule> corsRules = new ArrayList<>();
            
            CORSRule corsRule = new CORSRule();
            corsRule.setAllowedOrigins(allowedOrigins);
            corsRule.setAllowedMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "HEAD"));
            corsRule.setAllowedHeaders(Arrays.asList("*"));
            corsRule.setExposedHeaders(Arrays.asList("ETag", "x-amz-server-side-encryption", 
                    "x-amz-request-id", "x-amz-id-2"));
            corsRule.setMaxAgeSeconds(3000);
            corsRule.setAllowCredentials(true);
            
            corsRules.add(corsRule);
            configuration.setRules(corsRules);
            
            SetBucketCrossOriginConfigurationRequest request = new SetBucketCrossOriginConfigurationRequest(
                    s3ClientConfig.getBucket(), configuration);
            amazonS3.setBucketCrossOriginConfiguration(request);
            
            log.info("Successfully configured CORS for bucket: {}", s3ClientConfig.getBucket());
        } catch (Exception e) {
            log.error("Failed to configure CORS for bucket: {}", s3ClientConfig.getBucket(), e);
            throw new RuntimeException("Failed to configure S3 bucket CORS", e);
        }
    }

    /**
     * Create Latin character map for character replacement
     *
     * @return Map of Latin characters to ASCII equivalents
     */
    private static Map<String, String> createLatinCharMap() {
        Map<String, String> map = new HashMap<>();
        // Common Latin characters and their ASCII equivalents
        map.put("à", "a"); map.put("á", "a"); map.put("â", "a"); map.put("ã", "a"); map.put("ä", "a"); map.put("å", "a");
        map.put("è", "e"); map.put("é", "e"); map.put("ê", "e"); map.put("ë", "e");
        map.put("ì", "i"); map.put("í", "i"); map.put("î", "i"); map.put("ï", "i");
        map.put("ò", "o"); map.put("ó", "o"); map.put("ô", "o"); map.put("õ", "o"); map.put("ö", "o");
        map.put("ù", "u"); map.put("ú", "u"); map.put("û", "u"); map.put("ü", "u");
        map.put("ý", "y"); map.put("ÿ", "y");
        map.put("ñ", "n");
        map.put("ç", "c");
        map.put("À", "A"); map.put("Á", "A"); map.put("Â", "A"); map.put("Ã", "A"); map.put("Ä", "A"); map.put("Å", "A");
        map.put("È", "E"); map.put("É", "E"); map.put("Ê", "E"); map.put("Ë", "E");
        map.put("Ì", "I"); map.put("Í", "I"); map.put("Î", "I"); map.put("Ï", "I");
        map.put("Ò", "O"); map.put("Ó", "O"); map.put("Ô", "O"); map.put("Õ", "O"); map.put("Ö", "O");
        map.put("Ù", "U"); map.put("Ú", "U"); map.put("Û", "U"); map.put("Ü", "U");
        map.put("Ý", "Y");
        map.put("Ñ", "N");
        map.put("Ç", "C");
        return map;
    }

    /**
     * Check if filename contains dangerous SQL patterns
     *
     * @param fileName filename to check
     * @return true if contains dangerous patterns
     */
    private boolean containsDangerousPatterns(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        
        String upperCase = fileName.toUpperCase();
        
        // Check against dangerous SQL patterns
        for (Pattern pattern : DANGEROUS_SQL_PATTERNS) {
            if (pattern.matcher(fileName).find()) {
                return true;
            }
        }
        
        // Additional simple keyword checks (case-insensitive)
        String[] dangerousKeywords = {
            "DROP TABLE", "DROP DATABASE", "DELETE FROM", "TRUNCATE", "ALTER TABLE",
            "CREATE TABLE", "INSERT INTO", "UPDATE", "WHERE", "SELECT", "UNION",
            "EXEC", "EXECUTE", "SCRIPT", "JAVASCRIPT", "ONLOAD", "ONERROR",
            "OR 1=1", "OR '1'='1'", "'; --", "'; #"
        };
        for (String keyword : dangerousKeywords) {
            if (upperCase.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Replace Latin characters with ASCII equivalents
     *
     * @param value string to process
     * @return string with Latin characters replaced
     */
    private String replaceLatinCharacters(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            String charStr = String.valueOf(c);
            String replacement = LATIN_CHAR_MAP.get(charStr);
            result.append(replacement != null ? replacement : charStr);
        }
        return result.toString();
    }

    /**
     * Remove illegal characters based on safe character pattern
     *
     * @param value string to process
     * @return string with illegal characters removed
     */
    private String removeIllegalCharacters(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return SAFE_CHARACTERS.matcher(value).replaceAll("");
    }

    /**
     * Sanitize filename for S3 bucket safety and SQL injection prevention
     * 
     * This method:
     * 1. Checks for dangerous SQL patterns and removes/replaces them
     * 2. Replaces Latin characters with ASCII equivalents
     * 3. Removes illegal characters (keeps only safe characters)
     * 4. Replaces spaces with separator (default: dash)
     * 5. Ensures filename is safe for S3 storage
     *
     * @param fileName original filename
     * @param separator character to replace spaces with (default: "-")
     * @return sanitized filename safe for S3
     */
    public String sanitizeFileName(String fileName, String separator) {
        if (StringUtils.isBlank(fileName)) {
            return "file_" + System.currentTimeMillis();
        }

        // Validate separator
        if (StringUtils.isBlank(separator) || SAFE_CHARACTERS.matcher(separator).find()) {
            separator = "-";
        }

        // Trim whitespace
        String sanitized = fileName.trim();

        // Check for dangerous SQL patterns - if found, replace with safe alternative
        if (containsDangerousPatterns(sanitized)) {
            log.warn("Filename contains dangerous SQL patterns, sanitizing: {}", sanitized);
            // Remove dangerous patterns by replacing with underscore
            for (Pattern pattern : DANGEROUS_SQL_PATTERNS) {
                sanitized = pattern.matcher(sanitized).replaceAll("_");
            }
            // Additional cleanup for common SQL keywords (case-insensitive replacement)
            String[] dangerousKeywords = {
                "DROP", "TABLE", "DATABASE", "DELETE", "FROM", "TRUNCATE", "ALTER", "CREATE",
                "INSERT", "INTO", "UPDATE", "SET", "WHERE", "SELECT", "UNION", "EXEC", "EXECUTE",
                "SCRIPT", "JAVASCRIPT", "ONLOAD", "ONERROR", "OR 1=1", "OR '1'='1'"
            };
            for (String keyword : dangerousKeywords) {
                // Replace keyword (case-insensitive) with underscore
                sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(keyword), "_");
            }
            // Remove SQL injection characters
            sanitized = sanitized.replaceAll("[';\"#--]", "_");
        }

        // Replace Latin characters with ASCII equivalents
        sanitized = replaceLatinCharacters(sanitized);

        // Remove illegal characters (keep only safe characters)
        sanitized = removeIllegalCharacters(sanitized);

        // Replace spaces with separator
        sanitized = sanitized.replaceAll("\\s+", separator);

        // Remove multiple consecutive separators
        sanitized = sanitized.replaceAll(Pattern.quote(separator) + "{2,}", separator);

        // Remove leading/trailing separators, dots, and underscores
        sanitized = sanitized.replaceAll("^[._" + Pattern.quote(separator) + "]+|[._" + Pattern.quote(separator) + "]+$", "");

        // Ensure filename is not empty
        if (sanitized.isEmpty()) {
            sanitized = "file_" + System.currentTimeMillis();
        }

        // Limit filename length (S3 key limit is 1024, but keep reasonable)
        if (sanitized.length() > 255) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0 && lastDot < sanitized.length() - 1) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, lastDot);
            }
            int maxLength = 255 - extension.length();
            if (maxLength > 0) {
                sanitized = sanitized.substring(0, Math.min(sanitized.length(), maxLength)) + extension;
            } else {
                sanitized = "file_" + System.currentTimeMillis() + extension;
            }
        }

        return sanitized;
    }

    /**
     * Sanitize filename for S3 bucket safety (using default separator "-")
     *
     * @param fileName original filename
     * @return sanitized filename safe for S3
     */
    public String sanitizeFileName(String fileName) {
        return sanitizeFileName(fileName, "-");
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

    /**
     * Configure CORS on the S3 bucket to allow uploads from frontend origins
     * This should be called once during setup or can be done manually via AWS Console
     *
     * @param allowedOrigins list of allowed origins (e.g., http://localhost:8080, https://example.com)
     */
    public void configureBucketCors(List<String> allowedOrigins) {
        try {
            BucketCrossOriginConfiguration configuration = new BucketCrossOriginConfiguration();
            List<CORSRule> corsRules = new ArrayList<>();
            
            CORSRule corsRule = new CORSRule();
            corsRule.setAllowedOrigins(allowedOrigins);
            corsRule.setAllowedMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "HEAD", "OPTIONS"));
            corsRule.setAllowedHeaders(Arrays.asList("*"));
            corsRule.setExposedHeaders(Arrays.asList("ETag", "x-amz-server-side-encryption", 
                    "x-amz-request-id", "x-amz-id-2"));
            corsRule.setMaxAgeSeconds(3000);
            corsRule.setAllowCredentials(true);
            
            corsRules.add(corsRule);
            configuration.setRules(corsRules);
            
            SetBucketCrossOriginConfigurationRequest request = new SetBucketCrossOriginConfigurationRequest(
                    s3ClientConfig.getBucket(), configuration);
            amazonS3.setBucketCrossOriginConfiguration(request);
            
            log.info("Successfully configured CORS for bucket: {} with origins: {}", 
                    s3ClientConfig.getBucket(), allowedOrigins);
        } catch (Exception e) {
            log.error("Failed to configure CORS for bucket: {}", s3ClientConfig.getBucket(), e);
            throw new RuntimeException("Failed to configure S3 bucket CORS: " + e.getMessage(), e);
        }
    }
}

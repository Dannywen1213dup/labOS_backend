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
 * S3 对象存储操作
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
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return PutObjectResult
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3ClientConfig.getBucket(), key, file);
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * 上传对象（输入流方式）
     *
     * @param key         唯一键
     * @param inputStream 输入流
     * @param metadata    元数据
     * @return PutObjectResult
     */
    public PutObjectResult putObject(String key, InputStream inputStream, ObjectMetadata metadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3ClientConfig.getBucket(), key, inputStream, metadata);
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * 检查文件夹是否存在
     *
     * @param folderPath 文件夹路径
     * @return 是否存在
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
     * 创建文件夹（通过创建一个空对象实现）
     *
     * @param folderPath 文件夹路径
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
     * 获取或创建上传文件夹路径，并返回次数
     * 文件夹结构: labOS/{uuid}/{MMDDYYYY}/{count}/
     *
     * @param uuid 用户UUID
     * @return 完整的文件夹路径
     */
    public String getOrCreateUploadFolder(String uuid) {
        // 构建基础路径
        String basePath = PROJECT_PREFIX + "/" + uuid + "/";
        
        // 获取今天的日期 (MMDDYYYY格式)
        LocalDate today = LocalDate.now();
        String dateFolder = today.format(DateTimeFormatter.ofPattern("MMddyyyy"));
        String datePath = basePath + dateFolder + "/";

        // 检查 UUID 文件夹是否存在，不存在则创建
        if (!doesFolderExist(basePath)) {
            createFolder(basePath);
            log.info("Created UUID folder: {}", basePath);
        }

        // 检查日期文件夹是否存在，不存在则创建
        if (!doesFolderExist(datePath)) {
            createFolder(datePath);
            log.info("Created date folder: {}", datePath);
        }

        // 查找今天的最大次数
        int maxCount = getMaxCountForDate(datePath);
        int nextCount = maxCount + 1;

        // 创建新的次数文件夹
        String countPath = datePath + nextCount + "/";
        createFolder(countPath);
        log.info("Created count folder: {}", countPath);

        return countPath;
    }

    /**
     * 获取指定日期文件夹下的最大次数
     *
     * @param datePath 日期文件夹路径
     * @return 最大次数
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
                // 忽略非数字文件夹
            }
        }

        return maxCount;
    }

    /**
     * 删除文件夹及其所有内容
     *
     * @param folderPath 文件夹路径
     */
    public void deleteFolder(String folderPath) {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        // 列出所有对象
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
     * 获取文件夹下所有文件并打包成 ZIP
     *
     * @param folderPath 文件夹路径
     * @return ZIP 文件的临时文件
     * @throws IOException IO异常
     */
    public File downloadFolderAsZip(String folderPath) throws IOException {
        if (!folderPath.endsWith("/")) {
            folderPath = folderPath + "/";
        }

        // 创建临时 ZIP 文件
        File zipFile = File.createTempFile("download-", ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 列出文件夹下所有文件
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(s3ClientConfig.getBucket())
                    .withPrefix(folderPath);

            ListObjectsV2Result result;
            do {
                result = amazonS3.listObjectsV2(request);
                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    String key = objectSummary.getKey();
                    
                    // 跳过文件夹标记对象
                    if (key.endsWith("/")) {
                        continue;
                    }

                    // 下载文件
                    S3Object s3Object = amazonS3.getObject(s3ClientConfig.getBucket(), key);
                    
                    // 获取相对路径作为 ZIP 内的文件名
                    String fileName = key.replace(folderPath, "");
                    
                    // 添加到 ZIP
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
     * 生成预签名 URL
     *
     * @param key            对象键
     * @param expirationTime 过期时间（毫秒）
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String key, long expirationTime) {
        Date expiration = new Date(System.currentTimeMillis() + expirationTime);
        URL url = amazonS3.generatePresignedUrl(s3ClientConfig.getBucket(), key, expiration);
        return url.toString();
    }

    /**
     * 获取上传进度（返回文件夹中的文件数量）
     *
     * @param folderPath 文件夹路径
     * @return 文件数量
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
                // 只计算文件，不计算文件夹
                if (!objectSummary.getKey().endsWith("/")) {
                    fileCount++;
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return fileCount;
    }

    /**
     * 列出文件夹下的所有文件
     *
     * @param folderPath 文件夹路径
     * @return 文件列表
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
                // 只添加文件，不添加文件夹
                if (!key.endsWith("/")) {
                    files.add(key);
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return files;
    }
}


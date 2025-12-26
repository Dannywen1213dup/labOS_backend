package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Upload Statistics View Object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UploadStatisticsVO implements Serializable {

    /**
     * Total batches
     */
    private Long totalBatches;

    /**
     * Total files uploaded
     */
    private Long totalFiles;

    /**
     * Successful uploads
     */
    private Long successfulUploads;

    /**
     * Failed uploads
     */
    private Long failedUploads;

    /**
     * Pending uploads
     */
    private Long pendingUploads;

    /**
     * Overall success rate
     */
    private Double overallSuccessRate;

    /**
     * Total storage used in bytes
     */
    private Long totalStorageBytes;

    /**
     * Total storage formatted (e.g., "1.5 GB")
     */
    private String totalStorageFormatted;

    /**
     * Dataset files count
     */
    private Long datasetFilesCount;

    /**
     * Benchmark eval files count
     */
    private Long benchmarkEvalFilesCount;

    private static final long serialVersionUID = 1L;
}


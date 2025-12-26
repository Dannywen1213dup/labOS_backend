package com.labOS.backend.model.dto.file;

import com.labOS.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Upload History Query Request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UploadHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * Batch status filter: PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS
     */
    private String batchStatus;

    /**
     * Folder type filter: dataset, benchmark-eval
     */
    private String folderType;

    /**
     * Start date for filtering (inclusive)
     */
    private Date startDate;

    /**
     * End date for filtering (inclusive)
     */
    private Date endDate;

    /**
     * Search keyword (file name search)
     */
    private String keyword;

    private static final long serialVersionUID = 1L;
}


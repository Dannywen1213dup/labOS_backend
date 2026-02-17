package com.labOS.backend.common;

import com.labOS.backend.constant.CommonConstant;
import lombok.Data;

/**
 * paging requests
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class PageRequest {

    /**
     * current page number
     */
    private int current = 1;

    /**
     * page size
     */
    private int pageSize = 10;

    /**
     * sort field
     */
    private String sortField;

    /**
     * order (ascending/descending)
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}

package ${packageName}.model.dto.${dataKey};

import ${packageName}.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * Query ${dataName} request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ${upperDataKey}QueryRequest extends PageRequest implements Serializable {

    /**
     * Id
     */
    private Long id;

    /**
     * Not id
     */
    private Long notId;

    /**
     * Search text
     */
    private String searchText;

    /**
     * Title
     */
    private String title;

    /**
     * Content
     */
    private String content;

    /**
     * Tag list
     */
    private List<String> tags;

    /**
     * Creator user id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
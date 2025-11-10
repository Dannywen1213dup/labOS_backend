package ${packageName}.model.dto.${dataKey};

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Edit ${dataName} request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 */
@Data
public class ${upperDataKey}EditRequest implements Serializable {

    /**
     * Id
     */
    private Long id;

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

    private static final long serialVersionUID = 1L;
}
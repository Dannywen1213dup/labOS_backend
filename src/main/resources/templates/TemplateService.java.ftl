package ${packageName}.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}QueryRequest;
import ${packageName}.model.entity.${upperDataKey};
import ${packageName}.model.vo.${upperDataKey}VO;

import javax.servlet.http.HttpServletRequest;

/**
 * ${dataName} service
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 */
public interface ${upperDataKey}Service extends IService<${upperDataKey}> {

    /**
     * Validate data
     *
     * @param ${dataKey}
     * @param add Validate data for creation
     */
    void valid${upperDataKey}(${upperDataKey} ${dataKey}, boolean add);

    /**
     * Get query wrapper
     *
     * @param ${dataKey}QueryRequest
     * @return
     */
    QueryWrapper<${upperDataKey}> getQueryWrapper(${upperDataKey}QueryRequest ${dataKey}QueryRequest);
    
    /**
     * Get ${dataName} VO
     *
     * @param ${dataKey}
     * @param request
     * @return
     */
    ${upperDataKey}VO get${upperDataKey}VO(${upperDataKey} ${dataKey}, HttpServletRequest request);

    /**
     * Get ${dataName} VO page
     *
     * @param ${dataKey}Page
     * @param request
     * @return
     */
    Page<${upperDataKey}VO> get${upperDataKey}VOPage(Page<${upperDataKey}> ${dataKey}Page, HttpServletRequest request);
}
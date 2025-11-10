package ${packageName}.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${packageName}.annotation.AuthCheck;
import ${packageName}.common.BaseResponse;
import ${packageName}.common.DeleteRequest;
import ${packageName}.common.ErrorCode;
import ${packageName}.common.ResultUtils;
import ${packageName}.constant.UserConstant;
import ${packageName}.exception.BusinessException;
import ${packageName}.exception.ThrowUtils;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}AddRequest;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}EditRequest;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}QueryRequest;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}UpdateRequest;
import ${packageName}.model.entity.${upperDataKey};
import ${packageName}.model.entity.User;
import ${packageName}.model.vo.${upperDataKey}VO;
import ${packageName}.service.${upperDataKey}Service;
import ${packageName}.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * ${dataName} interface
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 */
@RestController
@RequestMapping("/${dataKey}")
@Slf4j
public class ${upperDataKey}Controller {

    @Resource
    private ${upperDataKey}Service ${dataKey}Service;

    @Resource
    private UserService userService;

    // region CRUD

    /**
     * Create ${dataName}
     *
     * @param ${dataKey}AddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> add${upperDataKey}(@RequestBody ${upperDataKey}AddRequest ${dataKey}AddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(${dataKey}AddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo Convert entity and DTO here
        ${upperDataKey} ${dataKey} = new ${upperDataKey}();
        BeanUtils.copyProperties(${dataKey}AddRequest, ${dataKey});
        // Data validation
        ${dataKey}Service.valid${upperDataKey}(${dataKey}, true);
        // todo Fill default values
        User loginUser = userService.getLoginUser(request);
        ${dataKey}.setUserId(loginUser.getId());
        // Write to database
        boolean result = ${dataKey}Service.save(${dataKey});
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // Return newly written data id
        long new${upperDataKey}Id = ${dataKey}.getId();
        return ResultUtils.success(new${upperDataKey}Id);
    }

    /**
     * Delete ${dataName}
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete${upperDataKey}(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // Check if exists
        ${upperDataKey} old${upperDataKey} = ${dataKey}Service.getById(id);
        ThrowUtils.throwIf(old${upperDataKey} == null, ErrorCode.NOT_FOUND_ERROR);
        // Only owner or admin can delete
        if (!old${upperDataKey}.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // Operate database
        boolean result = ${dataKey}Service.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Update ${dataName} (admin only)
     *
     * @param ${dataKey}UpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update${upperDataKey}(@RequestBody ${upperDataKey}UpdateRequest ${dataKey}UpdateRequest) {
        if (${dataKey}UpdateRequest == null || ${dataKey}UpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo Convert entity and DTO here
        ${upperDataKey} ${dataKey} = new ${upperDataKey}();
        BeanUtils.copyProperties(${dataKey}UpdateRequest, ${dataKey});
        // Data validation
        ${dataKey}Service.valid${upperDataKey}(${dataKey}, false);
        // Check if exists
        long id = ${dataKey}UpdateRequest.getId();
        ${upperDataKey} old${upperDataKey} = ${dataKey}Service.getById(id);
        ThrowUtils.throwIf(old${upperDataKey} == null, ErrorCode.NOT_FOUND_ERROR);
        // Operate database
        boolean result = ${dataKey}Service.updateById(${dataKey});
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Get ${dataName} by id (VO)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<${upperDataKey}VO> get${upperDataKey}VOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // Query database
        ${upperDataKey} ${dataKey} = ${dataKey}Service.getById(id);
        ThrowUtils.throwIf(${dataKey} == null, ErrorCode.NOT_FOUND_ERROR);
        // Get VO
        return ResultUtils.success(${dataKey}Service.get${upperDataKey}VO(${dataKey}, request));
    }

    /**
     * Get ${dataName} list by page (admin only)
     *
     * @param ${dataKey}QueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<${upperDataKey}>> list${upperDataKey}ByPage(@RequestBody ${upperDataKey}QueryRequest ${dataKey}QueryRequest) {
        long current = ${dataKey}QueryRequest.getCurrent();
        long size = ${dataKey}QueryRequest.getPageSize();
        // Query database
        Page<${upperDataKey}> ${dataKey}Page = ${dataKey}Service.page(new Page<>(current, size),
                ${dataKey}Service.getQueryWrapper(${dataKey}QueryRequest));
        return ResultUtils.success(${dataKey}Page);
    }

    /**
     * Get ${dataName} list by page (VO)
     *
     * @param ${dataKey}QueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<${upperDataKey}VO>> list${upperDataKey}VOByPage(@RequestBody ${upperDataKey}QueryRequest ${dataKey}QueryRequest,
                                                               HttpServletRequest request) {
        long current = ${dataKey}QueryRequest.getCurrent();
        long size = ${dataKey}QueryRequest.getPageSize();
        // Limit crawlers
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // Query database
        Page<${upperDataKey}> ${dataKey}Page = ${dataKey}Service.page(new Page<>(current, size),
                ${dataKey}Service.getQueryWrapper(${dataKey}QueryRequest));
        // Get VO
        return ResultUtils.success(${dataKey}Service.get${upperDataKey}VOPage(${dataKey}Page, request));
    }

    /**
     * Get current logged-in user's ${dataName} list by page
     *
     * @param ${dataKey}QueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<${upperDataKey}VO>> listMy${upperDataKey}VOByPage(@RequestBody ${upperDataKey}QueryRequest ${dataKey}QueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(${dataKey}QueryRequest == null, ErrorCode.PARAMS_ERROR);
        // Add query condition, only query current logged-in user's data
        User loginUser = userService.getLoginUser(request);
        ${dataKey}QueryRequest.setUserId(loginUser.getId());
        long current = ${dataKey}QueryRequest.getCurrent();
        long size = ${dataKey}QueryRequest.getPageSize();
        // Limit crawlers
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // Query database
        Page<${upperDataKey}> ${dataKey}Page = ${dataKey}Service.page(new Page<>(current, size),
                ${dataKey}Service.getQueryWrapper(${dataKey}QueryRequest));
        // Get VO
        return ResultUtils.success(${dataKey}Service.get${upperDataKey}VOPage(${dataKey}Page, request));
    }

    /**
     * Edit ${dataName} (for users)
     *
     * @param ${dataKey}EditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> edit${upperDataKey}(@RequestBody ${upperDataKey}EditRequest ${dataKey}EditRequest, HttpServletRequest request) {
        if (${dataKey}EditRequest == null || ${dataKey}EditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo Convert entity and DTO here
        ${upperDataKey} ${dataKey} = new ${upperDataKey}();
        BeanUtils.copyProperties(${dataKey}EditRequest, ${dataKey});
        // Data validation
        ${dataKey}Service.valid${upperDataKey}(${dataKey}, false);
        User loginUser = userService.getLoginUser(request);
        // Check if exists
        long id = ${dataKey}EditRequest.getId();
        ${upperDataKey} old${upperDataKey} = ${dataKey}Service.getById(id);
        ThrowUtils.throwIf(old${upperDataKey} == null, ErrorCode.NOT_FOUND_ERROR);
        // Only owner or admin can edit
        if (!old${upperDataKey}.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // Operate database
        boolean result = ${dataKey}Service.updateById(${dataKey});
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}

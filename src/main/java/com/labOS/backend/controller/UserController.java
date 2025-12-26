package com.labOS.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labOS.backend.annotation.AuthCheck;
import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.DeleteRequest;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.constant.UserConstant;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.exception.ThrowUtils;
import com.labOS.backend.model.dto.user.UserAddRequest;
import com.labOS.backend.model.dto.user.UserChangePasswordRequest;
import com.labOS.backend.model.dto.user.UserQueryRequest;
import com.labOS.backend.model.dto.user.UserUpdateNameRequest;
import com.labOS.backend.model.dto.user.UserUpdateMyRequest;
import com.labOS.backend.model.dto.user.UserUpdateRequest;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.LoginUserVO;
import com.labOS.backend.model.vo.UserVO;
import com.labOS.backend.satoken.SaTokenUtil;
import com.labOS.backend.service.UserService;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import static com.labOS.backend.service.impl.UserServiceImpl.SALT;

/**
 * User Interface
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;


    // region User Info

    /**
     * Get current logged-in user
     * Note: Registration and login are now handled by AuthController
     *
     * @param request HTTP servlet request
     * @return Current logged-in user information
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region CRUD

    /**
     * Create user
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // Default password 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * Delete user
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * Update user
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Get user by id (admin only)
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * Get VO by id
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * Get user list by page (admin only)
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * Get user VO list by page
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // Limit crawlers
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * Update personal information
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
            HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Update first name and last name (logged-in user)
     *
     * @param updateNameRequest request containing firstName and lastName
     * @return success
     */
    @PostMapping("/update/name")
    public BaseResponse<Boolean> updateName(@RequestBody UserUpdateNameRequest updateNameRequest) {
        if (updateNameRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }
        SaTokenUtil.checkLogin();
        Long userId = SaTokenUtil.getUserId();
        boolean ok = userService.updateUserName(userId, updateNameRequest.getFirstName(), updateNameRequest.getLastName());
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "Failed to update name");
        return ResultUtils.success(true);
    }

    /**
     * Change password (logged-in user)
     *
     * @param request change password request
     * @return success
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> changePassword(@RequestBody UserChangePasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }
        SaTokenUtil.checkLogin();
        Long userId = SaTokenUtil.getUserId();
        boolean ok = userService.changePassword(userId, request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "Failed to change password");
        return ResultUtils.success(true);
    }

    /**
     * Upload & replace user avatar
     *
     * Upload image to S3 under: labOS/UserAvatar/{userId}/...
     * Then update user.userAvatar (permanent URL) and delete previous avatar object in S3.
     *
     * @param file avatar image file
     * @return permanent URL of the new avatar
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> updateAvatar(@RequestPart("file") MultipartFile file) {
        SaTokenUtil.checkLogin();
        Long userId = SaTokenUtil.getUserId();
        String url = userService.updateUserAvatar(userId, file);
        return ResultUtils.success(url);
    }
}

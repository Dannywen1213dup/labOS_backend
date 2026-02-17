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
import com.labOS.backend.model.dto.user.GenerateAvatarPresignedUrlRequest;
import com.labOS.backend.model.dto.user.UpdateAvatarRequest;
import com.labOS.backend.model.dto.user.UserAddRequest;
import com.labOS.backend.model.dto.user.UserChangePasswordRequest;
import com.labOS.backend.model.dto.user.UserQueryRequest;
import com.labOS.backend.model.dto.user.UserUpdateNameRequest;
import com.labOS.backend.model.dto.user.UserUpdateMyRequest;
import com.labOS.backend.model.dto.user.UserUpdateRequest;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.AvatarPresignedUrlVO;
import com.labOS.backend.model.vo.LoginUserVO;
import com.labOS.backend.model.vo.UserVO;
import com.labOS.backend.satoken.SaTokenUtil;
import com.labOS.backend.service.UserService;
import com.labOS.backend.manager.S3Manager;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Resource
    private S3Manager s3Manager;


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
     * Generate presigned URL for avatar upload
     *
     * Backend creates object key ({version}.{ext}) and returns:
     * - presigned POST form fields with content-length restriction
     * - avatar key
     * - CloudFront URL
     */
    @PostMapping(value = "/avatar/presigned-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<AvatarPresignedUrlVO> generateAvatarPresignedUrl(
            @RequestBody GenerateAvatarPresignedUrlRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }
        SaTokenUtil.checkLogin();
        Long userId = SaTokenUtil.getUserId();

        String version = request.getVersion();
        String ext = request.getExt();
        String contentType = request.getContentType();
        Long sizeBytes = request.getSizeBytes();
        if (org.apache.commons.lang3.StringUtils.isBlank(version)
                || org.apache.commons.lang3.StringUtils.isBlank(ext)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "version and ext are required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(contentType) || sizeBytes == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "contentType and sizeBytes are required");
        }
        if (!contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar file must be an image");
        }
        if (sizeBytes <= 0 || sizeBytes > 1024L * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar file is too large (max 1MB)");
        }
        String normalizedExt = ext.startsWith(".") ? ext.substring(1) : ext;
        normalizedExt = normalizedExt.toLowerCase();
        if (!normalizedExt.matches("^[A-Za-z0-9]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid ext");
        }
        if (!version.matches("^[A-Za-z0-9_-]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid version");
        }
        if (!normalizedExt.matches("^(png|jpg|jpeg|webp|gif)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Unsupported image format");
        }
        String lowerContentType = contentType.toLowerCase();
        boolean typeMatches = false;
        if ("jpg".equals(normalizedExt) || "jpeg".equals(normalizedExt)) {
            typeMatches = lowerContentType.contains("jpeg") || lowerContentType.contains("jpg");
        } else {
            typeMatches = lowerContentType.contains(normalizedExt);
        }
        if (!typeMatches) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "contentType does not match ext");
        }

        String folderPath = s3Manager.getOrCreateUserAvatarFolder(String.valueOf(userId));
        String fileName = version + "." + normalizedExt;
        String avatarKey = folderPath + fileName;

        long expirationTime = request.getExpirationTime() != null
                ? request.getExpirationTime()
                : com.labOS.backend.constant.FileConstant.PRESIGNED_URL_EXPIRATION;
        String avatarBucket = s3Manager.getAvatarBucket();
        java.util.Map<String, String> formFields = s3Manager.generatePresignedPostFields(
                avatarBucket, avatarKey, contentType, 1024L * 1024, expirationTime);
        String uploadUrl = s3Manager.getPresignedPostUrl(avatarBucket);
        String avatarUrl = s3Manager.getCloudFrontUrl(avatarKey);

        AvatarPresignedUrlVO vo = new AvatarPresignedUrlVO();
        vo.setUploadMethod("POST");
        vo.setUploadUrl(uploadUrl);
        vo.setFormFields(formFields);
        vo.setAvatarKey(avatarKey);
        vo.setAvatarUrl(avatarUrl);
        return ResultUtils.success(vo);
    }

    /**
     * Update user avatar after client upload completes
     *
     * @param request avatar key from presigned upload
     * @return permanent URL of the new avatar
     */
    @PostMapping(value = "/avatar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<String> updateAvatar(@RequestBody UpdateAvatarRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }
        SaTokenUtil.checkLogin();
        Long userId = SaTokenUtil.getUserId();
        String url = userService.updateUserAvatar(userId, request.getAvatarKey());
        return ResultUtils.success(url);
    }
}

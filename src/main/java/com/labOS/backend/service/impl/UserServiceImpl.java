package com.labOS.backend.service.impl;

import static com.labOS.backend.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.constant.CommonConstant;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.manager.S3Manager;
import com.labOS.backend.mapper.UserMapper;
import com.labOS.backend.model.dto.user.UserQueryRequest;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.enums.UserRoleEnum;
import com.labOS.backend.model.vo.LoginUserVO;
import com.labOS.backend.model.vo.UserVO;
import com.labOS.backend.service.UserService;
import com.labOS.backend.utils.SqlUtils;
import com.amazonaws.services.s3.model.ObjectMetadata;
import cn.dev33.satoken.stp.StpUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;

/**
 * User service implementation
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private S3Manager s3Manager;

    /**
     * Salt value for password obfuscation
     */
    public static final String SALT = "Yifan";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. Validate
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters cannot be empty");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User account too short");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User password too short");
        }
        // Password and check password must match
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }
        synchronized (userAccount.intern()) {
            // Account cannot be duplicated
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account already exists");
            }
            // 2. Encrypt
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. Insert data
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Registration failed, database error");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. Validate
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters cannot be empty");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid account");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid password");
        }
        // 2. Encrypt
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // Check if user exists
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // User does not exist
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User does not exist or password is incorrect");
        }
        // 3. Record user login state
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }


    /**
     * Get current logged-in user
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // First check if logged in
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // Query from database (can comment out for performance, use cache directly)
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * Get current logged-in user (allow not logged in)
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // First check if logged in
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // Query from database (can comment out for performance, use cache directly)
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * Check if user is admin
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // Only admin can query
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * User logout
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Not logged in");
        }
        // Remove login state
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters cannot be empty");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public boolean updateUserName(Long userId, String firstName, String lastName) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid userId");
        }
        String fn = firstName == null ? null : firstName.trim();
        String ln = lastName == null ? null : lastName.trim();
        if (StringUtils.isBlank(fn) && StringUtils.isBlank(ln)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "First name or last name is required");
        }
        if (StringUtils.isNotBlank(fn) && fn.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "First name too long");
        }
        if (StringUtils.isNotBlank(ln) && ln.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Last name too long");
        }

        // Fill missing parts from DB, and keep userName consistent with (firstName + lastName)
        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        String finalFirstName = StringUtils.isBlank(fn) ? dbUser.getFirstName() : fn;
        String finalLastName = StringUtils.isBlank(ln) ? dbUser.getLastName() : ln;
        String combinedUserName = ((finalFirstName == null ? "" : finalFirstName.trim()) + " " +
                (finalLastName == null ? "" : finalLastName.trim())).trim();

        User update = new User();
        update.setId(userId);
        update.setFirstName(finalFirstName);
        update.setLastName(finalLastName);
        if (StringUtils.isNotBlank(combinedUserName)) {
            update.setUserName(combinedUserName);
        }
        return this.updateById(update);
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid userId");
        }
        if (StringUtils.isAnyBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords cannot be empty");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "New password too short");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }

        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        String oldEncrypted = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
        if (!oldEncrypted.equals(dbUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Old password is incorrect");
        }

        String newEncrypted = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        User update = new User();
        update.setId(userId);
        update.setUserPassword(newEncrypted);
        boolean ok = this.updateById(update);
        if (ok) {
            // Security: force logout all sessions for this user
            try {
                StpUtil.logout(userId);
            } catch (Exception e) {
                log.warn("Failed to logout user sessions after password change, userId={}", userId, e);
            }
        }
        return ok;
    }

    @Override
    public String updateUserAvatar(Long userId, String avatarKey) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid userId");
        }
        if (StringUtils.isBlank(avatarKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar key is required");
        }

        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        String folderPath = s3Manager.getOrCreateUserAvatarFolder(String.valueOf(userId));
        if (!avatarKey.startsWith(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar key does not match user folder");
        }

        String fileName = avatarKey.substring(folderPath.length());
        if (StringUtils.isBlank(fileName) || fileName.contains("/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid avatar file name");
        }

        if (!fileName.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar file name must be {version}.{ext}");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ext.matches("^(png|jpg|jpeg|webp|gif)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Unsupported image format");
        }

        ObjectMetadata metadata;
        try {
            metadata = s3Manager.getObjectMetadata(s3Manager.getAvatarBucket(), avatarKey);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar object not found");
        }
        String contentType = metadata.getContentType();
        if (StringUtils.isBlank(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar file must be an image");
        }
        long sizeBytes = metadata.getContentLength();
        if (sizeBytes <= 0 || sizeBytes > 1024L * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Avatar file is too large (max 1MB)");
        }

        String oldAvatarKey = dbUser.getUserAvatarKey();
        String permanentUrl = s3Manager.getCloudFrontUrl(avatarKey);

        User update = new User();
        update.setId(userId);
        update.setUserAvatar(permanentUrl);
        update.setUserAvatarKey(avatarKey);
        boolean ok = this.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update user avatar");
        }

        if (StringUtils.isNotBlank(oldAvatarKey) && !oldAvatarKey.equals(avatarKey)) {
            try {
                s3Manager.deleteObject(s3Manager.getAvatarBucket(), oldAvatarKey);
            } catch (Exception e) {
                log.warn("Failed to delete previous avatar from S3, oldKey={}", oldAvatarKey, e);
            }
        }

        return permanentUrl;
    }
}

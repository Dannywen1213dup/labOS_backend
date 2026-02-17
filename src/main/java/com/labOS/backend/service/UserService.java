package com.labOS.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.labOS.backend.model.dto.user.UserQueryRequest;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.LoginUserVO;
import com.labOS.backend.model.vo.UserVO;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * User service
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface UserService extends IService<User> {

    /**
     * User registration
     *
     * @param userAccount   User account
     * @param userPassword  User password
     * @param checkPassword Check password
     * @return New user id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * User login
     *
     * @param userAccount  User account
     * @param userPassword User password
     * @param request
     * @return Desensitized user information
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * Get current logged-in user
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * Get current logged-in user (allow not logged in)
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * Check if user is admin
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * Check if user is admin
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * User logout
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * Get desensitized logged-in user information
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * Get desensitized user information
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * Get desensitized user information
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * Get query wrapper
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * Update user's first and last name
     *
     * @param userId logged-in user id
     * @param firstName first name
     * @param lastName last name
     * @return success
     */
    boolean updateUserName(Long userId, String firstName, String lastName);

    /**
     * Change password for logged-in user (verify old password)
     *
     * @param userId logged-in user id
     * @param oldPassword old password (plain)
     * @param newPassword new password (plain)
     * @param confirmPassword confirm new password
     * @return success
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword);

    /**
     * Update user avatar after client uploads to S3 via presigned URL:
     * - Validate avatar key (must belong to user folder)
     * - Update user.userAvatar (CloudFront URL) and user.userAvatarKey (S3 key)
     * - Delete previous avatar object in S3 (best-effort)
     *
     * @param userId logged-in user id
     * @param avatarKey s3 object key for the new avatar
     * @return permanent URL of the new avatar
     */
    String updateUserAvatar(Long userId, String avatarKey);

}

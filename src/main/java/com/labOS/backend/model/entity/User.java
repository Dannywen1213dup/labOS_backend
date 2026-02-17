package com.labOS.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * User
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@TableName(value = "user")
@Data
public class User implements Serializable {

    /**
     * Id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * User account
     */
    private String userAccount;

    /**
     * Email address (unique, primary login method)
     */
    private String email;

    /**
     * User password
     */
    private String userPassword;

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    /**
     * Legal terms accepted (0: no, 1: yes)
     */
    private Integer legalAccepted;

    /**
     * User status: UNVERIFIED, ACTIVE, DISABLED
     */
    private String status;

    /**
     * Open platform id
     */
    private String unionId;

    /**
     * Official account openId
     */
    private String mpOpenId;

    /**
     * User nickname
     */
    private String userName;

    /**
     * User avatar
     */
    private String userAvatar;

    /**
     * User avatar S3 key (for deletion / replacement)
     */
    private String userAvatarKey;

    /**
     * User profile
     */
    private String userProfile;

    /**
     * User role: user/admin/ban
     */
    private String userRole;

    /**
     * Create time
     */
    private Date createTime;

    /**
     * Update time
     */
    private Date updateTime;

    /**
     * Is deleted
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
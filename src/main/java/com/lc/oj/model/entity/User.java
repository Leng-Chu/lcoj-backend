package com.lc.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "账号")
    @TableField("userName")
    private String userName;

    @ApiModelProperty(value = "密码")
    @TableField("userPassword")
    private String userPassword;

    @ApiModelProperty(value = "用户角色：user/admin/ban")
    @TableField("userRole")
    private String userRole;

    @ApiModelProperty(value = "创建时间")
    @TableField("createTime")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("updateTime")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "是否删除")
    @TableField("isDelete")
    @TableLogic
    private Integer isDelete;


}

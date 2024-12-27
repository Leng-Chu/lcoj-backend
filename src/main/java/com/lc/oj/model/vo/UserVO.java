package com.lc.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
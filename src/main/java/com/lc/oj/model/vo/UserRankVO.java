package com.lc.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户排名
 */
@Data
public class UserRankVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户排名
     */
    private Integer rank;
    /**
     * 已解决的题目数
     */
    private int acceptedNum;
}
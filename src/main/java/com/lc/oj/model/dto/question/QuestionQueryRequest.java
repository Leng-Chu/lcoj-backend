package com.lc.oj.model.dto.question;

import com.lc.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 题号
     */
    private Long num;
    /**
     * 标题
     */
    private String title;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 创建者
     */
    private String userName;
}
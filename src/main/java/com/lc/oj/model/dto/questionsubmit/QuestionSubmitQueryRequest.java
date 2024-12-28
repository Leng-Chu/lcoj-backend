package com.lc.oj.model.dto.questionsubmit;

import com.lc.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 编程语言
     */
    private String language;
    /**
     * 提交状态
     */
    private Integer status;
    /**
     * 题号
     */
    private Long questionNum;
    /**
     * 题目标题
     */
    private String questionTitle;
    /**
     * 提交者
     */
    private String userName;
}
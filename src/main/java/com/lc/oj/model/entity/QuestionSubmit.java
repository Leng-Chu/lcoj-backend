package com.lc.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 题目提交
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@Data
@TableName("question_submit")
public class QuestionSubmit implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "编程语言")
    @TableField("language")
    private String language;

    @ApiModelProperty(value = "用户代码")
    @TableField("code")
    private String code;

    @ApiModelProperty(value = "判题信息（json 对象）")
    @TableField("judgeInfo")
    private String judgeInfo;

    @ApiModelProperty(value = "判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty(value = "题目id")
    @TableField("questionId")
    private Long questionId;

    @ApiModelProperty(value = "题号")
    @TableField("questionNum")
    private Long questionNum;

    @ApiModelProperty(value = "题目标题")
    @TableField("questionTitle")
    private String questionTitle;

    @ApiModelProperty(value = "创建用户id")
    @TableField("userId")
    private Long userId;

    @ApiModelProperty(value = "创建用户昵称")
    @TableField("userName")
    private String userName;

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

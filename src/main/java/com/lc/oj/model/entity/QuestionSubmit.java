package com.lc.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    @ApiModelProperty(value = "判题结果（0 - 等待判题、1 - 通过题目、 2~7 - 未通过、 8 - 无测评数据）")
    @TableField("judgeResult")
    private Integer judgeResult;

    @ApiModelProperty(value = "最大耗时")
    @TableField("maxTime")
    private Long maxTime;

    @ApiModelProperty(value = "最大内存")
    @TableField("maxMemory")
    private Long maxMemory;

    @ApiModelProperty(value = "每个点的判题信息（json 对象）")
    @TableField("caseInfoList")
    private String caseInfoList;

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
    private Integer isDelete;


}

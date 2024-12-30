package com.lc.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 题目
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@Data
@TableName("question")
public class Question implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "题号")
    @TableField("num")
    private Long num;

    @ApiModelProperty(value = "标题")
    @TableField("title")
    private String title;

    @ApiModelProperty(value = "内容")
    @TableField("content")
    private String content;

    @ApiModelProperty(value = "标签列表（json 数组）")
    @TableField("tags")
    private String tags;

    @ApiModelProperty(value = "题目标程")
    @TableField("answer")
    private String answer;

    @ApiModelProperty(value = "标程语言")
    @TableField("language")
    private String language;

    @ApiModelProperty(value = "题目提交数")
    @TableField("submitNum")
    private Integer submitNum;

    @ApiModelProperty(value = "题目通过数")
    @TableField("acceptedNum")
    private Integer acceptedNum;

    @ApiModelProperty(value = "样例（json 数组）")
    @TableField("sampleCase")
    private String sampleCase;

    @ApiModelProperty(value = "判题配置（json 对象）")
    @TableField("judgeConfig")
    private String judgeConfig;

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

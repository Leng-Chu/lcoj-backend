package com.lc.oj.model.dto.judge;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * （总的）判题结果
     */
    private Integer judgeResult;

    /**
     * （最大）消耗内存
     */
    private Long memory;

    /**
     * （最大）消耗时间
     */
    private Long time;
}

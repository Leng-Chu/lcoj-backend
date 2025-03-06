package com.lc.oj.model.dto.judge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeSandboxRequest {

    /**
     * 代码
     */
    private String source_code;

    /**
     * 编程语言
     */
    private Integer language_id;

    /**
     * 输入数据
     */
    private String stdin;

    /**
     * 预期输出
     */
    private String expected_output;

    /**
     * 时间限制（s）
     */
    private double cpu_time_limit;

    /**
     * 内存限制（KB）
     */
    private Long memory_limit;

    /**
     * 编译选项
     */
    private String compiler_options;
}

package com.lc.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文件列表封装类
 * @TableName question
 */
@Data
public class FileListVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Case> inputFiles;
    private List<Case> outputFiles;

    @Data
    public static class Case {
        private String name;
        private Long size;
    }
}
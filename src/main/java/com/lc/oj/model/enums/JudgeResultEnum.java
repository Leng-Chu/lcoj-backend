package com.lc.oj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题结果枚举
 */
public enum JudgeResultEnum {

    WAITING("Waiting", 0),
    ACCEPTED("Accepted", 1),
    WRONG_ANSWER("Wrong Answer", 2),
    COMPILE_ERROR("Compilation Error", 3),
    RUNTIME_ERROR("Runtime Error", 4),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded", 5),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded", 6),
    SYSTEM_ERROR("System Error", 7),
    DATA_NOT_FOUND("Data Not Found", 8);;

    private final String text;

    private final Integer value;

    JudgeResultEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static JudgeResultEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeResultEnum anEnum : JudgeResultEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据 text 获取 value
     *
     * @return
     */
    public static Integer getValueByText(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return null;
        }
        for (JudgeResultEnum anEnum : JudgeResultEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum.value;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}

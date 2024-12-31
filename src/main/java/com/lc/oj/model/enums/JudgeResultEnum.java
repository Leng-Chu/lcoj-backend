package com.lc.oj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题结果枚举
 */
public enum JudgeResultEnum {

    WAITING("等待判题", 0),
    ACCEPTED("通过题目", 1),
    WRONG_ANSWER("答案错误", 2),
    COMPILE_ERROR("编译错误", 3),
    RUNTIME_ERROR("运行错误", 4),
    SYSTEM_ERROR("系统错误", 5),
    TIME_LIMIT_EXCEEDED("时间超限", 6),
    MEMORY_LIMIT_EXCEEDED("内存超限", 7),
    DATA_NOT_FOUND("无测评数据", 8);;

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

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}

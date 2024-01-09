package com.fans.ojsandbox.model.enums;


import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题信息消息枚举
 *
 * @author fansheng
 */
public enum JudgeInfoMessageEnum {

    ACCEPTED("成功", "Accepted"),
    COMPILE_ERROR("编译错误", "compile error"),
    DANGEROUS_OPERATION("危险操作", "dangerous operation"),
    MEMORY_LIMIT_EXCEEDED("超出内存限制", "memory limit exceeded"),
    OUTPUT_LIMIT_EXCEEDED("输出超出限制", "output limit exceeded"),
    PRESENTATION_ERROR("展示错误", "Accepted"),
    RUNTIME_ERROR("运行时异常", "runtime error"),
    SYSTEM_ERROR("系统错误", "system error"),
    TIME_LIMIT_EXCEEDED("超出时间限制", "time limit exceeded"),
    WRONG_ANSWER("答案错误", "wrong answer");


    private final String text;

    private final String value;

    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}

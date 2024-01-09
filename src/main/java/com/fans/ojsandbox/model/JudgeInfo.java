package com.fans.ojsandbox.model;


import com.fans.ojsandbox.model.enums.JudgeInfoMessageEnum;
import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private JudgeInfoMessageEnum message;
    /**
     * 内存占用
     */
    private Long memory;
    /**
     * 消耗时间
     */
    private Long time;
}

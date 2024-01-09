package com.fans.ojsandbox.model;

import lombok.Data;

@Data
public class ProcessOutput {
    private Integer value;
    private String message;
    private String errorMessage;
    private Long time;
    private Long memory;
}

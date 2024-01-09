package com.fans.ojsandbox;


import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;

public interface CodeSandBox {
    ExecuteQuestionResponse doExecute(ExecuteQuestionRequest executeQuestionRequest);
    ExecuteQuestionResponse getErrorResponse(Throwable e);
}

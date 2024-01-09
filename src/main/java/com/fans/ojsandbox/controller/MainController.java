package com.fans.ojsandbox.controller;

import cn.hutool.json.JSONUtil;
import com.fans.ojsandbox.JavaNativeCodeSandBoxAbsTemplateImpl;
import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class MainController {
//    服务调用方和提供方约定字符串
//    auth:
//    auth_header: fans
//    auth_header_key: fansheng

    private static String AUTH_HEADER = "fans";
    private static String AUTH_HEADER_KEY = "fansheng";

    @Resource
    private JavaNativeCodeSandBoxAbsTemplateImpl sandBox;

    @PostMapping("/test")
    public String test(){
        return "hello world";
    }



    @PostMapping("/runCode")
    public String execCode(@RequestBody ExecuteQuestionRequest executeQuestionRequest, HttpServletRequest request, HttpServletResponse response){
        String header = request.getHeader(AUTH_HEADER);
        if(!AUTH_HEADER_KEY.equals(header)){
            return null;
        }
        if(executeQuestionRequest == null || executeQuestionRequest.getCode() == null){
            ExecuteQuestionResponse nullResponse = new ExecuteQuestionResponse();
            nullResponse.setMessage("请求参数为空或代码为空");
            return null;
        }
        return JSONUtil.toJsonStr(sandBox.doExecute(executeQuestionRequest));
    }
}

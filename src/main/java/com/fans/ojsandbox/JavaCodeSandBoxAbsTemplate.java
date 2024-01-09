package com.fans.ojsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import com.fans.ojsandbox.model.JudgeInfo;
import com.fans.ojsandbox.model.ProcessOutput;
import com.fans.ojsandbox.utils.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

// 模版模式
@Slf4j
public abstract class JavaCodeSandBoxAbsTemplate implements CodeSandBox {
    public static final String GLOBAL_CODE_DIR = "tempCode";
    public static final String GLOBAL_CLASS_NAME = "Main.java";

    //    D:\FXM\Projects\fans-oj\oj-sandBox\src\main\resources\DefaultSecurityManager .class
    public static final String DEFAULT_SECURITY_MANAGER_PATH = "D:\\FXM\\Projects\\fans-oj\\oj-sandBox\\src\\main\\resources\\security";
    public static final String DEFAULT_SECURITY_CLASS = "DefaultSecurityManager";

    public static final Long TIME_OUT = 5000L;
    public String userCodeDir;

    /**
     * 创建目录，接收用户代码并转存文件
     * @param code 用户代码
     * @return File 用户代码文件
     */
    public File convertCodeToFile(String code) {
        // 创建目录
        String userDir = System.getProperty("user.dir");
        String globalCodeDir = userDir + File.separator + GLOBAL_CODE_DIR;
        if (!FileUtil.exist(globalCodeDir)) {
            FileUtil.mkdir(globalCodeDir);
        }
        // 转存代码
        userCodeDir = globalCodeDir + File.separator + UUID.randomUUID();
        String userCodeFilePath = userCodeDir + File.separator + GLOBAL_CLASS_NAME;
        File codeFile = FileUtil.writeString(code, userCodeFilePath, StandardCharsets.UTF_8);
        return codeFile;
    }

    /**
     * 编译用户代码文件
     * @param codeFile 用户代码文件
     * @return processOutput 编译输出结果
     */
    public ProcessOutput compileCodeFile(File codeFile) {
        String codeCompile = String.format("javac -encoding utf-8 %s", codeFile.getAbsolutePath());
        Process process;
        ProcessOutput processOutput;
        try {
            process = Runtime.getRuntime().exec(codeCompile);
            processOutput = ProcessUtil.runProcessByArgs(process, "编译");
        } catch (IOException e) {
            ProcessOutput errorProcessRes = new ProcessOutput();
            errorProcessRes.setErrorMessage(e.getMessage());
            return errorProcessRes;
        }
        return processOutput;
    }

    /**
     * 运行用户代码
     * @param inputList 输入用例列表
     * @return List<ProcessOutput> 控制台输出结果列表
     */
    public List<ProcessOutput> runCode(List<String> inputList){
        // 代码运行
        List<ProcessOutput> processOutputs = new ArrayList<>();
        for (String input : inputList) {
//          java -Xmx256m 软件限制最大堆内存，可能出现内存分配超出最大值；更严格的内容限制需要通过系统实现
            String codeRun = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeDir, input);
//            String codeRun = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp Main %s",
//                    userCodeDir, DEFAULT_SECURITY_MANAGER_PATH, DEFAULT_SECURITY_CLASS, input);
            try {
                Process process = Runtime.getRuntime().exec(codeRun);
                ProcessOutput processOutput = ProcessUtil.runProcessByArgs(process, "运行");
//                ProcessOutput processOutput = ProcessUtil.runProcessByInteractively(process,input);
                processOutputs.add(processOutput);
            } catch (IOException e) {
                ProcessOutput processOutput = new ProcessOutput();
                processOutput.setErrorMessage(e.getMessage());
                return Collections.singletonList(processOutput);
            }
        }
        return processOutputs;
    }

    /**
     * 检查控制台输出是否报错
     * @param processOutputList 控制台输出列表
     * @return ExecuteQuestionResponse 封装沙箱运行结果
     */
    public ExecuteQuestionResponse checkResult(List<ProcessOutput> processOutputList){
        // 统计结果
        ExecuteQuestionResponse executeQuestionResponse = new ExecuteQuestionResponse();
        List<String> outputList = new ArrayList<>();
        long useMaxTime = 0;
        long useMaxMemory= 0;
        for (ProcessOutput output : processOutputList) {
            String errorMessage = output.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                // TODO use Enum instead
                executeQuestionResponse.setMessage(errorMessage);
                executeQuestionResponse.setStatus(3);
                break;
            }
            Long time = output.getTime();
            Long memory = output.getMemory();
            if (time != null) {
                useMaxTime = Math.max(useMaxTime, time);
            }
            if (memory != null) {
                useMaxMemory = Math.max(useMaxMemory, memory);
            }
            outputList.add(output.getMessage());
        }
        executeQuestionResponse.setOutputList(outputList);
        executeQuestionResponse.setStatus(1);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(useMaxMemory);
        judgeInfo.setTime(useMaxTime);
        executeQuestionResponse.setJudgeInfo(judgeInfo);
        return executeQuestionResponse;
    }

    /**
     * 清理用户文件
     * @param userCodeDir 用户代码目录
     * @return Boolean 清理结果
     */
    public Boolean cleanStorageFiles(String userCodeDir){
        // 文件清理
        if (userCodeDir != null) {
            boolean del = FileUtil.del(userCodeDir);
            System.out.println("删除状况: " + (del ? "成功" : "失败"));
        }
        return true;
    }


    /**
     * 策略模式总流程-不可被子类修改
     * @param executeQuestionRequest 待提交沙箱请求
     * @return ExecuteQuestionResponse 沙箱运行结果
     */
    @Override
    public final ExecuteQuestionResponse doExecute(ExecuteQuestionRequest executeQuestionRequest) {
//      System.setSecurityManager(new DefaultSecurityManager());
        String code = executeQuestionRequest.getCode();
        List<String> inputList = executeQuestionRequest.getInputList();

        File codeFile = convertCodeToFile(code);
        ProcessOutput processOutput = compileCodeFile(codeFile);
        // 出现IO异常
        if (processOutput.getErrorMessage() != null) {
            return getErrorResponse(new RuntimeException(processOutput.getErrorMessage()));
        }

        List<ProcessOutput> runProcessOutputs = runCode(inputList);
        ExecuteQuestionResponse executeQuestionResponse = checkResult(runProcessOutputs);
        Boolean delRes = cleanStorageFiles(userCodeDir);
        log.info("用户文件清理结果：" + delRes);
        return executeQuestionResponse;
    }

    /**
     * 错误处理方法
     *
     * @param e 异常
     * @return ExecuteQuestionResponse 响应类
     */
    @Override
    public ExecuteQuestionResponse getErrorResponse(Throwable e) {
        ExecuteQuestionResponse executeQuestionResponse = new ExecuteQuestionResponse();
        executeQuestionResponse.setMessage(e.getMessage());
        executeQuestionResponse.setStatus(2);
        executeQuestionResponse.setJudgeInfo(new JudgeInfo());
        executeQuestionResponse.setOutputList(new ArrayList<>());
        return executeQuestionResponse;
    }
}

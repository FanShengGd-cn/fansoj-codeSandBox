package com.fans.ojsandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import com.fans.ojsandbox.model.JudgeInfo;
import com.fans.ojsandbox.model.ProcessOutput;
import com.fans.ojsandbox.utils.ProcessUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class JavaNativeSandBox implements CodeSandBox {

    public static final String GLOBAL_CODE_DIR = "tempCode";
    public static final String GLOBAL_CLASS_NAME = "Main.java";

    //   黑名单
    public static final List<String> BLACK_LIST = Arrays.asList("Files", "exec");
    //    D:\FXM\Projects\fans-oj\oj-sandBox\src\main\resources\DefaultSecurityManager.class
    public static final String DEFAULT_SECURITY_MANAGER_PATH = "D:\\FXM\\Projects\\fans-oj\\oj-sandBox\\src\\main\\resources\\security";
    public static final String DEFAULT_SECURITY_CLASS = "DefaultSecurityManager";
    public static final WordTree WORD_TREE = new WordTree();

    static {
        // 校验代码中是否包含危险命令
        WORD_TREE.addWords(BLACK_LIST);
    }

    @Override
    public ExecuteQuestionResponse doExecute(ExecuteQuestionRequest executeQuestionRequest) {
//        System.setSecurityManager(new DefaultSecurityManager());
        String language = executeQuestionRequest.getLanguage();
        String code = executeQuestionRequest.getCode();
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            getErrorResponse(new RuntimeException("包含限制词"));
        }
        List<String> inputList = executeQuestionRequest.getInputList();


        // 创建目录
        String userDir = System.getProperty("user.dir");
        String globalCodeDir = userDir + File.separator + GLOBAL_CODE_DIR;
        if (!FileUtil.exist(globalCodeDir)) {
            FileUtil.mkdir(globalCodeDir);
        }
        // 转存代码
        String userCodeDir = globalCodeDir + File.separator + UUID.randomUUID();
        String userCodeFilePath = userCodeDir + File.separator + GLOBAL_CLASS_NAME;
        File codeFile = FileUtil.writeString(code, userCodeFilePath, StandardCharsets.UTF_8);

        // 代码编译
        String codeCompile = String.format("javac -encoding utf-8 %s", codeFile.getAbsolutePath());
        Process process;
        try {
            process = Runtime.getRuntime().exec(codeCompile);
            ProcessOutput processOutput = ProcessUtil.runProcessByArgs(process, "编译");
        } catch (IOException e) {
            return getErrorResponse(e);
        }
        // 代码运行
        List<ProcessOutput> processOutputs = new ArrayList<>();
        for (String input : inputList) {
//          java -Xmx256m 软件限制最大堆内存，可能出现内存分配超出最大值；更严格的内容限制需要通过系统实现
//            String codeRun = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeDir, input);
            String codeRun = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s",
                    userCodeDir, DEFAULT_SECURITY_MANAGER_PATH, DEFAULT_SECURITY_CLASS, input);
            try {
                process = Runtime.getRuntime().exec(codeRun);
                ProcessOutput processOutput = ProcessUtil.runProcessByArgs(process, "运行");
//                ProcessOutput processOutput = ProcessUtil.runProcessByInteractively(process,input);
                processOutputs.add(processOutput);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        // 统计结果
        ExecuteQuestionResponse executeQuestionResponse = new ExecuteQuestionResponse();
        List<String> outputList = new ArrayList<>();
        long useMaxTime = 0;
        for (ProcessOutput output : processOutputs) {
            String errorMessage = output.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                // TODO use Enum instead
                executeQuestionResponse.setMessage(errorMessage);
                executeQuestionResponse.setStatus(3);
                break;
            }
            Long time = output.getTime();
            if (time != null) {
                useMaxTime = Math.max(useMaxTime, time);
            }
            outputList.add(output.getMessage());
        }
        executeQuestionResponse.setOutputList(outputList);
        executeQuestionResponse.setStatus(1);
        JudgeInfo judgeInfo = new JudgeInfo();
//        TODO 需要使用第三方库获取内存
//        judgeInfo.setMemory();
        judgeInfo.setTime(useMaxTime);
        executeQuestionResponse.setJudgeInfo(judgeInfo);

        // 文件清理
        if (codeFile.getParentFile() != null) {
            boolean del = FileUtil.del(codeFile.getParentFile());
            System.out.println("删除状况: " + (del ? "成功" : "失败"));
        }


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

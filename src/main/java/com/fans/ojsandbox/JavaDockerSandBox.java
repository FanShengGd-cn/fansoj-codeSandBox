package com.fans.ojsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import com.fans.ojsandbox.model.JudgeInfo;
import com.fans.ojsandbox.model.ProcessOutput;
import com.fans.ojsandbox.utils.ProcessUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Resource
public class JavaDockerSandBox implements CodeSandBox {
    public static final String GLOBAL_CODE_DIR = "tempCode";
    public static final String GLOBAL_CLASS_NAME = "Main.java";
    public static final boolean Init = false;

    //    D:\FXM\Projects\fans-oj\oj-sandBox\src\main\resources\DefaultSecurityManager .class
    public static final String DEFAULT_SECURITY_MANAGER_PATH = "D:\\FXM\\Projects\\fans-oj\\oj-sandBox\\src\\main\\resources\\security";
    public static final String DEFAULT_SECURITY_CLASS = "DefaultSecurityManager";


    @Override
    public ExecuteQuestionResponse doExecute(ExecuteQuestionRequest executeQuestionRequest) {
//        System.setSecurityManager(new DefaultSecurityManager());
        String language = executeQuestionRequest.getLanguage();
        String code = executeQuestionRequest.getCode();
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
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String containerId = null;
        if (Init) {
            String image = "openjdk:8-alpine";
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
            HostConfig hostConfig = new HostConfig();
            hostConfig.setBinds(new Bind(userCodeDir, new Volume("/app")));
            hostConfig.withMemory(100 * 1000 * 1000L);
            hostConfig.withCpuCount(1L);
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
            CreateContainerResponse createContainerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .exec();
            System.out.println(createContainerResponse);
            containerId = createContainerResponse.getId();

        } else {
//            dockerClient.
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withVolumeFilter(Collections.singleton("/app"));
            List<Container> jdkContainer = listContainersCmd.exec();
            if (!jdkContainer.isEmpty()) {
                containerId = jdkContainer.get(0).getId();
            }
            System.out.println("容器id：" + containerId);
        }

        dockerClient.startContainerCmd(containerId).exec();
        // 下载镜像，创建docker容器

        // 统计结果
        ExecuteQuestionResponse executeQuestionResponse = new ExecuteQuestionResponse();
        List<String> outputList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        long runTime = 0L;
        final long[] maxMemory = {0L};
        for (String input : inputList) {
            StopWatch stopWatch = new StopWatch();
            final String[] message = new String[1];
            final String[] errorMessage = new String[1];
            // 创建docker命令
            // docker exec reverent_goldberg java -cp /app Main 1 3
            String[] inputArray = input.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        errorList.add(errorMessage[0]);
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        outputList.add(errorMessage[0]);
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            //获取内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                    System.out.println("内存使用：" + maxMemory[0]);

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(statsResultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion();
                stopWatch.stop();
                statsCmd.close();
                runTime = stopWatch.getLastTaskTimeMillis();

                if (!errorList.isEmpty()) {
                    getErrorResponse(new RuntimeException("程序执行错误"));
                }
            } catch (InterruptedException e) {
                getErrorResponse(e);
            }

        }

        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setMessage();
        judgeInfo.setMemory(maxMemory[0]);
        judgeInfo.setTime(runTime);
        executeQuestionResponse.setOutputList(outputList);
        executeQuestionResponse.setStatus(1);
        executeQuestionResponse.setJudgeInfo(judgeInfo);


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

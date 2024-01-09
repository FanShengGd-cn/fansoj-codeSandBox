package com.fans.ojsandbox;

import cn.hutool.core.util.ArrayUtil;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import com.fans.ojsandbox.model.ProcessOutput;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

// java docker代码沙箱
@Component
public class JavaDockerCodeSandBoxAbsTemplateImpl extends JavaCodeSandBoxAbsTemplate {
    public static final boolean Init = false;

    @Override
    public File convertCodeToFile(String code) {
        return super.convertCodeToFile(code);
    }

    @Override
    public ProcessOutput compileCodeFile(File codeFile) {
        return super.compileCodeFile(codeFile);
    }

    @Override
    public List<ProcessOutput> runCode(List<String> inputList) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String containerId = null;
        if (Init) {
            // 下载镜像，创建docker容器
            String image = "openjdk:8-alpine";
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("拉取镜像完成");
            HostConfig hostConfig = new HostConfig();
            hostConfig.setBinds(new Bind(userCodeDir, new Volume("/app")));
            hostConfig.withMemory(100 * 1000 * 1000L);
            hostConfig.withCpuCount(1L);
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
            CreateContainerResponse createContainerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .exec();
            System.out.println(createContainerResponse);
            containerId = createContainerResponse.getId();
            dockerClient.startContainerCmd(containerId).exec();
        } else {
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
            List<Container> jdkContainer = listContainersCmd
                    .withAncestorFilter(Collections.singletonList("openjdk:8-alpine"))
                    .withVolumeFilter(Collections.singletonList("/app"))
                    .withShowAll(true)
                    .exec();
            if (!jdkContainer.isEmpty()) {
                for (Container con : jdkContainer) {
                    if ("pause".equals(con.getState()) || "exited".equals(con.getState())) {
                        containerId = con.getId();
                        dockerClient.startContainerCmd(containerId).exec();
                    } else if ("running".equals(con.getState())) {
                        containerId = con.getId();
                        break;
                    }
                }
            }
        }


        // 运行代码
        final long[] maxTime = {0L};
        final long[] maxMemory = {0L};
        List<ProcessOutput> processOutputList = new ArrayList<>();

        for (String input : inputList) {
            ProcessOutput processOutput = new ProcessOutput();
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
                    } else {
                        message[0] = new String(frame.getPayload());
                    }
                    super.onNext(frame);
                }
            };
//            //获取内存
//            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
//            ResultCallback resultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
//                @Override
//                public void close() throws IOException {
//
//                }
//
//                @Override
//                public void onStart(Closeable closeable) {
//
//                }
//
//                @Override
//                public void onNext(Statistics statistics) {
//                    System.out.println("内存使用：" + (statistics.getMemoryStats().getUsage()));
//                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
//
//                }
//
//                @Override
//                public void onError(Throwable throwable) {
//
//                }
//
//                @Override
//                public void onComplete() {
//                }
//            });
            try {
//                statsCmd.exec(resultCallback);
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
//                statsCmd.close();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            processOutput.setMessage(message[0]);
            processOutput.setErrorMessage(errorMessage[0]);
            processOutput.setMemory(maxMemory[0]);
            processOutput.setTime(stopWatch.getLastTaskTimeMillis());
            System.out.println(message[0]);
            System.out.println(processOutput);
            processOutputList.add(processOutput);
        }
        return processOutputList;
    }

    @Override
    public ExecuteQuestionResponse checkResult(List<ProcessOutput> processOutputList) {
        return super.checkResult(processOutputList);
    }

    @Override
    public Boolean cleanStorageFiles(String userCodeDir) {
        return super.cleanStorageFiles(userCodeDir);
    }

    @Override
    public ExecuteQuestionResponse getErrorResponse(Throwable e) {
        return super.getErrorResponse(e);
    }


}

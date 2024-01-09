package com.fans.ojsandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        String image  = "openjdk:8-alpine";
        DockerClient client = DockerClientBuilder.getInstance().build();
        PullImageCmd pullImageCmd = client.pullImageCmd(image);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像：" + item.getStatus());
                super.onNext(item);
            }
        };
        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        System.out.println("下载完成");
        CreateContainerCmd containerCmd = client.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd
                .exec();
        System.out.println(createContainerResponse);

    }
}

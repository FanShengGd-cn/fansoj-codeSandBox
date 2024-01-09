package com.fans.ojsandbox.utils;


import cn.hutool.core.util.StrUtil;
import com.fans.ojsandbox.model.ProcessOutput;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtil {
    /**
     * 获取运行结果
     *
     * @param process 当前运行进程
     * @return ProcessOutput 运行结果
     */
    public static ProcessOutput runProcessByArgs(Process process, String message) {

        ProcessOutput processOutput = new ProcessOutput();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int compileRes = process.waitFor();
            if (compileRes == 0) {
                System.out.println(message + "成功");
                processOutput.setValue(compileRes);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                List<String> outputList = new ArrayList<>();
                String outputLine;
                while ((outputLine = bufferedReader.readLine()) != null) {
                    outputList.add(outputLine);
                }
                String formatOutput = StringUtils.join(outputList,"\n");
                processOutput.setMessage(formatOutput);
            } else {
                System.out.println(message + "失败");
                processOutput.setValue(compileRes);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                List<String> errorOutputList = new ArrayList<>();
                String errorOutputLine;
                while ((errorOutputLine = bufferedReader.readLine()) != null) {
                    errorOutputList.add(errorOutputLine);
                }
                String formatErrorOutput = StringUtils.join(errorOutputList,"\n");
                processOutput.setErrorMessage(formatErrorOutput);
            }
            stopWatch.stop();
            processOutput.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            processOutput.setErrorMessage(e.getMessage());
        }
        return processOutput;
    }

    public static ProcessOutput runProcessByInteractively(Process process, String args) {
        // TODO 结果返回为空，待修复
        ProcessOutput processOutput = new ProcessOutput();
        OutputStream outputStream;
        OutputStreamWriter outputStreamWriter;
        BufferedReader bufferedReader;
        InputStream inputStream = process.getInputStream();
        ;
        outputStream = process.getOutputStream();
        outputStreamWriter = new OutputStreamWriter(outputStream);
        String s = StrUtil.join("\n", (Object) args.split(" ")) + "\n";
        ;
//           outputStreamWriter.write(StrUtil.join("\n", str) + "\n");
        try {
            outputStreamWriter.write(s);
            outputStreamWriter.flush();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder processResSB = new StringBuilder();
            String outputLine;
            while ((outputLine = bufferedReader.readLine()) != null) {
                processResSB.append(outputLine);
            }
            processOutput.setMessage(processResSB.toString());
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return processOutput;
    }

}

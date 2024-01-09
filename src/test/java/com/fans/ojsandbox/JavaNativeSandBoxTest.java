package com.fans.ojsandbox;

import com.fans.ojsandbox.model.ExecuteQuestionRequest;
import com.fans.ojsandbox.model.ExecuteQuestionResponse;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

//@SpringBootTest
class JavaNativeSandBoxTest {
    //    @Resource
//    private CodeSandBox codeSandBox;
    @Resource
    private static final CodeSandBox codeSandBox = new JavaDockerCodeSandBoxAbsTemplateImpl();

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecuteQuestionRequest executeQuestionRequest = ExecuteQuestionRequest.builder()
                .code("public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(args[0] + args[1]);\n" +
                        "    }\n" +
                        "}")
//                .code("FileUtil.writeString(\"ssdf\",\"sdfsdf\", Charset.defaultCharset());")
                .language("java")
                .inputList(Arrays.asList("1 2", "1 3", "1 5", "883283oi2809 123981273981247985", "start end", "赛况饭赛况返矿赛返矿赛 爱空来返矿sad返矿;爱冬季疯狂"))
                .build();
        ExecuteQuestionResponse executeQuestionResponse = codeSandBox.doExecute(executeQuestionRequest);
        System.out.println(executeQuestionResponse);
        countDownLatch.await();
    }

    @Test
    void doExecute() {
        ExecuteQuestionRequest executeQuestionRequest = ExecuteQuestionRequest.builder()
                .code("public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(args[0] + args[1]);\n" +
                        "    }\n" +
                        "}")
//                .code("FileUtil.writeString(\"ssdf\",\"sdfsdf\", Charset.defaultCharset());")
                .language("java")
                .inputList(Arrays.asList("1 2", "1 3"))
                .build();
        ExecuteQuestionResponse executeQuestionResponse = codeSandBox.doExecute(executeQuestionRequest);
        System.out.println(executeQuestionResponse);


    }
}
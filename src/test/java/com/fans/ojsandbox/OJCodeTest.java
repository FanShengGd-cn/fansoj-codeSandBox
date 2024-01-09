package com.fans.ojsandbox;

import cn.hutool.core.io.FileUtil;
import com.fans.ojsandbox.security.DefaultSecurityManager;

import java.io.File;
import java.nio.charset.Charset;

public class OJCodeTest {
    public static void main(String[] args) {
        System.setSecurityManager(new DefaultSecurityManager());
        File file = FileUtil.writeString("aaa", "123123", Charset.defaultCharset());
    }
}

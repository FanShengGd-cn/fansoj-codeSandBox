package com.fans.ojsandbox.security;

import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        System.out.println(perm);
//        不做任何限制
//        super.checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("权限异常" + cmd);
    }

    @Override
    public void checkRead(String file) {
        throw new SecurityException("权限异常" + file);
    }

    @Override
    public void checkWrite(String file) {
        throw new SecurityException("权限异常" + file);
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("权限异常" + host + port);
    }
}

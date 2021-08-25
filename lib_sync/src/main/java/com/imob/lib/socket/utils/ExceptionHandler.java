package com.imob.lib.socket.utils;

public class ExceptionHandler {
    private static final boolean isDebug = true;

    public static void print(Throwable throwable) {
        if (isDebug) {
            throwable.printStackTrace();
        }
    }
}

package com.imob.lib.nsd.utils;

public class ExceptionHandler {


    private static boolean debug = true;

    public static void print(Throwable throwable) {
        if (debug) {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }
}

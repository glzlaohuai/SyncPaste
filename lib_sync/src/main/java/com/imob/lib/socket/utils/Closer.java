package com.imob.lib.socket.utils;

import java.io.Closeable;
import java.io.IOException;

public class Closer {

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}

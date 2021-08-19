package com.imob.lib.nsd.utils;

import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class SocketHandler {

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private OnSocketMonitor onSocketMonitor;

    public interface OnSocketMonitor {
        void onIncomingData(String id, byte[] bytes);

        void onDataWrited(String id, byte[] bytes);

        void onDataWriteFailedInvalidData();

        void onDataWriteFailedDisconnected(Exception exception);

    }

    public SocketHandler(Socket socket, OnSocketMonitor onSocketMonitor) throws IOException {
        this.socket = socket;

        if (this.socket == null) {
            throw new NullPointerException("socket is null");
        }

        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.dataInputStream = new DataInputStream(socket.getInputStream());

        this.onSocketMonitor = onSocketMonitor;
    }

    public void disconnect() {
        Closer.close(dataInputStream);
        Closer.close(dataOutputStream);

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public void write(String id, byte[] bytes) {
        if (bytes != null && !TextUtils.isEmpty(id)) {
            try {
                dataOutputStream.writeUTF(id);
                dataOutputStream.writeLong(bytes.length);
                dataOutputStream.write(bytes);
                dataOutputStream.flush();
            } catch (IOException e) {
                onSocketMonitor.onDataWriteFailedDisconnected(e);
            }
        } else {
            onSocketMonitor.onDataWriteFailedInvalidData();
        }
    }


    public void writeFile(String id, File file) {
        if (!TextUtils.isEmpty(id) && file != null && file.exists()) {
            
        } else {
            onSocketMonitor.onDataWriteFailedInvalidData();
        }
    }

}

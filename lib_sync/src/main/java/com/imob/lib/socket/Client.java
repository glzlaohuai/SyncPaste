package com.imob.lib.socket;

import com.imob.lib.socket.msg.IMsg;
import com.imob.lib.socket.utils.Closer;
import com.imob.lib.socket.utils.ExceptionHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private final static ExecutorService readAndConnectExecutor = Executors.newCachedThreadPool();

    private String id = UUID.randomUUID().toString();

    private Socket socket;

    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private OnClientMonitor clientMonitor;

    //对clientMonitor的proxy
    private OnClientMonitor proxyClientMonitor;

    private boolean isDisconnectInvoked = false;

    public interface OnClientMonitor {

        void onSocketCreated(String id);

        void onSocketCreateFailed(String id, String reason, Throwable throwable);

        void onIOStreamOpened(String id);

        void onStartMonitoringIncomData(String id);

        void onLostConnection(String id, String reason, Throwable throwable);
    }


    public Client(String ip, int port, OnClientMonitor clientMonitor) {
        this.clientMonitor = clientMonitor;

        if (ip == null || port <= 0) {
            proxyClientMonitor.onSocketCreateFailed(id, "server ip or port is invalid, ip: " + ip + ", port: " + port, null);
        } else {
            readAndConnectExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isDisconnectInvoked) {
                            proxyClientMonitor.onSocketCreateFailed(id, "disconnect invoked before create socket", null);
                            return;
                        }
                        socket = new Socket(ip, port);
                        handleSocket(socket);
                    } catch (IOException e) {
                        ExceptionHandler.print(e);
                        proxyClientMonitor.onSocketCreateFailed(id, "create socket instance failed", e);
                    }
                }
            });
        }
    }

    public void disconnect() {
        isDisconnectInvoked = true;

        if (socket != null || inputStream != null || outputStream != null) {
            closeConnectionIfConnectedAlready();
        }
    }

    private void closeConnectionIfConnectedAlready() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                ExceptionHandler.print(e);
            }
            Closer.close(inputStream);
            Closer.close(outputStream);
        }
    }

    private void handleSocket(Socket socket) {
        this.socket = socket;

        if (isDisconnectInvoked) {
            proxyClientMonitor.onSocketCreateFailed(id, "trying to handle socket, but found disconnect was invoked", null);
            closeConnectionIfConnectedAlready();
            return;
        }

        if (this.socket != null) {
            proxyClientMonitor.onSocketCreated(id);

            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());

                proxyClientMonitor.onIOStreamOpened(id);

                readAndConnectExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        proxyClientMonitor.onStartMonitoringIncomData(id);
                        while (true) {
                            //start monitor incoming data from peer
                            try {
                                byte type = inputStream.readByte();
                                switch (type) {
                                    case IMsg.TYPE_MSG_STR:


                                        break;
                                    case IMsg.TYPE_MSG_FILE:



                                        break;
                                }
                            } catch (IOException e) {
                                ExceptionHandler.print(e);
                                proxyClientMonitor.onLostConnection(id, "error occured during incoming monitor", e);
                                closeConnectionIfConnectedAlready();
                                break;
                            }
                        }
                    }
                });
            } catch (IOException e) {
                ExceptionHandler.print(e);
                proxyClientMonitor.onSocketCreateFailed(id, "error occured when trying to get io stream from socket", e);
            }
        } else {
            proxyClientMonitor.onSocketCreateFailed(id, "socket is null", null);
        }
    }


    public Client(Socket socket, OnClientMonitor clientMonitor) {
        this.clientMonitor = clientMonitor;
        handleSocket(socket);
    }
}

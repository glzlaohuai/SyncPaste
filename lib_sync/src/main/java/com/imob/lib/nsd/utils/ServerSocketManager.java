package com.imob.lib.nsd.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerSocketManager {

    private static ServerSocketManager instance = new ServerSocketManager();

    public static ServerSocketManager getInstance() {
        return instance;
    }

    public interface OnServerSocketListener {
        void onCreated(ServerSocket serverSocket);

        void onErrorOccurred(Throwable throwable);

        void onCreateFailed();

        void onStartMonitor();

        void onIncomingSocket(Socket socket);

        void onStopped();
    }


    private boolean created = false;
    private ServerSocket serverSocket;
    private OnServerSocketListener listener;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<Socket> connectedSocketList = new ArrayList<>();

    private void closeServerSocketIfNeeded() {
        created = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
            serverSocket = null;
        }
    }

    public void create(OnServerSocketListener listener) {
        if (!(created && serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed())) {
            closeServerSocketIfNeeded();
            this.listener = listener;
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                created = true;
                ServerSocketManager.this.serverSocket = serverSocket;
                if (serverSocket != null) {
                    listener.onCreated(serverSocket);
                } else {
                    listener.onCreateFailed();
                }
            } catch (IOException e) {
                listener.onErrorOccurred(e);
            }
        }
    }

    private boolean isServerSocketAvailable() {
        return created && serverSocket.isBound() && !serverSocket.isClosed();
    }


    public void startMonitorConnectedSockets() {
        if (isServerSocketAvailable()) {
            listener.onStartMonitor();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (true && isServerSocketAvailable()) {
                        try {
                            Socket socket = serverSocket.accept();
                            connectedSocketList.add(socket);
                            listener.onIncomingSocket(socket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public void destroy() {
        if (isServerSocketAvailable()) {
            closeServerSocketIfNeeded();
            executorService.shutdown();
            connectedSocketList.clear();
            listener.onStopped();
        }
    }


}

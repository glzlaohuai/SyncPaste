package com.imob.lib.socket;

import com.imob.lib.socket.utils.ExceptionHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final static byte[] lock = new byte[0];
    private static ServerSocket serverSocket;
    private final static Map<String, Client> connectedClients = new HashMap<>();

    private final static ExecutorService executorService = Executors.newSingleThreadExecutor();


    public interface OnServerMonitor {
        void onCreate(String ip, int port);

        void onCreateFailed(String reason, Exception exception);

        void onAlreadyCreated(String ip, int port);

        void onClientConnectMonitoring();

        void onClientConnected(Client client);

        void onLostConnection(Exception exception);
    }


    public static void createServer(OnServerMonitor onServerMonitor) {
        if (serverSocket == null) {
            synchronized (lock) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (serverSocket == null) {
                            synchronized (lock) {
                                if (serverSocket == null) {
                                    try {
                                        serverSocket = new ServerSocket();
                                        onServerMonitor.onClientConnectMonitoring();
                                        while (true) {
                                            try {
                                                Socket socket = serverSocket.accept();

                                                Client client = new Client(socket, new Client.OnClientMonitor() {

                                                    @Override
                                                    public void onSocketCreated(String id) {

                                                    }

                                                    @Override
                                                    public void onSocketCreateFailed(String id, String reason, Throwable throwable) {

                                                    }

                                                    @Override
                                                    public void onIOStreamOpened(String id) {

                                                    }

                                                    @Override
                                                    public void onStartMonitoringIncomData(String id) {

                                                    }

                                                    @Override
                                                    public void onLostConnection(String id, String reason, Throwable throwable) {

                                                    }
                                                });

                                            } catch (IOException e) {
                                                ExceptionHandler.print(e);
                                                onServerMonitor.onLostConnection(e);
                                                clean();
                                            }
                                        }
                                    } catch (IOException e) {
                                        ExceptionHandler.print(e);
                                        onServerMonitor.onCreateFailed("error occured during create server socket", e);
                                    }
                                } else {
                                    onServerMonitor.onAlreadyCreated(serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());
                                }
                            }
                        } else {
                            onServerMonitor.onAlreadyCreated(serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());
                        }
                    }
                });
            }
        } else {
            onServerMonitor.onAlreadyCreated(serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());
        }
    }

    public static void destroy() {
        clean();
    }

    private static void clean() {
        try {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    ExceptionHandler.print(e);
                }
                for (Map.Entry<String, Client> item : connectedClients.entrySet()) {
                    item.getValue().disconnect();
                }
            }
        } finally {
            serverSocket = null;
            connectedClients.clear();
        }
    }


}

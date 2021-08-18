package com.imob.syncpaste;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.imob.lib.nsd.utils.ServerSocketManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private ServerSocket serverSocket;
    private List<Socket> socketList = new ArrayList<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.create_server).setOnClickListener(this);
        findViewById(R.id.monitor_incoming).setOnClickListener(this);
        findViewById(R.id.stop_server).setOnClickListener(this);
        findViewById(R.id.connect_to_server).setOnClickListener(this);
        findViewById(R.id.check_client).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_server:
                createServer();
                break;
            case R.id.monitor_incoming:
                monitorIncoming();
                break;
            case R.id.stop_server:
                stopServer();
                break;
            case R.id.connect_to_server:
                connectToServer();
                break;
            case R.id.check_client:
                checkClientSocketConnections();
                break;
        }
    }

    private void checkClientSocketConnections() {
        for (Socket socket : socketList) {
            Log.i(TAG, "checkClientSocketConnections: " + (socket == null ? false : socket.isClosed()));
            try {
                OutputStream outputStream = socket.getOutputStream();
                if (outputStream != null) {
                    outputStream.write(new byte[]{1, 2, 3});
                }
                Log.i(TAG, "checkClientSocketConnections: " + outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToServer() {
        if (serverSocket != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
                        socketList.add(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    private void stopServer() {
        ServerSocketManager.getInstance().destroy();
        serverSocket = null;
    }

    private void monitorIncoming() {
        if (serverSocket != null) {
            ServerSocketManager.getInstance().startMonitorConnectedSockets();
        }
    }

    private void createServer() {
        ServerSocketManager.getInstance().create(new ServerSocketManager.OnServerSocketListener() {
            @Override
            public void onCreated(ServerSocket serverSocket) {
                MainActivity.this.serverSocket = serverSocket;
                Log.i(TAG, "onCreated: ");
            }

            @Override
            public void onErrorOccurred(Throwable throwable) {
                Log.i(TAG, "onErrorOccurred: ");

            }

            @Override
            public void onCreateFailed() {
                Log.i(TAG, "onCreateFailed: ");
            }

            @Override
            public void onStartMonitor() {
                Log.i(TAG, "onStartMonitor: ");
            }

            @Override
            public void onIncomingSocket(Socket socket) {
                Log.i(TAG, "onIncomingSocket: " + socket);
            }

            @Override
            public void onStopped() {
                Log.i(TAG, "onStopped: ");
            }
        });
    }
}

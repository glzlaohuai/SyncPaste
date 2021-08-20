package com.imob.syncpaste;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.imob.lib.nsd.utils.ServerSocketManager;
import com.imob.lib.nsd.utils.SocketHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private ServerSocket serverSocket;

    private SocketHandler clientSocketHandler;
    private List<SocketHandler> connectedClientsList = new ArrayList<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.create_server).setOnClickListener(this);
        findViewById(R.id.monitor_incoming).setOnClickListener(this);
        findViewById(R.id.stop_server).setOnClickListener(this);
        findViewById(R.id.connect_to_server).setOnClickListener(this);
        findViewById(R.id.log_server_info).setOnClickListener(this);
        findViewById(R.id.write_to_server).setOnClickListener(this);
        findViewById(R.id.write_to_clients).setOnClickListener(this);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
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
            case R.id.log_server_info:
                logServerInfo();
                break;


            case R.id.write_to_server:
                writeToServer();
                break;

            case R.id.write_to_clients:
                writeToClients();
                break;
        }
    }


    private void writeToServer() {

        if (clientSocketHandler != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    clientSocketHandler.writeString(UUID.randomUUID().toString(), "send a random msg to server: " + UUID.randomUUID().toString());
                }
            });
        } else {
            Log.i(TAG, "writeToServer: no socket found");
        }
    }

    private void writeToClients() {


        if (connectedClientsList.size() == 0) {
            Log.i(TAG, "writeToClients: found no connected client sockets");
        } else {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (SocketHandler socketHandler : connectedClientsList) {
                        socketHandler.writeString(UUID.randomUUID().toString(), "send a random msg to connected client: " + UUID.randomUUID().toString());
                    }
                }
            });
        }
    }


    private void logServerInfo() {

        if (serverSocket != null) {
            Log.i(TAG, "logServerInfo: " + serverSocket.getInetAddress().getHostName() + ", " + serverSocket.getLocalPort());
        }

    }

    private void connectToServer() {

        EditText editText = new EditText(this);
        new AlertDialog.Builder(this).setView(editText).setPositiveButton("好", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ip = editText.getText().toString().split(":")[0].trim();
                String port = editText.getText().toString().split(":")[1].trim();


                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i(TAG, "run: connect to :" + ip + ", " + port);
                            Socket socket = new Socket(ip, Integer.parseInt(port));
                            clientSocketHandler = new SocketHandler(socket, new SocketHandler.OnSocketMonitor() {
                                @Override
                                public void onIncomingStr(String id, byte[] bytes) {
                                    Log.i(TAG, "onIncomingStr from server: " + id + ", " + new String(bytes));
                                }

                                @Override
                                public void onIncomingFileBytes(String id, String fileName, long totalBytes) {
                                    Log.i(TAG, "onIncomingFileBytes: " + id + ", " + fileName + ", " + totalBytes);
                                }

                                @Override
                                public void onIncomingFailed(String id) {
                                    Log.i(TAG, "onIncomingFailed: ");
                                }

                                @Override
                                public void onDataWrited(String id) {
                                    Log.i(TAG, "onDataWrited: " + id);

                                }

                                @Override
                                public void onDataWriteFailedInvalidData() {
                                    Log.i(TAG, "onDataWriteFailedInvalidData: ");
                                }

                                @Override
                                public void onDataWriteFailedDisconnected(Exception exception) {
                                    Log.i(TAG, "onDataWriteFailedDisconnected: ");
                                }

                                @Override
                                public void onReadMonitorFailedDueToConnectionFailed(Exception exception) {
                                    Log.i(TAG, "onReadMonitorFailedDueToConnectionFailed: ");
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }).create().show();
    }

    private void stopServer() {
        ServerSocketManager.getInstance().stop();
        serverSocket = null;
    }

    private void monitorIncoming() {
        if (serverSocket != null) {
            ServerSocketManager.getInstance().startMonitorConnectedSockets();
        } else {
            throw new NullPointerException("no server socket found, need to create server first");
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

                try {
                    SocketHandler socketHandler = new SocketHandler(socket, new SocketHandler.OnSocketMonitor() {
                        @Override
                        public void onIncomingStr(String id, byte[] bytes) {
                            Log.i(TAG, "onIncomingStr: ");
                        }

                        @Override
                        public void onIncomingFileBytes(String id, String fileName, long totalBytes) {
                            Log.i(TAG, "onIncomingFileBytes: ");
                        }

                        @Override
                        public void onIncomingFailed(String id) {
                            Log.i(TAG, "onIncomingFailed: ");
                        }

                        @Override
                        public void onDataWrited(String id) {
                            Log.i(TAG, "onDataWrited: ");
                        }

                        @Override
                        public void onDataWriteFailedInvalidData() {
                            Log.i(TAG, "onDataWriteFailedInvalidData: ");
                        }

                        @Override
                        public void onDataWriteFailedDisconnected(Exception exception) {

                            Log.i(TAG, "onDataWriteFailedDisconnected: ");
                        }

                        @Override
                        public void onReadMonitorFailedDueToConnectionFailed(Exception exception) {
                            Log.i(TAG, "onReadMonitorFailedDueToConnectionFailed: ");
                        }
                    });

                    connectedClientsList.add(socketHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStopped() {
                Log.i(TAG, "onStopped: ");
            }
        });
    }
}

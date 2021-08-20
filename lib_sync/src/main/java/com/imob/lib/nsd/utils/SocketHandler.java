package com.imob.lib.nsd.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketHandler {

    private static final String TAG = "SocketHandler";

    public final static byte TYPE_FILE = 0x0;
    public final static byte TYPE_STR = 0x1;


    public static final int FLAG_READ_FILE_FAILED = -1;
    public static final int FLAG_READ_FILE_FINISHED = 0;


    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private OnSocketMonitor onSocketMonitor;

    private static ExecutorService executorService = Executors.newCachedThreadPool();


    public interface OnSocketMonitor {
        void onIncomingStr(String id, byte[] bytes);

        void onIncomingFileReadFinished(String id, String fileName, long totalBytes);

        void onIncomingFileReadChunk(String id, String fileName, byte[] segBytes, long totalBytes);

        void onIncomingFileEncouterPeerWriteFailedFlag(String id);

        void onDataWrited(String id);

        void onDataWriteFailedInvalidData();

        void onDataWriteFailedDisconnected(Exception exception);

        void onReadMonitorFailedDueToConnectionFailed(Exception exception);

    }

    public SocketHandler(Socket socket, OnSocketMonitor onSocketMonitor) throws IOException {
        this.socket = socket;

        if (this.socket == null) {
            throw new NullPointerException("socket is null");
        }

        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.dataInputStream = new DataInputStream(socket.getInputStream());

        this.onSocketMonitor = onSocketMonitor;

        startMonitorInput();
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


    private void writeStringBytes(String id, byte[] bytes) {
        if (bytes != null && !TextUtils.isEmpty(id)) {
            try {
                dataOutputStream.writeUTF(id);
                dataOutputStream.writeByte(TYPE_STR);
                //total len
                dataOutputStream.writeLong(bytes.length);
                //seg len
                dataOutputStream.writeInt(bytes.length);
                dataOutputStream.write(bytes);
                dataOutputStream.flush();

                onSocketMonitor.onDataWrited(id);
            } catch (IOException e) {
                onSocketMonitor.onDataWriteFailedDisconnected(e);
            }
        } else {
            onSocketMonitor.onDataWriteFailedInvalidData();
        }
    }


    public void writeString(String id, String content) {
        byte[] bytes = TextUtils.isEmpty(content) ? null : content.getBytes();
        writeStringBytes(id, bytes);
    }


    public void writeFile(String id, File file) {
        if (!TextUtils.isEmpty(id) && file != null && file.exists() && file.isFile()) {

            long availableBytes = 0;
            boolean availableBytesKnown = false;
            String fileName = null;
            RandomAccessFile randomAccessFile = null;

            try {
                randomAccessFile = new RandomAccessFile(file, "r");
                availableBytes = randomAccessFile.length();
                fileName = file.getName();
                availableBytesKnown = true;
            } catch (IOException e) {
                ExceptionHandler.print(e);
            }

            if (!availableBytesKnown) {
                onSocketMonitor.onDataWriteFailedInvalidData();
            } else {
                if (availableBytes == 0) {
                    onSocketMonitor.onDataWriteFailedInvalidData();
                } else {
                    // write segments

                    try {
                        dataOutputStream.writeUTF(id);
                        dataOutputStream.writeByte(TYPE_FILE);
                        dataOutputStream.writeLong(availableBytes);

                        dataOutputStream.writeUTF(fileName);
                    } catch (IOException e) {
                        ExceptionHandler.print(e);
                        onSocketMonitor.onDataWriteFailedDisconnected(e);

                        return;
                    }


                    byte[] buffer = new byte[1024 * 10];
                    while (true) {
                        int readBytes = 0;
                        try {
                            if (!((readBytes = randomAccessFile.read(buffer)) != -1)) break;
                        } catch (IOException e) {
                            ExceptionHandler.print(e);

                            //read failed
                            try {
                                dataOutputStream.writeInt(FLAG_READ_FILE_FAILED);
                            } catch (IOException ioException) {
                                ExceptionHandler.print(ioException);
                                onSocketMonitor.onDataWriteFailedDisconnected(ioException);
                                return;
                            }

                            onSocketMonitor.onDataWriteFailedInvalidData();
                        }

                        try {
                            Log.i(TAG, "writeFile chunk:  " + readBytes);
                            dataOutputStream.writeInt(readBytes);
                            dataOutputStream.write(buffer, 0, readBytes);
                        } catch (IOException e) {
                            ExceptionHandler.print(e);

                            onSocketMonitor.onDataWriteFailedDisconnected(e);
                            return;
                        }
                    }

                    try {
                        dataOutputStream.writeInt(FLAG_READ_FILE_FINISHED);
                        dataOutputStream.flush();
                        onSocketMonitor.onDataWrited(id);
                    } catch (IOException e) {
                        ExceptionHandler.print(e);
                        onSocketMonitor.onDataWriteFailedDisconnected(e);
                    }
                }
            }
        } else {
            onSocketMonitor.onDataWriteFailedInvalidData();
        }
    }


    private void startMonitorInput() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String id = dataInputStream.readUTF();
                        byte type = dataInputStream.readByte();
                        long availableBytes = dataInputStream.readLong();

                        if (type == TYPE_STR) {
                            int segLen = dataInputStream.readInt();

                            byte[] bytes = new byte[segLen];
                            dataInputStream.read(bytes, 0, segLen);

                            onSocketMonitor.onIncomingStr(id, bytes);

                        } else {
                            String fileName = dataInputStream.readUTF();

                            int segLen;

                            while (true) {
                                segLen = dataInputStream.readInt();
                                Log.i(TAG, "file seg len: " + segLen);

                                //peer write file failed due to file read failed
                                if (segLen == FLAG_READ_FILE_FAILED) {
                                    onSocketMonitor.onIncomingFileEncouterPeerWriteFailedFlag(id);
                                    break;
                                } else if (segLen == FLAG_READ_FILE_FINISHED) {
                                    onSocketMonitor.onIncomingFileReadFinished(id, fileName, availableBytes);
                                    break;
                                } else {
                                    byte[] segBytes = new byte[segLen];
                                    dataInputStream.read(segBytes, 0, segBytes.length);
                                    onSocketMonitor.onIncomingFileReadChunk(id, fileName, segBytes, availableBytes);
                                }
                            }
                        }
                    } catch (IOException e) {
                        ExceptionHandler.print(e);
                        onSocketMonitor.onReadMonitorFailedDueToConnectionFailed(e);
                        break;
                    }
                }
            }
        });
    }


}

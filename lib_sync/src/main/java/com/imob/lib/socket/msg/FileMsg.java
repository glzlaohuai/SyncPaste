package com.imob.lib.socket.msg;

import com.imob.lib.socket.utils.ExceptionHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileMsg implements IMsg {

    private File file;
    private RandomAccessFile randomAccessFile;
    private byte[] buffer = new byte[getChunkSize()];

    public FileMsg(File file) {
        this.file = file;
        try {
            if (this.file != null) {
                randomAccessFile = new RandomAccessFile(file, "r");
            }
        } catch (FileNotFoundException e) {
            ExceptionHandler.print(e);
        }
    }

    @Override
    public String getID() {
        return String.valueOf((file.getAbsolutePath() + file.lastModified()).hashCode());
    }

    @Override
    public void seekTo(long position) throws IOException {
        randomAccessFile.seek(position);
    }

    @Override
    public MsgChunk readChunk() throws IOException, MsgReadCompletedException {
        int readed = randomAccessFile.read(buffer);
        if (readed == -1) {
            throw new MsgReadCompletedException();
        }
        return null;
    }


    @Override
    public boolean isValid() {
        return file != null && file.isFile() && file.exists() && file.canRead() && randomAccessFile != null;
    }

    @Override
    public boolean isChunkSupported() {
        return true;
    }

    @Override
    public byte getType() {
        return TYPE_MSG_FILE;
    }

    @Override
    public int getChunkSize() {
        return 10240;
    }
}

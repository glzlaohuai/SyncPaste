package com.imob.lib.socket.msg;

import android.text.TextUtils;

import java.io.IOException;

public class StringMsg implements IMsg {
    private String data;

    public StringMsg(String data) {
        this.data = data;
    }

    @Override
    public String getID() {
        return String.valueOf(data.hashCode());
    }

    @Override
    public void seekTo(long position) {
        throw new UnsupportedOperationException("seek to not supported on string data");
    }

    @Override
    public MsgChunk readChunk() throws IOException, MsgReadCompletedException {
        byte[] bytes = data.getBytes();
        return new MsgChunk(bytes, bytes.length);
    }

    @Override
    public boolean isValid() {
        return !TextUtils.isEmpty(data);
    }

    @Override
    public boolean isChunkSupported() {
        return false;
    }

    @Override
    public byte getType() {
        return TYPE_MSG_STR;
    }

    @Override
    public int getChunkSize() {
        return 0;
    }


}

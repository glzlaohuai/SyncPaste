package com.imob.lib.socket.msg;

import java.io.IOException;

public interface IMsg {
    byte TYPE_MSG_STR = 0x0;
    byte TYPE_MSG_FILE = 0x1;

    class MsgChunk {
        private byte[] bytes;
        private int size;

        public MsgChunk(byte[] bytes, int size) {
            this.bytes = bytes;
            this.size = size;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getSize() {
            return size;
        }
    }

    String getID();

    void seekTo(long position) throws IOException;

    MsgChunk readChunk() throws IOException, MsgReadCompletedException;

    boolean isValid();

    boolean isChunkSupported();

    byte getType();

    int getChunkSize();

}

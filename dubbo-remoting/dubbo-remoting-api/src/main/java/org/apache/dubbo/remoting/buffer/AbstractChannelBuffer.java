/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.remoting.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 通道缓冲区的抽象实现
 */
public abstract class AbstractChannelBuffer implements ChannelBuffer {

    /**
     * 缓冲区的读指针
     */
    private int readerIndex;

    /**
     * 缓冲区的写指针
     */
    private int writerIndex;

    /**
     * 缓冲区中被标记的读指针
     */
    private int markedReaderIndex;

    /**
     * 缓冲区中被标记的写指针
     */
    private int markedWriterIndex;

    /**
     * 返回读指针
     * @return
     */
    @Override
    public int readerIndex() {
        return readerIndex;
    }

    /**
     * 设置读指针
     * @param readerIndex
     */
    @Override
    public void readerIndex(int readerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.readerIndex = readerIndex;
    }

    /**
     * 返回写指针
     * @return
     */
    @Override
    public int writerIndex() {
        return writerIndex;
    }

    /**
     * 设置写指针
     * @param writerIndex
     */
    @Override
    public void writerIndex(int writerIndex) {
        if (writerIndex < readerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        this.writerIndex = writerIndex;
    }

    /**
     * 设置读写指针
     * @param readerIndex
     * @param writerIndex
     */
    @Override
    public void setIndex(int readerIndex, int writerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
    }

    /**
     * 清空缓冲区的数据，将读写指针都设置为0
     */
    @Override
    public void clear() {
        readerIndex = writerIndex = 0;
    }

    /**
     * 判断缓冲区是否可读
     * @return
     */
    @Override
    public boolean readable() {
        return readableBytes() > 0;
    }

    /**
     * 判断缓冲区是否可写
     * @return
     */
    @Override
    public boolean writable() {
        return writableBytes() > 0;
    }

    /**
     * 返回可读的字节数
     * @return
     */
    @Override
    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    /**
     * 返回可写的字节数
     * @return
     */
    @Override
    public int writableBytes() {
        return capacity() - writerIndex;
    }

    /**
     * 设置读指针标记
     */
    @Override
    public void markReaderIndex() {
        markedReaderIndex = readerIndex;
    }

    /**
     * 重设读指针为标记的读指针
     */
    @Override
    public void resetReaderIndex() {
        readerIndex(markedReaderIndex);
    }

    /**
     * 设置写指针标记
     */
    @Override
    public void markWriterIndex() {
        markedWriterIndex = writerIndex;
    }

    /**
     * 重设写指针为标记的写指针
     */
    @Override
    public void resetWriterIndex() {
        writerIndex = markedWriterIndex;
    }

    /**
     * 丢弃已读的的字节数据
     */
    @Override
    public void discardReadBytes() {
        if (readerIndex == 0) {
            return;
        }
        setBytes(0, this, readerIndex, writerIndex - readerIndex);
        writerIndex -= readerIndex;
        markedReaderIndex = Math.max(markedReaderIndex - readerIndex, 0);
        markedWriterIndex = Math.max(markedWriterIndex - readerIndex, 0);
        readerIndex = 0;
    }

    @Override
    public void ensureWritableBytes(int writableBytes) {
        if (writableBytes > writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void getBytes(int index, byte[] dst) {
        getBytes(index, dst, 0, dst.length);
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst) {
        getBytes(index, dst, dst.writableBytes());
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    @Override
    public void setBytes(int index, byte[] src) {
        setBytes(index, src, 0, src.length);
    }

    @Override
    public void setBytes(int index, ChannelBuffer src) {
        setBytes(index, src, src.readableBytes());
    }

    @Override
    public void setBytes(int index, ChannelBuffer src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    @Override
    public byte readByte() {
        if (readerIndex == writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        return getByte(readerIndex++);
    }

    @Override
    public ChannelBuffer readBytes(int length) {
        checkReadableBytes(length);
        if (length == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }
        ChannelBuffer buf = factory().getBuffer(length);
        buf.writeBytes(this, readerIndex, length);
        readerIndex += length;
        return buf;
    }

    @Override
    public void readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
    }

    @Override
    public void readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
    }

    @Override
    public void readBytes(ChannelBuffer dst) {
        readBytes(dst, dst.writableBytes());
    }

    @Override
    public void readBytes(ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    @Override
    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
    }

    @Override
    public void readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        getBytes(readerIndex, dst);
        readerIndex += length;
    }

    @Override
    public void readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        getBytes(readerIndex, out, length);
        readerIndex += length;
    }

    @Override
    public void skipBytes(int length) {
        int newReaderIndex = readerIndex + length;
        if (newReaderIndex > writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        readerIndex = newReaderIndex;
    }

    @Override
    public void writeByte(int value) {
        setByte(writerIndex++, value);
    }

    @Override
    public void writeBytes(byte[] src, int srcIndex, int length) {
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
    }

    @Override
    public void writeBytes(byte[] src) {
        writeBytes(src, 0, src.length);
    }

    @Override
    public void writeBytes(ChannelBuffer src) {
        writeBytes(src, src.readableBytes());
    }

    @Override
    public void writeBytes(ChannelBuffer src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    @Override
    public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
    }

    @Override
    public void writeBytes(ByteBuffer src) {
        int length = src.remaining();
        setBytes(writerIndex, src);
        writerIndex += length;
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        int writtenBytes = setBytes(writerIndex, in, length);
        if (writtenBytes > 0) {
            writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public ChannelBuffer copy() {
        return copy(readerIndex, readableBytes());
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return toByteBuffer(readerIndex, readableBytes());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChannelBuffer
                && ChannelBuffers.equals(this, (ChannelBuffer) o);
    }

    @Override
    public int hashCode() {
        return ChannelBuffers.hasCode(this);
    }

    @Override
    public int compareTo(ChannelBuffer that) {
        return ChannelBuffers.compare(this, that);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' +
                "ridx=" + readerIndex + ", " +
                "widx=" + writerIndex + ", " +
                "cap=" + capacity() +
                ')';
    }

    protected void checkReadableBytes(int minimumReadableBytes) {
        if (readableBytes() < minimumReadableBytes) {
            throw new IndexOutOfBoundsException();
        }
    }
}

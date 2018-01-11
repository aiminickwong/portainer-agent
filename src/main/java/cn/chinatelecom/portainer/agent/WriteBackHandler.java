/*
 *  Copyright 2017 WalleZhang
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cn.chinatelecom.portainer.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import jnr.unixsocket.UnixSocketChannel;

/**
 * Write back to portainer
 *
 * @author WalleZhang
 */
public class WriteBackHandler implements CompletionHandler<Integer, ByteBuffer> {

    private AsynchronousSocketChannel channel;
    private UnixSocketChannel unixSocketChannel;

    WriteBackHandler(AsynchronousSocketChannel channel, UnixSocketChannel unixSocketChannel) {
        this.channel = channel;
        this.unixSocketChannel = unixSocketChannel;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        // Whether the remaining bytes in the buffer have not been written
        if (attachment.hasRemaining()) {
            // Write remaining data
            channel.write(attachment, attachment, this);
        } else {
            // If the number of bytes read is less than the capacity, the data from docker unix socket has been all read
            // or read remaining data
            if (attachment.limit() < attachment.capacity()) {
                // Allocate read buffer
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // read data from portainer asynchronously
                channel.read(readBuffer, readBuffer, new ReadHandler(unixSocketChannel, channel));
            } else {
                attachment.clear();
                try {
                    // Read remaining data from docker unix socket
                    unixSocketChannel.read(attachment);
                    attachment.flip();
                    channel.write(attachment, attachment, this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        exc.printStackTrace();
        try {
            channel.close();
            unixSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.info("Write back to portainer error, close channels.");
    }
}

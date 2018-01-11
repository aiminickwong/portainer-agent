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
 * Read data from portainer and transfer to docker unix socket
 *
 * @author WalleZhang
 */
public class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {

    private UnixSocketChannel unixSocketChannel;
    private AsynchronousSocketChannel channel;

    ReadHandler(UnixSocketChannel unixSocketChannel, AsynchronousSocketChannel channel) {
        this.unixSocketChannel = unixSocketChannel;
        this.channel = channel;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        // read new data if result length is greater than 0
        if (result > 0) {
            attachment.flip();
            try {
                // write to docker unix socket
                unixSocketChannel.write(attachment);
                // All data is read if buffer limit is less than capacity
                if (attachment.limit() < attachment.capacity()) {
                    // Read data from docker unix socket
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    unixSocketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    // Write back to portainer
                    channel.write(byteBuffer, byteBuffer, new WriteBackHandler(channel, unixSocketChannel));

                } else {
                    // Read remain bytes
                    attachment.clear();
                    channel.read(attachment, attachment, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Read the end of the channel, close it
            try {
                unixSocketChannel.close();
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        exc.printStackTrace();
        try {
            unixSocketChannel.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.info("Read data from portainer error, close channels.");
    }
}

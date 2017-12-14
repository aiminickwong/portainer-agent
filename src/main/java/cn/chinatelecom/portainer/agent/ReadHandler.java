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
 * 从Portainer读取数据的处理方法，读取并转发到docker.sock
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
    // 如果读取的字节数大于0，则代表有数据
    if (result > 0) {
      // 写入docker socket
      attachment.flip();
      try {
        // 写入docker socket
        unixSocketChannel.write(attachment);
        // 如果buffer的存储数据小于容量，说明所有数据读取完成
        if (attachment.limit() < attachment.capacity()) {
          // 从docker socket读取数据
          ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
          unixSocketChannel.read(byteBuffer);
          byteBuffer.flip();
          // 回写到portainer中
          channel.write(byteBuffer, byteBuffer, new WriteBackHandler(channel, unixSocketChannel));

        } else {
          // 读取剩余字节
          attachment.clear();
          channel.read(attachment, attachment, this);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      // 读取到channel末尾，关闭channel
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
    Log.info("从portainer读取出错，关闭双向channel");
  }
}

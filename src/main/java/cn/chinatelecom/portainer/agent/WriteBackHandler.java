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
 * 回写到portainer中
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
    // buffer中是否还有剩余字节未写完
    if (attachment.hasRemaining()) {
      // 继续写入剩下的数据
      channel.write(attachment, attachment, this);
    } else {
      // 如果读取到的字节数小于容量，说明docker socket的数据已全部读完
      // 反之，需要继续读取剩余部分的数据
      if (attachment.limit() < attachment.capacity()) {
        // 分配读取缓冲区
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        // 异步读Portainer发送的数据
        channel.read(readBuffer, readBuffer, new ReadHandler(unixSocketChannel, channel));
      } else {
        attachment.clear();
        try {
          // 继续从docker socket中读取剩余的数据
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
    Log.info("写回portainer出错，关闭双向channel");
  }
}

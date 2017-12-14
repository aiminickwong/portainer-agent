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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

/**
 * 接收新连接之后的处理类
 *
 * @author WalleZhang
 */
public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

  private AsynchronousServerSocketChannel serverSocketChannel;

  AcceptHandler(AsynchronousServerSocketChannel serverSocketChannel) {
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  public void completed(AsynchronousSocketChannel channel, Object attachment) {
    // 接收下一个新连接
    serverSocketChannel.accept(null, this);

    // 连接docker.sock
    File path = new File("/var/run/docker.sock");
    int retries = 0;
    while (!path.exists()) {
      try {
        TimeUnit.MILLISECONDS.sleep(500L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      retries++;
      if (retries > 5) {
        Log.info(String.format("文件 %s 不存在", path.getAbsolutePath()));
        Main.shutdown();
      }
    }

    UnixSocketAddress address = new UnixSocketAddress(path);
    UnixSocketChannel unixSocketChannel;
    try {
      unixSocketChannel = UnixSocketChannel.open(address);
      Log.info("已经连接到 " + unixSocketChannel.getRemoteSocketAddress());
    } catch (IOException e) {
      e.printStackTrace();
      Log.info("连接docker socket失败");
      return;
    }

    // 分配读取缓冲区
    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    // 异步读Portainer发送的数据
    channel.read(readBuffer, readBuffer, new ReadHandler(unixSocketChannel, channel));
  }

  @Override
  public void failed(Throwable exc, Object attachment) {
    exc.printStackTrace();
  }
}

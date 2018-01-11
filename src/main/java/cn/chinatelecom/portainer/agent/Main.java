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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * @author WalleZhang
 */
public class Main {

    private static volatile CountDownLatch countDownLatch = new CountDownLatch(1);
    private static AsynchronousServerSocketChannel serverSocketChannel;

    public static void main(String[] args) throws IOException {
        serverSocketChannel = AsynchronousServerSocketChannel.open()
            .setOption(StandardSocketOptions.SO_REUSEADDR, true).bind(new InetSocketAddress(5000));

        serverSocketChannel.accept(null, new AcceptHandler(serverSocketChannel));

        // 自动注册Endpoint
        try {
            PortainerAPI.registerEndpoint();
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("注册Endpoint失败");
        }

        // 因为AIO不会阻塞调用进程，因此必须在主进程阻塞，才能保持进程存活。
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        countDownLatch.countDown();
    }
}

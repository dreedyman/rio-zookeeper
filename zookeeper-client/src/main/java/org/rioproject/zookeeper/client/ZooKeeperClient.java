/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.zookeeper.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Dennis Reedy
 */
public class ZooKeeperClient {
    private ZooKeeper zooKeeper;
    private final ExecutorService futuresExecutor = Executors.newCachedThreadPool();
    private static Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);

    public Future<ZooKeeper> connect(final String hosts, final int timeout) throws IOException, InterruptedException {
        ZooKeeperFutureTask task = new ZooKeeperFutureTask();
        zooKeeper = new ZooKeeper(hosts, timeout, task);
        return futuresExecutor.submit(task);
    }

    public void close() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                logger.warn("While close ZooKeeper", e);
            }
            zooKeeper = null;
        }
    }

    class ZooKeeperFutureTask implements Callable<ZooKeeper>, Watcher {
        private final CountDownLatch counter = new CountDownLatch(1);

        public ZooKeeper call() throws Exception {
            if (zooKeeper.getState().isConnected())
                return zooKeeper;
            if (counter.getCount() > 0)
                counter.await();
            return zooKeeper;
        }

        public void process(final WatchedEvent event) {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                counter.countDown();
            }
        }
    }
}

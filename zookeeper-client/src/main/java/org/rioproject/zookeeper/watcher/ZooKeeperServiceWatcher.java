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
package org.rioproject.zookeeper.watcher;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.rioproject.impl.fdh.FaultDetectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dennis Reedy
 */
public class ZooKeeperServiceWatcher implements Watcher {
    private final ZooKeeper zooKeeper;
    private final Map<String, FaultDetectionListener<String>> services = new ConcurrentHashMap<String, FaultDetectionListener<String>>();
    private static Logger logger = LoggerFactory.getLogger(ZooKeeperServiceWatcher.class);

    /**
     * Create a {code ZooKeeperServiceWatcher}.
     *
     * @param zooKeeper The {@code ZooKeeper}, must not be {@code null}.
     *
     * @throws IllegalArgumentException if the {@code zooKeeper} is {@code null}.
     */
    public ZooKeeperServiceWatcher(final ZooKeeper zooKeeper) {
        if(zooKeeper==null)
            throw new IllegalArgumentException("zooKeeper can not be null");
        this.zooKeeper = zooKeeper;
    }

    /**
     * Add a service to watch.
     *
     * @param zNode The ZooKeeper node (zNode), must not be {@code null}.
     * @param listener The {@link FaultDetectionListener} to be notified if the service fails.
     *
     * @throws IllegalArgumentException if the {@code zNode} or {@code listener} is {@code null}.
     */
    public void addService(final String zNode, final FaultDetectionListener<String> listener) {
        if(zNode==null)
            throw new IllegalArgumentException("zNode can not be null");
        if(listener==null)
            throw new IllegalArgumentException("listener can not be null");
        zooKeeper.getChildren(zNode, true, new AsyncCallback.Children2Callback() {
            public void processResult(int rc, String path, Object o, List<String> strings, Stat stat) {
                if(KeeperException.Code.OK.equals(KeeperException.Code.get(rc))) {
                    services.put(zNode, listener);
                } else {
                    logger.error("Unable to add watch for {}, {}", zNode, KeeperException.Code.get(rc));
                }
            }
        }, null);
    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        if(logger.isDebugEnabled())
            logger.debug("{}", event);
        if(path==null)
            return;
        if (event.getType() == Event.EventType.None && event.getState().equals(Event.KeeperState.Expired)) {
            services.remove(path).serviceFailure(null, path);
        } else {
            if(logger.isDebugEnabled())
                logger.debug("Path: {}", path);
            if (services.get(path)!=null) {
                /* Something has changed on the node, let's find out if it still exists */
                zooKeeper.exists(path, false, new AsyncCallback.StatCallback() {
                    public void processResult(int rc, String path, Object ctx, Stat stat) {
                        if(KeeperException.Code.NONODE.equals(KeeperException.Code.get(rc))) {
                            services.remove(path).serviceFailure(null, path);
                        }
                    }
                }, null);
            }
        }
    }
}

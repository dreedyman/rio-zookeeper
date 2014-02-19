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

import junit.framework.Assert;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.impl.fdh.FaultDetectionListener;
import org.rioproject.zookeeper.ZooKeeperStarter;
import org.rioproject.zookeeper.client.GroupManagement;
import org.rioproject.zookeeper.client.ZooKeeperClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@code ZooKeeperServiceWatcher}
 *
 * @author Dennis Reedy
 */
public class ZooKeeperServiceWatcherTest {
    private final List<Node> nodes = new ArrayList<Node>();
    private final ZooKeeperStarter zooKeeperStarter = new ZooKeeperStarter();
    private ZooKeeper zooKeeper;
    private GroupManagement groupManagement;

    @Before
    public void startZooKeeper() throws IOException, InterruptedException, ExecutionException {
        zooKeeperStarter.start();
        ZooKeeperClient client = new ZooKeeperClient();
        zooKeeper = client.connect("127.0.0.1:"+zooKeeperStarter.getPort(), 3000).get();
        groupManagement = new GroupManagement(zooKeeper);
        String group1 = "group";
        groupManagement.create(group1);
        for(int i=0; i<100; i++) {
            nodes.add(new Node(group1, String.format("member:%d", i)));
        }
    }

    @After
    public void stopZooKeeper() {
        zooKeeperStarter.stop(true);
        nodes.clear();
    }

    @Test
    public void testNodeDisconnect() throws InterruptedException {
        Listener listener = new Listener();
        setupNodes(listener);
        Thread.sleep(1000);
        for(Node node : nodes) {
            node.close();
        }
        int waited = 0;
        while(listener.counter.get()<100 && waited<10) {
            System.out.println("Received "+listener.counter+" notifications");
            Thread.sleep(500);
            waited++;
        }
        Assert.assertEquals(100, listener.counter.get());
    }

    @Test
    public void testDeleteGroup() throws InterruptedException {
        Listener listener = new Listener();
        setupNodes(listener);
        groupManagement.delete("group");
        int waited = 0;
        while(listener.counter.get()<100 && waited<10) {
            System.out.println("Received "+listener.counter+" notifications");
            Thread.sleep(500);
            waited++;
        }
        Assert.assertEquals(100, listener.counter.get());
    }

    void setupNodes(Listener listener) {
        for(Node node : nodes) {
            node.connect();
        }
        for(Node node : nodes) {
            node.join();
        }
        ZooKeeperServiceWatcher serviceWatcher = new ZooKeeperServiceWatcher(zooKeeper);
        zooKeeper.register(serviceWatcher);
        for(Node node : nodes) {
            serviceWatcher.addService(node.getPath(), listener);
        }
    }

    class Listener implements FaultDetectionListener<String> {
        AtomicInteger counter = new AtomicInteger(0);

        public void serviceFailure(Object service, String serviceID) {
            counter.incrementAndGet();
            System.out.println("======================================\nService "+serviceID+" FAILED\n======================================");
        }
    }

    class Node {
        final String group;
        final String zNode;
        final ZooKeeperClient client = new ZooKeeperClient();

        Node(String group, String zNode) {
            this.group = group;
            this.zNode = zNode;
        }

        String getPath() {
            return String.format("/%s/%s", group, zNode);
        }

        void connect() {
            try {
                client.connect("127.0.0.1:"+zooKeeperStarter.getPort(), 1000).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void join() {
            groupManagement.join(group, zNode);
        }

        void close() {
            groupManagement.delete(group, zNode);
            client.close();
        }
    }
}

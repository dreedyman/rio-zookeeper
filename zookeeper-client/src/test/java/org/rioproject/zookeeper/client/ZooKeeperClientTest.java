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

import junit.framework.Assert;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.zookeeper.ZooKeeperStarter;

import java.io.IOException;

/**
 * @author Dennis Reedy
 */
public class ZooKeeperClientTest {
    private final ZooKeeperStarter zooKeeperStarter = new ZooKeeperStarter();

    @Before
    public void startZooKeeper() throws IOException, InterruptedException {
        zooKeeperStarter.start();
    }

    @After
    public void stopZooKeeper() {
        zooKeeperStarter.stop(true);
    }

    @Test
    public void testConnect() throws Exception {
        ZooKeeperClient client = new ZooKeeperClient();
        ZooKeeper zooKeeper = client.connect("127.0.0.1:"+zooKeeperStarter.getPort(), 3000).get();
        Assert.assertNotNull(zooKeeper);
    }
}

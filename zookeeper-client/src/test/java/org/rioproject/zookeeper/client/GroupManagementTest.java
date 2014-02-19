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
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.zookeeper.ZooKeeperStarter;

/**
 * @author Dennis Reedy
 */
public class GroupManagementTest {
    private final ZooKeeperStarter zooKeeperStarter = new ZooKeeperStarter();
    private GroupManagement groupManagement;
    private ZooKeeper zooKeeper;

    @Before
    public void setUp() throws Exception {
        zooKeeperStarter.start();
        ZooKeeperClient client = new ZooKeeperClient();
        zooKeeper = client.connect("127.0.0.1:"+zooKeeperStarter.getPort(), 3000).get();
        groupManagement = new GroupManagement(zooKeeper);
    }

    @After
    public void tearDown() throws Exception {
        zooKeeperStarter.stop(true);
    }

    @Test
    public void testCreate() throws Exception {
        groupManagement.create("Foo");
        Assert.assertTrue(groupManagement.groupExists("Foo"));
    }

    @Test
    public void testJoin() throws Exception {
        testCreate();
        groupManagement.join("Foo", "Bar");
        Stat stat = zooKeeper.exists("/Foo/Bar", false);
        Assert.assertNotNull(stat);
    }

    @Test
    public void testDeleteGroup() throws Exception {
        testJoin();
        groupManagement.delete("Foo");
        Thread.sleep(500);
        Assert.assertFalse(groupManagement.groupExists("Foo"));
    }

   @Test
    public void testDeleteMember() throws Exception {
        testJoin();
        groupManagement.delete("Foo", "Bar");
        Assert.assertTrue(groupManagement.groupExists("Foo"));
       Thread.sleep(500);
        Stat stat = zooKeeper.exists("/Foo/Bar", false);
        Assert.assertNull(stat);
    }
}

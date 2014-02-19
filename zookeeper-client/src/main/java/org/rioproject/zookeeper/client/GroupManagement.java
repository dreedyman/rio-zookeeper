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

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides group management support for ZooKeeper clients.
 *
 * @author Dennis Reedy
 */
public class GroupManagement {
    private final ZooKeeper zooKeeper;
    private static Logger logger = LoggerFactory.getLogger(GroupManagement.class);

    public GroupManagement(final ZooKeeper zooKeeper) {
        if(zooKeeper==null)
            throw new IllegalArgumentException("ZooKeeper must not be null");
        this.zooKeeper = zooKeeper;
    }

    public boolean groupExists(final String groupName) {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        String path = String.format("/%s", groupName);
        try {
            Stat stat = zooKeeper.exists(path, false);
            return stat!=null;
        } catch (KeeperException e) {
            logger.error("Checking for existence of group {}", path, e);
        } catch (InterruptedException e) {
            logger.warn("Transaction was interrupted", e);
        }
        return false;
    }

    public void create(final String groupName) {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        String path = String.format("/%s", groupName);
        zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, new AsyncCallback.StringCallback() {
            public void processResult(int rc, String createdPath, Object context, String name) {
                if(KeeperException.Code.OK.equals(KeeperException.Code.get(rc))) {
                    logger.info("Created {}", createdPath);
                } else {
                    logger.error("Unable to create {}, {}", createdPath, KeeperException.Code.get(rc));
                }
            }
        }, null);
    }

    public void join(final String groupName, final String memberName) {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        if(memberName==null)
            throw new IllegalArgumentException("memberName must not be null");
        String path = String.format("/%s/%s", groupName, memberName);
        zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, new AsyncCallback.StringCallback() {
            public void processResult(int rc, String createdPath, Object context, String name) {
                if(KeeperException.Code.OK.equals(KeeperException.Code.get(rc))) {
                    logger.info("Created {}", createdPath);
                } else {
                    logger.error("Unable to join group {}, {}", createdPath, KeeperException.Code.get(rc));
                }
            }
        }, null);
    }

    public List<String> list(final String groupName) throws KeeperException, InterruptedException {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        List<String> list = new ArrayList<String>();
        String path = String.format("/%s", groupName);
        try {
            list.addAll(zooKeeper.getChildren(path, false));
        } catch (KeeperException.NoNodeException e) {
            logger.warn("Group {} does not exist", groupName);
        }
        return list;
    }

    public void delete(final String groupName) {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        String path = String.format("/%s", groupName);
        zooKeeper.getChildren(path, false, new AsyncCallback.Children2Callback() {
            public void processResult(int rc, String path, Object context, List<String> children, Stat stat) {
                try {
                    for (String child : children) {
                        zooKeeper.delete(String.format("%s/%s", path, child), -1);
                    }
                    zooKeeper.delete(path, -1);
                } catch (KeeperException.NoNodeException e) {
                    logger.warn("Group {} does not exist", groupName);
                } catch (InterruptedException e) {
                    logger.warn("Transaction was interrupted", e);
                } catch (KeeperException e) {
                    logger.warn("Server error", e);
                }
            }
        }, null);
    }

    public void delete(final String groupName, final String memberName) {
        if(groupName==null)
            throw new IllegalArgumentException("groupName must not be null");
        if(memberName==null)
            throw new IllegalArgumentException("memberName must not be null");
        String path = String.format("/%s/%s", groupName, memberName);
        zooKeeper.delete(path, -1, new AsyncCallback.VoidCallback() {
            public void processResult(int rc, String path, Object context) {
                if(KeeperException.Code.OK.equals(KeeperException.Code.get(rc))) {
                    logger.info("Deleted {}", path);
                }
            }
        }, null);
    }

}

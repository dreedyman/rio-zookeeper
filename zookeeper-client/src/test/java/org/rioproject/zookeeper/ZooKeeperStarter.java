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
package org.rioproject.zookeeper;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

import java.io.File;
import java.io.IOException;

/**
 * Starts ZooKeeper in-process
 *
 * @author Dennis Reedy
 */
public class ZooKeeperStarter {
    private String zkPort;
    private ServerCnxnFactory standaloneServerFactory;
    private File logDir;

    public void start() throws IOException, InterruptedException {
        int tickTime = 1000;
        int numConnections = 5000;
        String dataDirectory = System.getProperty("java.io.tmpdir");

        logDir = new File(dataDirectory, "zookeeper").getAbsoluteFile();
        if(logDir.exists())
            remove(logDir);

        ZooKeeperServer server = new ZooKeeperServer(logDir, logDir, tickTime);
        standaloneServerFactory = ServerCnxnFactory.createFactory(3010, numConnections);
        zkPort = Integer.toString(standaloneServerFactory.getLocalPort());
        //zkPort = "2181";
        standaloneServerFactory.startup(server);
    }

    public String getPort() {
        return zkPort;
    }

    public void stop(boolean clean) {
        if(standaloneServerFactory!=null)
            standaloneServerFactory.shutdown();
        if(clean && logDir!=null)
            remove(logDir);
    }

    boolean remove(File file) {
        if (file.exists()) {
            boolean removed;
            if(file.isDirectory()) {
                File[] files = file.listFiles();
                if(files==null)
                    return false;
                for (File f : files) {
                    if (f.isDirectory())
                        remove(f);
                    else {
                        if(f.delete()) {
                            System.out.println("Removed "+ f.getPath());
                        } else {
                            if(f.exists())
                                System.out.println("Unable to remove "+ f.getPath());
                        }
                    }
                }
                removed = file.delete();
                if(removed) {
                    System.out.println("Removed "+ file.getPath());
                } else {
                    if(file.exists())
                        System.out.println("Unable to remove "+ file.getPath());
                }
            } else {
                removed = file.delete();
                if(removed) {
                    System.out.println("Removed "+ file.getPath());
                } else {
                    if(file.exists())
                        System.out.println("Unable to remove " + file.getPath());
                }
            }
        }
        return false;
    }
}

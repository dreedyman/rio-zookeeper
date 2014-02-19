/*
 * Configuration file for integration test cases
 */
ITZookeeperDeployTest {
    groups = "ZookeeperTest"
    numCybernodes = 1
    numMonitors = 1
    //numLookups = 1
    opstring = '../src/main/opstring/zookeeper.groovy'
    autoDeploy = true
    //harvest = true
}


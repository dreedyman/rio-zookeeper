import org.rioproject.config.Constants

deployment(name:'Zookeeper') {
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* Declares the artifacts required for deployment. Note the 'dl'
     * classifier used for the 'download' jar */
    artifact id:'service', 'org.rioproject.zookeeper:zookeeper-service:1.0-SNAPSHOT'
    artifact id:'service-dl', 'org.rioproject.zookeeper:zookeeper-api:1.0-SNAPSHOT'

    /*
     * Declare the service to be deployed. The number of instances deployed
     * defaults to 1. If you require > 1 instances change as needed
     */
    service(name: 'Zookeeper') {
        interfaces {
            classes 'org.rioproject.zookeeper.api.Zookeeper'
            artifact ref:'service-dl'
        }
        implementation(class:'org.rioproject.zookeeper.service.ZookeeperImpl') {
            artifact ref:'service'
        }
        maintain 3
    }
}
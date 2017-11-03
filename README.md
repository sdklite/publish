## Getting Started

1. Configure the classpath

    ```groovy
    buildscript {
        ...
        dependencies {
            ...
            classpath 'com.sdklite.publishing:maven:0.1.0'
        }
    }
    ```

1. Apply the plugin

    ```groovy
    apply plugin: 'com.sdklite.publishing.maven'
    ```

1. Configure properties for Maven

    | Properties                | Description                          | Default Value       |
    |:--------------------------|:-------------------------------------|:--------------------|
    | `group`                   | The groupId                          | `project.group`     |
    | `artifactId`              | The artifactId                       | `project.name`      |
    | `version`                 | The version                          | `project.version`   |
    | `packaging`               | The packaging                        | aar                 |
    | `url`                     | The project URL                      | `project.SCM_URL`   |
    | `SCM_URL`                 | The SCM URL                          |                     |
    | `SCM_CONNECTION`          | The SCM connection URI               |                     |
    | `SCM_DEV_CONNECTION`      | The SCM connection URI for developer |                     |
    | `LICENSE_NAME`            | The license name                     |                     |
    | `LICENSE_URL`             | The license URL                      |                     |
    | `LICENSE_DIST`            | The license distribution             |                     |
    | `DEVELOPER_ID`            | The ID of developer                  | `${user.name}`      |
    | `DEVELOPER_NAME`          | The name of developer                | `${user.name}`      |
    | `RELEASE_REPOSITORY_URL`  | The repository URL for release       | `~/.m2/repository`  |
    | `SNAPSHOT_REPOSITORY_URL` | The repository URL for snapshot      | `~/.m2/repository`  | 
    | `REPOSITORY_USERNAME`     | The username                         |                     | 
    | `REPOSITORY_PASSWORD`     | The password                         |                     | 

1. Deploy the library

    ```bash
    $ ./gradlew clean uploadArchives
    ```
    
    By default a library only publishes its release variant. You can control which variant gets published:
    
    ```groovy
    android {
        defaultPublishConfig 'debug'
    }
    ```
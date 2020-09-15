pipeline {
    agent any
    // Triggers for calling builds
    triggers {
        githubPush()
    }
    // Additional credentials for gradle tasks
    environment {
        NEXUS = credentials("Jenkins-Nexus")
    }
    // Options to configure workspace
    options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()
    }
    // Tools to specify specific gradle/jdk/etc tools
    tools {
        gradle 'Gradle-6'
        jdk 'JDK-11'
    }
    stages {
        // Test code can compile successfully
        stage ('Compile') {
            steps {
                echo "Compiling the code..."
                sh 'gradle clean build'
            }
        }
        // Save the build artifacts for automatic deployment
        stage ('Archive') {
            steps {
                echo "Grabbing artifacts..."
                archiveArtifacts artifacts: '**/build/libs/*.jar', onlyIfSuccessful: true
            }
        }
        // Deploy the artifacts to maven
        stage ('Nexus Publish') {
            when {
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                echo "Publishing to nexus..."
                sh 'gradle :worldedit-bukkit:publish'
            }
        }
    }
}

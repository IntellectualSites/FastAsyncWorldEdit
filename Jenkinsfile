pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    stages {
        stage('Build') {
            steps {
                withEnv([
                        "PATH+JAVA=${tool 'Temurin-21.0.3_9'}/bin"
                ]) {
                    sh './gradlew clean build'
                }
            }
        }
        stage('Archive artifacts') {
            steps {
                sh 'rm -rf artifacts'
                sh 'mkdir artifacts'
                sh 'cp worldedit-bukkit/build/libs/FastAsyncWorldEdit*.jar artifacts/'
                sh 'cp worldedit-cli/build/libs/FastAsyncWorldEdit*.jar artifacts/'
                archiveArtifacts artifacts: 'artifacts/*.jar', followSymlinks: false
            }
        }
        stage('Fingerprint artifacts') {
            steps {
                fingerprint 'worldedit-bukkit/build/libs/FastAsyncWorldEdit*.jar'
            }
        }
        stage('Publish JUnit test results') {
            steps {
                junit 'worldedit-core/build/test-results/test/*.xml,worldedit-bukkit/build/test-results/test/*.xml'
            }
        }
    }
}

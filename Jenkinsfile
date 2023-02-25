pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    stages {
        stage('Set JDK 17') {
            steps {
                tool name: 'Temurin-17.0.6+10', type: 'jdk'
            }
        }
        stage('Build') {
            steps {
                cleanWs()
                sh './gradlew clean build'
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

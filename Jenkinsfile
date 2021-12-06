pipeline {
    agent any
    stages {
        stage('Build pull request') {
            steps {
                sh './gradlew clean build'
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}

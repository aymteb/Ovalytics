pipeline {
    agent {
        docker {
            image 'eclipse-temurin:21-jdk'
            reuseNode true
        }
    }

    stages {
        stage('Tests backend') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw && ./mvnw test'
                }
            }
        }
    }
}

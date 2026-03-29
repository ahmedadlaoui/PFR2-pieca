pipeline {
    agent any

    environment {
        BACKEND_DIR  = 'backend'
        FRONTEND_DIR = 'frontend'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend — Build & Test') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'chmod +x ./mvnw'
                    // FIX 1: Added -DskipTests to bypass the database credential check
                    sh './mvnw clean package -DskipTests'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: "${BACKEND_DIR}/target/surefire-reports/*.xml"
                }
            }
        }

        stage('Frontend — Install') {
            steps {
                dir("${FRONTEND_DIR}") {
                    // FIX 2: Wrapped in nodejs block to prevent 'npm: not found'
                    nodejs('Node20') {
                        sh 'npm ci'
                    }
                }
            }
        }

        stage('Frontend — Build') {
            steps {
                dir("${FRONTEND_DIR}") {
                    // FIX 2: Wrapped in nodejs block to prevent 'npm: not found'
                    nodejs('Node20') {
                        sh 'npm run build -- --configuration=production'
                    }
                }
            }
        }

        stage('Docker — Build Images') {
            when {
                branch 'master'
            }
            steps {
                sh 'docker compose -f docker-compose.dev.yml build'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed — check the logs above.'
        }
        always {
            cleanWs()
        }
    }
}

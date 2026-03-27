pipeline {
    agent any

    environment {
        FRONTEND_DIR = 'Data-Agent-Client'
        BACKEND_ROOT = 'Data-Agent-Server'
        BACKEND_MODULE = 'data-agent-server-app'
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Show Project Structure') {
            steps {
                sh 'pwd'
                sh 'ls -la'
                sh 'ls -la ${FRONTEND_DIR}'
                sh 'ls -la ${BACKEND_ROOT}'
            }
        }

        stage('Build Frontend') {
            steps {
        dir("${env.BACKEND_ROOT}") {
            sh '''
                set -e
                echo "JAVA_HOME=$JAVA_HOME"
                which java || true
                which javac || true
                java -version
                javac -version
                mvn -version
                mvn -pl data-agent-server-app -am clean package -DskipTests -e
            '''
                }
            }
        }

        stage('Build Backend') {
            steps {
                dir("${env.BACKEND_ROOT}") {
                    sh '''
                        set -e
                        java -version
                        mvn -version
                        mvn -pl data-agent-server-app -am clean package -DskipTests
                    '''
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'Data-Agent-Client/dist/**,Data-Agent-Server/data-agent-server-app/target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo '前后端构建成功'
        }
        failure {
            echo '构建失败，请查看 Console Output'
        }
    }
}
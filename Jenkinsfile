pipeline {
    agent any

<<<<<<< HEAD
    options {
        disableConcurrentBuilds()
    }

=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    environment {
        FRONTEND_DIR = 'Data-Agent-Client'
        BACKEND_ROOT = 'Data-Agent-Server'
        BACKEND_MODULE = 'data-agent-server-app'
<<<<<<< HEAD
        BACKEND_JAR = 'data-agent-server-app-0.0.1-SNAPSHOT.jar'
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
<<<<<<< HEAD
                sh '''
                    printf '\\n\\033[1;36m========== [SHOW PROJECT STRUCTURE] ==========\\033[0m\\n'
                    pwd
                    ls -la

                    printf '\\n\\033[1;36m========== [FRONTEND ROOT] ==========\\033[0m\\n'
                    ls -la "${FRONTEND_DIR}"

                    printf '\\n\\033[1;36m========== [BACKEND ROOT] ==========\\033[0m\\n'
                    ls -la "${BACKEND_ROOT}"
                '''
=======
                sh 'pwd'
                sh 'ls -la'
                sh 'ls -la ${FRONTEND_DIR}'
                sh 'ls -la ${BACKEND_ROOT}'
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
            }
        }

        stage('Build Frontend') {
            steps {
                dir("${env.FRONTEND_DIR}") {
                    sh '''
                        set -e
<<<<<<< HEAD
                        printf '\\n\\033[1;36m========== [BUILD FRONTEND] ==========\\033[0m\\n'

=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
                        node -v
                        npm -v

                        if [ -f package-lock.json ]; then
                          npm ci
                        else
                          npm install
                        fi

                        npm run build
<<<<<<< HEAD
                        test -f dist/index.html
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
                    '''
                }
            }
        }

        stage('Build Backend') {
            steps {
                dir("${env.BACKEND_ROOT}") {
                    sh '''
                        set -e
<<<<<<< HEAD
                        printf '\\n\\033[1;36m========== [BUILD BACKEND] ==========\\033[0m\\n'

=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
                        echo "JAVA_HOME=$JAVA_HOME"
                        which java || true
                        which javac || true
                        java -version
                        javac -version
                        mvn -version
<<<<<<< HEAD

                        mvn -pl ${BACKEND_MODULE} -am clean package -DskipTests -e
                        test -f ${BACKEND_MODULE}/target/${BACKEND_JAR}
=======
                        mvn -pl ${BACKEND_MODULE} -am clean package -DskipTests -e
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
                    '''
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'Data-Agent-Client/dist/**,Data-Agent-Server/data-agent-server-app/target/*.jar', fingerprint: true
            }
        }
<<<<<<< HEAD

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    printf '\\n\\033[1;36m========== [DEPLOY FEATURE BRANCH] ==========\\033[0m\\n'

                    sudo /usr/local/bin/deploy-data-agent.sh \
                      "$WORKSPACE/${FRONTEND_DIR}/dist" \
                      "$WORKSPACE/${BACKEND_ROOT}/${BACKEND_MODULE}/target/${BACKEND_JAR}"
                '''
            }
        }
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    post {
        success {
<<<<<<< HEAD
            sh '''
                printf '\\n\\033[1;32m========== [POST SUCCESS] ==========\\033[0m\\n'
                echo "当前任务已完成自动部署"
                curl -I http://127.0.0.1/ || true
                curl -I http://127.0.0.1:8081/api/ || true
            '''
        }

        failure {
            sh '''
                printf '\\n\\033[1;31m========== [POST FAILURE :: APP LOG] ==========\\033[0m\\n'
                sudo tail -n 100 /opt/data-agent/logs/app.log || true

                printf '\\n\\033[1;31m========== [POST FAILURE :: OPENRESTY ERROR LOG] ==========\\033[0m\\n'
                sudo docker exec 1Panel-openresty-XsQg sh -lc "tail -n 100 /var/log/nginx/error.log" || true
            '''
=======
            echo '前后端构建成功'
        }
        failure {
            echo '构建失败，请查看 Console Output'
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        }
    }
}
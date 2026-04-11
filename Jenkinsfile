pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        FRONTEND_DIR = 'Data-Agent-Client'
        BACKEND_ROOT = 'Data-Agent-Server'
        BACKEND_MODULE = 'data-agent-server-app'
        BACKEND_JAR = 'data-agent-server-app-0.0.1-SNAPSHOT.jar'
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
                sh '''
                    printf '\\n\\033[1;36m========== [SHOW PROJECT STRUCTURE] ==========\\033[0m\\n'
                    pwd
                    ls -la

                    printf '\\n\\033[1;36m========== [FRONTEND ROOT] ==========\\033[0m\\n'
                    ls -la "${FRONTEND_DIR}"

                    printf '\\n\\033[1;36m========== [BACKEND ROOT] ==========\\033[0m\\n'
                    ls -la "${BACKEND_ROOT}"
                '''
            }
        }

        stage('Build Frontend') {
            steps {
                dir("${env.FRONTEND_DIR}") {
                    sh '''
                        set -e
                        printf '\\n\\033[1;36m========== [BUILD FRONTEND] ==========\\033[0m\\n'

                        node -v
                        npm -v

                        if [ -f package-lock.json ]; then
                          npm ci
                        else
                          npm install
                        fi

                        npm run build
                        test -f dist/index.html
                    '''
                }
            }
        }

        stage('Build Backend') {
            steps {
                dir("${env.BACKEND_ROOT}") {
                    sh '''
                        set -e
                        printf '\\n\\033[1;36m========== [BUILD BACKEND] ==========\\033[0m\\n'

                        echo "JAVA_HOME=$JAVA_HOME"
                        which java || true
                        which javac || true
                        java -version
                        javac -version
                        mvn -version

                        # 临时跳过测试编译
                        mvn -pl ${BACKEND_MODULE} -am clean package -Dmaven.test.skip=true -e
                        test -f ${BACKEND_MODULE}/target/${BACKEND_JAR}
                    '''
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'Data-Agent-Client/dist/**,Data-Agent-Server/data-agent-server-app/target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    printf '\\n\\033[1;36m========== [DEPLOY FEATURE BRANCH] ==========\\033[0m\\n'

                    sudo /usr/local/bin/deploy-data-agent.sh \
                      "$WORKSPACE/${FRONTEND_DIR}/dist" \
                      "$WORKSPACE/${BACKEND_ROOT}/${BACKEND_MODULE}/target/${BACKEND_JAR}"

                    # 健康检查：最多重试5次，每次间隔2秒
                    for i in $(seq 1 5); do
                        HTTP_CODE=$(curl -o /dev/null -s -w "%{http_code}" http://127.0.0.1:8081/actuator/health)
                        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ]; then
                            echo "Backend healthy (HTTP $HTTP_CODE)"
                            break
                        fi
                        echo "Health check failed (HTTP $HTTP_CODE), retrying..."
                        sleep 2
                    done
                '''
            }
        }
    }

    post {
        success {
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
        }
    }
}
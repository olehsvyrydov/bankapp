pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'docker.io'
        HELM_CHART_PATH = 'helm/bank-app'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        SERVICES = 'accounts-service,auth-server,blocker-service,cash-service,exchange-service,exchange-generator-service,front-ui,gateway-service,notifications-service,transfer-service'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out Bank Application source code"
                checkout scm
            }
        }

        stage('Validate Umbrella Helm Chart') {
            steps {
                echo "Validating umbrella Helm chart"
                sh """
                    helm lint ${HELM_CHART_PATH}
                    helm dependency update ${HELM_CHART_PATH}
                """
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Build accounts-service') {
                    steps {
                        echo "Building accounts-service"
                        dir('accounts-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build auth-server') {
                    steps {
                        echo "Building auth-server"
                        dir('auth-server') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build blocker-service') {
                    steps {
                        echo "Building blocker-service"
                        dir('blocker-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build cash-service') {
                    steps {
                        echo "Building cash-service"
                        dir('cash-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build exchange-service') {
                    steps {
                        echo "Building exchange-service"
                        dir('exchange-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build exchange-generator-service') {
                    steps {
                        echo "Building exchange-generator-service"
                        dir('exchange-generator-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build notifications-service') {
                    steps {
                        echo "Building notifications-service"
                        dir('notifications-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build transfer-service') {
                    steps {
                        echo "Building transfer-service"
                        dir('transfer-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build gateway-service') {
                    steps {
                        echo "Building gateway-service"
                        dir('gateway-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build front-ui') {
                    steps {
                        echo "Building front-ui"
                        dir('front-ui') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Run All Tests') {
            steps {
                echo "Running unit tests for all services"
                sh '''
                    mvn test -pl accounts-service,auth-server,blocker-service,cash-service,exchange-service,exchange-generator-service,notifications-service,transfer-service,gateway-service,front-ui
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build All Docker Images') {
            parallel {
                stage('Docker accounts-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('accounts-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/accounts-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/accounts-service:${imageTag} ${DOCKER_REGISTRY}/accounts-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker auth-server') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('auth-server') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/auth-server:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/auth-server:${imageTag} ${DOCKER_REGISTRY}/auth-server:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker blocker-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('blocker-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/blocker-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/blocker-service:${imageTag} ${DOCKER_REGISTRY}/blocker-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker cash-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('cash-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/cash-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/cash-service:${imageTag} ${DOCKER_REGISTRY}/cash-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker exchange-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('exchange-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/exchange-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/exchange-service:${imageTag} ${DOCKER_REGISTRY}/exchange-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker exchange-generator-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('exchange-generator-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/exchange-generator-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/exchange-generator-service:${imageTag} ${DOCKER_REGISTRY}/exchange-generator-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker notifications-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('notifications-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/notifications-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/notifications-service:${imageTag} ${DOCKER_REGISTRY}/notifications-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker transfer-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('transfer-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/transfer-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/transfer-service:${imageTag} ${DOCKER_REGISTRY}/transfer-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker gateway-service') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('gateway-service') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/gateway-service:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/gateway-service:${imageTag} ${DOCKER_REGISTRY}/gateway-service:latest
                                """
                            }
                        }
                    }
                }
                stage('Docker front-ui') {
                    steps {
                        script {
                            def imageTag = "${BUILD_NUMBER}"
                            dir('front-ui') {
                                sh """
                                    docker build -t ${DOCKER_REGISTRY}/front-ui:${imageTag} .
                                    docker tag ${DOCKER_REGISTRY}/front-ui:${imageTag} ${DOCKER_REGISTRY}/front-ui:latest
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Push All Docker Images') {
            steps {
                echo "Pushing all Docker images to registry"
                script {
                    def imageTag = "${BUILD_NUMBER}"
                    def services = ['accounts-service', 'auth-server', 'blocker-service',
                                  'cash-service', 'exchange-service', 'exchange-generator-service',
                                  'notifications-service', 'transfer-service', 'gateway-service', 'front-ui']

                    withDockerRegistry([credentialsId: 'docker-registry-credentials', url: "https://${DOCKER_REGISTRY}"]) {
                        services.each { service ->
                            sh """
                                docker push ${DOCKER_REGISTRY}/${service}:${imageTag}
                                docker push ${DOCKER_REGISTRY}/${service}:latest
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            steps {
                echo "Deploying Bank Application to dev namespace"
                script {
                    def imageTag = "${BUILD_NUMBER}"
                    sh """
                        helm upgrade --install bank-app ${HELM_CHART_PATH} \
                            --namespace bank-app-dev \
                            --create-namespace \
                            --set global.image.tag=${imageTag} \
                            --set accounts-service.image.tag=${imageTag} \
                            --set auth-server.image.tag=${imageTag} \
                            --set blocker-service.image.tag=${imageTag} \
                            --set cash-service.image.tag=${imageTag} \
                            --set exchange-service.image.tag=${imageTag} \
                            --set exchange-generator-service.image.tag=${imageTag} \
                            --set notifications-service.image.tag=${imageTag} \
                            --set transfer-service.image.tag=${imageTag} \
                            --set gateway-service.image.tag=${imageTag} \
                            --set front-ui.image.tag=${imageTag} \
                            --values ${HELM_CHART_PATH}/values-dev.yaml \
                            --wait \
                            --timeout 10m
                    """
                }
            }
        }

        stage('Run Integration Tests - Dev') {
            steps {
                echo "Running integration tests in dev environment"
                sh """
                    helm test bank-app --namespace bank-app-dev --timeout 10m || true
                """
            }
        }

        stage('Deploy to Test') {
            when {
                branch 'master'
            }
            steps {
                echo "Deploying Bank Application to test namespace"
                script {
                    def imageTag = "${BUILD_NUMBER}"
                    sh """
                        helm upgrade --install bank-app ${HELM_CHART_PATH} \
                            --namespace bank-app-test \
                            --create-namespace \
                            --set global.image.tag=${imageTag} \
                            --set accounts-service.image.tag=${imageTag} \
                            --set auth-server.image.tag=${imageTag} \
                            --set blocker-service.image.tag=${imageTag} \
                            --set cash-service.image.tag=${imageTag} \
                            --set exchange-service.image.tag=${imageTag} \
                            --set exchange-generator-service.image.tag=${imageTag} \
                            --set notifications-service.image.tag=${imageTag} \
                            --set transfer-service.image.tag=${imageTag} \
                            --set gateway-service.image.tag=${imageTag} \
                            --set front-ui.image.tag=${imageTag} \
                            --values ${HELM_CHART_PATH}/values-test.yaml \
                            --wait \
                            --timeout 10m
                    """
                }
            }
        }

        stage('Run Integration Tests - Test') {
            when {
                branch 'master'
            }
            steps {
                echo "Running integration tests in test environment"
                sh """
                    helm test bank-app --namespace bank-app-test --timeout 10m || true
                """
            }
        }

        stage('Approval for Production') {
            when {
                branch 'master'
            }
            steps {
                input message: 'Deploy entire Bank Application to production?', ok: 'Deploy'
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'master'
            }
            steps {
                echo "Deploying Bank Application to production namespace"
                script {
                    def imageTag = "${BUILD_NUMBER}"
                    sh """
                        helm upgrade --install bank-app ${HELM_CHART_PATH} \
                            --namespace bank-app-prod \
                            --create-namespace \
                            --set global.image.tag=${imageTag} \
                            --set accounts-service.image.tag=${imageTag} \
                            --set auth-server.image.tag=${imageTag} \
                            --set blocker-service.image.tag=${imageTag} \
                            --set cash-service.image.tag=${imageTag} \
                            --set exchange-service.image.tag=${imageTag} \
                            --set exchange-generator-service.image.tag=${imageTag} \
                            --set notifications-service.image.tag=${imageTag} \
                            --set transfer-service.image.tag=${imageTag} \
                            --set gateway-service.image.tag=${imageTag} \
                            --set front-ui.image.tag=${imageTag} \
                            --values ${HELM_CHART_PATH}/values-prod.yaml \
                            --wait \
                            --timeout 10m
                    """
                }
            }
        }

        stage('Run Smoke Tests - Production') {
            when {
                branch 'master'
            }
            steps {
                echo "Running smoke tests in production environment"
                sh """
                    helm test bank-app --namespace bank-app-prod --timeout 10m || true
                """
            }
        }

        stage('Deployment Summary') {
            steps {
                script {
                    def imageTag = "${BUILD_NUMBER}"
                    echo """
                    ========================================
                    Bank Application Deployment Summary
                    ========================================
                    Build Number: ${BUILD_NUMBER}
                    Image Tag: ${imageTag}
                    Helm Chart: ${HELM_CHART_PATH}

                    Services Deployed:
                    - accounts-service
                    - auth-server
                    - blocker-service
                    - cash-service
                    - exchange-service
                    - exchange-generator-service
                    - notifications-service
                    - transfer-service
                    - gateway-service
                    - front-ui
                    - postgresql

                    Namespaces:
                    - bank-app-dev
                    - bank-app-test (master branch only)
                    - bank-app-prod (master branch only, with approval)

                    Access:
                    - Dev: http://bank-app.local (via Ingress)
                    - Test: http://bank-app-test.local (via Ingress)
                    - Prod: http://bank-app-prod.local (via Ingress)
                    ========================================
                    """
                }
            }
        }
    }

    post {
        always {
            echo "Cleaning up workspace"
            cleanWs()
        }
        success {
            echo "Bank Application pipeline completed successfully"
            // You can add notification here (email, Slack, etc.)
        }
        failure {
            echo "Bank Application pipeline failed"
            // You can add notification here (email, Slack, etc.)
        }
    }
}

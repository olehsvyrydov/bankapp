pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Container image tag to build and deploy')
    string(name: 'DOCKER_REGISTRY', defaultValue: 'registry.example.com/bank', description: 'Docker registry root where images are pushed')
    string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: '', description: 'Jenkins credentials ID for the container registry')
    string(name: 'KUBE_CONFIG_TEST', defaultValue: 'kubeconfig-test', description: 'Jenkins credentials ID with kubeconfig for the test cluster')
    string(name: 'KUBE_CONFIG_PROD', defaultValue: 'kubeconfig-prod', description: 'Jenkins credentials ID with kubeconfig for the prod cluster')
    booleanParam(name: 'DEPLOY_TO_PROD', defaultValue: false, description: 'Deploy to production after a successful build')
  }

  environment {
    MODULE = 'cash-service'
    SERVICE_NAME = 'cash-service'
    CHART_DIR = 'helm/services/cash'
    TEST_NAMESPACE = 'test'
    PROD_NAMESPACE = 'prod'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Prepare') {
      steps {
        script {
          env.DOCKER_REGISTRY_PATH = params.DOCKER_REGISTRY
          env.IMAGE_REPO = "${env.DOCKER_REGISTRY_PATH}/${env.SERVICE_NAME}"
          env.IMAGE_TAG = params.IMAGE_TAG
        }
      }
    }

    stage('Build & Test') {
      steps {
        sh 'mvn -pl ${MODULE} -am verify'
      }
    }

    

    stage('Build & Push Image') {
      steps {
        script {
          def buildPush = """
            docker build -t ${env.IMAGE_REPO}:${env.IMAGE_TAG} -f ${env.MODULE}/Dockerfile .
            docker push ${env.IMAGE_REPO}:${env.IMAGE_TAG}
          """.stripIndent()

          if (params.DOCKER_CREDENTIALS_ID?.trim()) {
            withCredentials([usernamePassword(credentialsId: params.DOCKER_CREDENTIALS_ID, usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
              sh """
                echo "$REG_PASS" | docker login $DOCKER_REGISTRY_PATH --username "$REG_USER" --password-stdin
              """.stripIndent()
              sh buildPush
            }
          } else {
            sh buildPush
          }
        }
      }
    }



    stage('Helm Lint') {
      steps {
        sh '''
          helm dependency update $CHART_DIR
          helm lint $CHART_DIR
        '''
      }
    }

    stage('Deploy to Test') {
      steps {
        withCredentials([kubeconfigFile(credentialsId: params.KUBE_CONFIG_TEST, variable: 'KUBECONFIG')]) {
          sh '''
            kubectl create namespace $TEST_NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
            helm upgrade --install ${SERVICE_NAME}-test $CHART_DIR \
              --namespace $TEST_NAMESPACE \
              -f helm/environments/test.yaml \
              --atomic --cleanup-on-fail \
              --set image.repository=$IMAGE_REPO \
              --set image.tag=$IMAGE_TAG
            helm test ${SERVICE_NAME}-test --namespace $TEST_NAMESPACE
          '''
        }
      }
    }

    stage('Prod Approval') {
      when { expression { params.DEPLOY_TO_PROD } }
      steps {
        timeout(time: 2, unit: 'HOURS') {
          input message: 'Deploy to production?', ok: 'Deploy'
        }
      }
    }

    stage('Deploy to Prod') {
      when { expression { params.DEPLOY_TO_PROD } }
      steps {
        withCredentials([kubeconfigFile(credentialsId: params.KUBE_CONFIG_PROD, variable: 'KUBECONFIG')]) {
          sh '''
            kubectl create namespace $PROD_NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
            helm upgrade --install ${SERVICE_NAME}-prod $CHART_DIR \
              --namespace $PROD_NAMESPACE \
              -f helm/environments/prod.yaml \
              --atomic --cleanup-on-fail \
              --set image.repository=$IMAGE_REPO \
              --set image.tag=$IMAGE_TAG
            helm test ${SERVICE_NAME}-prod --namespace $PROD_NAMESPACE
          '''
        }
      }
    }
  }

  post {
    success {
      echo "Pipeline for ${env.SERVICE_NAME} completed successfully."
    }
    failure {
      echo "Pipeline for ${env.SERVICE_NAME} failed."
    }
    always {
      sh 'docker logout $DOCKER_REGISTRY_PATH || true'
    }
  }
}

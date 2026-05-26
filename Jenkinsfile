pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'truongdocker1'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
        IMAGE_NAME = 'bookstore-shipping-service'
        TAG = "${BUILD_NUMBER}"

        K8S_DEPLOYMENT = 'shipping-service-deployment'
        K8S_CONTAINER = 'shipping-service'
    }

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 21'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build(
                        "${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}",
                        "."
                    )
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry(
                        'https://index.docker.io/v1/',
                        "${DOCKER_CREDENTIALS_ID}"
                    ) {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'db-creds', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD'),
                    usernamePassword(credentialsId: 'rabbitmq-cred', usernameVariable: 'RABBITMQ_USER', passwordVariable: 'RABBITMQ_PASS'),
                    string(credentialsId: 'ghn-api-token', variable: 'GHN_API_TOKEN')
                ]) {
                    sh '''
                set -eu
                export KUBECONFIG=/var/jenkins_home/.kube/config

                echo "==> Update image tag in k8s/deployment.yaml"
                sed -i "s|image: .*${IMAGE_NAME}:.*|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}|g" k8s/deployment.yaml

                echo "==> Apply ConfigMap"
                kubectl apply -f k8s/configmap.yaml

                echo "==> Create / update shipping-service secret"
                kubectl create secret generic shipping-service-secret \
                  --from-literal=SPRING_DATASOURCE_USERNAME="$DB_USERNAME" \
                  --from-literal=SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
                  --from-literal=SPRING_RABBITMQ_USERNAME="$RABBITMQ_USER" \
                  --from-literal=SPRING_RABBITMQ_PASSWORD="$RABBITMQ_PASS" \
                  --from-literal=GHN_API_TOKEN="$GHN_API_TOKEN" \
                  --dry-run=client -o yaml | kubectl apply -f -

                echo "==> Apply Deployment + Service"
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml

                echo "==> Force rollout to pick up new image / env"
                kubectl rollout restart deployment/${K8S_DEPLOYMENT}

                echo "==> Wait for rollout"
                kubectl rollout status deployment/${K8S_DEPLOYMENT} --timeout=600s
                '''
                }
            }
        }
    }

    post {
        success {
            echo "Build & Deploy SUCCESS"
        }
        failure {
            echo "Build FAILED - dumping last pod logs for debug"
            sh '''
            export KUBECONFIG=/var/jenkins_home/.kube/config
            kubectl get pods -l app=shipping-service -o wide || true
            POD=$(kubectl get pods -l app=shipping-service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
            if [ -n "$POD" ]; then
              echo "----- describe $POD -----"
              kubectl describe pod "$POD" || true
              echo "----- logs $POD -----"
              kubectl logs "$POD" --tail=200 || true
            fi
            '''
        }
    }
}

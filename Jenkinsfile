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
                    usernamePassword(credentialsId: 'rabbitmq-creds', usernameVariable: 'SPRING_RABBITMQ_USERNAME', passwordVariable: 'SPRING_RABBITMQ_PASSWORD'),
                    string(credentialsId: 'ghn-api-token', variable: 'GHN_API_TOKEN')
                ]) {
                    sh '''
                export KUBECONFIG=/var/jenkins_home/.kube/config

                # Update image tag robustly, even if the workspace still has an older build tag.
                sed -i "s|image: .*${IMAGE_NAME}:.*|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}|g" k8s/deployment.yaml

                # ConfigMap is safe to keep in Git.
                kubectl apply -f k8s/configmap.yaml

                # App secret from Jenkins Credentials. Do not apply k8s/secret.yaml with real values.
                kubectl create secret generic shipping-service-secret \
                  --from-literal=SPRING_DATASOURCE_USERNAME="$DB_USERNAME" \
                  --from-literal=SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
                  --from-literal=SPRING_RABBITMQ_USERNAME="$SPRING_RABBITMQ_USERNAME" \
                  --from-literal=SPRING_RABBITMQ_PASSWORD="$SPRING_RABBITMQ_PASSWORD" \
                  --from-literal=GHN_API_TOKEN="$GHN_API_TOKEN" \
                  --dry-run=client -o yaml | kubectl apply -f -

                # Deploy app.
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml

                kubectl rollout status deployment/${K8S_DEPLOYMENT} --timeout=180s
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
            echo "Build FAILED"
        }
    }
}

pipeline {
    agent none

    triggers {
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Tests backend') {
            agent {
                docker {
                    image 'eclipse-temurin:21-jdk'
                    reuseNode true
                }
            }
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw && ./mvnw test'
                }
            }
        }

        stage('Terraform validate') {
            agent {
                docker {
                    image 'hashicorp/terraform:1.15'
                    reuseNode true
                    args '--entrypoint='
                }
            }
            steps {
                dir('infra/terraform') {
                    sh '''
                        terraform init -backend=false -input=false
                        terraform validate
                        terraform fmt -check -recursive
                    '''
                }
            }
        }

        stage('CD kind') {
            when {
                branch 'main'
            }
            agent any
            steps {
                sh '''
                    set -e

                    if ! command -v docker >/dev/null 2>&1; then
                        echo "docker absent dans Jenkins — stage CD ignoré"
                        exit 0
                    fi
                    if ! command -v kind >/dev/null 2>&1; then
                        echo "kind absent dans Jenkins — stage CD ignoré (voir README)"
                        exit 0
                    fi
                    if ! command -v terraform >/dev/null 2>&1; then
                        echo "terraform absent dans Jenkins — stage CD ignoré (voir README)"
                        exit 0
                    fi
                    if ! command -v kubectl >/dev/null 2>&1; then
                        echo "kubectl absent dans Jenkins — stage CD ignoré (voir README)"
                        exit 0
                    fi
                    if ! kind get clusters 2>/dev/null | grep -qx ovalytics; then
                        echo "Cluster kind ovalytics absent — stage CD ignoré"
                        exit 0
                    fi

                    echo "CD : build image + load kind + terraform apply"
                    docker build -t ovalytics-backend:local ./backend
                    kind load docker-image ovalytics-backend:local --name ovalytics

                    mkdir -p "$HOME/.kube"
                    kind get kubeconfig --name ovalytics > "$HOME/.kube/config"
                    sed -i 's/127.0.0.1/host.docker.internal/g' "$HOME/.kube/config"
                    export KUBECONFIG="$HOME/.kube/config"

                    cd infra/terraform
                    terraform init -input=false
                    terraform apply -auto-approve -input=false
                '''
            }
        }
    }
}

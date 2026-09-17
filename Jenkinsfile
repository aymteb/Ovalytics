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

                    mkdir -p "$HOME/bin"
                    export PATH="$HOME/bin:$PATH"
                    ARCH="$(uname -m)"
                    case "$ARCH" in
                      aarch64|arm64) ARCH=arm64 ;;
                      x86_64|amd64) ARCH=amd64 ;;
                    esac

                    curl -fsSL -o "$HOME/bin/kind" "https://kind.sigs.k8s.io/dl/v0.32.0/kind-linux-${ARCH}"
                    chmod +x "$HOME/bin/kind"
                    if ! command -v kubectl >/dev/null 2>&1; then
                        KVER="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
                        curl -fsSL -o "$HOME/bin/kubectl" "https://dl.k8s.io/release/${KVER}/bin/linux/${ARCH}/kubectl"
                        chmod +x "$HOME/bin/kubectl"
                    fi
                    if ! command -v terraform >/dev/null 2>&1; then
                        TF=1.11.4
                        curl -fsSL -o /tmp/terraform.zip "https://releases.hashicorp.com/terraform/${TF}/terraform_${TF}_linux_${ARCH}.zip"
                        unzip -qo /tmp/terraform.zip -d "$HOME/bin"
                        rm -f /tmp/terraform.zip
                    fi

                    if ! kind get clusters 2>/dev/null | grep -qx ovalytics; then
                        kind create cluster --config infra/kind-config.yaml
                    fi

                    echo "CD : build image + load kind + terraform apply"
                    docker build -t ovalytics-backend:local ./backend
                    kind load docker-image ovalytics-backend:local --name ovalytics

                    mkdir -p "$HOME/.kube"
                    kind get kubeconfig --name ovalytics > "$HOME/.kube/config"
                    sed -i 's/127.0.0.1/host.docker.internal/g' "$HOME/.kube/config"
                    export KUBECONFIG="$HOME/.kube/config"
                    kubectl config set-cluster kind-ovalytics --insecure-skip-tls-verify=true

                    cd infra/terraform
                    terraform init -input=false
                    terraform apply -auto-approve -input=false
                '''
            }
        }
    }
}

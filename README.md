# 🏉 Ovalytics

Application full-stack pour analyser les matchs de rugby. L'objectif n'est pas seulement d'afficher des scores, mais de proposer une lecture de match (analyse, absences, classement). On commence avec le Top 14.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular_21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)

## 🏗️ Architecture rapide

- `backend/` : API REST + job Spring Batch (import CSV)
- `frontend/` : site Angular (matchs, résultats, classement, fiche match)
- `docker-compose.yml` : Postgres + API en conteneurs
- `infra/` : cluster local **kind** + déploiement **Terraform** (Postgres + API)
- `Jenkinsfile` : CI/CD (tests + Terraform validate ; CD kind sur `main`)
- Branches : travail sur **`develop`**, vitrine / prod locale via PR vers **`main`**

---

## 🚀 Lancer avec Docker (API + base)

Prérequis : Docker Desktop (ou Docker Engine + Compose).

Si Postgres tourne déjà en local (`brew services`), arrête-le pour éviter les conflits, ou laisse Docker gérer la base **sans** exposer le port 5432 (cas actuel).

```bash
docker compose up --build
```

- API : http://localhost:8080/api/health
- Exemple : http://localhost:8080/api/competitions/TOP14/matches?status=SCHEDULED

Arrêt :

```bash
docker compose down
```

---

## ☸️ Kubernetes + Terraform (kind)

Déploie la même chose que Docker Compose (Postgres + API), mais sur un cluster Kubernetes local. Terraform crée le namespace, le secret, Postgres, le backend et les Services.

### Prérequis

- Docker Desktop ouvert
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Terraform](https://developer.hashicorp.com/terraform/install) (>= 1.5)

Sur macOS (une commande après l’autre) :

```bash
brew install kind
brew install kubectl
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

(`terraform` n’est plus dans le core Homebrew : il faut le tap HashiCorp.)

### 1. Créer le cluster kind

```bash
kind create cluster --config infra/kind-config.yaml
```

Le fichier mappe le port **8080** de ta machine vers le Service backend (NodePort `30080`).

### 2. Construire et charger l'image backend

```bash
docker build -t ovalytics-backend:local ./backend
kind load docker-image ovalytics-backend:local --name ovalytics
```

### 3. Appliquer Terraform

```bash
cd infra/terraform
terraform init
terraform apply
```

Confirme avec `yes`.

### 4. Vérifier

```bash
kubectl get pods -n ovalytics
```

Quand les pods sont `Running` / `Ready` :

- API : http://localhost:8080/api/health

Si le port 8080 est déjà pris (Compose ou Spring local), arrête-les d'abord, ou utilise un port-forward :

```bash
kubectl port-forward -n ovalytics svc/backend 8080:8080
```

### 5. Arrêt

```bash
cd infra/terraform
terraform destroy
kind delete cluster --name ovalytics
```

---

## 💻 Lancer en local (sans Docker)

### Base

```bash
brew services start postgresql@14
```

Base `ovalytics`, utilisateur local (voir `backend/src/main/resources/application.yaml`).

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

- Site : http://localhost:4200
- Le proxy Angular relaie `/api` vers `http://localhost:8080`.

---

## 🧪 Tests backend

```bash
cd backend
./mvnw test
```

---

## 📥 Import Batch (matchs J3)

Avec l'API démarrée :

```bash
curl -X POST http://localhost:8080/api/jobs/match-import
```

Fichier source : `backend/src/main/resources/data/top14-j3.csv`.

---

## Fin de session (local)

```bash
brew services stop postgresql@14
```

Si un cluster kind tourne encore :

```bash
kind delete cluster --name ovalytics
```

---

## ⚙️ CI / CD Jenkins (`develop` → PR → `main`)

### Workflow Git (habitude)

```text
Tu travailles sur develop          →  git push origin develop
Quand un palier est prêt           →  Pull Request develop → main sur GitHub
Jenkins build la PR                →  tests + terraform validate
Check GitHub vert                  →  Merge autorisé vers main
Sur main (après merge)             →  + stage CD kind (si cluster up)
```

Ne pousse plus directement sur `main` une fois la protection activée.

### Stages du `Jenkinsfile`

1. **Tests backend** — `./mvnw test` (Java 21)
2. **Terraform validate** — `init` + `validate` + `fmt -check`
3. **CD kind** — uniquement sur la branche **`main`** : build image → `kind load` → `terraform apply` (ignoré si cluster / outils absents)

### Prérequis Jenkins

- Plugins : **Pipeline**, **Docker Pipeline**, **GitHub Branch Source** (et dépendances proposées)
- Docker Desktop ouvert + client `docker` **dans** le conteneur Jenkins

### Jenkins en local (option Docker)

```bash
docker run -d --name jenkins \
  -p 8081:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

Mot de passe initial :

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Branche `develop`

Une fois (depuis un `main` à jour) :

```bash
git checkout main
git pull
git checkout -b develop
git push -u origin develop
```

Travail quotidien : rester sur `develop`.

### Job Multibranch (remplace l’ancien Pipeline simple)

1. (Optionnel) Supprime l’ancien job `Ovalytics` s’il n’était que “Pipeline from SCM” sur `main`
2. **New Item** → nom `Ovalytics` → **Multibranch Pipeline** → OK
3. **Branch Sources** → **Add source** → **GitHub**
4. Credentials : PAT GitHub (voir ci-dessous)
5. Repository : `aymteb/Ovalytics` (ou l’URL HTTPS du repo)
6. Behaviors : découvrir les branches `main` et `develop` ; découvrir les **Pull requests** (origin)
7. Build Configuration : mode **by Jenkinsfile**, chemin `Jenkinsfile`
8. Scan Multibranch Pipeline Triggers : périodique (ex. toutes les **2 minutes**) si pas de webhook
9. **Save** → **Scan Multibranch Pipeline Now**

### Token GitHub (PAT) pour Jenkins

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens**
2. Crée un token (classic) avec au minimum : `repo`, `read:org` (si besoin), et la capacité de **commit status** (`repo:status` est inclus dans `repo`)
3. Jenkins → **Manage Jenkins** → **Credentials** → Add → **Secret text** ou **Username with password** (user GitHub + PAT en mot de passe)
4. Réutilise ce credential dans la Branch Source GitHub

Sans ça, Jenkins build peut marcher en clone public, mais le **status check sur la PR** risque de ne pas remonter.

### Protection de `main` (checks obligatoires)

1. GitHub → repo → **Settings** → **Branches** → **Add branch protection rule**
2. Branch name pattern : `main`
3. Coche **Require a pull request before merging**
4. Coche **Require status checks to pass before merging**
5. Après le **premier** build Jenkins sur une PR, le nom du check apparaît : sélectionne-le (souvent lié à Jenkins / continuous-integration)
6. Save

Tant que le check n’a jamais tourné, GitHub ne peut pas encore le proposer : ouvre d’abord une PR test `develop` → `main`.

### Outils pour le stage CD (une fois dans le conteneur)

```bash
docker exec -u root jenkins bash -c "apt-get update && apt-get install -y docker.io curl unzip"
```

```bash
docker exec -u root jenkins bash -c '
  curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  chmod +x /usr/local/bin/kubectl
  curl -fsSL -o /usr/local/bin/kind https://kind.sigs.k8s.io/dl/v0.27.0/kind-linux-amd64
  chmod +x /usr/local/bin/kind
  curl -fsSL -o /tmp/terraform.zip https://releases.hashicorp.com/terraform/1.15.8/terraform_1.15.8_linux_amd64.zip
  unzip -o /tmp/terraform.zip -d /usr/local/bin
  chmod +x /usr/local/bin/terraform
'
```

Sur Mac Apple Silicon, adapte les URLs en `arm64` si besoin.

### Comportement du CD

| Situation | Résultat |
|-----------|----------|
| Build sur `develop` ou PR | Pas de CD (stages tests + validate seulement) |
| Build sur `main` + cluster kind absent | CD ignoré, pipeline vert |
| Build sur `main` + cluster up + outils OK | Deploy kind |

### Webhook GitHub (optionnel)

Le scan périodique du Multibranch suffit en local. Un webhook demande que GitHub joigne ton Jenkins (tunnel type ngrok sur 8081).

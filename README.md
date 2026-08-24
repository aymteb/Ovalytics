# 🏉 Ovalytics

Application full-stack pour analyser les matchs de rugby. L'objectif n'est pas seulement d'afficher des scores, mais de proposer une lecture de match (analyse, absences, classement). On commence avec le Top 14.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular_21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)

## 🏗️ Architecture rapide

- `backend/` : API REST + job Spring Batch (import CSV)
- `frontend/` : site Angular (matchs, résultats, classement, fiche match)
- `docker-compose.yml` : Postgres + API en conteneurs

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

---

## ⚙️ CI Jenkins

Le fichier `Jenkinsfile` à la racine lance `./mvnw test` dans `backend/`.

### Prérequis Jenkins

- Plugins : **Pipeline**, **Docker Pipeline**
- Jenkins doit pouvoir parler à Docker (Docker Desktop ouvert, ou socket monté si Jenkins tourne en conteneur)

### Créer le job

1. Jenkins → **New Item** → **Pipeline** → nom `Ovalytics`
2. **Pipeline** → Definition : **Pipeline script from SCM**
3. SCM : **Git**, URL du repo GitHub, branche `main`
4. Script Path : `Jenkinsfile`
5. **Save** → **Build Now**

Build vert = les tests backend passent (comme en local avec `./mvnw test`).

### Jenkins en local (option Docker)

Exemple minimal (Jenkins sur http://localhost:8081) :

```bash
docker run -d --name jenkins \
  -p 8081:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

Récupère le mot de passe initial :

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Puis configure les plugins et le pipeline comme ci-dessus.

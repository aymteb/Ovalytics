# Ovalytics

**Hub rugby** pour les fans qui ne veulent rien louper de leurs clubs : calendrier, scores, classements, fiche match avec forme, absences et une vraie lecture d’avant-match.

Pas un site de paris. Pas de cotes. Une vitrine fan, dans l’esprit Flashscore + une analyse qui se mouille.

Les compétitions visées : **Top 14, Pro D2, Nationale**, Premiership, **URC**, **Super Rugby**… On pose le socle avec le **Top 14** (données de démo), puis on élargit — notamment via import / scrap.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular_21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## Pourquoi ce projet

Je suis développeur junior en fin de scolarité, en recherche d’emploi. Ovalytics me sert de **projet GitHub concret** pour montrer ce que je sais construire de bout en bout.

Après **16 mois chez Digital Edge Studio** (expérience en équipe produit / delivery), je voulais un repo à moi où l’on voit clairement :

- du **Java** et **Spring Boot** (API, persistance, Batch)
- un **front Angular** (parcours fan : accueil, matchs, résultats, classement, fiche)
- une base **PostgreSQL** et un déploiement local réaliste (**Docker**, et un peu de CI/CD)

Chaque techno a un rôle dans le produit — pas une case cochée pour le CV.

---

## Ce que fait l’app aujourd’hui

- Accueil + navigation vers les matchs à venir, résultats et classement
- Fiche match : score ou affiche, analyse, absents, forme (saison en cours + filet N-1), domicile / extérieur, confrontations
- Classement avec règles de bonus **par compétition** (défensif + offensif, ex. différence d’essais vs essais marqués)
- Import CSV via **Spring Batch** (création / mise à jour des matchs, scores, essais) — base pour brancher un scrap ensuite

---

## Stack

| Brique | Rôle |
|--------|------|
| **Java 21 + Spring Boot** | API REST, domaine rugby, calculs classement / forme |
| **Spring Batch** | Import calendrier & résultats (CSV → base) |
| **Hibernate / JPA + PostgreSQL** | Persistance compétitions, clubs, matchs |
| **Angular** | Interface fan |
| **Docker Compose** | Lancer API + Postgres rapidement |
| **Jenkins / kind / Terraform** | CI et déploiement local (présents dans le repo, hors focus README) |

---

## Structure du repo

```text
backend/     API Spring + job d'import
frontend/    Site Angular
infra/       kind + Terraform (optionnel)
```

Branches : travail sur **`develop`**, intégration vers **`main`** par pull request.

---

## Lancer le projet

### Option rapide — Docker (API + base)

```bash
docker compose up --build
```

- Santé API : http://localhost:8080/api/health  
- Exemple : http://localhost:8080/api/competitions/TOP14/matches?status=SCHEDULED

```bash
docker compose down
```

### Front (à part)

```bash
cd frontend
npm install
npm start
```

Site : http://localhost:4200 (le proxy renvoie `/api` vers le backend).

### Local sans Docker

```bash
brew services start postgresql@14
cd backend && ./mvnw spring-boot:run
```

Config base : `backend/src/main/resources/application.yaml`.  
Arrêt Postgres en fin de session : `brew services stop postgresql@14`.

### Tests

```bash
cd backend && ./mvnw test
```

### Import Batch (optionnel)

```bash
curl -X POST http://localhost:8080/api/jobs/match-import
```

Fichier : `backend/src/main/resources/data/top14-import.csv` (scores + essais). Le job crée ou met à jour sans doublon.

---

## Suite

Prochaine brique déterminante : **alimenter les données par scrap / import riche**, pour sortir du seed démo et couvrir plus de compétitions, de journées et de saisons — ce qui nourrira forme, classements et fiches match.

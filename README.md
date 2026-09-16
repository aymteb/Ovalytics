# Ovalytics

**Hub rugby** pour les fans qui ne veulent rien louper de leurs clubs : calendrier, scores, classements, actu, mercato, fiche match avec forme, absences et une vraie lecture d’avant-match.

Pas un site de paris. Pas de cotes. Une vitrine fan, dans l’esprit Flashscore + une analyse qui se mouille.

Compétitions en cours : **Top 14** et **Pro D2** (saison 2026-2027), alimentées par scrap + Spring Batch. Nationale, Premiership, URC, Super Rugby : visés plus tard.

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

- **Accueil** : fil d’actualités rugby (RSS Rugbyrama)
- **Matchs à venir** : hub multi-ligues (Top 14 + Pro D2), vues par date ou par championnat
- **Résultats** et **classement** avec bonus par compétition
- **Fiche match** : analyse, absents, forme (saison + filet N-1), domicile / extérieur, confrontations
- **Transferts** : journal mercato + vue club (source **All Rugby**)
- **Effectifs** : vue mercato alimentée par scrap All Rugby (poste, âge, taille, fin de contrat)
- **Scores en direct** : poll Flashscore (~60 s), sections « En direct » sur accueil / résultats / classement
- **Refresh joueurs post-match** : match `FINISHED` → file d’attente → scrape fiches All Rugby → import stats saison (cron 23 h)
- **Spring Batch** : import matchs, actus, transferts, effectifs, fiches joueur depuis CSV

---

## Stack

| Brique | Rôle |
|--------|------|
| **Java 21 + Spring Boot** | API REST, domaine rugby, calculs classement / forme |
| **Spring Batch** | Imports CSV (matchs, news, transferts, effectifs, fiches joueur) |
| **Hibernate / JPA + PostgreSQL** | Persistance compétitions, clubs, joueurs, matchs |
| **Angular** | Interface fan |
| **Docker Compose** | API + Postgres + volume `data/import` |
| **Python** | Scrapers LNR, All Rugby, Rugbyrama |
| **Jenkins / kind / Terraform** | CI et déploiement local (présents dans le repo) |

---

## Structure du repo

```text
backend/     API Spring + jobs Batch
frontend/    Site Angular
scripts/     Scrapers (matchs LNR, All Rugby, actu)
data/import/ CSV produits par les scrapers
infra/       kind + Terraform (optionnel)
```

Branches : travail sur **`develop`**, intégration vers **`main`** par pull request.

---

## Lancer le projet

### Docker (API + base)

```bash
docker compose up --build
```

- Santé : http://localhost:8080/api/health
- Matchs Top 14 : http://localhost:8080/api/competitions/TOP14/matches?status=SCHEDULED

### Front

```bash
cd frontend
npm install
npm start
```

Site : http://localhost:4200 (proxy `/api` → backend).

### Local sans Docker

```bash
brew services start postgresql@14
cd backend && ./mvnw spring-boot:run
```

Config : `backend/src/main/resources/application.yaml`.  
Arrêt Postgres : `brew services stop postgresql@14`.

### Tests

```bash
cd backend && ./mvnw test
```

---

## Données : scrap → CSV → Batch

Les scrapers écrivent dans `data/import/`. Avec Docker, ce dossier est monté sur `/import` dans le backend.

### 1. Produire les CSV

```bash
python3 scripts/scrape_top14.py --no-demo-map
python3 scripts/scrape_top14.py --competition PROD2 --no-demo-map
python3 scripts/scrape_news.py
python3 scripts/scrape_transfers.py
python3 scripts/scrape_squads.py
python3 scripts/scrape_player_profiles.py --teams BRI,VAL
python3 scripts/scrape_absences.py
python3 scripts/scrape_logos.py
```

| Script | Source | Fichier |
|--------|--------|---------|
| `scrape_top14.py` | LNR | `top14-matches.csv` |
| `scrape_top14.py --competition PROD2` | LNR Pro D2 | `prod2-matches.csv` |
| `scrape_news.py` | Rugbyrama RSS | `news.csv` |
| `scrape_transfers.py` | All Rugby mercato | `transfers.csv` |
| `scrape_squads.py` | All Rugby effectifs | `squads.csv` |
| `scrape_player_profiles.py` | All Rugby fiches joueur | `player-profiles.csv` |
| `scrape_absences.py` | All Rugby Top 14 + Rugbyrama Pro D2 | `absences.csv` |
| `scrape_logos.py` | Flashscore CDN | `frontend/public/clubs/*.png` |

### 2. Lancer les imports (backend up)

```bash
curl -X POST http://localhost:8080/api/jobs/match-import
curl -X POST http://localhost:8080/api/jobs/match-import/prod2
curl -X POST http://localhost:8080/api/jobs/news-import
curl -X POST http://localhost:8080/api/jobs/transfer-import
curl -X POST http://localhost:8080/api/jobs/squad-import
curl -X POST http://localhost:8080/api/jobs/player-profile-import
curl -X POST http://localhost:8080/api/jobs/absence-import
```

Les jobs **upsert** (pas de doublon sur les clés métier). Re-lancer un import met à jour les lignes existantes. L’import absences **remplace** le snapshot Top 14 + Pro D2 (AllRugby + article infirmeries Rugbyrama). Un même joueur listé deux fois (ex. suspendu + blessé) est **dédupliqué** : la suspension gagne.

### Refresh joueurs après un match

Quand un match passe `FINISHED` (live Flashscore ou import CSV), les deux clubs sont ajoutés à une **file d’attente**. À **23 h** (ou manuellement) :

```bash
curl http://localhost:8080/api/jobs/pending-teams
curl -X POST http://localhost:8080/api/jobs/team-refresh
```

En local (Python disponible) : scrape des clubs en file → import `player-profiles.csv` → file vidée.

Scrape ciblé sans attendre le cron :

```bash
python3 scripts/scrape_player_profiles.py --teams BRI,VAL
curl -X POST http://localhost:8080/api/jobs/player-profile-import
```

Ordre recommandé : **effectifs** (`squads.csv`) puis **fiches** (`player-profiles.csv`). L’import fiches ne met à jour que les joueurs déjà en base.

### Variables Docker (déjà dans `docker-compose.yml`)

| Variable | Fichier |
|----------|---------|
| `OVALYTICS_IMPORT_FILE` | `top14-matches.csv` |
| `OVALYTICS_IMPORT_PROD2_FILE` | `prod2-matches.csv` |
| `OVALYTICS_IMPORT_NEWS_FILE` | `news.csv` |
| `OVALYTICS_IMPORT_TRANSFER_FILE` | `transfers.csv` |
| `OVALYTICS_IMPORT_SQUAD_FILE` | `squads.csv` |
| `OVALYTICS_IMPORT_PLAYER_PROFILE_FILE` | `player-profiles.csv` |
| `OVALYTICS_TEAM_REFRESH_SCRAPE_ENABLED` | `false` (pas de Python dans l’image ; scraper sur l’hôte puis `player-profile-import`) |

---

## Suite

Prochain cap produit : **forme et fiches match** sur les calendriers importés (moins de seed démo), puis enrichissement des analyses avant-match.

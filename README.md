# Ovalytics

**Hub rugby** pour les fans qui ne veulent rien louper de leurs clubs : calendrier, scores, classements, actu, mercato, fiche match avec forme, absences et une vraie lecture d’avant-match.

Pas un site de paris. Pas de cotes. Une vitrine fan, dans l’esprit Flashscore + une analyse qui se mouille.

Compétitions en cours : **Top 14** et **Pro D2** (saison 2026-2027), alimentées par scrap + Spring Batch. Nationale, Premiership, URC, Super Rugby : visés plus tard.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular_21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-ready-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)

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
| **Docker Compose** | API + Postgres (+ CSV embarqués dans l’image backend) |
| **Python** | Scrapers LNR, All Rugby, Rugbyrama |
| **Jenkins / kind / Terraform** | CI et déploiement local (présents dans le repo) |
| **Railway** | Démo publique prévue (Postgres + backend + front) |

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

Les scrapers écrivent dans `data/import/`. En Docker / Railway, ces CSV sont **copiés dans l’image backend** (`/import`). Pour prendre en compte un nouveau scrap : rebuild l’image, ou ré-importer via les jobs une fois les fichiers mis à jour dans l’image / le volume.

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

| Variable | Rôle |
|----------|------|
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | Postgres (même schéma que Railway) |
| `SPRING_PROFILES_ACTIVE=prod` | déjà dans l’image backend |

---

## Déploiement Railway (simple)

**Principe :** le local est la source de vérité. Quand les données locales changent, on **miroir** la base vers Railway. Pas besoin de clé OpenAI en prod pour ça.

### Services

1. **PostgreSQL** (plugin) — à **lier** au service backend (Railway injecte alors `PGHOST`, `PGUSER`, etc.)
2. **backend** — Root Directory = `backend`, Dockerfile = `Dockerfile`
3. **frontend** — Root Directory = `frontend`, Dockerfile = `Dockerfile`

Les CSV ne sont plus dans l’image backend (`/import` vide au boot). Les données viennent du miroir Postgres local → Railway.

### Variables à poser (le minimum)

**Backend** — lier le plugin Postgres au service (Railway injecte `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`).  
La config prod construit l’URL JDBC avec `sslmode=require`.  
**Ne pas** poser `SPRING_DATASOURCE_URL` / `DATABASE_URL` à la main (ça écrase et casse souvent le démarrage).  
Si tu en as déjà : les **supprimer**, Apply, redéployer.

**Frontend** — une seule :

| Variable | Exemple |
|----------|---------|
| `BACKEND_UPSTREAM` | `http://<nom-du-service-backend>.railway.internal:8080` |

`SPRING_PROFILES_ACTIVE=prod` est déjà dans le Dockerfile.  
Le front reverse-proxy `/api` → pas besoin de CORS à configurer.

### Miroir local → Railway

```bash
# 1. Export de ta Postgres locale
./scripts/mirror-db-to-railway.sh

# 2. Restore sur Railway (URL Postgres du dashboard)
export DATABASE_URL='postgresql://user:pass@host:port/railway'
./scripts/mirror-db-to-railway.sh --restore-railway
```

Les dumps sont écrits dans `data/backups/` (ignoré par git).

### Après le premier déploiement

- Coller l’URL publique du site en haut de ce README.
- Quand tu mets à jour le local (scrap, analyses) : relancer le miroir.

---

## Suite

Mettre Ovalytics en ligne sur Railway, puis reprendre le produit (compos, etc.).

# MS6 – UniLearn AI

Microservice FastAPI de recommandations et de personnalisation pour UniLearn. Les profils d’apprentissage sont isolés par sujet Keycloak ; les recommandations utilisent les cours publiés de MS1 et expliquent les intérêts correspondants.

## Prérequis

- Python 3.12
- Docker Desktop et Docker Compose (pour PostgreSQL et le Keycloak de développement)

## Installation locale (PowerShell)

Depuis ce dossier :

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
$env:TEMP = Join-Path $env:LOCALAPPDATA "Temp"
$env:TMP = $env:TEMP
python -m pip install --upgrade pip
python -m pip install -e ".[dev]"
```

Configure les variables MS6 dans l’environnement local. `.env.example` fournit des placeholders ; remplace le mot de passe par une valeur locale avant de lancer PostgreSQL. Ne commite jamais `.env`.

| Variable | Défaut |
|---|---|
| `MS6_APP_NAME` | `UniLearn AI` |
| `MS6_HOST` | `127.0.0.1` |
| `MS6_PORT` | `8086` |
| `MS6_DB_HOST` | `localhost` |
| `MS6_DB_PORT` | `5433` (port hôte local) |
| `MS6_DB_NAME` | `unilearn_ms6` |
| `MS6_DB_USER` | `unilearn_ms6` |
| `MS6_DB_PASSWORD` | Requis ; aucun défaut dans le code |
| `MS6_POSTGRES_PORT` | `5433` (port hôte publié par Compose) |

Le conteneur PostgreSQL conserve son port interne standard `5432`; Compose publie par défaut le port hôte `5433` pour éviter la collision avec d’autres instances PostgreSQL. La base et le volume MS6 sont distincts de MySQL/MS1.

Depuis la racine du dépôt, crée le fichier local et remplace les mots de passe placeholders :

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Après avoir modifié `.env`, démarre le Keycloak local, PostgreSQL et l’API :

```powershell
docker compose up --build -d keycloak postgres-ms6 ms6-ai
docker compose ps keycloak postgres-ms6 ms6-ai
```

Pour démarrer l’API dans Compose, l’image applique les migrations puis lance le serveur sur le port 8086. Le realm `unilearn` est importé au premier démarrage Keycloak. Il contient uniquement deux étudiants synthétiques (`demo-student-1` et `demo-student-2`) pour vérifier l’isolation des profils. Aucun mot de passe étudiant n’est enregistré dans le dépôt ; définissez des mots de passe locaux pour ces utilisateurs dans la console d’administration Keycloak avant de demander un jeton. Le compte administrateur initial est configuré par `KEYCLOAK_ADMIN_USERNAME` et `KEYCLOAK_ADMIN_PASSWORD` dans `.env`.

`MS6_KEYCLOAK_ISSUER_URI` doit correspondre à l’émetteur public des jetons (`http://localhost:8180/realms/unilearn` par défaut). Dans Compose, MS6 récupère les clés publiques via `http://keycloak:8080/...`; la variable `MS6_KEYCLOAK_JWK_SET_URI` permet de remplacer cette adresse si nécessaire. Le mode `start-dev` et les comptes ci-dessus sont réservés au développement local.

Pour démarrer l’API directement sur l’hôte, depuis ce dossier :

```powershell
$env:MS6_HOST = "127.0.0.1"
$env:MS6_PORT = "8086"
$env:MS6_DB_HOST = "localhost"
$env:MS6_DB_PORT = "5433"
# Le fichier .env à la racine est chargé automatiquement par les paramètres MS6.
python -m alembic upgrade head
python -m app.main
```

Vérifie l’état du service sur <http://127.0.0.1:8086/health>. La réponse attendue est `{"status":"ok"}`.

## Health checks

`GET /health` vérifie que le processus API répond et retourne `{"status":"ok"}`. `GET /health/ready` vérifie aussi PostgreSQL avec un `SELECT 1` ; si la base est inaccessible ou si `MS6_DB_PASSWORD` manque, la route retourne HTTP 503 sans exposer les détails de connexion.

## Profils et recommandations

Hors mode démo, toutes les routes personnalisées exigent un bearer JWT Keycloak valide avec le rôle `STUDENT` ou `ADMIN`. MS6 vérifie la signature RS256 et l’émetteur ; les paramètres `MS6_KEYCLOAK_ISSUER_URI` et `MS6_KEYCLOAK_JWK_SET_URI` configurent cette validation.

- `GET /api/v1/profile/me` consulte le profil de l’utilisateur authentifié.
- `PUT /api/v1/profile/me` crée ou met à jour ses intérêts et objectifs.
- `DELETE /api/v1/profile/me` supprime son profil enregistré.
- `GET /api/v1/recommendations/courses?limit=10` récupère les cours `PUBLISHED` et les inscriptions de MS1 en transmettant le même bearer, exclut les cours déjà suivis, puis renvoie les cours correspondants triés par score et avec une explication.
- `POST /api/v1/assistant/chat` reçoit `course_id`, `question` et un historique facultatif. L’API récupère les leçons autorisées de MS1 et répond avec le LLM OpenAI-compatible configuré, en validant les références citées. Sans endpoint LLM ou en cas d’échec, elle revient aux extraits cités (`/ask` reste un alias).
- `POST /api/v1/analysis/results` calcule une moyenne, une médiane et un écart entre le résultat le plus ancien et le plus récent à partir des notes soumises dans la requête.
- `POST /api/v1/analysis/failure-risk` calcule un indicateur heuristique à partir des notes, de la progression et des évaluations manquées.
- `GET /api/v1/learning-paths/preview?limit=10` ordonne les cours recommandés par pertinence aux intérêts et objectifs.

Le classement et le parcours sont transparents ; l’ordre ne tient pas compte de prérequis, car MS1 ne fournit pas ces données. Configure `MS6_LLM_BASE_URL` et `MS6_LLM_MODEL` pour activer la génération; `MS6_LLM_API_KEY` est optionnelle selon le fournisseur. Sans LLM, l’assistant renvoie des extraits cités. L’analyse et le risque utilisent des notes soumises dans la requête, non vérifiées auprès de MS2 ni persistées. Le risque est un indicateur explicable, non une probabilité calibrée ni une décision académique; règle les seuils `MS6_RISK_*` avant usage institutionnel. MS6 ne lit jamais la base de MS1. Configure `MS6_MS1_COURSES_URL` si MS1 n’est pas à l’adresse locale par défaut.

## Démonstration Swagger locale

Pour une présentation sans Keycloak, MS1 ni PostgreSQL, active le mode démo avec des données entièrement synthétiques. Le mode démo utilise SQLite en mémoire et est désactivé par défaut. Les routes de profil/recommandations/chat restent protégées par JWT hors mode démo.

```powershell
$env:MS6_DEMO_MODE = "true"
$env:MS6_DEMO_DATABASE_URL = "sqlite+pysqlite:///:memory:"
$env:MS6_HOST = "127.0.0.1"
$env:MS6_PORT = "8086"
python -m app.main
```

Ouvre <http://127.0.0.1:8086/docs>. Le profil synthétique `demo-student-001`, les cours et les leçons sont chargés automatiquement au démarrage.

## Migrations

Les connexions utilisent le driver `postgresql+psycopg`. Alembic lit la même configuration MS6 que SQLAlchemy :

```powershell
python -m alembic upgrade head
```

La migration initiale crée `learning_profiles`. Les nouvelles tables doivent être ajoutées par migrations versionnées.

## Tests

```powershell
python -m pytest
```

Les tests unitaires utilisent SQLite en mémoire et ne nécessitent pas de Docker. La connexion réelle à MS2 pour vérifier les résultats et l’enregistrement Eureka restent à intégrer.

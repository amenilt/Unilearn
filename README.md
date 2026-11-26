# UniLearn

Plateforme d’apprentissage universitaire en **architecture microservices** (Spring Boot), avec une application web et une application desktop Java.

Module AWD · Mini-projet · Validation 1 · Année universitaire 2026-2027

## 1. Sujet

La digitalisation de l’éducation pousse les universités à utiliser des plateformes en ligne, mais les outils sont dispersés et le suivi des étudiants reste difficile.

**UniLearn** centralise dans un seul espace :

- les cours organisés en chapitres et leçons ;
- les quiz (QCM) pour l’auto-évaluation ;
- les réclamations et leur suivi ;
- un forum Q&A pour les échanges entre étudiants ;
- des duels éducatifs et des badges pour motiver.

**Objectif :** un apprentissage plus clair, mieux structuré et plus motivant, avec un suivi pédagogique facilité.

## 2. Acteurs et fonctionnalités

| Acteur | Fonctionnalités |
|---|---|
| Étudiant | Consulter les cours, passer des quiz, déposer et suivre une réclamation, utiliser le forum, participer aux duels, gagner des badges |
| Enseignant | Publier des cours, créer des quiz, traiter les réclamations, modérer le forum |
| Administrateur | Gérer les utilisateurs et les rôles, gérer les badges, superviser la plateforme |

## 3. Équipe

| Membre | Microservice responsable |
|--|---|
| Ayoub Jleli | MS1 Cours |
| Ameni Ltaifi | Eureka + MS2 Quiz |
| Mohamed Rhouma | MS3 Réclamations |
| Akrem Jouini | MS4 Forum Q&A |
| Toute l’équipe | MS5 Duels et badges (microservice commun), Frontend |

Gestion des tâches : [lien Trello ou GitHub Projects]

### Eureka + MS2 Quiz — Ameni Ltaifi

Le serveur Eureka à deux pairs et le microservice Quiz sont dans `Backend/eureka-server/` et `Backend/quiz-service/`. Guide, commandes, contrat REST et Actuator : [README MS2](Backend/quiz-service/README.md).

Capture du dashboard Eureka (à ajouter après le lancement des deux pairs et des deux instances Quiz) : `Backend/quiz-service/docs/img/eureka-dashboard.png`.

## 4. Architecture

![Architecture globale](Documentation/Diagrams/architecture.png)

| Composant | Rôle | Port |
|---|---|---|
| Spring Cloud Gateway | Point d’entrée unique | 8080 |
| Keycloak | Authentification et rôles | 8180 |
| Eureka Server | Registre des services | 8761 |
| Config Server | Configuration centralisée | 8888 |
| RabbitMQ | Communication asynchrone | 5672 |

### Microservices

| Microservice | Responsabilité | Technologie | Base | Port |
|---|---|---|---|---|
| MS1 Cours | Cours, chapitres, leçons | Spring Boot | MongoDB | 8081 |
| MS2 Quiz | Quiz, questions, tentatives, scores | Spring Boot | PostgreSQL | 8082 |
| MS3 Réclamations | Dépôt et suivi des réclamations | Spring Boot | MySQL | 8083 |
| MS4 Forum Q&A | Questions, réponses, votes | Spring Boot | MongoDB | 8084 |
| MS5 Duels et badges | Duels, badges, classement | Python (FastAPI) | PostgreSQL | 8085 |

Chaque microservice a **sa propre base de données** : aucune base n’est partagée.

### Communication

- **Synchrone (OpenFeign)** : MS2 appelle MS1 pour vérifier l’existence d’un cours ; MS5 appelle MS2 pour récupérer les questions d’un duel.
- **Asynchrone (RabbitMQ)** : `quiz.completed` (MS2 vers MS5), `reclamation.status.changed` (MS3), `forum.answer.accepted` (MS4 vers MS5).
- **Panne d’un service appelé** : circuit breaker (Resilience4j) avec réponse de secours ; les messages RabbitMQ restent en file jusqu’au retour du service.

## 5. Contrats d’API (principaux endpoints)

Tous les appels passent par la Gateway (`http://localhost:8080`).

| Service | Endpoint | Codes |
|---|---|---|
| MS1 | `POST /api/courses` · `GET /api/courses/{id}` · `POST /api/courses/{id}/chapters` | 201, 200, 404 |
| MS2 | `POST /api/quizzes` · `GET /api/quizzes/{id}` · `POST /api/quizzes/{id}/attempts` | 201, 200, 404 |
| MS3 | `POST /api/claims` · `GET /api/claims?status=` · `PATCH /api/claims/{id}/status` | 201, 200, 409 |
| MS4 | `POST /api/questions` · `GET /api/questions?tag=` · `POST /api/questions/{id}/answers` | 201, 200, 404 |
| MS5 | `POST /api/duels` · `GET /api/badges/{userId}` · `GET /api/leaderboard` | 201, 200, 404 |

## 6. Structure du dépôt

```
Unilearn/
├── Backend/
│   ├── config-server/
│   ├── eureka-server/
│   ├── api-gateway/
│   ├── cours-service/             (MS1)
│   ├── quiz-service/              (MS2)
│   ├── reclamation-service/       (MS3)
│   ├── forum-service/             (MS4)
│   └── duels-badges-service/      (MS5)
├── Frontend/                      (application web + desktop Java)
├── Documentation/
│   ├── Presentation/              (support de présentation)
│   └── Diagrams/                  (architecture, cas d’utilisation, classes, séquence)
└── README.md
```

## 7. Technologies

- **Backend :** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Spring Data MongoDB, Spring Cloud (Gateway, Eureka, Config, OpenFeign), Resilience4j, Lombok
- **MS5 :** Python 3.11, FastAPI
- **Sécurité :** Keycloak (OAuth2 / JWT)
- **Messages :** RabbitMQ
- **Bases :** MongoDB, PostgreSQL, MySQL
- **Frontend :** application web, application desktop JavaFX
- **Déploiement :** Docker, Docker Compose
- **Outils :** Git, GitHub, Maven, IntelliJ IDEA

## 8. Prérequis et lancement

**Prérequis :** JDK 17, Maven 3.9+, Docker Desktop, Git (et Python 3.11 pour MS5).

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/amenilt/Unilearn.git
   cd Unilearn
   ```
2. Démarrer les bases, RabbitMQ et Keycloak :
   ```bash
   docker compose up -d
   ```
3. Lancer dans cet ordre : `config-server`, `eureka-server`, `api-gateway`, puis chaque microservice :
   ```bash
   cd Backend/cours-service
   mvn spring-boot:run
   ```
4. Vérifier l’enregistrement des services sur http://localhost:8761.

## 9. Organisation du travail avec Git

- Branche principale : `main` (ne pas y travailler directement).
- Une branche par fonctionnalité : `feature/<microservice>-<description>`, par exemple `feature/quiz-create-endpoint`.
- Des commits courts et clairs, faits depuis son **propre compte**.
- Une pull request relue par un autre membre avant la fusion.

## 10. Planning prévisionnel

| Séance | Objectif |
|---|---|
| 4 | Validation 1 : conception, dépôt, projets créés |
| 5 | Config Server, Eureka, Gateway, Keycloak |
| 6 à 8 | Logique métier de MS1 à MS4 |
| 9 | MS5, RabbitMQ, frontend |
| 10 | Intégration, tests, Docker Compose |

## 11. État d’avancement

- [x] Conception et découpage en microservices
- [x] Structure du dépôt
- [ ] Projets Spring Boot créés dans `Backend/`
- [ ] Config Server, Eureka, Gateway
- [ ] Logique métier des microservices
- [ ] Frontend
- [ ] Intégration et Docker Compose

## 12. Documentation

- Document technique : `Documentation/`
- Présentation : `Documentation/Presentation/`
- Diagrammes : `Documentation/Diagrams/`
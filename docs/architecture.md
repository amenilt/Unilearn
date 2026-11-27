# UniLearn — architecture et plan de réalisation

## 1. Objet et état initial

UniLearn est une plateforme d'apprentissage universitaire organisée autour de six capacités métier. Le PowerPoint `UniLearn_Validation_de_la_conception_v3.pptx` est la référence fonctionnelle principale pour MS1 à MS5. Les exigences MS6 proviennent du brief utilisateur.

Le dossier de travail était vide lors de l'analyse. Il n'y avait ni code applicatif, ni manifeste, ni documentation de projet, ni dépôt Git local. Le dépôt Git détecté au-dessus du dossier UniLearn n'est pas le dépôt du projet et ne doit pas être utilisé pour ses opérations Git.

Ce document est une proposition d'architecture. Les ports et choix explicitement marqués « proposés » sont des conventions de développement local, pas des exigences extraites du PowerPoint.

### État d'implémentation au 26 novembre 2026

- Socle réalisé : Eureka Server, Config Server et Spring Cloud Gateway.
- MS1 – Cours réalisé et testé avec les migrations Flyway et les tests API H2.
- MS2 à MS6, Keycloak, RabbitMQ et frontend Angular ne sont pas encore implémentés.
- Le PowerPoint de conception n'est pas présent dans le workspace courant. Le contrat MS1 synthétisé lors de l'analyse précédente reste la référence disponible ; les détails de contenu ci-dessous qui ne sont pas dans ce contrat sont explicitement provisoires.

## 2. Exigences confirmées

- Java 17, Spring Boot 3.5.x, Maven et Spring Cloud compatible.
- Versions de départ validées pour le socle : Spring Boot 3.5.15 et Spring Cloud 2025.0.3 (train compatible avec Boot 3.5.x).
- Spring Cloud Config Server, Netflix Eureka, Spring Cloud Gateway et clients Eureka.
- Spring Security, Keycloak OAuth2/OIDC et validation JWT.
- Angular, TypeScript et Bootstrap 5.
- Python 3, FastAPI et scikit-learn pour MS6.
- MySQL 8 et PostgreSQL dans la stack demandée.
- Docker, Docker Compose, Git/GitHub, JUnit 5, Mockito et OpenAPI.
- Six services indépendants, chacun propriétaire de ses données ; aucun partage de base de données.
- Rôles généraux : `STUDENT`, `TEACHER`, `ADMIN`.
- Le PowerPoint décrit OpenFeign pour les appels synchrones, RabbitMQ pour les événements et WebSocket/STOMP pour les duels temps réel.

## 3. Architecture logique proposée

```text
Navigateur (Angular)
        |
        | OIDC / JWT
        +----------------------> Keycloak
        |
        v
Spring Cloud Gateway :8080
        |
        +---- Eureka :8761 <---- Config Server :8888
        |
        +---- MS1 Cours :8081
        +---- MS2 Quiz :8082
        +---- MS3 Réclamations :8083
        +---- MS4 Forum Q&A :8084
        +---- MS5 Duels & Badges :8085 ---- WebSocket/STOMP
        +---- MS6 UniLearn AI :8086 (FastAPI)

MS1–MS6 : appels REST ciblés via découverte de services
MS2/MS3/MS4/MS5 : événements métier durables via RabbitMQ
Chaque service conserve et migre son propre stockage.
```

La Gateway est le point d'entrée HTTP du frontend. Chaque backend valide également le JWT : la sécurité ne repose pas uniquement sur la Gateway. Les services n'enregistrent pas de comptes ou de mots de passe locaux ; ils conservent le `sub` Keycloak comme identifiant externe.

Les services ne doivent pas appeler directement les tables d'un autre service. Les appels synchrones servent uniquement aux décisions qui nécessitent une réponse immédiate. Les événements asynchrones doivent être versionnés, traités de manière idempotente et associés à des mécanismes de retry et dead-letter.

## 4. Responsabilités et données par service

### MS1 — Cours

- Capacités : catégories et matières, cours, chapitres, leçons/contenus et inscriptions.
- Modèle implémenté : `Category`, `Subject`, `Course`, `Chapter`, `Lesson`, `Enrollment`.
- Le PowerPoint définit les relations `Course 1—* Chapter 1—* Lesson` et les champs principaux du cours.
- API implémentée :
  - CRUD des catégories sur `/api/categories` et des matières sur `/api/subjects`.
  - CRUD des cours sur `/api/courses`; filtres de liste par catégorie, matière et statut.
  - Inscriptions sur `/api/courses/{id}/enrollments` et `/api/enrollments/me`.
  - Consultation des chapitres et leçons via `GET /api/courses/{id}/chapters`, création via `POST /api/courses/{id}/chapters` et `POST /api/chapters/{id}/lessons`.
- Statuts de cours implémentés : `DRAFT`, `PUBLISHED`, `ARCHIVED`. Un nouveau cours est en brouillon ; seuls les cours publiés sont visibles aux étudiants et peuvent recevoir des inscriptions.
- Autorisations implémentées : un enseignant gère uniquement ses cours ; `ADMIN` peut gérer tous les cours ; les étudiants peuvent consulter les cours publiés et gérer leurs propres inscriptions.
- Modèle provisoire de contenu, faute de schéma détaillé disponible dans le workspace : chapitres ordonnés par cours, leçons textuelles ordonnées par chapitre, contenu limité à 10 000 caractères. Les fichiers vidéo, documents joints et types de contenu ne sont pas couverts.
- Stockage implémenté : MySQL 8, migrations Flyway versionnées et Hibernate réglé sur `validate`. Le profil de test exécute les mêmes migrations sur H2 en mode MySQL ; le test Testcontainers cible MySQL 8.
- Base assignée dans le PowerPoint : MySQL.

### MS2 — Quiz

- Capacités : création de quiz, questions/options, tentatives, correction automatique et résultats.
- Modèle candidat : `Quiz`, `Question`, `Option`, `Attempt`; chaque tentative est associée à un étudiant via son identifiant Keycloak.
- API de référence : `POST /api/quizzes`, `GET /api/quizzes/{id}`, `POST /api/quizzes/{id}/submit`, `GET /api/quizzes/{id}/results`.
- Appels : vérification du cours dans MS1 ; MS5 peut demander les questions nécessaires à un duel.
- Événement du PowerPoint : `quiz.completed` vers MS5.
- Base assignée dans le PowerPoint : PostgreSQL.

### MS3 — Réclamations

- Capacités : création, suivi, statuts et commentaires/réponses.
- Modèle candidat : `Claim`, `ClaimComment`; états proposés dans le PowerPoint : `OPEN`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`.
- API de référence : `POST /api/claims`, `GET /api/claims?status=`, `PATCH /api/claims/{id}/status`, `POST /api/claims/{id}/comments`.
- La création ne dépend pas de la disponibilité de MS1 ; la vérification d'un cours peut être différée.
- Événement du PowerPoint : `claim.status.changed` vers MS5.
- Base confirmée : H2, comme prévu dans le PowerPoint. Pour éviter la perte de données au redémarrage des conteneurs, le profil Docker devra utiliser un fichier H2 sur un volume exclusivement monté par MS3 ; le mode mémoire reste adapté aux tests.

### MS4 — Forum Q&A

- Capacités : questions, réponses, commentaires/interactions et recherche par cours.
- Modèle du PowerPoint : `Question` et réponses imbriquées, avec votes et réponse acceptée. Le modèle précis des commentaires reste à définir.
- API de référence : `POST /api/questions`, `GET /api/questions?course=`, `POST /api/questions/{id}/answers`, `POST /api/answers/{id}/vote`, `PATCH /api/answers/{id}/accept`.
- Appel : vérification de cours via MS1.
- Événement du PowerPoint : `answer.posted` vers MS5.
- Base confirmée : MongoDB, comme prévu dans le PowerPoint. Les réponses imbriquées seront stockées avec leur question, conformément au modèle documentaire décrit.

### MS5 — Duels & Badges

- Capacités : défis, acceptation, manches, scores, points, classements, badges et notifications.
- Modèle candidat : `Duel`, `DuelRound`, `Badge`, `UserBadge`, `Points`, `Notification`.
- API de référence : `POST /api/duels`, `POST /api/duels/{id}/accept`, `POST /api/duels/{id}/answer`, `GET /api/badges/{userId}`, `GET /api/leaderboard`, `GET /api/notifications/{userId}`.
- Appel : questions de duel fournies par MS2.
- Consomme `quiz.completed`, `answer.posted`, `claim.status.changed`; publie `duel.finished`.
- Temps réel : WebSocket/STOMP.
- Base assignée dans le PowerPoint : PostgreSQL.

### MS6 — UniLearn AI

- Capacités demandées : recommandations de cours, analyse des résultats, assistant pédagogique et parcours personnalisés.
- Stack : Python 3, FastAPI, scikit-learn, PostgreSQL, client Eureka à sélectionner et valider.
- Modèle candidat : `LearningProfile`, `Interaction`, `PerformanceSnapshot`, `Recommendation`, `LearningPath`, `RiskIndicator`, `ContentReference`.
- Les données nécessaires (catalogue, inscriptions, résultats, progression) seront obtenues par contrats d'API autorisés ou événements explicitement définis, jamais en lisant les bases des autres services.
- Aucune source de cours, donnée d'entraînement, métrique ML, politique de conservation ou règle de seuil n'a été fournie. En conséquence, aucune précision de prédiction ne peut être promise. L'assistant devra citer les contenus effectivement consultés et s'abstenir si aucune source fiable n'est disponible.
- Les indicateurs de risque sont explicables et destinés à orienter un accompagnement humain, pas à prendre automatiquement des décisions académiques.

## 5. Données et isolation

Les identifiants d'entités sont locaux à leur service ; les références interservices utilisent des identifiants opaques, sans clé étrangère vers une autre base. Les tables/collections persistantes incluent au minimum les champs d'audit de création et de mise à jour. Les résultats, réclamations, messages de forum et données IA doivent être protégés selon leur propriétaire et leur rôle.

Cartographie retenue, confirmée par l'utilisateur et conforme aux stockages du PowerPoint :

| Service | Base retenue | Isolation |
|---|---|---|
| MS1 | MySQL 8 | Base et identifiants réservés à MS1 |
| MS2 | PostgreSQL | Base et identifiants réservés à MS2 |
| MS3 | H2 | Fichier et volume réservés à MS3 en environnement Docker |
| MS4 | MongoDB | Base et utilisateur réservés à MS4 |
| MS5 | PostgreSQL | Base et identifiants réservés à MS5 |
| MS6 | PostgreSQL | Base et identifiants réservés à MS6 |

Cette répartition est une décision confirmée : ne pas remplacer H2 ou MongoDB par une autre technologie sans justification technique et confirmation préalable. Une instance PostgreSQL peut héberger les bases distinctes de MS2, MS5 et MS6, avec comptes à privilèges séparés et aucun accès croisé ; si l'isolation d'instance devient nécessaire, chaque base pourra être déplacée sans changer le propriétaire métier des données. Aucun service ne reçoit les identifiants d'une autre base.

## 6. Ports proposés pour le développement local

Les ports du PowerPoint n'étant pas spécifiés, les valeurs suivantes sont des propositions sans conflit connu :

| Composant | Port local proposé |
|---|---:|
| Angular (`ng serve`) | 4200 |
| Gateway | 8080 |
| Eureka | 8761 |
| Config Server | 8888 |
| MS1 Cours | 8081 |
| MS2 Quiz | 8082 |
| MS3 Réclamations | 8083 |
| MS4 Forum Q&A | 8084 |
| MS5 Duels & Badges | 8085 |
| MS6 UniLearn AI | 8086 |
| Keycloak | 8180 |
| MySQL | 3306 |
| PostgreSQL | 5432 |
| RabbitMQ AMQP / management | 5672 / 15672 |
| MongoDB pour MS4 | 27017 |
| H2 MS3 | Intégré au processus MS3 ; stockage fichier sur volume, console TCP désactivée par défaut |

MS1 dispose d'une route Gateway explicite `lb://MS1-COURSES` pour les chemins API,
dont `/api/chapters/**`, ainsi que d'une route pour Swagger/OpenAPI. La route
requiert l'enregistrement de MS1 dans Eureka. Le port 8080 était occupé lors des
vérifications précédentes ; une surcharge du port Gateway peut être nécessaire.

Les consoles DB et RabbitMQ ne seront exposées qu'en développement. Les secrets ne seront pas placés dans Git. Les ports des services pourront rester internes au réseau Compose en déploiement.

## 7. Organisation du monorepo proposée

```text
UniLearn/
├── backend/
│   ├── pom.xml                         # parent Maven : propriétés et BOMs
│   ├── platform/
│   │   ├── config-server/
│   │   ├── eureka-server/
│   │   └── gateway/
│   └── services/
│       ├── ms1-courses/
│       ├── ms2-quizzes/
│       ├── ms3-claims/
│       ├── ms4-forum/
│       └── ms5-duels-badges/
├── ai/
│   └── ms6-unilearn-ai/
│       ├── app/
│       ├── tests/
│       ├── pyproject.toml
│       └── Dockerfile
├── frontend/
│   └── unilearn-web/
├── infra/
│   ├── config/                         # configuration externalisée non secrète
│   ├── keycloak/
│   └── docker/
├── docs/
│   ├── architecture.md
│   └── api/                            # contrats OpenAPI et événements
├── docker-compose.yml
├── .env.example
└── .gitignore
```

Le parent Maven centralise les versions, mais chaque service Spring Boot reste démarrable et déployable séparément. Les contrats REST, payloads d'événements et conventions de sécurité seront documentés indépendamment de leur implémentation.

## 8. Sécurité et communications

- Keycloak fournit l'authentification OIDC et les JWT ; mappage explicite des rôles realm/client vers les autorités Spring Security.
- Règles d'accès minimales : étudiants sur leurs propres tentatives/réclamations/notifications ; enseignants sur les cours qu'ils gèrent et les opérations pédagogiques autorisées ; administration réservée à `ADMIN`.
- Chaque service vérifie le jeton et l'autorisation métier. Les opérations sensibles contrôlent également la propriété de la ressource, pas seulement le rôle.
- OpenFeign pour les opérations synchrones listées dans le PowerPoint, avec délais d'attente et circuit breakers/fallbacks explicites. Une panne n'est jamais transformée en réussite ou en ressource fictive.
- RabbitMQ pour les événements documentés. Files durables, reprises bornées, dead-letter et consommateurs idempotents.
- Le rattachement de MS6 aux contrats MS1/MS2 et aux événements demande une définition complémentaire avant son intégration.

## 9. Feuille de route et critères de vérification

1. **Figer les décisions d'architecture** : stockage MS3/MS4, contrats d'inscription et de commentaires, conventions d'identité/rôles et périmètre IA.
2. **Initialiser le dépôt local UniLearn** : Git dans ce dossier uniquement, structure du monorepo, Maven parent, configuration d'exemple et règles de secrets.
3. **Socle d'infrastructure** : Config Server, Eureka, Gateway, Keycloak, RabbitMQ et bases retenues ; health checks et démarrage Compose.
4. **MS1 — terminé côté code et tests H2** : modèle cours, catégories, matières, contenus, inscriptions, migrations, API/OpenAPI et tests. La validation MySQL 8 via Testcontainers et la vérification d'enregistrement/routage en exécution restent à faire lorsque Docker/MySQL et le socle seront disponibles.
5. **MS2 puis MS3/MS4** : modèle/API/tests ; intégrer les appels MS1 et tester les erreurs/droits.
6. **MS5** : états de duel, règles de score/badges, WebSocket, événements et reprise.
7. **MS6** : ingestion conforme aux contrats, stockage PostgreSQL, API d'analyse/recommandation, baselines mesurables, tests et limites affichées.
8. **Frontend Angular** : OIDC, navigation par rôle, écrans métier et flux WebSocket.
9. **Intégration finale** : tests unitaires JUnit/Mockito et pytest, tests d'intégration, contrats, démarrage Docker Compose et parcours de démonstration documenté dans `README.md`.

Critères de sortie : services enregistrés et routés ; accès non autorisés rejetés à la Gateway et au service ; bases isolées ; migrations reproductibles ; événements rejouables sans doublon d'effet ; API documentées ; tests et démarrage Compose réussis.

## 10. Informations encore manquantes

- Formats détaillés des catégories/matières, des inscriptions et des règles d'accès/capacité aux cours.
- Schéma exact des chapitres/leçons, types de contenu, pièces jointes, règles de publication et éventuelles limites de taille ; le modèle de contenu actuellement codé est provisoire.
- Barème, limites de tentatives, durée et politique de consultation des réponses correctes des quiz.
- Qui peut commenter une réclamation, répondre au forum, voter et accepter une réponse ; définition des recherches.
- Règles de duel (égalité, abandon, expiration, sélection des adversaires), attribution des points/badges et modération.
- Contrats et événements permettant à MS6 d'obtenir le catalogue et les résultats de manière autorisée.
- Corpus pour l'assistant, fournisseur éventuel de modèle génératif, données d'entraînement et protocole d'évaluation des risques/recommandations.
- Politique de conservation/suppression des données personnelles et environnement de déploiement cible.

Ces éléments ne bloquent pas le document d'architecture, mais ceux qui changent la persistance ou les frontières de service devront être décidés avant le code concerné.

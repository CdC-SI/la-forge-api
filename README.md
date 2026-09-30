# La Forge

**La Forge** est le backend d'une plateforme collaborative d'entraînement technique. Elle aide des développeurs à progresser par la pratique, la révision structurée, la veille technologique et les échanges collectifs autour de sujets tels que Java ou Angular.

Le produit ne cherche ni à exécuter du code à distance, ni à classer des collaborateurs, ni à devenir un outil RH. Il fournit un cadre d'apprentissage pour une entreprise ou un groupe pilote : chaque personne choisit un exercice, réalise une tentative, analyse son résultat, organise ses révisions et partage son expérience dans des défis collectifs.

## Objectifs pédagogiques

- Transformer des connaissances techniques en compétences observables par la pratique.
- Encourager une boucle courte : exercice, soumission, débrief, auto-évaluation et révision.
- Rendre la veille actionnable grâce à des fiches sourcées, reliées aux thèmes et aux exercices.
- Développer les compétences de collaboration : lecture de code, argumentation technique, retours constructifs et rédaction de contenus.
- Faire évoluer progressivement la plateforme vers une gestion d'agents d'apprentissage, capables d'accompagner sans se substituer à l'expertise humaine.

## Fonctionnalités couvertes

| Domaine | Capacités |
| --- | --- |
| Identité et sécurité | Inscription locale, activation par courriel, sessions JWT RS256, renouvellement rotatif et révocation. |
| Profil | Préférences de thèmes, tableau de bord personnel, progression et historique privé. |
| Catalogue | Thèmes, exercices publiés et versionnés, objectifs pédagogiques, indices et contenu sans correction exposée. |
| Pratique | Création de tentatives, soumission, abandon, consultation d'indices, débrief et auto-évaluation. |
| Révision | File de révisions et échéances mises à jour de façon atomique après une soumission. |
| Veille technologique | Articles sourcés, consultation et gestion éditoriale des fiches de veille. |
| Collectif | Défis, participation par code, comparaison des réponses et commentaires de débrief. |
| Édition | Brouillons d'exercices, relecture éditoriale et cycle `DRAFT → IN_REVIEW → APPROVED → PUBLISHED`. |
| Tuteur | Échanges d'assistance IA optionnels après une soumission ; l'IA ne remplace jamais une correction validée. |

Le contrat ne prévoit pas d'exécution de code distante, de génération automatique de contenu, de classement RH ou de gestion multi-entreprises.

## Vers des agents d'apprentissage spécialisés

La Forge prépare une évolution vers une orchestration d'agents spécialisés, contrôlés et traçables. L'objectif est d'offrir une aide contextualisée à la progression, tout en préservant le rôle du développeur et des relecteurs humains.

Les premières compétences visées sont :

- **Agent de code review** : analyser une soumission ou une proposition de changement, relever les risques, la maintenabilité, les tests manquants et la conformité aux conventions, puis formuler un retour pédagogique et priorisé.
- **Agent architecte solution** : guider la décomposition d'un problème, expliciter les compromis, les dépendances, les frontières de domaine et les décisions d'architecture, sans livrer une solution opaque toute faite.
- **Agent de veille** : rapprocher les contenus sourcés des thèmes suivis, des lacunes identifiées et des exercices pertinents.
- **Agent tuteur** : poser des questions, donner des indices progressifs et soutenir l'auto-évaluation après la soumission.

Cette évolution doit respecter les principes du produit : données minimales et autorisées par agent, réponses explicables, traçabilité des échanges, validation humaine pour les corrections et aucune écriture automatique sur un contenu éditorial validé.

## API et règles métier

Le contrat OpenAPI est la source de vérité :

[`src/main/resources/api/dev-practice.openapi.yaml`](src/main/resources/api/dev-practice.openapi.yaml)

L'API est servie sous le préfixe `/api/v1`. Elle utilise JSON UTF-8, des erreurs au format `application/problem+json`, une pagination par curseur opaque et un jeton JWT transmis dans l'en-tête `Authorization`.

Les règles notables sont les suivantes :

- L'utilisateur courant est toujours déduit du `sub` du JWT, jamais d'un identifiant envoyé dans le corps de requête.
- Les entrées avec des propriétés inconnues sont rejetées.
- Les opérations sensibles de création utilisent `Idempotency-Key`.
- Les mutations de brouillons éditoriaux exigent un `If-Match` fort.
- Les publications sont immuables ; tentatives et défis référencent explicitement une version d'exercice.
- Les contenus Markdown et le code sont non fiables et doivent être assainis avant rendu.

## Architecture

Le backend est organisé par domaines métier sous `ch.admin.zas.jweb.laforge` :

```text
<domaine>/
├── web/          contrôleurs REST
├── dto/          contrats d'entrée et de sortie
├── service/      logique métier et transactions
├── domain/       entités, enums et objets de valeur
├── repository/   accès aux données Spring Data JPA
└── mapper/       conversions entité <-> DTO
```

Les domaines principaux sont `security`, `profile`, `catalog`, `practice`, `review`, `discovery`, `collective`, `authoring` et `tutor`. Le code transverse est regroupé dans `common` : erreurs RFC 7807, pagination, idempotence, ETag, limitation de débit et persistance.

La dépendance applicative est stricte : `web → service → repository → domain`. Les entités JPA ne traversent jamais la frontière HTTP.

## Stack technique

| Composant | Technologies |
| --- | --- |
| Langage et runtime | Java 25 |
| Framework | Spring Boot 4.1.1 |
| API | REST, OpenAPI 3.0.3, JSON |
| Persistance | Spring Data JPA, PostgreSQL, Flyway |
| Sécurité | Spring Security, OAuth2 Resource Server, JWT RS256, BCrypt |
| Courriel | Spring Mail et FreeMarker |
| Tests | JUnit 5, Mockito, `@WebMvcTest`, `@DataJpaTest`, H2 en mode PostgreSQL |

Spring Boot 4 utilise Jackson 3. Les classes de sérialisation sont donc fournies sous les paquets `tools.jackson.*`.

## Démarrer le projet

### Prérequis

- Java 25
- Maven 3.9.12
- PostgreSQL

La configuration par défaut attend une base PostgreSQL locale nommée `laforge`, avec l'utilisateur et le mot de passe `laforge`. Les migrations Flyway sont appliquées au démarrage.

| Variable | Valeur par défaut |
| --- | --- |
| `LAFORGE_DB_HOST` | `localhost` |
| `LAFORGE_DB_PORT` | `5432` |
| `LAFORGE_DB_NAME` | `laforge` |
| `LAFORGE_DB_USER` | `laforge` |
| `LAFORGE_DB_PASSWORD` | `laforge` |
| `LAFORGE_MAIL_HOST` | `localhost` |
| `LAFORGE_MAIL_PORT` | `25` |
| `LAFORGE_MAIL_FROM` | `no-reply@la-forge.local` |

```powershell
mvn clean install
mvn spring-boot:run
```

Pour travailler avec les données de démonstration, activez le profil `dev` :

```powershell
mvn -Dspring-boot.run.profiles=dev spring-boot:run
```

Une fois les dépendances présentes dans le dépôt Maven local, le build peut également être exécuté hors ligne :

```powershell
mvn -o clean install
```

## Tests

```powershell
mvn test
```

Les tests de service utilisent JUnit 5 et Mockito sans contexte Spring. Les contrôleurs sont couverts avec `@WebMvcTest` et les repositories avec `@DataJpaTest` sur H2 en compatibilité PostgreSQL.

## Contribution

Toute évolution de comportement HTTP doit mettre à jour le contrat OpenAPI en même temps que le code. Les DTO sont des `record`, l'injection se fait par constructeur et les messages destinés aux utilisateurs sont rédigés en français.

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
| Identité et sécurité | Inscription locale, activation par courriel, sessions JWT RS256, renouvellement rotatif et révocation des jetons de renouvellement ; administration des rôles. |
| Profil | Préférences de thèmes, tableau de bord personnel, progression et historique privé. |
| Catalogue | Thèmes, exercices publiés et versionnés, indicateur personnel `completed` (« déjà fait »), objectifs pédagogiques, indices et contenu sans correction exposée. |
| Pratique | Création de tentatives, soumission, abandon, consultation d'indices, débrief et auto-évaluation. |
| Révision | File de révisions et échéances mises à jour de façon atomique après une soumission. |
| Veille technologique | Articles sourcés, consultation et gestion éditoriale des fiches de veille. |
| Collectif | Défis, participation par code, comparaison des réponses et commentaires de débrief. |
| Édition | Brouillons privés enregistrables avec un titre seul, puis publication directe `DRAFT → PUBLISHED`. |
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

### Créer, compléter et publier

Un compte `AUTHOR` ou `ADMIN` peut créer un brouillon avec `{"content":{"title":"Mon exercice"}}`. Seul son auteur peut le lister, le lire, le modifier ou le publier : aucun accès spécial pour un autre `ADMIN` (404). Une nouvelle version d'un exercice publié reste réservée à son auteur.

Le `PUT` remplace **tout** le contenu : les scalaires absents ou `null` restent non renseignés, les collections omises ou `null` deviennent vides ; aucune ancienne valeur n'est conservée implicitement. Le titre reste obligatoire. La publication directe exige un contenu complet et cohérent ; un refus renvoie 422 avec les chemins de champs dans `violations`, sans créer de version. `ETag`, `If-Match`, 412 et 428 restent applicables. Modifier une publication immuable exige un nouveau brouillon.

Dans les exercices et les articles, une technologie peut se limiter à `{"technology":"Java"}`. `minimumVersion`, `featureStatus` et `notes` sont facultatifs et acceptent `null`, sans valeur inventée. Une version renseignée doit être non blanche ; un statut renseigné conserve les valeurs `STABLE`, `PREVIEW` ou `INCUBATOR`. Les préférences de stack du profil ne changent pas.

### Déjà fait et refaire

`completed` est un booléen obligatoire dans `ExerciseSummary` et `Exercise`, y compris dans le tableau de bord, les défis, les révisions et le retour de publication. Il vaut `true` dès qu'une tentative **de l'utilisateur courant**, toutes versions de l'exercice confondues, est `SUBMITTED` : ni réussite ni auto-évaluation requise. `IN_PROGRESS` et `ABANDONED` seuls ne comptent pas ; une nouvelle tentative ne remet pas l'indicateur à `false`.

Refaire un exercice en individuel reste permis, avec les restrictions existantes sur les tentatives en cours, défis, révisions et clés d'idempotence. Le client peut afficher « Déjà fait » et « Refaire » ; aucun nouveau filtre ni tri n'est ajouté.

### Rôles et administration

Les rôles cumulatifs sont `LEARNER`, `AUTHOR` et `ADMIN`. Les fiches de veille sont gérées par `AUTHOR` ou `ADMIN`.

- `GET /admin/accounts?query=...&limit=...&cursor=...` : recherche administrative par nom ou courriel, sans distinction de casse, pagination opaque, ordre `displayName` croissant puis `id` croissant.
- `PUT /admin/accounts/{accountId}/roles` : remplace les rôles d'un autre compte `ACTIVE`, par exemple `{"roles":["LEARNER","AUTHOR"]}`. `LEARNER` est obligatoire, sans doublons. Auto-modification interdite (403), compte absent (404), compte inactif ou retrait du dernier `ADMIN` actif (409). JSON invalide : 400 ; attributions invalides : 422. L'opération est atomique et idempotente, sans ETag.

Ces routes sont réservées à `ADMIN` et ne renvoient que `id`, `email`, `displayName`, `status` et `roles`. Leurs rôles reflètent les **attributions en base** ; `/me.roles` et les autorisations reflètent les **permissions du JWT courant**. Connexion et renouvellement émettent les nouveaux droits. Un ancien jeton d'accès reste valable avec ses anciens droits jusqu'à expiration, même après renouvellement : pas de révocation immédiate.

#### Premier administrateur

Aucun compte privilégié n'est créé par défaut et le premier inscrit n'est pas promu automatiquement. Inscrire le compte choisi, puis confirmer son courriel via le parcours normal pour obtenir le statut `ACTIVE` et `LEARNER`. Un opérateur habilité doit ensuite, dans une transaction PostgreSQL contrôlée, vérifier précisément son identifiant et son statut, puis ajouter `ADMIN` dans `account_role` en conservant `LEARNER`. Ne pas activer un compte ni modifier un mot de passe ou un jeton directement pour contourner la vérification. Se reconnecter ou renouveler la session après provisionnement, puis utiliser l'API pour les autres comptes.

### Migration et client

Les anciennes étapes `IN_REVIEW` et `APPROVED` redeviennent `DRAFT`, **sans publication automatique** ; leurs ETag sont invalidés. Les publications existantes, contenus, auteurs, versions et tentatives sont conservés. Les anciennes relectures restent une archive inerte en base, non exposée. Les attributions `REVIEWER` sont converties en `AUTHOR` sans doublons ; un ancien claim `REVIEWER` est ignoré, sans conférer `AUTHOR` ni invalider les autres rôles reconnus du JWT.

Coordonner le déploiement avec le client : retrait sans alias des routes `/authoring/drafts/{draftId}/submit` et `/authoring/drafts/{draftId}/review`, des anciens états et de `Draft.reviews` ; ajout obligatoire de `completed`. Les révisions pédagogiques (`/me/reviews`) et les exercices de revue de code restent disponibles. Vérifier séparément les migrations et les changements de rôles concurrents sur PostgreSQL : les tests H2 avec Flyway désactivé ne suffisent pas.

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

Les tests PostgreSQL `SimplificationMigrationTest` (migrations) et `PostgresAccountRoleLockRepositoryTest` (changements de rôles concurrents) sont opt-in : définir `LAFORGE_MIGRATION_TEST_URL` (URL JDBC), `LAFORGE_MIGRATION_TEST_USER` et `LAFORGE_MIGRATION_TEST_PASSWORD` pour une base de test, puis exécuter `mvn -o test "-Dtest=SimplificationMigrationTest,PostgresAccountRoleLockRepositoryTest"`. Ils appliquent les migrations dans des schémas isolés créés puis supprimés ; le compte technique doit pouvoir créer et supprimer ces schémas. Sans URL, ces tests sont ignorés. Ne pas utiliser une base de production ni enregistrer les identifiants dans le dépôt.

## Contribution

Toute évolution de comportement HTTP doit mettre à jour le contrat OpenAPI en même temps que le code. Les DTO sont des `record`, l'injection se fait par constructeur et les messages destinés aux utilisateurs sont rédigés en français.

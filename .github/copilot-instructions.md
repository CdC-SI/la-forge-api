# Instructions pour GitHub Copilot — La Forge

## Projet

Backend de **La Forge**, plateforme collaborative d'entraînement technique (Java, Angular, etc.).
Le contrat d'API est la source de vérité : `src/main/resources/api/dev-practice.openapi.yaml`.
Toute évolution de comportement HTTP doit être répercutée dans ce fichier avant ou en même temps
que le code.

## Environnement et commandes

- Java **25**, Maven **3.9.12**, PostgreSQL en production, H2 (compatibilité PostgreSQL) en tests.
- Dépôt Maven local : `D:\maven\repository`. Le build doit fonctionner hors-ligne (`-o`) une fois
  les dépendances déjà résolues.
- **Ne pas utiliser le Maven Wrapper** (`mvnw`/`mvnw.cmd`). Utiliser directement la commande `mvn`.
- Commandes de référence :
  - `mvn clean install` — build complet avec tests.
  - `mvn clean install -DskipTests` — build rapide sans tests.
  - `mvn test` — tests uniquement.
  - `mvn test -Dtest=NomDeLaClasse` — une classe de test ciblée.
  - `mvn -o clean install` — build hors-ligne si le repo local est déjà peuplé.
- Le parent Maven `ch.admin.zas.jweb:zas-springboot-parent:4.1.1-2` fixe les versions des starters
  Spring Boot (4.1.1) : ne pas déclarer de version explicite sur les `spring-boot-starter-*`.
- **Spring Boot 4 embarque Jackson 3** : `ObjectMapper`, `JavaType`, `JacksonException`,
  `TypeReference` vivent désormais sous le paquet **`tools.jackson.*`** (groupId
  `tools.jackson.core`), pas `com.fasterxml.jackson.databind`/`jackson.core`. Seules les
  **annotations** (`@JsonProperty`, `@JsonInclude`, `@JsonTypeInfo`, `@JsonSubTypes`, …) restent
  sous `com.fasterxml.jackson.annotation` (module `jackson-annotations` inchangé). Ne pas ajouter
  de dépendance `com.fasterxml.jackson.core:jackson-databind` : elle n'est pas utilisée par la
  chaîne Spring Boot 4 et créerait une confusion de classpath.

## Architecture

Découpage **par domaine métier**, pas par couche technique globale. Chaque domaine sous
`ch.admin.zas.jweb.laforge.<domaine>` possède ses propres sous-packages :

```
<domaine>/
├─ web/          contrôleurs REST (@RestController)
├─ dto/          records d'entrée/sortie exposés par l'API
├─ service/      logique métier, transactions
├─ domain/       entités JPA, enums, value objects internes
├─ repository/   interfaces Spring Data JPA
└─ mapper/       conversion entité <-> DTO
```

Domaines : `security` (comptes, JWT), `profile`, `catalog`, `practice`, `review`, `discovery`,
`collective`, `authoring`, `tutor`, plus `common` pour le code transverse (erreurs, pagination,
idempotence, ETag, rate limiting, persistance de base).

**Règle de dépendance stricte** : `web → service → repository → domain`. Une entité JPA ne doit
**jamais** être retournée ou acceptée directement par un contrôleur ; toujours passer par un DTO
et un mapper.

## Style de code Java 25

- **Records** pour tous les DTO (entrée et sortie), avec constructeur compact pour les invariants
  simples. Pas de builder ni de setters sur les DTO.
- **`sealed interface`** pour les hiérarchies fermées du contrat (ex. `Answer` avec ses 4
  implémentations) ; utiliser le **pattern matching sur `switch`** de façon exhaustive, sans
  `default` superflu quand tous les cas sont couverts.
- Blocs de texte (`"""`) pour les chaînes SQL, gabarits ou messages multi-lignes.
- `var` pour les variables locales dont le type est évident au site d'appel.
- Pas d'API preview ni `--enable-preview` : rester sur les fonctionnalités standard de Java 25.
- Injection par constructeur uniquement (pas de `@Autowired` sur champ).
- Javadoc et messages utilisateur en **français** ; identifiants de code (classes, méthodes,
  variables) en **anglais**, conformément au contrat.

## Erreurs et contrat HTTP

- Toutes les erreurs utilisent `application/problem+json` conformément au schéma `Problem` du
  contrat (`type`, `title`, `status`, `code`, `detail`, `traceId`, `violations`).
- Respecter strictement les codes définis par opération dans le contrat (400/401/403/404/409/422/
  428/429/500/503).
- Jackson doit rejeter les propriétés inconnues en entrée (`FAIL_ON_UNKNOWN_PROPERTIES`).
- Pagination par curseur opaque, jamais par offset numérique exposé au client.
- `Idempotency-Key` obligatoire sur les POST qui créent des ressources sensibles (tentatives,
  défis, commentaires) ; `If-Match` obligatoire sur les mutations de brouillon éditorial.

## Sécurité

- Comptes créés localement (`/auth/registrations`), activés par jeton de vérification envoyé par
  courriel (`/auth/verifications`), puis authentification par JWT émis en interne
  (`/auth/sessions`). Ne jamais faire confiance à un identifiant utilisateur transmis dans le
  corps de requête : toujours dériver l'utilisateur courant du `sub` du JWT validé.
- Mots de passe hachés avec BCrypt. Jetons de vérification et de renouvellement stockés
  **hachés** (jamais en clair) en base.

## Tests

- Tests unitaires de service avec **JUnit 5 + Mockito** (pas de contexte Spring).
- Tests de contrôleur avec `@WebMvcTest` (sécurité et JSON, dépendances de service mockées).
- Tests de repository avec `@DataJpaTest` sur H2 en mode compatibilité PostgreSQL.
- Toujours exécuter `mvn clean install` avant de considérer une tâche terminée.

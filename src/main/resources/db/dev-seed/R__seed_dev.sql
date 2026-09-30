-- Jeu de données de démonstration pour le profil dev uniquement (voir application.yml :
-- laforge.flyway.locations additionnel activé sous le profil `dev`). Migration répétable :
-- rejouée à chaque changement de contenu, les insertions sont donc idempotentes (ON CONFLICT
-- DO NOTHING) et s'appuient sur des identifiants fixes plutôt que générés.

-- Thèmes -----------------------------------------------------------------------------------

INSERT INTO topic (id, slug, label, created_at, updated_at) VALUES
    ('11111111-0000-0000-0000-000000000001', 'java', 'Java', now(), now()),
    ('11111111-0000-0000-0000-000000000002', 'spring', 'Spring', now(), now()),
    ('11111111-0000-0000-0000-000000000003', 'angular', 'Angular', now(), now()),
    ('11111111-0000-0000-0000-000000000004', 'algorithmes', 'Algorithmes', now(), now())
ON CONFLICT (id) DO NOTHING;

-- Exercice CODE_REVIEW (thème Java) ---------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000001', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000001',
    '22222222-0000-0000-0000-000000000001',
    1,
    'Revue de code : fuite de ressource dans un traitement de fichier',
    'CODE_REVIEW',
    'INTERMEDIATE',
    15,
    'Relisez le patch ci-dessous qui ajoute la lecture d''un fichier de configuration. Formulez le '
    'verdict de revue attendu (approuver, demander des changements ou demander des informations) '
    'et justifiez-le.',
    '["Repérer une fuite de ressource (flux non fermé)", "Proposer try-with-resources"]'::jsonb,
    '[{"path":"ConfigLoader.java","language":"java","kind":"DIFF","content":"+ FileInputStream in = new FileInputStream(path);\n+ Properties props = new Properties();\n+ props.load(in);\n+ return props;"}]'::jsonb,
    '[{"technology":"Java","minimumVersion":"17","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"REVIEW","choices":[]}'::jsonb,
    '[{"level":1,"markdown":"Que se passe-t-il si `props.load(in)` lève une exception ?"},{"level":2,"markdown":"Le flux `in` est-il fermé dans tous les cas, y compris en cas d''erreur ?"}]'::jsonb,
    '{"explanationMarkdown":"Le flux n''est jamais fermé : une exception pendant `load` fuit la ressource. Il faut un try-with-resources.","acceptedAnswers":[],"correctChoiceIds":[],"expectedVerdicts":["REQUEST_CHANGES"],"criteria":[{"id":"resource-leak","label":"Identifie la fuite de ressource","importance":"BLOCKING","explanationMarkdown":"Le flux FileInputStream n''est jamais fermé."}],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Exercice PREDICTION (thème Java) -----------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000002', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000002',
    '22222222-0000-0000-0000-000000000002',
    1,
    'Prédiction : ordre d''exécution d''un bloc try-finally avec retour',
    'PREDICTION',
    'BEGINNER',
    5,
    'Quelle valeur retourne la méthode ci-dessous ?',
    '["Comprendre la priorité du bloc finally sur un return du try"]'::jsonb,
    '[{"path":"Sample.java","language":"java","kind":"SOURCE","content":"static int run() {\n  try {\n    return 1;\n  } finally {\n    return 2;\n  }\n}"}]'::jsonb,
    '[{"technology":"Java","minimumVersion":"17","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"SINGLE_CHOICE","choices":[{"id":"one","label":"1"},{"id":"two","label":"2"},{"id":"exception","label":"Lève une exception"}]}'::jsonb,
    '[{"level":1,"markdown":"Un `return` dans un bloc `finally` a-t-il priorité sur celui du `try` ?"}]'::jsonb,
    '{"explanationMarkdown":"Le `return 2` du bloc finally remplace celui du try : la méthode retourne 2.","acceptedAnswers":[],"correctChoiceIds":["two"],"expectedVerdicts":[],"criteria":[],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Exercice DIAGNOSIS (thème Spring) -----------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000003', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000003',
    '22222222-0000-0000-0000-000000000003',
    1,
    'Diagnostic : bean introuvable au démarrage',
    'DIAGNOSIS',
    'INTERMEDIATE',
    10,
    'Le démarrage échoue avec `NoSuchBeanDefinitionException` pour `OrderRepository`. Expliquez la '
    'cause la plus probable au vu de l''extrait de configuration fourni.',
    '["Diagnostiquer une erreur de scan de composants Spring"]'::jsonb,
    '[{"path":"Application.java","language":"java","kind":"SOURCE","content":"@SpringBootApplication\n@ComponentScan(\"ch.example.web\")\npublic class Application { }"}]'::jsonb,
    '[{"technology":"Spring Boot","minimumVersion":"3.0","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"FREE_TEXT","choices":[]}'::jsonb,
    '[{"level":1,"markdown":"Dans quel package se trouve `OrderRepository` par rapport au `@ComponentScan` déclaré ?"}]'::jsonb,
    '{"explanationMarkdown":"Le `@ComponentScan` restreint le scan à `ch.example.web` ; si `OrderRepository` vit dans `ch.example.data`, il n''est jamais détecté.","acceptedAnswers":["Le composant scan est restreint à un sous-package qui n''inclut pas le repository"],"correctChoiceIds":[],"expectedVerdicts":[],"criteria":[{"id":"scan-scope","label":"Identifie la restriction du component scan","importance":"BLOCKING","explanationMarkdown":"Le repository est hors du package scanné."}],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- Exercice REFACTORING (thème Java) -----------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000004', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000004',
    '22222222-0000-0000-0000-000000000004',
    1,
    'Refactoring : remplacer une chaîne de if par un switch à motifs',
    'REFACTORING',
    'ADVANCED',
    20,
    'Réécrivez la méthode ci-dessous à l''aide d''un `switch` à motifs de records (Java 21+), sans '
    'changer son comportement observable.',
    '["Utiliser le pattern matching for switch sur une hiérarchie scellée"]'::jsonb,
    '[{"path":"Shape.java","language":"java","kind":"SOURCE","content":"double area(Object shape) {\n  if (shape instanceof Circle c) return Math.PI * c.radius() * c.radius();\n  if (shape instanceof Square s) return s.side() * s.side();\n  throw new IllegalArgumentException();\n}"}]'::jsonb,
    '[{"technology":"Java","minimumVersion":"21","featureStatus":"STABLE","notes":"Pattern matching for switch"}]'::jsonb,
    '{"kind":"FREE_TEXT","choices":[]}'::jsonb,
    '[{"level":1,"markdown":"La hiérarchie `Circle`/`Square` peut-elle être scellée (`sealed interface Shape`) ?"},{"level":2,"markdown":"Un `switch` à motifs de records peut décomposer directement les composants (`case Circle(var r)`)."}]'::jsonb,
    '{"explanationMarkdown":"Un `switch` exhaustif sur l''interface scellée `Shape`, avec décomposition de motifs, remplace la chaîne de `if` sans `default` nécessaire.","acceptedAnswers":[],"correctChoiceIds":[],"expectedVerdicts":[],"criteria":[{"id":"exhaustive-switch","label":"Switch exhaustif sans branche par défaut","importance":"MAJOR","explanationMarkdown":"L''exhaustivité du switch sur type scellé remplace la vérification manuelle."}],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Exercice TECH_DISCOVERY (thème Angular) ------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000005', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000005',
    '22222222-0000-0000-0000-000000000005',
    1,
    'Découverte technique : les signaux Angular',
    'TECH_DISCOVERY',
    'INTERMEDIATE',
    10,
    'Parmi les propositions suivantes sur les signaux Angular, cochez celles qui sont exactes.',
    '["Découvrir les primitives signal/computed/effect"]'::jsonb,
    NULL,
    '[{"technology":"Angular","minimumVersion":"17","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"MULTIPLE_CHOICE","choices":[{"id":"reactive","label":"Un signal notifie ses consommateurs à chaque changement de valeur"},{"id":"computed-cache","label":"Un `computed` recalcule sa valeur à chaque lecture, sans mise en cache"},{"id":"effect-side","label":"Un `effect` peut exécuter des effets de bord en réaction aux signaux qu''il lit"}]}'::jsonb,
    '[{"level":1,"markdown":"Un `computed` mémorise-t-il sa dernière valeur tant que ses dépendances n''ont pas changé ?"}]'::jsonb,
    '{"explanationMarkdown":"Les signaux sont réactifs et les effets réagissent aux lectures ; en revanche `computed` met en cache sa valeur (pas de recalcul à chaque lecture).","acceptedAnswers":[],"correctChoiceIds":["reactive","effect-side"],"expectedVerdicts":[],"criteria":[],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000005', '11111111-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- Exercice TECHNICAL_CHOICE (thème Algorithmes) ------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000006', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000006',
    '22222222-0000-0000-0000-000000000006',
    1,
    'Choix technique : structure de données pour un cache LRU',
    'TECHNICAL_CHOICE',
    'INTERMEDIATE',
    12,
    'Vous devez implémenter un cache LRU (accès et éviction en O(1) amorti). Quelle structure '
    'choisissez-vous ?',
    '["Associer complexité algorithmique et choix de structure de données"]'::jsonb,
    NULL,
    '[{"technology":"Java","minimumVersion":"17","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"SINGLE_CHOICE","choices":[{"id":"linked-hash-map","label":"LinkedHashMap en mode accès, avec removeEldestEntry"},{"id":"array-list","label":"ArrayList trié par date d''accès"},{"id":"tree-map","label":"TreeMap trié par clé"}]}'::jsonb,
    '[{"level":1,"markdown":"Quelle structure combine déjà table de hachage et ordre d''insertion/accès ?"}]'::jsonb,
    '{"explanationMarkdown":"`LinkedHashMap` en mode accès (`accessOrder=true`) combinée à `removeEldestEntry` offre l''éviction LRU en O(1) amorti.","acceptedAnswers":[],"correctChoiceIds":["linked-hash-map"],"expectedVerdicts":[],"criteria":[],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000006', '11111111-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;

-- Exercice QUIZ (thème Spring) -----------------------------------------------------------------

INSERT INTO exercise (id, created_at, updated_at) VALUES
    ('22222222-0000-0000-0000-000000000007', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version (
    id, exercise_id, version_number, title, type, difficulty, estimated_minutes, prompt_markdown,
    learning_objectives, files, technologies, response_spec, hints, correction, published_at, created_at, updated_at
) VALUES (
    '33333333-0000-0000-0000-000000000007',
    '22222222-0000-0000-0000-000000000007',
    1,
    'Quiz : portées des beans Spring',
    'QUIZ',
    'BEGINNER',
    5,
    'Quelle est la portée par défaut d''un bean Spring déclaré sans annotation `@Scope` ?',
    '["Connaître la portée singleton par défaut"]'::jsonb,
    NULL,
    '[{"technology":"Spring","minimumVersion":"6.0","featureStatus":"STABLE","notes":null}]'::jsonb,
    '{"kind":"SINGLE_CHOICE","choices":[{"id":"singleton","label":"singleton"},{"id":"prototype","label":"prototype"},{"id":"request","label":"request"}]}'::jsonb,
    '[{"level":1,"markdown":"Combien d''instances Spring crée-t-il par défaut pour un même bean dans un même contexte ?"}]'::jsonb,
    '{"explanationMarkdown":"Sans annotation `@Scope`, un bean Spring est `singleton` : une seule instance par contexte d''application.","acceptedAnswers":[],"correctChoiceIds":["singleton"],"expectedVerdicts":[],"criteria":[],"sources":[]}'::jsonb,
    now(), now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exercise_version_topic (exercise_version_id, topic_id) VALUES
    ('33333333-0000-0000-0000-000000000007', '11111111-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- Fiche de veille (discovery) reliée à l'exercice de refactoring --------------------------------

INSERT INTO article (
    id, title, summary, body_markdown, technologies, sources, published_at, revision, created_at, updated_at
) VALUES (
    '44444444-0000-0000-0000-000000000001',
    'Le pattern matching for switch en Java 21+',
    'Tour d''horizon des switch à motifs sur hiérarchies scellées, avec décomposition de records.',
    'Depuis Java 21, `switch` accepte des motifs de types et de records, rendant obsolètes de '
    'nombreuses chaînes de `instanceof`. Combiné à `sealed`, le compilateur vérifie l''exhaustivité '
    'sans clause `default`.',
    '[{"technology":"Java","minimumVersion":"21","featureStatus":"STABLE","notes":null}]'::jsonb,
    '[{"title":"JEP 441: Pattern Matching for switch","url":"https://openjdk.org/jeps/441","accessedAt":"2024-01-15T00:00:00Z"}]'::jsonb,
    now(),
    1,
    now(), now()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO article_topic (article_id, topic_id) VALUES
    ('44444444-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO article_related_exercise_version (article_id, exercise_version_id) VALUES
    ('44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;

# Backend — règles propres

Backend unique : il sert le backoffice, l'application coach et le portail parent à travers le même contrat d'API. Les règles générales sont dans `../CLAUDE.md` et s'appliquent ici. La stack est actée par `docs/decisions/0024-stack-backend.md`.

## Stack

- Java 21, Spring Boot **4.1.1** (décision 0024), Maven.
- PostgreSQL. Spring Data JPA (Hibernate 7). Flyway.
- Spring MVC pour l'API REST. Spring Security. Spring Boot DevTools en développement seulement.
- Lombok, limité (voir « Conventions de code »).
- Contrat : OpenAPI dans `../contracts/`, interfaces et modèles générés par `openapi-generator-maven-plugin`. springdoc pour Swagger UI en profil `dev` uniquement.
- Bouncy Castle (Argon2), fastexcel (export Excel). Validées pour F01 (décision 0029). Client S3 : à demander avec le choix d'hébergement.
- Tests : JUnit 5, AssertJ, Spring Boot Test, Testcontainers PostgreSQL, ArchUnit.
- Toute dépendance absente de cette liste est demandée à Omar avant d'être ajoutée (`../CLAUDE.md` §5).

À savoir sur Spring Boot 4 : Spring Framework 7, Jakarta EE 11, Hibernate 7, Jackson 3 (package `tools.jackson`). Vérifier la compatibilité de chaque bibliothèque avant de l'utiliser.

## Arborescence

Base package `ma.clubify`. Classe principale `ClubifyBackendApplication`.

Un package par domaine métier, nommé d'après `docs/glossaire.md` et la section 6.2 du cahier des charges. Les couches sont à l'intérieur du domaine, jamais à la racine.

```
ma.clubify
├── ClubifyBackendApplication.java
├── config/            configuration Spring transverse (JPA, Jackson, i18n, OpenAPI, Clock)
├── security/          authentification, jeton, TenantContext, permissions
├── exception/         GlobalExceptionHandler (@ControllerAdvice), exceptions communes
├── common/            Money, AuditLog, événements, connecteurs abstraits, base entity
├── platform/          club, site, utilisateurs, rôles, paramètres du club (PLT, ADM, SEC)
├── family/            familles, tuteurs, adhérents, pièces, consentements (FAM, ONB)
├── catalog/           activités, niveaux, lieux, groupes, saison, calendrier (ACT, NIV, GRP, PLA)
├── offer/             formules, forfaits, grilles, remises (OFR, TAR)
├── enrollment/        inscriptions (INS)
├── billing/           factures, échéanciers, paiements, ventilation, reçus, chèques, avoirs (FAC, CPT)
├── cash/              sessions de caisse, bordereaux, dépenses, comptes (FIN)
├── pos/               articles, ventes comptoir (POS)
└── …                  un package par domaine suivant, ajouté avec la feature qui le crée
```

Dans chaque domaine :

```
family
├── controller/        @RestController implémentant les interfaces générées depuis le contrat
├── service/           logique métier, transactions
├── repository/        interfaces Spring Data
└── model/
    ├── request/       XxxRequest (record) — corps de requête, si non généré depuis le contrat
    ├── response/      XxxResponse (record) — corps de réponse, si non généré depuis le contrat
    ├── dto/           XxxDto (record) — objets passés du contrôleur au service
    └── entity/        entités JPA
```

Règles de dépendance entre packages, vérifiées par ArchUnit :
- `controller` dépend de `service` et de `model`, jamais de `repository` ni d'une entité.
- `service` dépend de `repository`, `model`, `common` ; jamais de `controller`.
- Un domaine ne dépend d'un autre domaine que par son `service` public ou par un événement métier ; jamais par son `repository` ni ses entités.
- `common`, `config`, `security`, `exception` ne dépendent d'aucun domaine.

## Rôle des couches

Contrôleur : reçoit la requête, valide la forme (`@Valid`), convertit en DTO, appelle le ou les services, met en forme la réponse. **Aucune logique métier, aucune condition sur des données, aucun accès direct à un repository.** Vérifier un statut d'inscription à partir de ses relations est de la logique métier : elle vit dans le service.

Service : règles métier, transformations, transactions (`@Transactional` au niveau service), publication des événements métier, écriture de l'audit. Lève des exceptions typées ; ne retourne jamais une entité au contrôleur.

Repository : interfaces Spring Data ; aucune logique. Toute requête est filtrée par club (voir « Sécurité et multi-tenant »).

Entité : état et invariants propres à l'objet ; pas d'accès aux services.

## Conventions de code

- Noms techniques en anglais, tirés de `docs/glossaire.md`. Un terme, un nom.
- Records Java pour request, response et DTO. Aucun Lombok dessus.
- Entités JPA : `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor` autorisés. **Interdits : `@Data`, `@EqualsAndHashCode`, `@ToString`** (relations lazy, récursion). `equals`/`hashCode` sur l'identifiant seulement, écrits à la main quand nécessaires.
- Identifiants : UUID v7, colonne `uuid`. Jamais d'auto-incrément exposé dans l'API.
- Numéros de facture et de reçu (série continue par club et par exercice) : table de séquences verrouillée en transaction (`SELECT … FOR UPDATE`), jamais une séquence ou un auto-incrément de la base (trous possibles).
- Montants : `@Embeddable Money { long amountCents; String currency }`. Jamais `BigDecimal` ni `double` en base ni dans le contrat.
- Dates : `Instant` pour les instants (colonne `timestamptz`), `LocalDate` pour les dates pures (naissance, bornes de saison). JVM, Hibernate (`hibernate.jdbc.time_zone=UTC`) et Jackson (`spring.jackson.time-zone=UTC`) en UTC. Le fuseau du club est un attribut de `Club`, utilisé pour l'affichage et les règles calendaires, jamais pour stocker.
- `java.time.Clock` injecté partout où l'heure courante est lue ; jamais `Instant.now()` direct dans un service (testabilité).
- `@Version` sur toute entité touchant à l'argent (facture, paiement, chèque, session de caisse, solde).
- Toute entité porte : `club_id`, `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at` (via une classe de base et Spring Data Auditing).
- Suppression logique : `@SoftDelete` d'Hibernate ; aucune requête `DELETE` sur une table métier. Unicité avec suppression logique : index partiel `WHERE deleted_at IS NULL`.
- `spring.jpa.open-in-view=false`. `ddl-auto=validate`. Pas de `@Transactional` sur un contrôleur.
- Mappers manuels entre DTO et entités, dans le domaine concerné.

## Contrat d'API

- Source de vérité : `../contracts/openapi.yaml` (un fichier par domaine possible, référencés depuis l'index). Modifié en premier dans chaque feature, dans la même livraison.
- Le plugin `openapi-generator` produit les interfaces de contrôleurs et les modèles ; les contrôleurs implémentent ces interfaces. Le code généré n'est jamais modifié à la main ni commité.
- Préfixe `/api/v1`. Ressources au pluriel, en anglais, kebab-case. Pagination par `page`, `size`, `sort`.
- Le contrat porte des codes, jamais des libellés. Montants en centimes avec devise, dates en UTC ISO-8601.
- Les identifiants de club et de site n'apparaissent jamais dans le contrat : ils viennent du jeton.

## Erreurs et validation

- `GlobalExceptionHandler` annoté `@ControllerAdvice`, étend `ResponseEntityExceptionHandler`, renvoie des `ProblemDetail` (RFC 9457).
- Chaque erreur porte un `code` stable (ex. `family.phone.invalid`) et un message résolu par `MessageSource` selon `Accept-Language` ; `messages_fr.properties` d'abord, `ar` et `en` ensuite. Aucun libellé en dur dans le code.
- Validation de forme : Bean Validation sur les requests. Validation métier : dans les services, par exceptions typées (`BusinessRuleException` avec code).
- Jamais de stack trace, de donnée d'enfant ou de donnée de santé dans une réponse d'erreur.

## Sécurité et multi-tenant

- Authentification : JWT auto-émis, stateless (OAuth2 Resource Server, clés propres). Claims : `sub`, `club_id`, `site_id` (optionnel), rôles. Refresh token stocké en base, révocable. Mots de passe : `DelegatingPasswordEncoder` avec Argon2 par défaut. Second facteur optionnel pour le staff (SEC-01). Code à usage unique pour les parents (R8, même mécanisme d'émission).
- Isolation par club : schéma partagé, colonne `club_id` sur chaque table, à une exception près : `user_account` est global (un compte peut appartenir à plusieurs clubs, PLT-02) et c'est `membership` qui porte `club_id` et le rôle (décision 0029). Hibernate `@TenantId` sur la colonne, `CurrentTenantIdentifierResolver` alimenté par `TenantContext`, lui-même alimenté par le jeton au filtre de sécurité. Aucune requête sans filtre de club ; aucune méthode de repository ne prend `club_id` en paramètre. Row-Level Security PostgreSQL activée sur les tables métier en seconde ligne.
- Les identifiants de club et de site viennent de l'authentification, jamais de la requête du client.
- Permissions fines (SEC-02, décision 0028) : `@PreAuthorize` sur les services (pas seulement sur les contrôleurs). Rôles : administrateur (titulaire du compte, tous droits, non retirables), gérant, administratif, coach, comptable, parent. Le rôle donne le jeu par défaut ; une surcharge par utilisateur est possible, auditée. Grammaire des permissions : `domaine.objet.action`, actions fermées (`consulter`, `creer`, `modifier`, `desactiver`, `valider`, `exporter`) plus actions sensibles nommées ; une permission peut porter un paramètre (plafond). Granularité : un droit par décision engageant l'argent, un droit d'une personne ou une donnée sensible ; un droit par écran ou liste ; jamais par champ. Chaque feature déclare ses permissions dans sa fiche et les décrit dans `../docs/droits.md` ; `../docs/matrice-droits.xlsx` s'en régénère. **Aucun point d'entrée sans permission déclarée**, **aucun droit sans description**, et **aucun rôle recevant « exporter » sans « consulter »** : vérifiés par ArchUnit.
- Journal d'audit (SEC-04) : table `audit_log` en ajout seul (entité, action, avant, après, auteur, motif, horodatage), écrite dans la même transaction que l'action par un abonné `BEFORE_COMMIT` à l'événement métier. **Si l'audit échoue, l'action échoue** : c'est l'unique exception à la règle « un abonné en échec ne bloque pas le métier » (décision 0029). L'auteur est un utilisateur, un parent ou le système avec la règle identifiée ; `club_id` n'est nullable que pour les événements d'authentification. L'utilisateur de base de l'application n'a ni `UPDATE` ni `DELETE` sur cette table. Obligatoire sur tout ce qui touche à l'argent, aux remises, aux dérogations et aux données sensibles.
- Données sensibles (SEC-03) : santé, CIN, pièces chiffrées au champ par `AttributeConverter` (AES-GCM), clé fournie par variable d'environnement, jamais dans le dépôt. Chaque lecture est journalisée. Jamais dans les logs.
- Suppression logique uniquement. Facture et paiement jamais supprimés : avoir ou contre-passation.
- Connecteurs : interfaces `MessagingProvider` et `PaymentProvider` dans `common`, implémentation `noop` en R1 ; aucun prestataire codé en dur. Un canal en échec ne bloque pas le métier.

## Événements métier

- Un seul moteur (PLT-04) : chaque fait métier est publié une fois par le service, via `ApplicationEventPublisher`, à la validation de la transaction (`@TransactionalEventListener`).
- Table outbox pour les effets externes (messages, exports) : l'événement est persisté dans la transaction, traité ensuite, rejoué en cas d'échec.
- L'audit, les notifications, les tâches et les rapports s'abonnent aux événements ; un domaine n'appelle jamais directement le service d'un autre pour ces effets. L'audit est synchrone et bloquant (`BEFORE_COMMIT`) ; tout autre abonné passe par l'outbox et ne bloque jamais.

## Règles propres

- Montants en centimes avec devise. Dates en UTC ; le fuseau du club sert à l'affichage et aux règles calendaires.
- Aucune règle listée en section 9.8 du cahier des charges n'est codée en dur : elle est lue dans la configuration du club (`ClubSetting`, JSONB), avec une valeur par défaut documentée dans la fiche de feature.
- Numérotation continue des factures et reçus par club et par exercice.
- Logs structurés (natif Boot 4), identifiant de corrélation par requête. Jamais de nom d'enfant, de téléphone, de CIN ni de donnée de santé dans un log ; masquage systématique.

## Configuration

- `src/main/resources/application.properties` : propriétés communes, `spring.profiles.active` par défaut `dev`, placeholders (`${database.url}`, `${server.port}`) résolus par profil.
- `application-dev.properties`, `application-test.properties`, `application-demo.properties`, `application-prod.properties`.
- Aucun secret dans un fichier versionné : variables d'environnement. `application-local.properties` ignoré par git.
- Docker Compose (support natif Boot) pour PostgreSQL en local. Actuator : `health` et `info` seulement, sans détail en `prod`.

## Migrations

- Flyway propriétaire du schéma. Nommage `V<yyyyMMddHHmm>__<description>.sql`. Une migration par feature au minimum, dans le même commit que le code.
- Une migration appliquée ne se modifie jamais. Toute migration destructive (drop, perte de données) est demandée à Omar avant d'être écrite.
- Chaque table métier porte `club_id NOT NULL` indexé, les colonnes d'audit et `deleted_at`.

## Stratégie de tests

- Les règles métier (tarification, ventilation, remises, statuts) sont testées avant d'être codées (`docs/processus-feature.md`, étape 4).
- Unitaires : services avec repositories mockés, `Clock` fixe. Intégration : `@SpringBootTest` + Testcontainers PostgreSQL ; jamais H2.
- Tests obligatoires par feature touchant à l'argent, aux remises, aux dérogations ou aux données sensibles : un test d'isolation par club (deux clubs, une requête, aucune fuite) et un test d'audit (l'action produit sa ligne `audit_log`). Une classe de base fournit les deux.
- ArchUnit : les règles de dépendance de l'arborescence, « aucun `@RestController` ne dépend d'un `Repository` », « aucune logique dans `controller` », « aucun point d'entrée sans permission déclarée ».
- Jeux de test : familles fictives reproduisant les structures observées (fratries, reliquats, chèque différé). Aucun nom réel, aucune donnée issue des captures.

## Commandes

À compléter à l'initialisation du projet (build, tests, génération du contrat, migration, lancement local).

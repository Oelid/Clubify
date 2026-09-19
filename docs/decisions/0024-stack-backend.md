# 0024 — Stack technique du backend

## Contexte

Le découpage en releases est acté (0023). Aucune feature ne peut être cadrée ni codée sans stack. Le backend est unique pour le backoffice, l'application coach et le portail parent, à travers le même contrat d'API. Les invariants du `CLAUDE.md` (isolation par club, centimes, UTC, suppression logique, audit, i18n, connecteurs abstraits) doivent être portés par le squelette dès la première feature.

## Décision

| Sujet | Choix |
| --- | --- |
| Langage et plateforme | Java 21, Spring Boot 4.0.x (dernière version de correctif au moment de l'initialisation) |
| Build | Maven |
| Base de données | PostgreSQL |
| Persistance et migrations | Spring Data JPA (Hibernate 7) ; Flyway propriétaire du schéma, `ddl-auto=validate` |
| API | Spring MVC REST ; contrat OpenAPI écrit d'abord dans `contracts/`, interfaces et modèles générés par `openapi-generator-maven-plugin` ; Swagger UI via springdoc en dev seulement |
| Sécurité | Spring Security ; JWT auto-émis et stateless (OAuth2 Resource Server, clés propres), claims `club_id` et rôles, refresh token révocable en base, Argon2 ; permissions fines par `@PreAuthorize` |
| Multi-tenant | Schéma partagé, colonne `club_id` sur chaque table ; Hibernate `@TenantId` alimenté par un `TenantContext` issu du jeton ; Row-Level Security PostgreSQL en seconde ligne |
| Code | Lombok limité à `@Getter`/`@Setter`/`@Builder` sur les entités ; records Java pour request, response et DTO ; base package `ma.clubify`, un package par domaine métier avec ses couches à l'intérieur |
| Tests | JUnit 5, Testcontainers PostgreSQL pour l'intégration, ArchUnit pour les règles d'architecture |
| Configuration | `application.properties` + profils `dev`, `test`, `demo`, `prod` ; secrets par variables d'environnement uniquement |
| Développement | Spring Boot DevTools, Docker Compose pour la base locale |

Le détail des conventions est dans `backend/CLAUDE.md`.

## Raison

Stack standard, connue des agents de développement, qui couvre tous les invariants sans brique supplémentaire : Hibernate porte nativement le discriminant de tenant et la suppression logique, Spring Security porte le JWT sans serveur d'identité, Spring publie les événements métier sans broker. PostgreSQL apporte ce que l'application ne peut pas garantir seule : isolation par club au niveau base (Row-Level Security), DDL transactionnel pour des migrations sûres, index partiels pour l'unicité avec suppression logique, JSONB pour les règles configurables par club (section 9.8). Le contrat d'abord respecte la règle « contrat modifié en premier » du `CLAUDE.md`.

## Alternatives écartées

- MySQL : proposé initialement ; pas de Row-Level Security, pas de DDL transactionnel, pas d'index partiel ; l'isolation reposerait à 100 % sur l'application.
- Gradle : plus rapide, moins uniforme dans ce que produisent les agents.
- Packages par couche à la racine (`controller/`, `service/`…) : illisible à 32 domaines ; un domaine par package donne à chaque agent un périmètre clair.
- Spring Modulith : bon candidat (frontières de modules, registre d'événements) ; non retenu pour l'instant, ArchUnit et une table outbox suffisent ; à reconsidérer si les frontières dérivent.
- Session serveur ou Keycloak pour l'authentification : la session convient mal au coach mobile et au portail parent ; Keycloak ajoute une brique à héberger et à conformer (0018).
- Code d'abord avec spec exportée : le contrat suivrait le code au lieu de le précéder.
- H2 pour les tests : masque les différences de dialecte.
- MapStruct, Envers : non retenus au départ ; mappers manuels et table d'audit ciblée ; à reconsidérer si le volume le justifie.

## Source dans le cahier des charges

Sections 6.2 (modules), 6.3, 9.5, 9.6 (à concevoir dès le départ), 9.8 ; PLT-01, PLT-04, PLT-05, SEC-01 à SEC-04, INT-01, INT-02, FAC-01, FAC-07.

## Écarts ou points ouverts

- Aucun écart avec le cahier des charges : il ne prescrit aucune technologie.
- Nouvelles dépendances validées par Omar le 2026-09-19 : openapi-generator-maven-plugin, Testcontainers PostgreSQL, ArchUnit, springdoc-openapi (Swagger UI, dev). Toute dépendance supplémentaire repasse par lui.
- La stack frontend fait l'objet d'une décision distincte, à prendre avant la première feature comportant un écran.
- Hébergement et localisation des données (0018) : à choisir avant le premier client ; sans effet sur cette décision.
- Version exacte de Spring Boot 4.0.x et compatibilité de chaque dépendance avec Spring Framework 7 / Jackson 3 / Hibernate 7 : à vérifier à l'initialisation du projet.

## Date

2026-09-19

## Statut

Acceptée.

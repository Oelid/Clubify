# 0029 — Choix structurants du socle (F01)

## Contexte

F01 pose les invariants de la section 9.6 du cahier des charges. Son plan d'implémentation (fiche F01, étape 3) a fait apparaître huit points d'impact majeur, chacun irréversible sans refonte, dont deux exceptions explicites aux règles déjà écrites du dépôt et quatre nouvelles dépendances.

## Décision

1. **Utilisateur global.** `user_account` ne porte pas de `club_id` : un même compte peut appartenir à plusieurs clubs (PLT-02). L'appartenance `membership` porte le club et le rôle. C'est la seule exception à « identifiant de club sur chaque donnée » ; toute donnée métier reste rattachée au club.
2. **`audit_log.club_id` nullable pour les seuls événements d'authentification** (connexion échouée sur un identifiant inconnu) ; une contrainte en base l'impose pour tout autre type d'action.
3. **Audit synchrone et bloquant.** L'abonné audit s'exécute dans la transaction de l'action (`BEFORE_COMMIT`) ; si l'audit échoue, l'action échoue. C'est l'unique exception à la règle « un abonné en échec ne bloque jamais le métier » ; tout autre abonné passe par l'outbox.
4. **Catalogues en code, variations en base.** Rôles, permissions et paramètres configurables sont déclarés en code, versionnés avec les features qui les introduisent ; la base ne porte que les surcharges par utilisateur et les valeurs par club.
5. **Chiffrement applicatif** AES-256-GCM des champs sensibles et des fichiers, clé fournie par l'environnement, identifiant de clé stocké pour permettre la rotation ; le stockage ne voit jamais le clair.
6. **Row-Level Security PostgreSQL** activée dès F01, en seconde ligne derrière le discriminant Hibernate.
7. **Export synchrone** en R1.
8. **Dépendances validées** : Bouncy Castle (Argon2), fastexcel (Excel), angularx-qrcode (QR du second facteur). TOTP implémenté avec le JDK. **Client S3 non retenu** : F01 livre l'interface de stockage avec le disque local et une implémentation mémoire ; l'implémentation S3 et sa dépendance viendront avec le choix d'hébergement (0018).

## Raison

1. Sans compte global, le rattachement d'un coach à deux clubs (PLT-02) imposerait une migration de données plus tard.
2. Une tentative de connexion sur un identifiant inconnu n'appartient à aucun club ; la journaliser est indispensable (10.I), l'inventer un club serait faux.
3. Un encaissement en espèces non journalisé est pire qu'un encaissement refusé. Les règles antérieures (« même transaction » et « aucun abonné ne bloque ») étaient contradictoires pour l'audit ; la transaction l'emporte.
4. Rien ne garantit qu'une feature déclare ses permissions si le catalogue vit en base ; en code, le test d'architecture le vérifie.
5. La localisation des données n'est pas encore choisie (0018) ; le chiffrement applicatif rend ce choix moins critique.
6. L'isolation par club est l'invariant le plus grave à rater ; deux lignes de défense coûtent peu.
7. Les listes de R1 tiennent en quelques centaines de lignes.
8. Argon2 est la recommandation OWASP et Spring Security exige Bouncy Castle pour le fournir ; fastexcel est léger ; le QR côté navigateur évite une dépendance backend. Le client S3 n'a pas d'usage tant que l'hébergeur n'est pas choisi.

## Alternatives écartées

- Un utilisateur par club, fusion en R9 : migration garantie.
- Pseudo-club « plateforme » pour l'authentification : complique l'isolation.
- Audit asynchrone dérivé de l'outbox : cohérent avec la règle 21, mais fenêtre où l'action existe sans ligne d'audit.
- Catalogues en base : administrables sans livraison, mais aucune garantie de déclaration.
- Chiffrement par l'hébergeur seulement : le clair transite, dépendance au choix 0018.
- Reporter la Row-Level Security : l'isolation ne tiendrait que par l'application.
- BCrypt intégré : acceptable, moins recommandé. CSV seul : contraire à INT-03. ZXing côté backend : dépendance de plus.
- Client S3 dès F01 : proposé, non retenu par Omar en l'absence d'hébergeur.

## Source dans le cahier des charges

9.6, 9.4 point 12, 10.I, PLT-01, PLT-02, PLT-04, PLT-05, SEC-03, SEC-04, INT-03 ; fiche F01 (étape 3, points M1 à M8) ; décisions 0018, 0024, 0025, 0028.

## Écarts ou points ouverts

- Deux exceptions aux règles du dépôt, désormais écrites dans `backend/CLAUDE.md` : compte utilisateur global ; audit bloquant.
- `backend/CLAUDE.md` et `frontend/CLAUDE.md` listent les dépendances validées.
- Le choix d'hébergement (0018) reste ouvert ; il déclenchera la demande du client S3.

## Date

2026-09-20

## Statut

Acceptée.

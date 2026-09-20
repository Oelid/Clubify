# Feature : F01 — Socle plateforme

Première feature de R1. Elle ne porte aucune entité métier : elle pose les invariants que toutes les autres features consomment sans jamais les rattraper (`CLAUDE.md` §3, cahier section 9.6). Elle est invisible pour le club ; ses critères d'acceptation sont techniques et vérifiables par test.

## Identifiant(s) du backlog et source

| ID | Libellé | Ce que F01 en livre |
| --- | --- | --- |
| PLT-01 | Multi-tenant | Club, site, isolation par club sur chaque donnée |
| PLT-04 | Bus d'événements métier | Publication unique des faits métier, outbox, premier abonné : l'audit |
| PLT-05 | Gestion documentaire | Fichiers privés rattachés à une entité, liens temporaires ; premier fichier : le logo |
| SEC-01 | Authentification | Staff : identifiant, mot de passe, second facteur TOTP obligatoire pour le gérant, optionnel pour les autres |
| SEC-02 | Rôles et permissions | Six rôles dont l'administrateur du compte, permissions fines nommées `domaine.objet.action`, ajustables par utilisateur, contrôlées côté service |
| SEC-03 | Données sensibles | Mécanisme de chiffrement au champ et au fichier, accès journalisé |
| SEC-04 | Journal d'audit | Journal en ajout seul : qui, quoi, quand, avant, après |
| ADM-01 | Paramètres du club | Identité, logo, fuseau, devise, langue, numérotation ; registre des règles configurables |
| INT-03 | Exports | Export CSV et Excel de toute liste, journalisé |

Sections du cahier : 5 (protection des données, mineurs, hébergement, langues, téléphones, identité), 6.2 (module « Socle plateforme »), 6.3 (principes), 9.4 point 12, 9.6, 9.7, 9.8, 10.H, 10.I. Décisions : 0018 (CNDP), 0023 (PLT-02, PLT-03 et PLT-08 à cheval), 0024 (stack backend), 0025 (stack frontend), 0027 (second facteur), 0028 (rôle administrateur et granularité des permissions).

Aucun cas d'utilisation de la section 8 n'est couvert de bout en bout : F01 est préalable à tous.

## Objectif

Poser une fois, avec des tests, tout ce que le cahier des charges exige « dès le départ pour éviter une refonte » : isolation par club, authentification et rôles, audit inviolable, fichiers privés, événements métier, paramètres du club, i18n. Pour le gérant, le premier résultat visible est de pouvoir se connecter, paramétrer son club et créer les comptes de son équipe.

## Acteurs

| Acteur | Ce qu'il fait dans F01 |
| --- | --- |
| Éditeur (Clubify) | Crée le club, son site par défaut et le premier compte administrateur. Aucun écran en R1 : commande d'amorçage versionnée (l'assistant de démarrage ADM-02 est en R9). Tranché (Q1). |
| Administrateur | Titulaire du compte du club. Détient tous les droits, non retirables ; crée, modifie, désactive les utilisateurs, attribue les rôles et ajuste les permissions ; consulte le journal ; ferme les sessions ; réinitialise un second facteur. Au club pilote, c'est la même personne que le gérant. |
| GER | Se connecte avec second facteur ; modifie les paramètres du club et l'offre par défaut, sauf droit retiré par l'administrateur ; consulte le journal d'audit ; exporte les listes. |
| ADM | Se connecte ; exporte les listes que son rôle l'autorise à voir. |
| COA | Se connecte. Ses écrans arrivent en R2 ; le compte existe dès F01. |
| PAR | Aucune action. Aucun compte parent avant R8 (code à usage unique, APP-01). Aucun compte pour l'enfant, jamais (section 5). |
| SYS | Filtre chaque requête par club ; hache les mots de passe ; publie les événements ; écrit l'audit ; chiffre les données sensibles ; génère et expire les liens de fichiers ; journalise les exports. |

## Règles métier

Chaque règle cite sa source. « À confirmer » renvoie aux questions de la section « Questions ».

### Isolation par club

1. Chaque donnée porte l'identifiant du club. Toute lecture et toute écriture sont filtrées par le club de l'utilisateur authentifié ; un identifiant de club n'apparaît jamais dans une requête du client. — PLT-01, 9.6, `contracts/README.md`, `backend/CLAUDE.md`.
2. Un club possède au moins un site ; le site par défaut est créé avec le club. Toute donnée rattachée à un lieu porte l'identifiant de site. Les écrans multi-sites ne sont pas dans ce plan (PLT-03 écarté, décision 0023). — PLT-03, 9.6.
3. Un utilisateur est rattaché à un club avec un rôle par club ; le modèle admet plusieurs clubs par utilisateur, l'écran de bascule est en R9 (PLT-02). — PLT-02, décision 0023.

### Authentification

4. Un membre du staff s'authentifie par identifiant et mot de passe. — SEC-01.
5. Le second facteur est un code temporaire par application (TOTP). Il est **obligatoire pour le rôle gérant** et optionnel pour les autres utilisateurs du staff. Un gérant sans second facteur actif doit l'activer à sa première connexion, avant tout accès. L'administrateur peut le réinitialiser pour un utilisateur qui a perdu son téléphone ; l'opération est auditée. Un appareil peut être marqué « de confiance » pour 30 jours (paramétrable par club) : le code n'y est plus demandé ; la liste des appareils de confiance est visible par l'utilisateur, révocable par lui ou par l'administrateur, avec audit (benchmark B4). À l'activation, des codes de secours à usage unique sont remis, régénérables ; ils permettent de se connecter sans le téléphone (benchmark B5). — SEC-01 ; écart avec « optionnel » consigné dans la décision 0027 (tranché le 2026-09-20).
6. Aucun compte n'existe pour un enfant. Les parents n'ont aucun accès avant R8. — Section 5 (Mineurs), APP-01, décision 0023.
7. Les mots de passe sont hachés (Argon2), jamais stockés ni journalisés en clair. Longueur minimale de 12 caractères, aucune règle de complexité imposée. — Décision 0024 ; tranché le 2026-09-20 (Q3).
8. La session est un jeton stateless avec jeton de rafraîchissement révocable ; la déconnexion et la désactivation du compte révoquent. L'administrateur voit les sessions ouvertes d'un utilisateur et peut les fermer sans le désactiver ; l'action est auditée (benchmark B6). — Décision 0024.
9. Les tentatives de connexion échouées sont journalisées. Après 5 échecs consécutifs, le compte est verrouillé 15 minutes ; le verrouillage est audité. — SEC-04, 10.I ; tranché le 2026-09-20 (Q3).

### Rôles et permissions

10. Six rôles : **administrateur**, gérant, administratif, coach, comptable, parent. L'administrateur est le titulaire du compte du club : au moins un par club, créé avec le club ; il détient tous les droits, qui ne peuvent lui être retirés ; il gère les utilisateurs et leurs droits. Le gérant détient par défaut tout l'opérationnel, y compris l'offre, les tarifs et les paramètres du club, et l'administrateur peut lui en retirer. — SEC-02 ; sixième rôle consigné dans la décision 0028 (tranché le 2026-09-20). À l'écran, le libellé est « Administrateur du compte » pour ne pas le confondre avec le rôle administratif (accueil).
11. Les permissions sont fines, en particulier sur les finances, les remises, les dérogations et la santé ; elles sont contrôlées dans la couche service, jamais seulement à l'écran. Granularité : **un droit par décision qui engage de l'argent, un droit d'une personne ou une donnée sensible ; un droit par écran ou liste consultable ; jamais un droit par champ.** Chaque permission est nommée `domaine.objet.action`, avec un vocabulaire d'actions fermé — consulter, créer, modifier, désactiver, valider, exporter — complété d'actions sensibles nommées ; une permission peut porter un paramètre (par exemple un plafond de remise, TAR-05). Chaque feature déclare ses permissions dans sa rubrique « Points de sécurité » ; aucun point d'entrée de l'API n'existe sans permission déclarée, ce qu'un test d'architecture vérifie. — SEC-02, `backend/CLAUDE.md`, décision 0028.
11a. Le rôle donne le jeu de permissions par défaut. L'administrateur peut ajouter ou retirer une permission à un utilisateur précis ; la surcharge est visible sur la fiche de l'utilisateur et auditée. La surcharge est l'exception, le rôle reste la règle. — Décision 0028 (benchmark B8).
11b. Permissions déclarées par F01 : `club.settings.consulter`, `club.settings.modifier`, `users.consulter`, `users.creer`, `users.modifier`, `users.desactiver`, `users.permissions.modifier` (administrateur seul, non délégable), `users.sessions.fermer`, `users.mfa.reinitialiser`, `audit.consulter`, `files.consulter` et `files.deposer` (par domaine propriétaire), et pour chaque domaine une action `exporter` distincte de `consulter`. — SEC-02, décision 0028.
12. Par défaut, seul l'administrateur crée, modifie, désactive un utilisateur, attribue un rôle et ajuste les permissions. Il peut déléguer la gestion des utilisateurs au gérant, jamais l'ajustement des permissions ni la gestion des administrateurs. Le dernier administrateur d'un club ne peut être ni désactivé ni rétrogradé. — SEC-02, décision 0028. Les six rôles existent dès F01 ; en R1, seuls gérant, administratif et coach ont des utilisateurs ; comptable et parent n'en reçoivent qu'avec les features qui les font entrer (R3 pour les exports comptables, R8 pour le portail parent). Tranché le 2026-09-20 (Q4).
13. Un utilisateur ne se supprime pas : il se désactive et ne peut plus se connecter. — 9.6 (suppression logique).

### Données sensibles

14. Santé, CIN et pièces sont chiffrées au repos, accessibles selon le rôle, et chaque accès est journalisé. F01 fournit le mécanisme (chiffrement au champ et au fichier, clé hors dépôt, journal d'accès) et l'applique aux fichiers ; les champs santé et CIN arrivent avec F02 et F03. — SEC-03, section 5 (Protection des données, Identité), décision 0024.
15. Aucune donnée sensible, aucun nom d'enfant, aucun téléphone dans les logs techniques. — `CLAUDE.md` §7, `backend/CLAUDE.md`.

### Journal d'audit

16. Toute action sensible produit une entrée : qui, quoi, quand, état avant, état après, motif quand une règle l'exige. L'auteur est un utilisateur, un parent (R8) ou le système ; dans ce dernier cas, la règle automatique qui a agi est identifiée (benchmark B1). — SEC-04, 9.7.
17. Le journal est en ajout seul. Aucune modification ni suppression, garanties au niveau de la base (l'utilisateur de base de l'application n'a ni `UPDATE` ni `DELETE` sur la table). Il n'est jamais purgé en R1 ; toute durée de conservation relève de SEC-06 (R4) et de l'avis du comptable (benchmark B2). — SEC-04 (« non modifiable »), 9.4 point 12, 10.I, décision 0024.
18. Actions auditées par F01 : connexion réussie et échouée, création, modification et désactivation d'utilisateur, changement de rôle, modification des paramètres du club, export, accès à un fichier privé. — SEC-04, SEC-03, INT-03.
19. L'administrateur et le gérant consultent le journal en R1 ; personne d'autre. Il se consulte avec des filtres — utilisateur, période, type d'action, entité — et chaque entrée renvoie vers l'entité concernée (benchmark B3). — SEC-04 ; tranché le 2026-09-20 (Q5, complété par B3).

### Événements métier

20. Chaque fait métier est publié une fois, à la validation de la transaction ; audit, notifications (R3), tâches (R5) et rapports s'y abonnent. Les effets externes passent par une table outbox rejouable. — PLT-04, 6.3, `backend/CLAUDE.md`.
21. Un abonné en échec ne bloque jamais l'action métier. — 6.3, INT-01 (principe « un canal en échec ne bloque pas le métier »).
22. Premier événement et premier abonné livrés par F01 : les événements de F01 (utilisateur, paramètres, export, fichier) alimentent le journal d'audit par abonnement, pas par appel direct. — PLT-04, SEC-04.

### Fichiers

23. Un fichier est privé, rattaché à une entité propriétaire et au club, servi par un lien temporaire signé. Durée du lien : 15 minutes par défaut, paramétrable par club. — PLT-05 ; tranché le 2026-09-20 (Q7).
24. Types et taille maximale des fichiers sont des paramètres du club. Valeurs par défaut : PDF, JPEG, PNG ; 10 Mo. — PLT-05, 9.8 (par extension) ; tranché le 2026-09-20 (Q7).
25. Un fichier se supprime logiquement ; la purge physique relève des durées de conservation (SEC-06, R4). — 9.6, SEC-06.
26. Premier fichier livré par F01 : le logo du club (ADM-01). — ADM-01, PLT-05.
27. Le stockage des fichiers est écrit derrière une interface unique ; F01 livre le disque local et une implémentation mémoire pour les tests ; la cible S3 compatible et sa dépendance viendront avec le choix d'hébergement. Le lieu d'hébergement (Maroc ou Europe) reste à valider avec le juriste du club avant la mise en service (décision 0018) ; il ne bloque pas l'étape 3. — Section 5 (Hébergement), décision 0018 ; approche tranchée le 2026-09-20 (Q6).

### Paramètres du club

28. Identité du club : nom, adresse, téléphone, courriel, logo, forme juridique, ICE, IF, RC. F08 (CPT-02) ajoutera le régime fiscal, les taux de taxe et décidera des mentions portées sur la facture ; ces champs d'identité restent en F01. — ADM-01, CPT-02 ; tranché le 2026-09-20 (Q8).
29. Fuseau horaire du club (défaut `Africa/Casablanca`), devise (défaut MAD), langue par défaut (fr). Les dates sont stockées en UTC ; le fuseau sert à l'affichage et aux règles calendaires. — Section 5, 9.6, `CLAUDE.md` §3.
30. Numérotation : F01 porte les paramètres de format (préfixes, exercice de départ) des séries de factures et de reçus ; la mécanique de série continue est livrée par F08 et F09. — ADM-01, FAC-01, FAC-07, 9.8 (« numérotation et mentions des documents »).
31. Registre des règles configurables par club (section 9.8) : chaque règle a une clé, un type, une valeur par défaut documentée, un club. F01 crée le mécanisme ; chaque feature y ajoute ses règles avec leur défaut. Aucune règle de 9.8 n'est codée en dur. — 9.8, 6.3, `CLAUDE.md` §3.
32. Les paramètres se modifient par le gérant seul, et chaque modification est auditée. — ADM-01 (acteur GER), SEC-04.

### Exports

33. Toute liste est exportable en CSV et en Excel. Exporter est une permission distincte de consulter, par domaine ; par défaut accordée à l'administrateur, au gérant et à l'administratif, pas au coach (benchmark B7). Les colonnes sensibles sont celles de SEC-03 — santé, CIN des parents et des personnes autorisées, pièces — et n'entrent dans aucun export en R1, quel que soit le rôle. Si un besoin apparaît, une permission dédiée sera cadrée avec F03. — INT-03, SEC-03 (tranché le 2026-09-20).
34. Chaque export est journalisé : qui, quelle liste, quels filtres, combien de lignes, quand. — INT-03 (« export journalisé »), SEC-04.

### Langues et formats

35. Aucun libellé en dur. Le backend renvoie des codes ; les messages sont résolus par langue, FR livré, arabe et anglais en R8 (PLT-08). Chaque utilisateur porte une langue, `fr` par défaut ; seule `fr` est proposée à l'écran jusqu'à R8. — PLT-08, section 5 (Langues), `CLAUDE.md` §3, décision 0023 ; tranché le 2026-09-20 (Q10).
36. Les téléphones sont stockés au format E.164, indicatif +212 par défaut. F01 fournit la validation ; elle s'applique dès le téléphone de l'utilisateur. — Section 5 (Téléphones).

### Invariants techniques posés par F01

37. Montants en centimes avec devise (type `Money`), dates en UTC, suppression logique, identifiants UUID v7, colonnes d'audit sur chaque table (`club_id`, créé et modifié par et quand, `deleted_at`). — 9.6, `CLAUDE.md` §3, décision 0024.
38. Connecteurs de messagerie et de paiement définis par une interface unique, avec une implémentation vide en R1 ; aucun prestataire codé en dur. — INT-01, INT-02, `CLAUDE.md` §3, décisions 0011 et 0024.

## Critères d'acceptation

Clubs et personnes fictifs. « Club A » et « Club B » sont deux clubs distincts ; « la gérante » et « l'agent d'accueil » désignent des comptes de test.

| # | Règle | Situation de départ | Action | Résultat attendu |
| --- | --- | --- | --- | --- |
| C1 | 1 | Club A et Club B ont chacun un utilisateur et un paramètre modifié | La gérante de A liste les utilisateurs, puis lit les paramètres | Elle ne voit que ceux de A ; toute tentative d'accès direct à un identifiant de B renvoie « introuvable », jamais « interdit » (ne pas révéler l'existence) |
| C2 | 1 | Une requête porte un `club_id` en paramètre ou en corps | Envoi de la requête | Le paramètre est ignoré ; le club appliqué est celui du jeton |
| C3 | 2 | Création d'un club | — | Un site par défaut existe, portant le nom du club ; toute donnée rattachée à un lieu exige un site |
| C4 | 3 | Un utilisateur est rattaché à A comme coach et à B comme gérant | Il se connecte sur A | Ses permissions sont celles d'un coach de A ; rien de B n'est accessible |
| C5 | 4, 7 | Compte créé avec un mot de passe | Connexion avec le bon mot de passe, puis avec un mauvais | Succès puis échec ; la base ne contient qu'un haché Argon2 ; aucun mot de passe dans les logs |
| C5b | 7 | Création d'un compte | Mot de passe de 11 caractères, puis de 12 | Refus avec un code stable, puis acceptation ; aucune exigence de caractère spécial |
| C6 | 5 | Second facteur activé sur un compte administratif | Connexion avec mot de passe seul | Refus tant que le code n'est pas fourni |
| C6b | 5 | Compte gérant créé, second facteur non encore activé | Première connexion | Le seul écran accessible est l'activation du second facteur ; rien d'autre avant |
| C6c | 5 | Gérant ayant perdu son téléphone | L'administrateur réinitialise son second facteur | Réinitialisation effective, entrée d'audit avec l'auteur et la cible |
| C6d | 5 | Gérant connecté avec code, « se souvenir de cet appareil » coché, durée à 30 jours | Reconnexion le lendemain sur le même navigateur, puis 31 jours plus tard, puis après révocation de l'appareil par l'administrateur | Pas de code le lendemain ; code exigé à 31 jours ; code exigé après révocation ; chaque révocation auditée |
| C6e | 5 | Second facteur activé, codes de secours remis | Connexion avec un code de secours, puis réutilisation du même code | Succès, puis refus ; l'usage est audité ; l'utilisateur peut régénérer la série, ce qui invalide l'ancienne |
| C7 | 6 | Une famille avec un enfant existe (F02) | Recherche d'un compte au nom de l'enfant | Aucun compte n'existe et aucun ne peut être créé pour un adhérent mineur |
| C8 | 8 | Utilisateur connecté | Déconnexion, puis réutilisation de l'ancien jeton de rafraîchissement | Refus |
| C8b | 8 | Utilisateur connecté sur deux appareils | L'administrateur ferme ses sessions sans le désactiver | Les deux jetons sont révoqués ; l'utilisateur peut se reconnecter ; entrée d'audit |
| C8c | 8 | Navigateur connecté | On inspecte le jeton de renouvellement depuis la page, puis on renouvelle avec le seul cookie | Illisible par un script (`HttpOnly`, `SameSite=Strict`, chemin borné) ; le renouvellement aboutit sans corps ; sans cookie ni corps, refus (décision 0030) |
| C9 | 8, 13 | Utilisateur connecté | La gérante le désactive | Ses jetons sont révoqués ; il ne peut plus se connecter ; sa ligne existe toujours, marquée désactivée |
| C10 | 9 | Compte existant | Trois connexions échouées | Trois entrées d'audit « connexion échouée » avec l'identifiant tenté, sans le mot de passe |
| C10b | 9 | Compte existant | Cinq échecs consécutifs, puis le bon mot de passe | Refus pendant 15 minutes même avec le bon mot de passe ; entrée d'audit « compte verrouillé » ; succès après le délai |
| C11 | 10, 11 | Compte administratif | Il tente de modifier les paramètres du club par l'API, sans passer par l'écran | Refus au niveau service, entrée d'audit « accès refusé » |
| C11a | 11a | Compte gérant, permissions par défaut | L'administrateur lui retire `club.settings.modifier`, puis le gérant tente de changer le fuseau ; l'administrateur lui rend le droit | Refus après retrait, succès après restitution ; la surcharge apparaît sur la fiche de l'utilisateur ; les deux changements sont audités |
| C11b | 11 | Un point d'entrée de l'API est ajouté sans permission déclarée (test) | Exécution des tests d'architecture | Échec du build, avec le nom du point d'entrée |
| C11c | 11 | Permission paramétrée déclarée en test avec un plafond de 100 | Action à 80, puis à 120, par un utilisateur qui la détient | Succès, puis refus avec un code stable |
| C12 | 12 | Compte administratif, puis compte gérant sans délégation | Chacun tente de créer un utilisateur | Refus pour les deux ; l'administrateur le peut ; après délégation, le gérant le peut |
| C12a | 12 | Club avec un seul administrateur | Tentative de le désactiver, puis de le rétrograder en gérant | Deux refus avec un code stable ; possible dès qu'un second administrateur existe |
| C12b | 10 | Compte administrateur | Un autre administrateur tente de lui retirer une permission | Refus : les droits de l'administrateur ne se retirent pas |
| C13 | 14 | Un fichier privé est déposé | Lecture du fichier sur le support de stockage, hors application | Contenu illisible (chiffré) ; l'application le restitue en clair à un rôle autorisé |
| C14 | 14, 18 | Un fichier privé est déposé | Un utilisateur autorisé l'ouvre | Une entrée d'audit « accès fichier » avec l'utilisateur, le fichier, l'heure |
| C15 | 15 | Un utilisateur porte un téléphone | Toute opération le concernant | Le téléphone n'apparaît dans aucun log technique |
| C16 | 16 | Paramètre « nom du club » = « Club A » | La gérante le change en « Club A Sport » | Entrée d'audit : gérante, `club.settings.updated`, avant « Club A », après « Club A Sport », horodatage UTC |
| C16b | 16 | Une règle automatique de test agit sur une donnée | — | Entrée d'audit dont l'auteur est « système » avec l'identifiant de la règle, jamais un utilisateur |
| C16c | 16 | Un utilisateur se connecte | — | Entrée d'audit dont l'auteur est cet utilisateur, avec son adresse, et non « système » : au moment où la connexion réussit, aucun jeton n'est encore posé (décision 0030) |
| C17 | 17 | Une entrée d'audit existe | Tentative d'`UPDATE` puis de `DELETE` sur la table avec l'utilisateur de base de l'application | Les deux échouent |
| C18 | 18 | — | Chaque action de la liste de la règle 18, une fois | Une entrée d'audit par action, avec les champs de la règle 16 |
| C19 | 19 | Comptes administrateur, gérant et administratif | Chacun ouvre le journal | L'administrateur et le gérant le lisent ; l'administratif est refusé |
| C19b | 19 | Journal contenant des entrées de trois utilisateurs sur deux semaines | Filtre sur un utilisateur, une semaine et le type « paramètres », puis clic sur une entrée | Seules les entrées correspondantes s'affichent ; le clic ouvre le paramètre concerné |
| C20 | 20, 22 | Événement `user.created` publié dans une transaction qui échoue ensuite | — | Aucun événement consommé, aucune entrée d'audit |
| C21 | 20 | Un effet externe est en outbox et son traitement échoue une première fois | Rejeu | L'effet est traité une fois et une seule |
| C22 | 21 | Un abonné externe (outbox, de test) est rendu défaillant | Modification d'un paramètre | Le paramètre est bien modifié et audité ; l'échec de l'abonné externe est tracé et rejoué |
| C22b | 17, 21 | L'abonné audit est rendu défaillant en test | Modification d'un paramètre | L'action échoue entièrement : aucun paramètre modifié, aucune entrée partielle (M3) |
| C23 | 23 | Un fichier privé existe, durée des liens à 15 minutes | Obtention d'un lien, attente de 16 minutes, ouverture | Le lien est refusé après expiration ; un lien réémis fonctionne |
| C24 | 23 | Un lien valide obtenu par la gérante de A | Un utilisateur de B l'utilise | Refus |
| C25 | 24 | Valeurs par défaut du club | Dépôt d'un PDF de 12 Mo, puis d'un fichier `.exe` de 1 Mo, puis d'un PNG de 2 Mo | Deux refus avec un code d'erreur stable, puis acceptation |
| C26 | 25 | Un fichier existe | Suppression | Le fichier n'est plus servi ; sa ligne reste, marquée supprimée |
| C27 | 26, 27 | Club sans logo | La gérante dépose un logo | Le logo est un fichier privé du club, affiché dans le backoffice ; le même test passe sur l'implémentation disque et sur l'implémentation mémoire sans changement des appels, ce qui garantit qu'une implémentation S3 s'ajoutera sans refonte |
| C28 | 28 | Club sans identité | La gérante renseigne nom, adresse, téléphone, courriel, forme juridique, ICE, IF, RC | Les valeurs sont enregistrées ; le téléphone est normalisé en E.164 ; un ICE mal formé est refusé |
| C29 | 29 | Club au fuseau `Africa/Casablanca` | Une date est enregistrée à 10 h 00 heure locale | Stockée en UTC ; restituée à 10 h 00 dans le fuseau du club |
| C30 | 30 | Paramètres de numérotation | La gérante fixe un préfixe de reçu et l'exercice de départ | Enregistré et audité ; aucun numéro n'est encore émis (F09) |
| C31 | 31 | Registre des règles | Une feature de test déclare une règle avec une valeur par défaut | Sans saisie, la valeur par défaut est lue ; après saisie par la gérante, la valeur du club est lue ; le changement est audité |
| C32 | 32 | Compte administratif | Il tente de changer le fuseau du club | Refus |
| C33 | 33 | Liste des utilisateurs de A | La gérante exporte en CSV puis en Excel | Deux fichiers avec les mêmes lignes, en-têtes traduits en FR, aucune ligne de B |
| C33b | 33 | Compte coach pouvant consulter une liste de test | Il tente de l'exporter | Refus : consulter n'emporte pas exporter |
| C34 | 33 | Une liste comportant une colonne marquée sensible (test) | Export par la gérante | La colonne est absente du fichier ; aucun rôle ne peut l'inclure |
| C35 | 34 | Export effectué | Lecture du journal | Entrée : utilisateur, liste, filtres, nombre de lignes, horodatage |
| C36 | 35 | Une erreur de validation survient | Réponse de l'API | Elle porte un code stable et un message en FR ; aucun libellé n'est écrit en dur dans le code |
| C37 | 36 | Saisie de « 06 12 34 56 78 » | Enregistrement | Stocké « +212612345678 » ; « +33 6 12 34 56 78 » est accepté ; « 1234 » est refusé |
| C38 | 37 | Toute table créée par F01 | Inspection du schéma | `club_id` non nul indexé, colonnes d'audit, `deleted_at`, identifiants UUID |
| C39 | 38 | Aucun prestataire configuré | Un événement qui déclencherait un message | L'implémentation vide reçoit l'appel, l'action métier réussit |

Les critères C1, C11, C11b, C14, C16 et C17 constituent le test d'isolation, le test de permissions et le test d'audit exigés par `CLAUDE.md` §6 ; leur mécanique est réutilisable par toutes les features suivantes.

## Benchmark

Étape 2, réalisée le 2026-09-20.

### Complétude

Vérification par rapport au cahier des charges, pas au marché.

| Identifiant | Règles | Critères |
| --- | --- | --- |
| PLT-01 | 1, 2, 3 | C1 à C4 |
| PLT-04 | 20, 21, 22 | C20 à C22 |
| PLT-05 | 23 à 27 | C13, C14, C23 à C27 |
| SEC-01 | 4 à 9 | C5 à C10b |
| SEC-02 | 10 à 13 | C4, C9, C11, C12, C19, C32 |
| SEC-03 | 14, 15 | C13 à C15, C34 |
| SEC-04 | 16 à 19 | C10, C14, C16 à C19, C35 |
| ADM-01 | 28 à 32 | C27 à C32 |
| INT-03 | 33, 34 | C33 à C35 |

Sections du cahier honorées : 5 (règles 6, 14, 27, 29, 35, 36), 6.3 (20, 21), 9.6 (37), 9.7 (16), 9.8 (31). Aucun cas d'utilisation de la section 8 n'est rattaché à F01.

Un manque relevé et corrigé dans « Hors périmètre » : PLT-05 mentionne « les PDF générés » ; F01 les stocke, mais leur génération appartient aux features qui les produisent (fiche d'inscription F03, reçu F09).

### Benchmark

Borné au socle : rôles et permissions du staff, second facteur, journal d'audit, sessions, exports, paramètres. Cinq applications, sources consultées le 2026-09-20. Les pages d'aide d'iClassPro et de Glofox ont refusé l'accès direct ; leurs lignes s'appuient sur les extraits de recherche, marqués « n.v. » (non vérifié sur la page).

| Sujet | Jackrabbit Class | iClassPro | Gymdesk | Glofox | GymMaster | Clubify (fiche) |
| --- | --- | --- | --- | --- | --- | --- |
| Permissions | Par utilisateur, cases à cocher, modèles de rôle à cloner ; mise en garde sur les suppressions et les finances | Groupes d'utilisateurs avec permissions fines ; « Admin Access » total (n.v.) | Par utilisateur, par catégories ; seul le propriétaire modifie le staff | Quatre rôles fixes : Super Admin, Admin, Trainer, Receptionist (n.v.) | Niveaux d'accès par rôle | Cinq rôles, permissions fines par feature (règles 10, 11) |
| Second facteur | Non documenté ; SSO Google/Facebook | Application TOTP, exigible par portail (n.v.) | Non documenté | Non documenté | Google Authenticator, par utilisateur, demandé sur nouvel appareil ou après un délai ; récupération non documentée | TOTP, obligatoire gérant (règle 5, 0027) |
| Journal d'audit | Journal d'activité : « toute l'activité n'est pas journalisée », conservé « un temps limité », plus ancien sur sauvegardes via le support ; rapport de recherche par utilisateur avec liens vers famille et transaction | Journal complet : inscriptions, transactions, données famille, actions du staff, des familles et des automatismes ; réservé aux admins (n.v.) | Non documenté | Non documenté | Journal des passages (contrôle d'accès) | Ajout seul, avant/après, garanti en base (règles 16, 17) |
| Suppressions et clôture | Suppression de transactions possible par permission ; « impossible de clôturer les livres » | — | — | — | — | Aucune suppression ; clôture de période en R3 (CPT-04) |
| Sessions | « User ID Login Status » : voir qui est connecté | — | — | — | Nouvelle demande de code après un délai | Jeton court, rafraîchissement révocable (règle 8) |
| Exports | Rapports soumis à permission | — | Aucune permission d'export distincte | Rapports réservés aux Admin (n.v.) | — | Export dans les limites de consultation, journalisé, colonnes sensibles exclues (33, 34) |
| Multi-site | Restrictions par lieu et catégorie | Portail entreprise multi-sites (n.v.) | Vue franchise | Multi-sites | — | Identifiant de site, écrans écartés (PLT-03) |

Écarts relevés, avec l'intérêt pour le club pilote et la recommandation. La colonne « Décision » est remplie par Omar.

| # | Écart observé | Où | Intérêt pour le pilote | Recommandation | Décision |
| --- | --- | --- | --- | --- | --- |
| B1 | Le journal attribue aussi les actions aux familles (portail) et aux automatismes | iClassPro | Fort : les relances automatiques (R3) et le portail parent (R8) touchent l'argent et les dossiers ; sans auteur « système » ou « parent », le journal aura des trous | Intégrer : la règle 16 précise que l'auteur est un utilisateur, un parent (R8) ou le système, avec la règle automatique identifiée | Intégré (règle 16, C16b) |
| B2 | Journal conservé « un temps limité », le reste via le support | Jackrabbit | Fort, en négatif : c'est exactement ce qu'il ne faut pas faire pour de l'argent encaissé en espèces | Intégrer : la règle 17 précise que le journal n'est jamais purgé en R1 ; toute durée relève de SEC-06 (R4) et de l'avis du comptable | Intégré (règle 17) |
| B3 | Écran de recherche dans le journal : par utilisateur, période, type d'action, avec lien vers l'entité concernée | Jackrabbit | Fort : un journal sans recherche ne sert pas au gérant qui cherche l'origine d'un écart de caisse (10.I) | Intégrer : règle 19 complétée, critère C19b | Intégré |
| B4 | Second facteur demandé seulement sur un nouvel appareil ou après un délai | GymMaster | Fort pour l'adoption : le gérant se connecte chaque jour sur le même PC ; un code à chaque connexion finirait contourné | Intégrer : « se souvenir de cet appareil » 30 jours, révocable, audité ; règle 5, critère C6d | Intégré |
| B5 | Récupération en cas de téléphone perdu non documentée chez les cinq | Tous | Fort : si l'unique gérant perd son téléphone, la règle 5 (réinitialisation par un gérant) ne suffit pas | Intégrer : codes de secours à usage unique remis à l'activation ; règle 5, critère C6e. Source : pratique standard, aucun des cinq ne la documente | Intégré |
| B6 | Voir les sessions ouvertes d'un utilisateur et les fermer sans le désactiver | Jackrabbit | Moyen : utile après un vol de téléphone ou un départ ; C9 ne couvre que la désactivation | Intégrer : règle 8 complétée, critère C8b | Intégré |
| B7 | Permission d'export distincte de la consultation | Glofox (rapports Admin seulement), par contraste avec Gymdesk | Moyen à fort : un export est un fichier qui quitte l'application (voir Q9) ; consulter une liste et l'emporter ne sont pas le même risque | Intégrer : permission « exporter » par domaine, distincte de « consulter » ; gérant et administratif par défaut, coach non ; règle 33, critère C33b | Intégré |
| B8 | Permissions ajustables utilisateur par utilisateur, à partir d'un modèle de rôle | Jackrabbit, Gymdesk, iClassPro | Faible en R1 : trois ou quatre comptes, des rôles fixes suffisent ; utile quand un administratif tient aussi la caisse et un autre non | Backlog : nouvel identifiant SEC-07 « Permissions ajustées par utilisateur », R9 ; le modèle de F01 attache les permissions au rôle sans empêcher une surcharge par utilisateur plus tard | **Intégré dès F01**, avec un rôle Administrateur au-dessus du gérant qui octroie les droits ; granularité fixée par décision-sensible et par écran. Décision 0028 (règles 10, 11, 11a, 12 ; C11a, C11c, C12, C12a, C12b) |
| B9 | Connexion par compte Google ou Facebook | Jackrabbit | Nul : dépendance à un tiers hors Maroc pour un accès à des données d'enfants ; incompatible avec 0018 et avec le second facteur maîtrisé | Écarter | Écarté |
| B10 | Restrictions d'accès par lieu ou par catégorie d'activité | Jackrabbit | Nul en R1 : un site, une équipe | Écarter, déjà couvert par le retrait de PLT-03 | Écarté |
| B11 | Rôles fixes sans aucune personnalisation | Glofox | — | Écarter comme cible : c'est le point de départ de R1, mais le modèle reste ouvert (B8) | Écarté |

Ce que le benchmark confirme sans rien changer : aucune des cinq applications ne va aussi loin que la fiche sur l'audit (ajout seul garanti en base, avant/après), sur l'interdiction de supprimer une transaction, ni sur le traitement des colonnes sensibles à l'export. Ces choix viennent du cahier (9.4 point 12, 9.6, SEC-03) et du contexte marocain (espèces, CNDP), pas du marché.

Sources : [Jackrabbit — permissions](https://help.jackrabbitclass.com/help/user-id-permissions), [Jackrabbit — guide de protection du compte](https://help.jackrabbitclass.com/help/guidelines-user-permissions), [Jackrabbit — suivi d'activité](https://help.jackrabbitclass.com/help/user-id-activity-tracking), [Jackrabbit — contrôles comptables](https://help.jackrabbitclass.com/help/accounting-control-in-jackrabbit), [Jackrabbit — statut de connexion](https://help.jackrabbitclass.com/help/user-id-status), [iClassPro — permissions et groupes](https://support.iclasspro.com/hc/en-us/articles/218569918-How-Do-I-Configure-Staff-Permissions-and-User-Groups), [iClassPro — second facteur](https://support.iclasspro.com/hc/en-us/articles/4416122785687-What-is-Two-Factor-Authentication), [iClassPro — journal d'audit](https://support.iclasspro.com/hc/en-us/articles/218569928-What-is-the-Audit-Log), [Gymdesk — staff](https://docs.gymdesk.com/en/help/docs/managers), [Glofox — rôles et permissions par défaut](https://support.glofox.com/hc/en-us/articles/52585050344852-Staff-Roles-and-Default-Permissions), [GymMaster — sécurité du staff](https://www.gymmaster.com/user-manual/manual_staffmembers_add_staff_details_security/), [GymMaster — second facteur](https://www.gymmaster.com/blog/two-factor-authentication/).

## Hors périmètre

- Code à usage unique des parents et portail parent : R8 (APP-01).
- Bascule entre clubs pour un même utilisateur : R9 (PLT-02). Écrans multi-sites : écartés (PLT-03).
- Assistant de démarrage d'un club et champs personnalisés : R9 (ADM-02, ADM-03).
- Droits des personnes, export et purge d'une famille, durées de conservation : R4 (SEC-05, SEC-06).
- Régime fiscal, taux de taxe, mentions portées sur la facture : F08 (CPT-02). Séries continues de factures et de reçus : F08, F09.
- Notifications, modèles, connecteurs réels de messagerie : R3. Connecteur de paiement réel : R8.
- Champs santé et CIN eux-mêmes : F02, F03. F01 livre le mécanisme, pas les champs.
- Toute entité métier : famille, adhérent, activité, inscription, facture.
- Génération des PDF (fiche d'inscription, reçu, facture) : F03, F09, F08. F01 les stocke (PLT-05), ne les produit pas.
- Écrans autres que : connexion, paramètres du club, utilisateurs et rôles, journal d'audit, bouton d'export.

## Dépendances

- Aucune feature.
- Décisions 0024 (stack backend) et 0025 (stack frontend), qui fixent les moyens de chaque règle.
- Préalables non logiciels, avant l'étape 3 : choix d'hébergement et de localisation des données (0018), qui détermine le support de stockage des fichiers (règle 27).
- Avant la mise en service de R1, pas de F01 : déclaration CNDP déposée (0018).

## Plan d'implémentation

Étape 3, rédigée le 2026-09-20. Ordre imposé par le processus : contrat, backend, frontend. F01 étant la première feature, l'étape 0 crée les deux squelettes techniques actés par 0024 et 0025.

### Étape 0 — Squelettes

Backend (`backend/`) : projet Maven mono-module, Spring Boot 4.0.x, Java 21 ; profils `dev`, `test`, `demo`, `prod` ; Docker Compose PostgreSQL pour `dev` ; Flyway avec une migration `V0` vide ; classe de base de test Testcontainers ; règles ArchUnit de `backend/CLAUDE.md` ; `openapi-generator-maven-plugin` branché sur `contracts/openapi.yaml` ; springdoc en `dev` ; logs structurés avec identifiant de corrélation ; Actuator `health` et `info`.

Frontend (`frontend/`) : workspace Angular, projets `backoffice`, `ui`, `api-client` ; Tailwind ; PrimeNG en mode thémable ; Transloco avec `fr` ; Storybook ; Playwright ; génération d'`api-client` depuis le même contrat ; budget de bundle dans `angular.json`.

Résultat vérifiable : `mvn verify` et `ng test` passent sur un projet vide ; l'API répond `/actuator/health` ; le backoffice affiche une page vide traduite.

### Étape 1 — Contrat d'API

Fichiers dans `contracts/` : `openapi.yaml` (index), `common.yaml` (`ProblemDetail` avec `code`, pagination, `Money`), `auth.yaml`, `club.yaml`, `users.yaml`, `audit.yaml`, `files.yaml`, `exports.yaml`. Préfixe `/api/v1`. Détail des points d'entrée dans « Impact sur le contrat d'API ».

### Étape 2 — Socle technique commun (`ma.clubify.common`, `security`, `config`, `exception`)

Dans cet ordre, chaque brique avec son test :

1. `BaseEntity` : UUID v7, `club_id`, colonnes d'audit, `deleted_at`, `@SoftDelete`, `@Version` optionnel. `Money` embeddable. `Clock` injecté.
2. Isolation : `TenantContext` alimenté par le filtre de sécurité ; `@TenantId` sur `club_id` ; `CurrentTenantIdentifierResolver` ; migration Flyway activant la Row-Level Security sur chaque table métier avec `SET LOCAL app.club_id` par transaction. Classe de test `IsolationTest` réutilisable : deux clubs, une requête, aucune fuite (C1, C2).
3. Événements : `DomainEvent`, publication par `ApplicationEventPublisher` dans le service ; abonnés internes en `BEFORE_COMMIT` (audit) ; table `outbox_event` écrite dans la transaction pour les effets externes, relecteur planifié avec tentatives et journal d'erreur (C20, C21, C22).
4. Audit : `AuditLog` en ajout seul ; abonné aux événements ; migration créant le rôle de base `clubify_app` sans `UPDATE` ni `DELETE` sur `audit_log` ; `AuditTest` réutilisable (C16, C17).
5. Permissions : registre en code (`PermissionRegistry`, codes `domaine.objet.action`, paramètre optionnel), jeux par rôle déclarés par feature, surcharges en base ; intégration `@PreAuthorize` par un évaluateur maison ; règle ArchUnit « tout point d'entrée porte une permission » (C11, C11a, C11b, C11c).
6. Paramètres : `SettingRegistry` en code (clé, type, portée club ou plateforme, défaut), valeurs de club en base (`club_setting`, JSONB), lecture typée avec repli sur le défaut, changement audité (C31).
7. Données sensibles : `EncryptedStringConverter` AES-256-GCM, clé par variable d'environnement avec identifiant de clé pour rotation ; chiffrement des fichiers au dépôt et déchiffrement à la lecture ; masquage dans les logs (C13, C15).
8. i18n et erreurs : `MessageSource`, `messages_fr.properties`, `GlobalExceptionHandler` en `ProblemDetail` avec `code` (C36).
9. Fichiers : interface `FileStorage`, implémentations disque local (`dev`, `prod` provisoire) et mémoire (`test`), liens signés à durée limitée (C23 à C27). L'implémentation S3 compatible arrive avec le choix d'hébergement (0018), hors F01.
10. Connecteurs : interfaces `MessagingProvider` et `PaymentProvider`, implémentation `noop` (C39).
11. Validation : téléphone E.164 avec +212 par défaut (C37).

### Étape 3 — Domaine plateforme (`ma.clubify.platform`)

1. Migrations Flyway : `club`, `site`, `user_account`, `membership`, `user_permission_override`, `refresh_token`, `trusted_device`, `recovery_code`, `club_setting`, `stored_file`, `audit_log`, `outbox_event`.
2. Authentification : connexion, second facteur TOTP (RFC 6238, implémenté avec le JDK), défi en deux temps, appareil de confiance, codes de secours, verrouillage, jetons JWT courts et rafraîchissement révocable, déconnexion, sessions par utilisateur (C5 à C10b).
3. Club et site : lecture et modification de l'identité, logo, paramètres régionaux, numérotation, règles configurables (C3, C27 à C32).
4. Utilisateurs : liste, création, modification, désactivation, rôle, surcharges de permissions, réinitialisation du second facteur, fermeture des sessions ; protection du dernier administrateur (C4, C9, C12 à C12b).
5. Journal : consultation filtrée, lien vers l'entité (C18, C19, C19b).
6. Exports : service générique CSV et Excel à partir d'une définition de colonnes avec drapeau « sensible » ; première liste : les utilisateurs ; permission `exporter` par domaine ; entrée d'audit (C33 à C35).
7. Amorçage : commande `--seed-club` créant un club, son site, son premier administrateur et le club de test (C3, Q1).

### Étape 4 — Frontend (`frontend/projects/ui`, `backoffice`)

1. `ui` : jetons (couleurs Clubify et marque par club, typographie, espacements, mouvement), enveloppes PrimeNG pour bouton, champ, sélecteur, tableau, dialogue, toast, onglets ; pipes `money` et `clubDate` ; chaque composant avec sa story FR et RTL.
2. `backoffice/core` : intercepteur d'authentification et de langue, garde par permission, `TenantContext`, gestion du défi second facteur.
3. Écrans : connexion ; code second facteur avec « se souvenir de cet appareil » ; activation du second facteur (QR, codes de secours) imposée à l'administrateur et au gérant ; profil (mon second facteur, mes appareils) ; paramètres du club en quatre onglets (identité et logo, régional, numérotation, règles) ; utilisateurs (liste avec export, fiche avec rôle, permissions et surcharges, sessions, second facteur) ; journal d'audit (filtres, détail avant/après, lien).
4. Playwright : parcours « première connexion de l'administrateur jusqu'au journal » ; Storybook : validation RTL des composants de `ui` avant la première livraison d'écran.

### Étape 5 — Livraison

Contrat, `docs/modele-donnees.md` (entités ci-dessous), fiche, tests verts, `docs/suivi-features.xlsx`. Mise à jour de `backend/CLAUDE.md` et `frontend/CLAUDE.md` : rubrique « Commandes ».

## Impacts et régressions

Étape 3. Aucune feature livrée avant F01 : **aucune régression possible**. Mais F01 pose chaque invariant de la section 9.6 ; tout choix ci-dessous est irréversible sans refonte. Niveau global : **majeur**, par nature.

| Critère du processus | Niveau | Pourquoi |
| --- | --- | --- |
| Migration de données existantes | Maîtrisé | Aucune donnée |
| Contrat d'API déjà consommé | Maîtrisé | Premier contrat |
| Calcul d'argent | Maîtrisé | Aucun montant en F01 ; `Money` est défini, pas utilisé |
| Invariant du `CLAUDE.md` | **Majeur** | F01 les pose tous ; deux exceptions explicites (M1, M2) |
| Feature déjà livrée | Maîtrisé | Aucune |
| Nouvelle dépendance | **Majeur** | Quatre demandées (M6) |

Points majeurs, validés par Omar le 2026-09-20 (décision 0029), sauf le client S3 de M6, non retenu pour F01 :

| # | Point | Choix proposé | Alternative |
| --- | --- | --- | --- |
| M1 | `User` sans `club_id` | L'utilisateur est global (PLT-02 : un même compte dans plusieurs clubs) ; l'appartenance `Membership` porte `club_id` et le rôle. Exception documentée à « `club_id` sur chaque donnée » ; toute donnée métier reste rattachée au club | Un utilisateur par club, à fusionner en R9 : migration de données garantie plus tard |
| M2 | `audit_log.club_id` nullable pour les seuls événements d'authentification | Une connexion échouée sur un identifiant inconnu n'a pas de club ; contrainte `CHECK` : `club_id` obligatoire pour tout autre type d'action | Pseudo-club « plateforme » : complique l'isolation |
| M3 | Audit synchrone et bloquant | L'abonné audit s'exécute dans la transaction de l'action (`BEFORE_COMMIT`) : si l'audit échoue, l'action échoue. Une action sur l'argent sans trace ne doit pas exister. C'est l'unique exception à la règle 21 ; les abonnés externes (messages, exports) passent par l'outbox et ne bloquent jamais. **C22 est réécrit** : l'abonné rendu défaillant est un abonné outbox, pas l'audit | Audit dérivé de l'outbox, asynchrone, rejouable : cohérent avec la règle 21 mais fenêtre où l'action est validée sans ligne d'audit |
| M4 | Rôles, permissions et paramètres définis en code, surcharges et valeurs en base | Le catalogue est versionné avec les features qui le déclarent ; la base ne porte que ce qui varie par club ou par utilisateur | Tout en base : administrable sans livraison, mais rien ne garantit qu'une feature déclare ses permissions |
| M5 | Chiffrement applicatif des fichiers et des champs sensibles | AES-256-GCM, clé par environnement hors dépôt, identifiant de clé stocké pour rotation ; le stockage ne voit jamais le clair (C13) | Chiffrement côté stockage seulement : le clair transite et dépend de l'hébergeur (0018) |
| M6 | Nouvelles dépendances | Validées : Bouncy Castle (Argon2, exigé par Spring Security), `fastexcel` (Excel), `angularx-qrcode` (QR du second facteur côté frontend). TOTP implémenté avec le JDK, sans dépendance. **Client S3 non retenu pour F01** : F01 livre l'interface `FileStorage` avec l'implémentation disque et une implémentation mémoire pour les tests ; l'implémentation S3 et sa dépendance viendront avec le choix d'hébergement (0018) | BCrypt intégré à la place d'Argon2 ; CSV seul en R1 ; QR généré côté backend avec ZXing |
| M7 | Row-Level Security PostgreSQL | Activée dès F01 : `SET LOCAL app.club_id` par transaction, rôle de base non superutilisateur ; seconde ligne derrière `@TenantId` | Reporter : l'isolation ne tiendrait que par l'application |
| M8 | Export synchrone | Génération à la demande, réponse directe ; listes de R1 petites | File d'attente : inutile avant des milliers de lignes |

Régressions : aucune feature livrée. Les classes de test `IsolationTest`, `AuditTest` et `PermissionTest` créées ici deviennent le harnais de non-régression de toutes les features suivantes (`CLAUDE.md` §6).

## Impact sur le modèle de données

Étape 3. Entités créées, toutes avec `club_id` sauf mention, colonnes d'audit et `deleted_at`. À reporter dans `docs/modele-donnees.md` à la livraison.

| Entité | Rôle | Points notables |
| --- | --- | --- |
| `Club` | Le tenant | Identité, forme juridique, ICE, IF, RC, logo (→ `StoredFile`), fuseau, devise, langue, statut. Pas de `club_id` sur elle-même |
| `Site` | Lieu d'exploitation | Un site par défaut créé avec le club |
| `User` | Compte d'un membre du staff | **Global, sans `club_id`** (M1) ; courriel unique ; mot de passe haché ; secret TOTP chiffré ; langue ; compteur d'échecs et verrouillage |
| `Membership` | Appartenance d'un utilisateur à un club | `club_id`, rôle (six valeurs), statut ; une par couple utilisateur–club |
| `UserPermissionOverride` | Surcharge d'une permission pour une appartenance | Code, accordée ou retirée, paramètre JSONB, auteur |
| `RefreshToken` | Session | Haché, appareil, expiration, révocation |
| `TrustedDevice` | Appareil de confiance | Identifiant aléatoire, libellé, expiration, révocation |
| `RecoveryCode` | Code de secours | Haché, usage unique |
| `ClubSetting` | Valeur d'une règle configurable | Clé du registre, valeur JSONB ; le défaut vit en code |
| `StoredFile` | Fichier privé | Propriétaire (type, id), nature, clé de stockage, type MIME, taille, empreinte, identifiant de clé de chiffrement |
| `AuditLog` | Journal | Auteur (type : utilisateur, parent, système ; id ; libellé de règle), action, entité, avant, après, motif, horodatage, identifiant de requête ; **ajout seul** ; `club_id` nullable pour l'authentification seulement (M2) |
| `OutboxEvent` | Événement à effet externe | Type, charge JSONB, tentatives, dernière erreur, traité le |

Hors base : `Role` (énumération), `Permission` et `Setting` (registres en code, M4).

## Impact sur le contrat d'API

Étape 3. Tout en `/api/v1`, codes d'erreur stables, aucun identifiant de club dans les requêtes. Fichiers : `contracts/openapi.yaml`, `common.yaml`, `auth.yaml`, `club.yaml`, `users.yaml`, `audit.yaml`, `files.yaml`, `exports.yaml`.

| Domaine | Points d'entrée | Permission |
| --- | --- | --- |
| auth | `POST /auth/login` (courriel, mot de passe → jetons, ou défi second facteur), `POST /auth/mfa/verify` (défi, code, appareil de confiance), `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` | Aucune (pré-authentification) ou authentifié |
| auth, profil | `POST /auth/mfa/setup` (URI otpauth, codes de secours), `POST /auth/mfa/confirm`, `POST /auth/mfa/recovery-codes/regenerate`, `GET /auth/devices`, `DELETE /auth/devices/{id}` | Authentifié, sur soi-même |
| club | `GET /club`, `PUT /club`, `PUT /club/logo`, `GET /club/settings`, `PUT /club/settings`, `GET /club/settings/definitions` | `club.settings.consulter`, `club.settings.modifier` |
| users | `GET /users`, `POST /users`, `GET /users/{id}`, `PUT /users/{id}`, `POST /users/{id}/disable`, `POST /users/{id}/enable`, `PUT /users/{id}/role`, `GET /users/{id}/permissions`, `PUT /users/{id}/permissions`, `POST /users/{id}/sessions/revoke`, `POST /users/{id}/mfa/reset`, `GET /permissions` | `users.consulter`, `users.creer`, `users.modifier`, `users.desactiver`, `users.permissions.modifier`, `users.sessions.fermer`, `users.mfa.reinitialiser` |
| audit | `GET /audit-entries` (auteur, période, action, entité, pagination) | `audit.consulter` |
| files | `POST /files`, `GET /files/{id}/link` (lien signé), `GET /files/dl/{token}` (public, signé, expirant), `DELETE /files/{id}` | `files.deposer`, `files.consulter`, par domaine propriétaire |
| exports | `POST /exports` (liste, filtres, format → fichier) | `<domaine>.exporter` |

## Paramètres configurables par club

Étape 3. Portée « club » = modifiable par l'administrateur ; « plateforme » = fixée par Clubify, hors 9.8.

| Clé | Portée | Défaut | Source |
| --- | --- | --- | --- |
| `club.timezone` | club | `Africa/Casablanca` | Section 5, règle 29 |
| `club.currency` | club | `MAD` | 9.6, règle 29 |
| `club.default_language` | club | `fr` | PLT-08, règle 29 |
| `security.mfa.trusted_device_days` | club | 30 | Règle 5 (B4) |
| `security.password.min_length` | plateforme | 12 | Règle 7 (Q3) |
| `security.lockout.max_attempts` | plateforme | 5 | Règle 9 (Q3) |
| `security.lockout.minutes` | plateforme | 15 | Règle 9 (Q3) |
| `security.access_token.minutes` | plateforme | 15 | Règle 8 |
| `security.refresh_token.days` | plateforme | 30 | Règle 8 |
| `files.max_size_mb` | club | 10 | Règle 24 (Q7) |
| `files.allowed_types` | club | `pdf, jpeg, png` | Règle 24 (Q7) |
| `files.link_ttl_minutes` | club | 15 | Règle 23 (Q7) |
| `billing.invoice_prefix` | club | `F` | Règle 30 ; utilisé par F08 |
| `billing.receipt_prefix` | club | `R` | Règle 30 ; utilisé par F09 |
| `billing.fiscal_year_start_month` | club | 1 | Règle 30 ; exercice comptable à confirmer avec le comptable (question ouverte 3 de 0023) |

Le registre lui-même (règle 31) est le mécanisme que toutes les règles de la section 9.8 utiliseront.

## Points de sécurité et données sensibles

Étape 3.

Permissions introduites (règle 11b), avec leur jeu par défaut :

| Permission | Administrateur | Gérant | Administratif | Coach | Comptable |
| --- | --- | --- | --- | --- | --- |
| `club.settings.consulter` | oui | oui | oui | non | oui |
| `club.settings.modifier` | oui | oui | non | non | non |
| `users.consulter` | oui | oui | non | non | non |
| `users.creer`, `users.modifier`, `users.desactiver` | oui | par délégation | non | non | non |
| `users.permissions.modifier` | oui, non délégable | non | non | non | non |
| `users.sessions.fermer`, `users.mfa.reinitialiser` | oui | non | non | non | non |
| `audit.consulter` | oui | oui | non | non | non |
| `files.deposer`, `files.consulter` (domaine club) | oui | oui | oui | non | non |
| `users.exporter` | oui | oui | oui | non | non |

Le rôle parent n'a aucune permission en F01.

Mesures : hachage Argon2 ; TOTP obligatoire administrateur et gérant ; appareils de confiance et codes de secours ; verrouillage ; jetons courts, rafraîchissement révocable, sessions fermables ; isolation `@TenantId` et Row-Level Security ; journal en ajout seul garanti en base ; chiffrement AES-256-GCM des secrets TOTP et des fichiers, clé hors dépôt avec rotation possible ; liens de fichiers signés et expirants ; colonnes sensibles exclues des exports ; aucune donnée personnelle dans les logs ; `ProblemDetail` sans détail interne ; aucun secret dans un fichier versionné.

## Questions

Toutes tranchées le 2026-09-20. Reste ouvert hors F01 : le lieu d'hébergement (Q6), à valider avec le juriste avant la mise en service de R1.

| # | Question | Ce que je propose, faute de mieux |
| --- | --- | --- |
| Q1 | Qui crée le club pilote et le premier compte gérant, et comment ? | **Tranché** : Nous, par une commande d'amorçage versionnée ; aucun écran de création de club avant R9. |
| Q2 | Second facteur : lequel, et obligatoire pour le gérant ? | **Tranché** : TOTP par application, obligatoire pour le gérant, optionnel pour les autres. Décision 0027. |
| Q3 | Politique de mot de passe et verrouillage : longueur minimale, complexité, nombre d'échecs, durée de blocage. | **Tranché** : 12 caractères minimum, pas de règle de complexité ; verrouillage 15 minutes après 5 échecs. |
| Q4 | Rôles comptable et parent en R1 : on les crée sans aucun utilisateur ? Le comptable du club est externe. | **Tranché** : Les cinq rôles existent dès F01 ; seuls gérant, administratif et coach ont des utilisateurs en R1. |
| Q5 | Qui peut lire le journal d'audit ? | **Tranché** : Le gérant seul en R1 ; le comptable en lecture quand il aura un compte. Complété par 0028 : l'administrateur aussi. |
| Q6 | Hébergement et localisation des données (0018) : décidés avant l'étape 3 de F01 ? Ils fixent le support de stockage des fichiers. | **Tranché sur l'approche** : interface unique, cible S3 compatible, disque local en dev et test. Le lieu d'hébergement (Maroc ou Europe) reste à valider avec le juriste avant la mise en service (0018) ; n'est plus bloquant pour l'étape 3. |
| Q7 | Durée des liens temporaires, types et taille maximale des fichiers. | **Tranché** : 15 minutes ; PDF, JPEG, PNG ; 10 Mo. Le tout paramétrable par club. |
| Q8 | ICE, IF, RC, forme juridique : dans les paramètres du club (F01) ou avec les taxes (F08, CPT-02) ? | **Tranché** : Dans F01, comme identité du club ; F08 y ajoute régime fiscal et taux, et décide de leur affichage sur la facture. |
| Q9 | Export des colonnes sensibles : lesquelles, et qui peut les inclure ? | **Tranché** : la liste de SEC-03 telle quelle (santé, CIN, pièces) ; personne en R1. |
| Q10 | Verrouillage de la langue : FR seule à l'écran en R1, même si le champ « langue » existe ? | **Tranché** : Oui ; le champ existe, seule `fr` est proposée jusqu'à R8. |

## Tests

Étape 4, écrite le 2026-09-20. **64 tests en place, tous rouges** : 7 échecs de règles
d'architecture et 57 erreurs faute de schéma et de contrôleurs. `./mvnw test` le vérifie.

Les tests de F01 s'écrivent **au niveau HTTP, contre le contrat** de `contracts/openapi.yaml`,
jamais contre les services. Ils décrivent donc des parcours réels, échouent aujourd'hui parce
qu'aucun contrôleur n'existe, et passeront à l'étape 5 sans être réécrits.

Trois classes forment le harnais réutilisable exigé par le `CLAUDE.md` §6 : `IsolationApiTest`,
`AuditApiTest` et `UsersApiTest` (permissions). Toute feature suivante les rejoue.

| Critère | Classe de test | Méthode |
| --- | --- | --- |
| C1 | `IsolationApiTest` | `c1_listeCloisonnee`, `c1_pasDeFuiteParLeCodeDErreur` |
| C2 | `IsolationApiTest` | `c2_clubIdDuClientIgnore` |
| C3 | `ClubApiTest` | `c3_siteParDefaut` |
| C4 | `IsolationApiTest` | `c4_rolesParClub` |
| C5 | `AuthApiTest` | `c5_motDePasse` |
| C5b | `AuthApiTest` | `c5b_longueurMinimale` |
| C6 | `AuthApiTest` | `c6_defiSecondFacteur` |
| C6b | `AuthApiTest` | `c6b_enrolementImpose` |
| C6c | `AuthApiTest` | `c6c_reinitialisation` |
| C6d | `AuthApiTest` | `c6d_appareilDeConfiance` |
| C6e | `AuthApiTest` | `c6e_codesDeSecours` |
| C7 | `UsersApiTest` | `c7_aucunCompteEnfant` |
| C8 | `AuthApiTest` | `c8_deconnexion` |
| C8b | `UsersApiTest` | `c8b_fermetureDesSessions` |
| C9 | `AuthApiTest` | `c9_desactivation` |
| C10 | `AuthApiTest` | `c10_echecsJournalises` |
| C10b | `AuthApiTest` | `c10b_verrouillage` |
| C11 | `UsersApiTest` | `c11_permissionCoteService` |
| C11a | `UsersApiTest` | `c11a_surchargeParUtilisateur` |
| C11b | `ArchitectureTest` | `toutPointDEntreePorteUnePermission` |
| C11c | `UsersApiTest` | `c11c_permissionParametree` |
| C12 | `UsersApiTest` | `c12_creationReservee`, `c12_ajustementNonDelegable` |
| C12a | `UsersApiTest` | `c12a_dernierAdministrateurProtege` |
| C12b | `UsersApiTest` | `c12b_droitsAdministrateurNonRetirables` |
| C13 | `FilesApiTest` | `c13_chiffrementAuRepos` |
| C14 | `FilesApiTest` | `c14_accesJournalise` |
| C15 | `PlatformInvariantsTest` | `c15_pasDeDonneePersonnelleDansLesLogs` |
| C16 | `AuditApiTest` | `c16_avantApres` |
| C16b | `AuditApiTest` | `c16b_auteurSysteme` |
| C17 | `AuditApiTest` | `c17_ajoutSeul` |
| C18 | `AuditApiTest` | `c18_couvertureDesActions` |
| C19 | `AuditApiTest` | `c19_lecteursDuJournal` |
| C19b | `AuditApiTest` | `c19b_recherche` |
| C20 | `PlatformInvariantsTest` | `c20_evenementLieALaTransaction` |
| C21 | `PlatformInvariantsTest` | `c21_outboxRejouable` |
| C22 | `PlatformInvariantsTest` | `c22_abonneExterneNonBloquant` |
| C22b | `PlatformInvariantsTest` | `c22b_auditBloquant` |
| C23 | `FilesApiTest` | `c23_lienTemporaire` |
| C24 | `FilesApiTest` | `c24_lienCloisonne` |
| C25 | `FilesApiTest` | `c25_tailleEtType` |
| C26 | `FilesApiTest` | `c26_suppressionLogique` |
| C27 | `FilesApiTest` | `c27_logoDuClub` |
| C28 | `ClubApiTest` | `c28_identite` |
| C29 | `ClubApiTest` | `c29_fuseauHoraire` |
| C30 | `ClubApiTest` | `c30_numerotation` |
| C31 | `ClubApiTest` | `c31_registreDesReglesConfigurables`, `c31_definitions` |
| C32 | `ClubApiTest` | `c32_parametresReservesAuGerant` |
| C33 | `ExportsApiTest` | `c33_csvEtExcel` |
| C33b | `ExportsApiTest` | `c33b_exporterEstUnDroitDistinct` |
| C34 | `ExportsApiTest` | `c34_colonnesSensiblesExclues` |
| C35 | `ExportsApiTest` | `c35_exportJournalise` |
| C36 | `PlatformInvariantsTest` | `c36_erreursTraduites` |
| C37 | `PlatformInvariantsTest` | `c37_telephoneE164` |
| C38 | `PlatformInvariantsTest` | `c38_schemaConforme` |
| C39 | `PlatformInvariantsTest` | `c39_connecteursAbstraits` |

Les 55 critères sont couverts. Six règles d'architecture de `backend/CLAUDE.md` s'ajoutent
dans `ArchitectureTest` sans correspondre à un critère : dépendances entre couches et entre
domaines, Lombok encadré sur les entités, horloge injectée.

Ce que les tests exigent du code de l'étape 5, au-delà des règles de la fiche :
- des fonctions utilitaires réservées aux migrations de test (`current_totp_for_test`,
  `raw_content_for_test`, `replay_outbox_for_test`, `run_system_rule_for_test`, les bascules
  d'échec d'abonné) ; elles ne doivent jamais exister hors du profil `test` ;
- quelques points d'entrée de test (`/test/plafond`, `/test/echec-apres-publication`,
  `/test/effet-externe`, `/test/notifier`), pour éprouver le socle sans attendre une feature
  métier ; même contrainte.

### Frontend

Workspace Angular créé (Angular 21, PrimeNG 21, Tailwind 4, Transloco, Playwright).
**15 tests Vitest : 10 rouges, 5 verts.**

| Sujet | Fichier | État |
| --- | --- | --- |
| Montants en centimes formatés dans la devise du club | `money.pipe.spec.ts` | 5 rouges |
| Instants UTC affichés dans le fuseau du club, dates pures inchangées | `club-date.pipe.spec.ts` | 5 rouges |
| Aucune propriété physique gauche ou droite dans les feuilles de style | `tokens.spec.ts` | vert |
| Jetons sémantiques présents, marque du club par-dessus Clubify | `tokens.spec.ts` | 2 verts |
| Mouvement coupé sur `prefers-reduced-motion`, chiffres tabulaires | `tokens.spec.ts` | 2 verts |
| Première connexion : mot de passe, second facteur imposé, journal | `e2e/premiere-connexion.spec.ts` | rouge |

Les cinq verts ne sont pas un oubli : ce sont des invariants du système de design,
déjà satisfaits par `tokens.css`. Le droite-à-gauche se tient dès le premier jour
(PLT-08) ; le contrôle échouera si un écran introduit un `margin-left`.

Reste à écrire : les stories Storybook, une fois les premiers composants de `ui`
implémentés à l'étape 5.

## Livraison

Étape 5 close le 2026-09-20. **121 tests verts : 66 backend, 55 frontend.**
Parcours vérifié de bout en bout dans un navigateur, contre le backend réel :
connexion, activation du second facteur, paramètres du club, utilisateurs,
journal d'audit filtrable.

### Ce que l'implémentation a tranché

Six points laissés ouverts par la fiche et par la décision 0029 ; tous consignés
dans **la décision 0030** : jeton de renouvellement en cookie `HttpOnly`,
activation du second facteur ouvrant la session, horodatage d'émission à la
milliseconde et adresse dans le jeton, couleurs de marque en règles
configurables, auteur nommé sur une connexion, objets de transfert aux
frontières de service.

### Défauts trouvés à l'usage, et fermés par un test

Trois défauts qu'aucun test ne couvrait, trouvés en parcourant l'application :

| Défaut | Conséquence | Test qui le ferme |
| --- | --- | --- |
| L'écran de second facteur déduisait l'étape de l'existence d'un jeton | Un compte déjà inscrit voyait son secret et ses codes de secours régénérés en silence | `mfa.page.spec.ts` |
| Le journal attribuait les connexions au « système », sans auteur | La colonne que le gérant regarde en premier restait vide (B3) | `AuditApiTest.c16c` |
| Une tolérance d'une seconde sur la borne de révocation | Un jeton survivait à la fermeture de ses sessions | `UsersApiTest.c8b` |

Un quatrième, trouvé à l'amorçage : les écritures hors transaction se
validaient une à une, laissant un club créé sans trace d'audit. La commande
ouvre désormais sa transaction explicitement — la règle 17 tient.

### Reste à faire dans F01

- Stories Storybook des composants de `ui`, support de validation des maquettes
  avec l'accueil et le gérant (`frontend/CLAUDE.md`).
- Parcours Playwright `premiere-connexion`, à brancher sur un backend de test.
- Création d'utilisateur et export depuis l'écran : les boutons sont posés et
  protégés par droit, l'action reste à écrire.

### Questions toujours ouvertes

- Chemin d'amorçage en production, à cadrer avec le choix d'hébergement.
- Dépôt CNDP avant janvier 2027, lieu d'hébergement, fournisseur WhatsApp
  (questions de 0023, hors périmètre de F01).

## Statut

Étape 5 close — 2026-09-20. En attente de validation d'Omar (définition de
« terminé », `CLAUDE.md` section 6).

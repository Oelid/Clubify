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
27. Le stockage des fichiers est écrit derrière une interface unique ; cible : stockage objet compatible S3, disque local en développement et en test. Le lieu d'hébergement (Maroc ou Europe) reste à valider avec le juriste du club avant la mise en service (décision 0018) ; il ne bloque pas l'étape 3. — Section 5 (Hébergement), décision 0018 ; approche tranchée le 2026-09-20 (Q6).

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
| C17 | 17 | Une entrée d'audit existe | Tentative d'`UPDATE` puis de `DELETE` sur la table avec l'utilisateur de base de l'application | Les deux échouent |
| C18 | 18 | — | Chaque action de la liste de la règle 18, une fois | Une entrée d'audit par action, avec les champs de la règle 16 |
| C19 | 19 | Comptes administrateur, gérant et administratif | Chacun ouvre le journal | L'administrateur et le gérant le lisent ; l'administratif est refusé |
| C19b | 19 | Journal contenant des entrées de trois utilisateurs sur deux semaines | Filtre sur un utilisateur, une semaine et le type « paramètres », puis clic sur une entrée | Seules les entrées correspondantes s'affichent ; le clic ouvre le paramètre concerné |
| C20 | 20, 22 | Événement `user.created` publié dans une transaction qui échoue ensuite | — | Aucun événement consommé, aucune entrée d'audit |
| C21 | 20 | Un effet externe est en outbox et son traitement échoue une première fois | Rejeu | L'effet est traité une fois et une seule |
| C22 | 21 | L'abonné audit est rendu défaillant en test | Modification d'un paramètre | Le paramètre est bien modifié ; l'échec de l'abonné est tracé et rejouable |
| C23 | 23 | Un fichier privé existe, durée des liens à 15 minutes | Obtention d'un lien, attente de 16 minutes, ouverture | Le lien est refusé après expiration ; un lien réémis fonctionne |
| C24 | 23 | Un lien valide obtenu par la gérante de A | Un utilisateur de B l'utilise | Refus |
| C25 | 24 | Valeurs par défaut du club | Dépôt d'un PDF de 12 Mo, puis d'un fichier `.exe` de 1 Mo, puis d'un PNG de 2 Mo | Deux refus avec un code d'erreur stable, puis acceptation |
| C26 | 25 | Un fichier existe | Suppression | Le fichier n'est plus servi ; sa ligne reste, marquée supprimée |
| C27 | 26, 27 | Club sans logo, profil `test` sur disque local | La gérante dépose un logo | Le logo est un fichier privé du club, affiché dans le backoffice ; le même test passe sur un stockage compatible S3 sans changement de code |
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

Étape 3. Non commencé.

## Impacts et régressions

Étape 3. Non commencé. Aucune feature livrée avant celle-ci : aucune régression possible ; l'analyse portera sur les invariants.

## Impact sur le modèle de données

Étape 3. Entités pressenties, à confirmer par le plan : `Club`, `Site`, `User`, `UserClubRole`, `Role`, `Permission`, `UserPermissionOverride`, `TrustedDevice`, `RecoveryCode`, `RefreshToken`, `AuditLog`, `DomainEvent` / `Outbox`, `StoredFile`, `ClubSetting`. Toutes déjà listées en brouillon dans `docs/modele-donnees.md` (socle plateforme), sauf `UserClubRole`, `UserPermissionOverride`, `TrustedDevice`, `RecoveryCode`, `RefreshToken`, `Outbox`.

## Impact sur le contrat d'API

Étape 3. Domaines pressentis dans `contracts/` : `auth`, `club`, `users`, `audit`, `files`, `exports`.

## Paramètres configurables par club

Étape 3. Pressentis : fuseau horaire, devise, langue par défaut, préfixes et exercice de numérotation, types et taille maximale de fichiers, durée des liens temporaires ; plus le registre lui-même (règle 31).

## Points de sécurité et données sensibles

Étape 3. Permissions de F01 déclarées à la règle 11b. Déjà identifiés par les règles 7, 8, 9, 14, 15, 17, 23, 24, 33 : hachage, révocation, verrouillage, chiffrement au champ et au fichier, absence de secrets dans le dépôt et les logs, journal inviolable, liens signés à durée limitée, exclusion des colonnes sensibles à l'export.

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

## Statut

Cadrée — 2026-09-20. Étapes 1 et 2 closes, onze écarts de benchmark tranchés ; étape 3 (plan d'implémentation et analyse d'impact) à faire.

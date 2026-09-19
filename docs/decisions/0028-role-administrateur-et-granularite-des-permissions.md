# 0028 — Rôle Administrateur et granularité des permissions

## Contexte

SEC-02 prévoit cinq rôles — gérant, administratif, coach, comptable, parent — avec des « permissions fines sur finances, remises, santé », sans dire qui les attribue ni jusqu'où elles descendent. Le cahier met le gérant au sommet et confond gérant et propriétaire (section 1.2). Le benchmark de F01 (écart B8) montre que Jackrabbit, Gymdesk et iClassPro attachent les droits à un rôle par défaut et les ajustent utilisateur par utilisateur ; seul Glofox a des rôles fixes.

Omar veut qu'un titulaire du compte puisse octroyer ou retirer des droits à chaque utilisateur, y compris au gérant, par exemple sur le paramétrage et la modification de la tarification. Reste à fixer la maille des droits pour que les fonctionnalités soient protégées sans devenir ingérables.

## Décision

1. **Un sixième rôle, Administrateur**, titulaire du compte du club : au moins un par club, créé avec le club par Clubify ; il détient tous les droits, qui ne peuvent lui être retirés ; il crée, modifie et désactive les utilisateurs, attribue les rôles et ajuste les permissions. Le dernier administrateur d'un club ne peut être ni désactivé ni rétrogradé. Il peut déléguer la gestion des utilisateurs au gérant, jamais l'ajustement des permissions ni la gestion des administrateurs. Libellé écran : « Administrateur du compte », code `ACCOUNT_ADMIN`, pour ne pas le confondre avec l'administratif (accueil).
2. **Le gérant** détient par défaut tout l'opérationnel, y compris l'offre, les tarifs et les paramètres du club ; l'administrateur peut lui en retirer.
3. **Le rôle donne le jeu de permissions par défaut ; la surcharge par utilisateur est l'exception.** L'administrateur ajoute ou retire une permission à un utilisateur précis ; la surcharge est visible sur sa fiche et auditée.
4. **Granularité** : un droit par décision qui engage de l'argent, un droit d'une personne ou une donnée sensible ; un droit par écran ou liste consultable ; jamais un droit par champ.
5. **Grammaire** : chaque permission est nommée `domaine.objet.action`, avec un vocabulaire d'actions fermé — consulter, créer, modifier, désactiver, valider, exporter — complété d'actions sensibles nommées. Une permission peut porter un paramètre (plafond de remise, TAR-05). Exporter est distinct de consulter.
6. Chaque fiche de feature déclare ses permissions dans sa rubrique « Points de sécurité ». Aucun point d'entrée de l'API n'existe sans permission déclarée ; un test d'architecture le vérifie.

## Raison

Un SaaS multi-clubs a besoin d'un titulaire de compte distinct de l'exploitant : dès qu'un propriétaire embauche un gérant, ou dès le deuxième club. Au club pilote, les deux casquettes sont sur la même personne, ce qui ne coûte rien.

La maille retenue est celle que le cahier nomme déjà : remise manuelle avec plafond (TAR-05), tarif négocié (TAR-08), dérogation (GRP-04), publication de l'offre (OFR-06), encaissement à date passée (FAC-03), avoir (FAC-04), double validation de la remise (FIN-03), prix d'article (POS-02), lecture des données de santé et CIN (SEC-03). Une dizaine de décisions sensibles pour toute R1, plus une consultation et un export par domaine. Plus fin, personne ne saurait plus qui peut quoi ; plus gros, ces règles du cahier ne pourraient plus être protégées.

Attacher les droits au rôle et n'ajuster qu'en exception garde la lisibilité : trois comptes restent trois rôles, pas soixante cases à cocher.

## Alternatives écartées

- Cinq rôles, gérant au sommet, sans surcharge : fidèle au cahier, mais un propriétaire ne peut pas limiter un gérant salarié, et chaque exception demanderait un nouveau rôle.
- Nommer le rôle « Propriétaire » : proposé ; Omar retient « Administrateur ». Le libellé écran « Administrateur du compte » et le code `ACCOUNT_ADMIN` évitent la confusion avec l'administratif.
- Surcharge par utilisateur comme mode normal, sans rôle par défaut (modèle Gymdesk pur) : illisible dès quelques comptes.
- Droits par champ : des dizaines de droits dès R1, sans gain réel ; le chiffrement et le journal d'accès protègent déjà les champs sensibles.
- Droits par domaine seulement : TAR-05, FAC-03, FAC-04, GRP-04 ne pourraient plus être protégés comme le cahier le demande.
- Reporter la surcharge en R9 (recommandation initiale du benchmark) : le mécanisme de permissions est construit en F01 de toute façon ; le rattraper coûterait plus cher que de le poser maintenant.

## Source dans le cahier des charges

SEC-02, SEC-03, SEC-04, 1.2, 9.4 point 12, 9.8 (plafonds de remise par rôle), TAR-05, TAR-08, GRP-04, OFR-06, FAC-03, FAC-04, FIN-03, POS-02 ; fiche F01, benchmark B7 et B8 ; décision 0024.

## Écarts ou points ouverts

- Écart avec SEC-02 : six rôles au lieu de cinq. Tranché par Omar le 2026-09-20.
- Les permissions par défaut de chaque rôle se fixent feature par feature ; F01 déclare les siennes (fiche, règle 11b). Les permissions de R1 nommées ci-dessus seront déclarées par F04 à F14.
- Le glossaire, `backend/CLAUDE.md`, le gabarit de fiche et l'étape 3 du processus sont complétés en conséquence.

## Date

2026-09-20

## Statut

Acceptée.

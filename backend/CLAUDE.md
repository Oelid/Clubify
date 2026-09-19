# Backend — règles propres

Squelette. Complété quand la stack sera choisie. Les règles générales sont dans `../CLAUDE.md` et s'appliquent ici.

## Stack

À définir.

## Arborescence

À définir. Contrainte : un découpage par module métier (voir section 6.2 du cahier des charges), pas par couche technique seule.

## Conventions de nommage

À définir. Contrainte : noms techniques en anglais, tirés de `docs/glossaire.md`.

## Stratégie de tests

À définir. Contraintes déjà connues :
- les règles métier (tarification, ventilation, remises, statuts) sont testées avant d'être codées ;
- les jeux de test n'utilisent aucun nom réel ni donnée issue des captures.

## Commandes

À définir.

## Sécurité et multi-tenant

- Chaque donnée porte l'identifiant du club et, quand elle en a un, celui du site. Isolation stricte : aucune requête sans filtre de club.
- Les identifiants de club et de site viennent de l'authentification, jamais de la requête du client.
- Journal d'audit non modifiable sur tout ce qui touche à l'argent, aux remises, aux dérogations et aux données sensibles (SEC-04).
- Données sensibles (santé, CIN, pièces) : chiffrement, accès par rôle, accès journalisé (SEC-03). Jamais dans les logs.
- Suppression logique uniquement. Facture et paiement jamais supprimés : avoir ou contre-passation.
- Rôles et permissions fines (SEC-02) : gérant, administratif, coach, comptable, parent.
- Connecteurs de paiement et de messagerie derrière une interface unique ; aucun prestataire codé en dur.
- Mécanisme technique d'isolation (schéma, ligne, base) : à définir.

## Règles propres

- Montants en centimes avec devise. Dates en UTC ; le fuseau du club sert à l'affichage et aux règles calendaires.
- Aucune règle listée en section 9.8 du cahier des charges n'est codée en dur : elle est lue dans la configuration du club.
- Un seul moteur d'événements : chaque fait métier est publié une fois (PLT-04).
- Numérotation continue des factures et reçus par club et par exercice.

# Sources du cahier des charges

Ce dossier contient les pièces d'origine à partir desquelles le cahier des charges v1 a été construit.
Elles servent à retrouver un détail ou à lever un doute. Elles ne font pas foi : c'est le cahier des
charges, puis les fiches de `docs/features/`, qui font foi.

## Confidentialité

Les captures montrent des **noms et prénoms d'enfants, des numéros de téléphone, des numéros de chèque
et des montants**.

- Le dossier `captures/` est exclu du dépôt par le `.gitignore` de ce dossier. Ne pas retirer cette règle
  sans avoir flouté les images.
- Aucun nom réel ne doit être recopié dans le code, les tests, les jeux de données, les fiches ou les logs.
- Pour les jeux d'essai, inventer des familles fictives qui reproduisent la structure observée
  (fratries, reliquats, chèque différé), jamais les personnes.

## Correspondance captures → documents du cahier des charges

Les identifiants D1 à D11 sont ceux de la section 1 du cahier des charges.

| Fichier | Contenu | Document |
| --- | --- | --- |
| `01-liste-inscription-annuelle.jpeg` | Classeur « Liste d'inscription 2025-2026 », onglet annuel | D2 |
| `02-liste-carnet-10-seances.jpeg` | Même classeur, onglet « carnet » (10 séances) | D3 |
| `03-liste-inscription-annuelle-bis.jpeg` | Onglet annuel, vue complète des onglets du classeur | D2 |
| `04-groupes-natation.jpeg` | Composition des groupes piscine par jour, âge et créneau | D4 |
| `05-groupes-gymnastique.jpeg` | Composition des groupes gymnastique, mention « c-e » = cours d'essai | D4 |
| `06-groupes-judo.jpeg` | Composition des groupes judo, avec niveaux débutants / avancés / baby / ado | D4 |
| `07-grille-tarifaire-trimestrielle.jpeg` | Grille trimestrielle, 1 ou 2 séances par semaine | D5 |
| `08-grille-tarifaire-annuelle-10-mois.jpeg` | Grille annuelle 10 mois | D5 |
| `09-grille-tarifaire-decembre-8-mois.jpeg` | Grille d'entrée en décembre | D5 |
| `10-grille-tarifaire-janvier-7-mois.jpeg` | Grille d'entrée en janvier | D5 |
| `11-grille-tarifaire-fevrier-6-mois.jpeg` | Grille d'entrée en février | D5 |
| `12-grille-tarifaire-mars-5-mois.jpeg` | Grille d'entrée en mars (contient une erreur de recopie sur la colonne 1 séance) | D5 |
| `13-encaissement-janvier-recettes-depenses.jpeg` | Journal de caisse de janvier 2026 : recettes et dépenses | D6 |
| `14-encaissement-janvier-reste-caisse.jpeg` | Même onglet : total des dépenses et reste caisse | D6 |
| `15-encaissement-vente-materiel.jpeg` | Même onglet : ventes de matériel et total des recettes | D6 |
| `16-etat-de-remises-journalier.jpeg` | Bordereau journalier de remise des espèces et chèques | D7 |
| `17-formulaire-demande-inscription.jpeg` | Formulaire papier « Demande d'inscription » | D1 |
| `18-affiche-planning-gymnastique.jpeg` | Affiche du planning gymnastique | D9 |
| `19-affiche-planning-judo.jpeg` | Affiche du planning judo | D9 |
| `20-whatsapp-ceremonie-fin-annee.png` | Annonce WhatsApp de la cérémonie de fin d'année et de sa cotisation | D10 |
| `21-affiche-summer-camp-2026.png` | Affiche du Summer Camp : une colonne par jour, une ligne par créneau | D11 |
| `22-whatsapp-rappel-summer-camp.png` | Rappel WhatsApp du démarrage du Summer Camp | D10 |

Le carnet de reçus papier (D8) n'a pas été photographié : son existence est déduite de la numérotation
continue des reçus visible dans les captures 13, 15 et 16.

## Attention aux grilles tarifaires

Les captures 07 à 12 décrivent le fonctionnement **actuel**, par mois d'entrée dans la saison. La cible
retenue est différente : tarification par durée d'offre (1, 2, 3, 6, 9 ou 10 mois), forfait multi-séances
à prix réduit et tarifs saisonniers optionnels. Se référer au cahier des charges, pas aux grilles.

## Dossier `brainstorming/`

Les deux documents sont versionnés dans le dépôt (choix d'Omar, 19/09/2026).

Les deux documents de cadrage initiaux du projet. Le document de fonctionnalités est construit autour de
la réservation à la séance (fenêtres, pénalités, no-show) : ce modèle n'a **pas** été retenu comme cœur du
produit. Il reste utile comme inventaire d'idées.

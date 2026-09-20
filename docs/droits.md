# Droits — descriptions métier

Ce fichier est la **source lisible** de `docs/matrice-droits.xlsx`, régénéré par
`tools/generer-matrice-droits.py`. Le classeur ne se saisit pas : il serait
écrasé.

La liste des droits, leur attribution par défaut et leur caractère délégable
**viennent du code** (`backend/.../common/security/Permissions.java`) : c'est lui
qui fait foi, et c'est lui que l'application applique. Ce fichier n'ajoute que ce
que le code ne peut pas dire — ce que chaque droit ouvre concrètement, en mots du
club.

Un droit ajouté au code sans description ici apparaît dans la feuille « Écarts »
du classeur, et inversement. Chaque feature qui introduit un droit complète ce
fichier dans la même livraison (`CLAUDE.md` §6).

## Rôles

Les libellés affichés viennent des fichiers de traduction : ils ne sont pas
répétés ici. Cette table dit qui est qui, et ce que le rôle recouvre.

| Rôle | Qui c'est | Remarque |
| --- | --- | --- |
| ACCOUNT_ADMIN | Le titulaire du compte Clubify du club | Détient tout, et ses droits ne se retirent pas (décision 0028, critère C12b) |
| MANAGER | Le gérant : il pilote l'offre, les tarifs, l'argent | Ses droits s'ajustent, sauf l'attribution des droits elle-même |
| FRONT_DESK | L'accueil : elle inscrit et encaisse au comptoir | Le rôle le plus employé au quotidien |
| COACH | L'intervenant : ses séances, ses présences | Aucun droit en R1 ; son application arrive en R4 |
| ACCOUNTANT | Le comptable, souvent externe au club | Lecture seule, élargie aux exports comptables en R3 |
| PARENT | Le parent, depuis le portail | Aucun compte avant R8 ; le rôle existe pour que le modèle ne bouge pas |

## Droits

« Ce qu'il ouvre » se lit du point de vue de la personne : ce qu'elle peut faire
une fois le droit accordé, et ce qu'elle ne peut toujours pas.

| Code | Libellé | Ce qu'il ouvre | Feature |
| --- | --- | --- | --- |
| club.settings.consulter | Consulter les paramètres du club | Lire l'identité du club et ses règles configurables. Ne permet aucune modification. | F01 |
| club.settings.modifier | Modifier les paramètres du club | Changer l'identité, le fuseau, la devise et les règles configurables. Chaque changement est tracé avec son avant et son après. | F01 |
| users.consulter | Consulter les utilisateurs | Ouvrir la liste du personnel : rôle, second facteur, statut, dernière connexion. N'emporte pas le droit de l'exporter. | F01 |
| users.creer | Créer un utilisateur | Ouvrir un compte pour un membre du personnel et lui donner un rôle. Aucun compte n'est créable pour un adhérent mineur. | F01 |
| users.modifier | Modifier un utilisateur | Corriger le nom, le téléphone, la langue. Ne change ni le rôle ni les droits. | F01 |
| users.desactiver | Désactiver un utilisateur | Fermer l'accès de quelqu'un sans effacer son compte ni ses traces. Le dernier administrateur actif ne peut pas être désactivé. | F01 |
| users.permissions.modifier | Ajuster les droits d'un utilisateur | Accorder ou retirer un droit à une personne, par-dessus son rôle. **Jamais délégable** : c'est le seul droit qu'aucune surcharge ne peut donner. | F01 |
| users.sessions.fermer | Fermer les sessions d'un utilisateur | Déconnecter quelqu'un partout, immédiatement — un téléphone perdu, un départ — sans désactiver son compte. | F01 |
| users.mfa.reinitialiser | Réinitialiser le second facteur | Rendre à quelqu'un l'accès à son compte quand son téléphone est perdu et ses codes de secours épuisés. | F01 |
| users.exporter | Exporter la liste des utilisateurs | Sortir la liste du personnel en CSV ou Excel. Se demande à part : un export quitte l'application et ne se rattrape pas. | F01 |
| audit.consulter | Consulter le journal d'audit | Lire qui a fait quoi, quand, et sur quoi. Le journal ne se modifie ni ne s'efface, pour personne. | F01 |
| files.deposer | Déposer un fichier | Joindre une pièce ou un logo. Le contenu est chiffré avant d'être rangé. | F01 |
| files.consulter | Consulter un fichier | Obtenir un lien temporaire vers une pièce. Chaque accès laisse une trace. | F01 |

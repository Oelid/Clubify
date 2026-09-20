# 0031 — Le second facteur est rappelé, non imposé

## Contexte

La décision 0027 impose le second facteur à l'administrateur du compte et au
gérant, et le rend bloquant dès la première connexion : sans lui, ils n'accèdent
à rien.

À l'usage, cette règle bloque le premier pas. Celui qui installe l'application
— le club, ou nous chez le club — doit avoir un téléphone sous la main, avec une
application d'authentification déjà installée, avant même de voir un écran. Si
le téléphone n'est pas là, le paramétrage du club s'arrête net.

## Décision

Le second facteur est **attendu des rôles sensibles, rappelé en permanence, et
n'empêche rien**.

- L'administrateur et le gérant qui ne l'ont pas activé voient, sur chaque écran,
  un message qui ne se masque pas, avec de quoi l'activer en un geste.
- Le rappel ne s'adresse qu'à ces deux rôles : l'accueil et le coach ne le voient
  jamais.
- Deux règles configurables permettent à un club d'aller plus loin :

| Règle | Défaut | Effet |
| --- | --- | --- |
| `security.mfa.required` | `false` | À `true`, le second facteur devient bloquant |
| `security.mfa.grace_days` | `7` | Délai avant qu'il le devienne, une fois imposé |

- Posé à `true` avec un délai de zéro, on retrouve exactement le comportement de
  0027.
- Le délai court depuis la création du compte, et non depuis sa première
  connexion : un compte créé puis laissé de côté ne gagne pas de sursis en
  restant inutilisé.

## Raison

Le besoin est concret : la mise en service d'un club ne doit pas dépendre d'un
téléphone. Une règle qui empêche d'installer le logiciel est une règle qui sera
contournée — par un compte de secours sans second facteur, par exemple, ce qui
serait pire que tout.

C'est la pratique des grands systèmes : on rappelle, on insiste, on n'enferme
pas dehors celui qui n'a pas encore eu le temps. Le rappel permanent, non
masquable, fait le travail dans l'immense majorité des cas ; la contrainte reste
disponible pour le club qui la veut.

Le rendre configurable plutôt que d'en faire un choix de code suit la règle
générale : aucune règle métier de la section 9.8 n'est codée en dur, et chaque
défaut est documenté (`CLAUDE.md` §3).

## Alternatives écartées

- **Garder 0027 tel quel.** Bloque la mise en service, et pousse à des
  contournements.
- **Imposer après un délai par défaut** (sept jours, bloquant ensuite). Proposée,
  écartée par Omar au profit du rappel seul : le délai reste disponible pour le
  club qui choisit d'imposer.
- **Ne lever l'obligation qu'en développement et en recette**, par profil. Ne
  couvre pas la mise en service chez le club, qui est précisément le moment où
  le problème se pose.
- **Un rappel masquable.** Masqué une fois, il ne revient jamais, et la règle ne
  vaut plus rien.

## Source dans le cahier des charges

SEC-01, section 9.8 (règles configurables). Décisions 0027 et 0028.

## Écarts ou points ouverts

- Amende la décision 0027, qui reste vraie pour un club qui pose
  `security.mfa.required`.
- **Point de vigilance assumé** : par défaut, le compte qui ouvre la caisse, les
  soldes des familles et les données de santé peut fonctionner sans second
  facteur, indéfiniment. Le rappel est le seul rempart. À revoir avant la mise en
  service du club pilote : le gérant a-t-il activé le sien ? Si la réponse est
  non au bout de quelques semaines, c'est que le rappel ne suffit pas, et il
  faudra poser `security.mfa.required`.
- À confirmer à la mise en service : que se passe-t-il si l'administrateur du
  compte perd son téléphone et ses codes de secours ? Personne au-dessus de lui
  ne peut le débloquer (question déjà ouverte dans 0030).

## Date

2026-09-20

## Statut

Acceptée

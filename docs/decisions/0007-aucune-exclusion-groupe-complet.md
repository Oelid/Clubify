# 0007 — Aucune activité exclue d'un forfait ; groupe complet

## Contexte

Faut-il interdire certaines combinaisons d'activités dans un forfait ? Et que se passe-t-il quand un groupe est plein ?

## Décision

Aucune activité n'est exclue d'un forfait par nature. La seule indisponibilité vient des groupes complets. Au MVP, un groupe complet n'accepte plus aucune inscription : pas de liste d'attente. La liste d'attente avec promotion arrive en V2 (RES-02).

## Raison

Confirmé par le club : les indisponibilités sont une question de capacité à l'instant de l'inscription. La liste d'attente est un mécanisme à part entière, planifié en V2.

## Alternatives écartées

- Exclusions d'activités configurées : aucun besoin observé.
- Masquer une activité pleine : le cahier préfère la montrer comme indisponible.
- Liste d'attente dès le MVP : reportée en V2 par le backlog.

## Source dans le cahier des charges

OFR-03, GRP-03, RES-02, UC15, section 11 (forfait, dernière phrase).

## Écarts ou points ouverts

- L'incohérence de la v1 (OFR-03 renvoyait à une liste d'attente prévue en V2) est corrigée dans la version 1.1 du cahier des charges : au MVP, un groupe complet refuse l'inscription et l'activité est affichée indisponible, non masquée.
- Dérogation de capacité (surcapacité autorisée, 9.8) : règle configurable, à cadrer dans la feature Groupes.

## Date

2026-09-19

## Statut

Acceptée.

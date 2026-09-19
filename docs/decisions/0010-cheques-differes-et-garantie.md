# 0010 — Chèques différés et chèques de garantie dès le MVP

## Contexte

Le club reçoit des chèques à encaisser à une date future et des chèques pris en garantie. Un chèque encaissable plus tard ne solde pas la dette le jour où il est reçu ; un chèque de garantie n'est jamais un paiement.

## Décision

Le registre des chèques du MVP distingue la nature (encaissement ou garantie) et le statut (reçu, différé, remis, encaissé, rejeté, restitué). Un chèque différé compte comme encaissement attendu et suspend les relances jusqu'à sa date de remise. Un chèque de garantie n'est jamais compté comme paiement et figure dans les pièces à restituer.

## Raison

Confirmé par le club : les deux cas existent. Sans cette distinction, le solde famille serait faux.

## Alternatives écartées

- Chèque = paiement immédiat : solde faux, relances à tort.
- Reporter en V2 : les chèques représentent une part importante des encaissements dès le premier jour.

## Source dans le cahier des charges

FAC-08, FAC-06, UC4, UC5, section 5 (Chèques), section 11 (chèques différés ou de garantie).

## Écarts ou points ouverts

Aucun. Frais de rejet optionnels (UC5) : paramètre à cadrer dans la feature.

## Date

2026-09-19

## Statut

Acceptée.

# Modèle de données — brouillon

> **Statut : brouillon.** Liste des entités pressenties d'après la section 6 du cahier des charges. Pas d'attributs, pas de types. Ce fichier sera précisé feature par feature ; chaque livraison le met à jour.

## Principes (section 9.6)

- Modèle construit à neuf, sans reprise du schéma dbdiagram existant (décision 0016).
- Identifiant de club, et de site quand il existe, sur chaque entité.
- Famille comme entité racine.
- Inscription distincte de la facture.
- Un paiement se ventile sur plusieurs lignes.
- Montants en centimes avec devise ; dates en UTC ; suppression logique ; journal d'audit.
- Un seul registre financier : inscriptions, frais annuels, packs, articles et frais de grade produisent tous des lignes de facture famille (6.3).

Les noms techniques sont ceux de `docs/glossaire.md`.

## Socle plateforme

**Livré par F01** (migrations `V202609200900` à `V202609200930`). Quatorze tables,
les seules dont les attributs soient arrêtés à ce jour.

| Table | Rôle | À retenir |
| --- | --- | --- |
| `club` | Le tenant ; possède tout le reste | Fuseau, devise et langue par défaut y vivent (ADM-01) |
| `site` | Appartient à un club ; portera lieux, caisses et plannings | Créé dès R1 par invariant, sans écran (PLT-03 écarté) |
| `user_account` | Personne qui se connecte | **Sans `club_id`** : un compte peut servir plusieurs clubs (PLT-02) |
| `membership` | Rattachement d'un compte à un club, avec son rôle | Porte le `club_id` et le rôle ; c'est elle que le discriminant filtre |
| `user_permission_override` | Surcharge d'un droit pour un utilisateur | Le rôle donne le jeu par défaut, la surcharge l'ajuste (0028) |
| `refresh_token` | Session ouverte, révocable | Seule l'empreinte est conservée ; rotation à chaque renouvellement |
| `mfa_challenge` | Défi de second facteur en cours | À usage unique, expirant |
| `trusted_device` | Appareil reconnu, pour ne pas redemander de code | Durée réglable par le club (benchmark B4) |
| `recovery_code` | Code de secours à usage unique | Sans eux, un téléphone perdu ferme le club (B5) |
| `club_setting` | Valeur d'une règle configurable (9.8) | Ne contient que ce qu'un club a saisi ; le défaut vit dans le code |
| `stored_file` | Pièce ou PDF, privé | Contenu chiffré avant d'atteindre le support (SEC-03) |
| `file_link` | Lien signé et expirant vers un fichier | Seul chemin de lecture d'un fichier (C23) |
| `outbox_event` | Effet externe en attente | Publié dans la transaction, produit ensuite, rejouable |
| `audit_log` | Trace non modifiable des actions sensibles | En ajout seul, garanti par un déclencheur ; `club_id` nul admis pour l'authentification |

Colonnes communes à toute table métier : `club_id`, `created_at`, `created_by`,
`updated_at`, `updated_by`, `deleted_at`. Identifiants en UUID v7. Les entités
touchant à l'argent porteront `version`.

Deux points appris à l'implémentation, qui valent pour tout le modèle à venir :

- `user_account` est la seule table sans `club_id`, et l'authentification est le
  seul chemin qui interroge `membership` hors contexte de club — la requête est
  isolée et commentée dans `MembershipRepository`. Aucun autre code ne contourne
  le discriminant.
- Le jeton d'accès porte l'adresse de son titulaire, reportée dans `audit_log` :
  une trace reste lisible après un renommage de compte.

**Non encore modélisé** : ce qui suit reste un brouillon d'entités pressenties,
sans attributs, précisé feature par feature.

## Familles et adhérents

- **Family** : racine ; regroupe les tuteurs, les adhérents et les factures.
- **Guardian** : adulte d'une famille ; l'un d'eux est le responsable financier, historisé.
- **Member** : adhérent, enfant ou adulte ; appartient à une famille ; son statut est calculé depuis ses inscriptions.
- **EmergencyContact** : rattaché à un adhérent ; au moins un.
- **AuthorizedPickup** : rattaché à un adhérent ; modifications tracées.
- **HealthRecord** : rattaché à un adhérent ; donnée sensible.
- **Document** : pièce du dossier, rattachée à un adhérent ou à un tuteur, avec expiration.
- **Consent** : accepté par un tuteur, versionné, horodaté.
- **Lead** (V2) : prospect converti en famille sans ressaisie.

## Catalogue

- **Activity** : discipline du club ; porte ses tranches d'âge, niveaux et pièces requises.
- **Level** : palier ordonné d'une activité.
- **Venue** : lieu d'un site, avec capacité.
- **Group** : activité + tranche d'âge + niveau + lieu + coach + capacité ; rattaché à une saison.
- **TimeSlot** : récurrence hebdomadaire d'un groupe.

## Calendrier et séances

- **Season** : période de référence d'un club ; toute inscription lui appartient.
- **ClubCalendar** : fériés, vacances, fermetures, plages réservées par un événement (PLA-05).
- **Session** : occurrence datée, générée depuis un créneau ou créée à l'unité (ponctuelle, privée, essai, rattrapage, stage) ; porte le coach effectif.

## Inscriptions

- **Enrollment** : lie un adhérent, une saison, une offre et un ou plusieurs groupes ; historise ses statuts, changements de groupe et prix appliqué.
- **TrialSession** : place ponctuelle d'un non-inscrit sur une séance de groupe, avec issue.
- **WaitlistEntry** (V2) : demande ordonnée sur un groupe complet.
- **Override** : dérogation motivée et validée, liée à l'inscription concernée.
- **LevelPromotion** (V2) : passage de niveau d'un adhérent, historisé.

## Tarification et offre

- **Offer** : formule paramétrée par le gérant, versionnée par saison, avec statut de publication.
- **Bundle** : forfait multi-séances défini sur une liste d'activités, avec son mode de calcul.
- **PriceGrid** : matrice activité × durée × séances par semaine d'une saison ; **PriceGridCell** en est une cellule.
- **AdditionalSessionSurcharge** : supplément par activité et par formule.
- **SeasonalRate** : prix propre à une fenêtre de dates, sur une offre.
- **AnnualFee** : frais annuels d'une saison.
- **DiscountRule** : barème automatique (fratrie) d'un club ; **Discount** : remise appliquée sur une ligne, avec auteur et motif.
- **SessionPack** : carnet acheté par un adhérent, avec solde et expiration ; débité par les présences.

## Facturation et encaissements

- **Invoice** : facture famille, numérotée en continu ; **InvoiceLine** : ligne par enfant, activité, frais, article ou événement.
- **PaymentSchedule** : tranches datées d'une facture.
- **Payment** : encaissement sur un compte de trésorerie, avec mode et référence.
- **PaymentAllocation** : part d'un paiement affectée à une ligne ou à une échéance.
- **Cheque** : registre des chèques, lié au paiement quand il en est un ; nature encaissement ou garantie ; statuts historisés.
- **Receipt** : reçu numéroté, lié à un paiement ; annulé par contre-passation.
- **CreditNote** : avoir lié à la facture d'origine.
- **FamilyCredit** : trop-perçu imputable sur les factures de la famille.
- **PaymentPromise** : promesse datée sur une créance ; suspend les relances.

## Caisse et dépenses

- **CashAccount** : caisse ou banque d'un site.
- **CashSession** : session de caisse d'un poste et d'un jour ; regroupe les paiements et sorties du jour.
- **DepositSlip** : bordereau généré depuis les paiements d'une session, à double validation.
- **Expense** : dépense catégorisée, liée à une session de caisse ou à un compte, rattachable à une activité.

## Présences

- **Attendance** : présence, absence ou retard d'un adhérent sur une séance ; corrections historisées ; débite un carnet le cas échéant.
- **CheckIn** : arrivée enregistrée à l'accueil, avec le statut administratif constaté.

## Progression (V2)

- **SkillFramework** : référentiel de compétences d'un niveau, versionné par saison.
- **Assessment** : évaluation d'un adhérent par un coach.

## Coachs et rémunération

- **Coach** : profil d'intervenant lié à un utilisateur ; activités et niveaux enseignés.
- **CoachAvailability** (V2) : plages et absences.
- **CompensationModel** (V2) : ligne de rémunération d'un coach (fixe, à la séance, par participant).
- **CompensationStatement** (V2) : état mensuel calculé depuis les séances tenues, validé puis figé ; **Advance** : avance imputée sur l'état suivant.

## Boutique et stock

- **Product** : article vendu au comptoir, avec variantes.
- **Sale** : vente comptoir produisant une ligne de facture et un reçu de la même série.
- **StockMovement** (V2) : entrée, sortie ou ajustement, jamais supprimé.

## Notifications et communication

- **NotificationTemplate** : modèle par canal et par langue.
- **NotificationRule** : événement → modèle → canal, paramétrée par club.
- **Notification** : message envoyé, avec statut, coût et échec.
- **ChannelConsent** : préférence et opposition d'un tuteur par canal.
- **Broadcast** : annonce de masse, avec suivi des envois.

## Événements

- **Event** : cérémonie, camp, stage ; daté, avec cotisation ou tarif propre.
- **CampProgram** (V2) : semaines thématiques et emploi du temps journalier d'un camp.
- **EventRegistration** : inscription distincte d'un adhérent ou d'un non-adhérent à un événement.
- **Certificate** (V2) : diplôme lié à un passage de niveau et à un événement.

## CRM et tâches (V2)

- **Lead**, **LeadStage** : entonnoir de prospects.
- **Task** : à faire par rôle, créée à la main ou par règle.

## Points ouverts

- Recalcul du rang fratrie après résiliation d'un enfant (décision 0021).
- Mécanisme technique d'isolation multi-tenant : schéma PostgreSQL partagé, colonne `club_id` sur chaque table, discriminant Hibernate `@TenantId` alimenté par le jeton, Row-Level Security en seconde ligne (décision 0024).

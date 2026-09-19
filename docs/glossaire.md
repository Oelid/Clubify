# Glossaire

Termes métier tirés du cahier des charges v1, avec le nom technique anglais à utiliser dans le code, le modèle et le contrat d'API. Les références entre parenthèses renvoient aux identifiants du backlog (section 7) ou aux sections du cahier des charges.

Règle : un terme, un nom technique. Si un terme manque, l'ajouter ici avant de l'utiliser dans le code.

## Organisation

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Club | Le tenant : une organisation cliente de Clubify, avec ses données isolées de celles des autres clubs (PLT-01). | `Club` (tenant) |
| Site | Lieu d'exploitation d'un club (bâtiment, piscine). Plusieurs sites par club possibles ; identifiant présent dès le MVP, écrans en V3 (PLT-03). | `Site` |
| Saison | Période de référence de 10 mois, de septembre à juin, à laquelle toute inscription appartient ; juillet est un complément (PLA-01, section 11). | `Season` |
| Lieu | Espace où se tient une séance : salle, piscine, tatami. Un lieu ne porte qu'une séance à la fois, sauf lieu partageable (ACT-04). | `Venue` |

## Familles et personnes

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Famille | Entité racine regroupant parents, enfants et factures. Toute facture appartient à une famille (FAM-01). | `Family` |
| Responsable financier | Le parent destinataire des factures et des relances. Exactement un par famille, modifiable, historisé (FAM-03). | `BillingContact` |
| Parent | Adulte de la famille : tuteur légal ou contact. Au moins un téléphone valide au format international (FAM-02). | `Guardian` |
| Adhérent | Personne inscrite au club : enfant ou adulte. Le tuteur n'est obligatoire que pour un mineur (FAM-09). Son statut est calculé, jamais saisi (FAM-10). | `Member` |
| Enfant | Adhérent mineur, 3 à 14 ans pour le club pilote. Fiche : identité, sexe, date et lieu de naissance, nationalité, photo (FAM-04). | `Child` (Member mineur) |
| Personne autorisée | Personne autorisée à récupérer l'enfant à la sortie : nom, CIN, téléphone, photo optionnelle. Visible du coach et de l'accueil (FAM-06). | `AuthorizedPickup` |
| Contact d'urgence | Personne à prévenir en cas d'urgence : nom, lien, téléphone. Au moins un obligatoire (FAM-05). | `EmergencyContact` |
| Fiche santé | Allergies, contre-indications, remarques. Donnée sensible à accès restreint et journalisé (FAM-07). | `HealthRecord` |
| Pièce | Document du dossier : CIN parents, photo, certificat médical, avec date d'expiration (FAM-08). | `Document` |
| Consentement | Acceptation horodatée et versionnée par un parent : règlement intérieur, données, droit à l'image, WhatsApp (ONB-03). | `Consent` |
| Prospect | Famille potentielle, non encore inscrite (CRM-01, V2). | `Lead` |

## Catalogue

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Activité | Discipline proposée par le club : judo, natation, gymnastique… Catalogue unique utilisé par formulaires, tarifs et planning (ACT-01). | `Activity` |
| Tranche d'âge | Bornes d'âge min et max d'une activité ou d'un groupe ; âge évalué à une date de référence configurable (ACT-02). | `AgeRange` |
| Niveau | Palier ordonné propre à une activité : Baby, Débutant, Avancé, ceintures (NIV-01). | `Level` |
| Groupe | Activité + tranche d'âge + niveau + créneaux + lieu + coach + capacité. La capacité est saisie groupe par groupe, jamais codée en dur (GRP-01). | `Group` |
| Capacité | Nombre de places d'un groupe (6 par défaut en piscine pour le club pilote). Essais et places réservées comptés à part (GRP-01, GRP-03). | `capacity` |
| Roster | Liste des inscrits d'un groupe, toujours générée depuis les inscriptions, jamais saisie (GRP-02). | `Roster` |
| Créneau | Récurrence hebdomadaire d'un groupe : jour, heure de début et de fin (PLA-02). | `TimeSlot` |
| Séance | Occurrence datée d'un créneau, générée sur la saison. Socle commun du cours collectif, du cours privé, de l'essai, du rattrapage et du stage (PLA-03, 9.5). | `Session` |
| Coach | Intervenant qui anime des séances ; peut cumuler plusieurs modèles de rémunération (COA-01). | `Coach` |

## Inscriptions

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Inscription | Un enfant, un ou plusieurs groupes, une saison, une formule, des dates. Statuts : en attente, active, en pause, expirée, résiliée (INS-01). Distincte de la facture. | `Enrollment` |
| Cours d'essai | Place ponctuelle dans un groupe pour un enfant non inscrit ; nombre limité par enfant, gratuit ou payant (RES-01). | `TrialSession` |
| Cours privé | Séance avec coach dédié, réservée à l'unité selon la disponibilité du coach et de la salle, tarif propre (RES-06). | `PrivateSession` |
| Liste d'attente | File ordonnée des demandes pour un groupe complet, avec promotion dans l'ordre (RES-02, V2). Au MVP, un groupe complet n'accepte plus d'inscription et l'activité est affichée indisponible (OFR-03, décision 0007). | `WaitlistEntry` |
| Rattrapage | Séance dans un autre groupe après une absence, limitée en nombre et en délai (RES-05, V2). | `MakeupSession` |
| Dérogation | Exception à une règle d'âge ou de niveau à l'inscription, avec motif, validée par le gérant et journalisée (GRP-04). La même activité deux fois par semaine n'en est pas une (décision 0020). | `Override` |
| Passage de niveau | Changement de niveau d'un adhérent, individuel ou en masse, prononcé par le coach ; historisé (NIV-05). | `LevelPromotion` |
| Réinscription | Campagne de saison : place prioritaire jusqu'à une date, puis libération (INS-05, V2). | `Renewal` (saison) |

## Offre et tarification

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Offre | Formule paramétrée par le gérant : nom, durée, modes de paiement, activités et âges éligibles, statut brouillon / publiée / archivée (OFR-01). | `Offer` |
| Durée d'offre | Durée souscrite : 1, 2, 3, 6, 9 ou 10 mois, ou n séances. Le prix dépend de la durée, jamais du mois d'entrée (TAR-02, INS-03). | `OfferDuration` |
| Forfait | Offre combinant N séances par semaine sur une liste d'activités, au prix réduit du forfait, jamais par addition des prix unitaires (OFR-02). | `Bundle` |
| Supplément de séance additionnelle | Montant ajouté, par activité et par formule, pour chaque séance hebdomadaire au-delà de la première dans un forfait (OFR-02, section 11). | `AdditionalSessionSurcharge` |
| Grille tarifaire | Prix par activité × durée d'offre × séances par semaine, versionnée par saison (TAR-01, OFR-04). | `PriceGrid` |
| Tarif saisonnier | Prix propre à une fenêtre de dates, qui s'applique aux seules inscriptions dont la date de début tombe dans la fenêtre (TAR-02). | `SeasonalRate` |
| Frais annuels | Assurance et inscription (500 DH au club pilote), une fois par enfant et par saison (TAR-03). | `AnnualFee` |
| Remise | Réduction sur une ligne ou une facture : automatique (fratrie, TAR-04) ou manuelle avec plafond et motif (TAR-05). | `Discount` |
| Remise fratrie | Barème par rang d'enfant d'une même famille, rang par ordre d'inscription, assiette hors frais annuels (5 % sur le 2e, 10 % sur le 3e par défaut), paramétrable (TAR-04, décision 0021). | `SiblingDiscount` |
| Carnet | Pack de crédits de séances (10 séances), avec validité et activités éligibles, débité au pointage (TAR-06, RES-04). | `SessionPack` |
| Tarif personnalisé | Prix négocié sur une inscription, réservé au gérant (TAR-08). | `CustomPrice` |

## Facturation et encaissement

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Facture | Facture famille : lignes par enfant, activité, frais, article. Numérotation continue par club et exercice ; jamais supprimée (FAC-01). | `Invoice` |
| Échéancier | Découpage du dû en tranches datées ; total des tranches = total dû (FAC-02). | `PaymentSchedule` |
| Encaissement | Paiement reçu : espèces, chèque, virement, carte TPE ; référence, date, utilisateur (FAC-03). | `Payment` |
| Ventilation | Affectation d'un paiement à plusieurs lignes, enfants ou factures ; par défaut sur l'échéance la plus ancienne (FAC-05). | `PaymentAllocation` |
| Solde famille | Dû, encaissé, attendu (chèques différés), reste, crédit ; calculé en temps réel (FAC-06). | `FamilyBalance` |
| Reliquat | Reste dû après un ou plusieurs paiements partiels ; suivi de mémoire aujourd'hui, suivi par le solde famille demain (section 1.4, 11). | `outstandingBalance` |
| Reçu | Preuve de paiement numérotée en série continue ; annulation par contre-passation, jamais par suppression (FAC-07). | `Receipt` |
| Chèque | Instrument de paiement suivi dans un registre : banque, numéro, titulaire, montant, date de remise prévue, nature, statut (FAC-08). | `Cheque` |
| Chèque différé | Chèque à remettre à une date future : compte comme encaissement attendu et suspend les relances jusqu'à sa date (FAC-08). | `Cheque` nature `deferred` |
| Chèque de garantie | Chèque pris en garantie : jamais compté comme paiement, figure dans les pièces à restituer (FAC-08). | `Cheque` nature `guarantee` |
| Avoir | Document qui annule ou corrige une facture, lié à la facture d'origine, validé par le gérant (FAC-04). | `CreditNote` |
| Crédit famille | Trop-perçu conservé, imputable sur toute facture de la famille (FAC-09). | `FamilyCredit` |
| Promesse de paiement | Date promise par le parent, qui suspend les relances jusqu'à cette date (IMP-03). | `PaymentPromise` |

## Caisse

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Session de caisse | Ouverture, fond de caisse, clôture, comptage, écart ; une par poste et par jour (FIN-01). | `CashSession` |
| Bordereau de remise | État des espèces et chèques remis au gérant ou à la banque, généré depuis les paiements, à double validation (FIN-03). | `DepositSlip` |
| Dépense | Sortie d'argent catégorisée, avec justificatif, rattachable à une activité (FIN-02). | `Expense` |
| Compte de trésorerie | Caisse ou banque ; tout paiement va sur un compte (FIN-04). | `CashAccount` |

## Présences

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Appel | Pointage fait par le coach sur mobile : présent, absent, retard ; modifiable jusqu'à un délai (PRE-01). | `Attendance` |
| Pointage accueil | Enregistrement de l'arrivée à l'accueil, avec statut administratif (PRE-02, PRE-03). | `CheckIn` |

## Événements

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Camp d'été | Offre autonome de juillet : programme journalier, tranche d'âge propre, barème 1, 2, 3 semaines ou le mois, repas inclus, inscription distincte, ouverte aux non-adhérents (EVT-01 à EVT-05). | `SummerCamp` |
| Cérémonie | Événement de fin d'année : invitation des familles, cotisation par enfant activable à chaque édition, remise des diplômes (EVT-06, EVT-07). | `Ceremony` |
| Cotisation d'événement | Montant dû par enfant pour un événement, en ligne de facture famille, jamais bloquant pour la participation (EVT-06). | `EventFee` |
| Diplôme | Certificat matérialisant un passage de niveau pour les activités à grades (judo, karaté) ; fabrication sous-traitée (EVT-07). | `Certificate` |
| Annonce de masse | Diffusion unidirectionnelle aux parents, type liste WhatsApp, messages de service uniquement (EVT-08). | `Broadcast` |

## Transverse

| Terme | Définition | Nom technique |
| --- | --- | --- |
| Journal d'audit | Qui a fait quoi, quand, avant/après ; non modifiable (SEC-04). | `AuditLog` |
| Événement métier | Fait publié une fois sur le bus (inscription créée, paiement reçu…), auquel notifications, tâches et rapports s'abonnent (PLT-04). | `DomainEvent` |
| Notification | Message produit par une règle à partir d'un événement, via un canal (WhatsApp, SMS, email, in-app) (NOT-01). | `Notification` |
| Rôle | Administrateur (titulaire du compte, tous droits), gérant, administratif, coach, comptable, parent ; permissions fines (SEC-02, décision 0028). | `Role` |
| Administrateur | Titulaire du compte du club : au moins un par club, tous droits non retirables, gère les utilisateurs et leurs permissions. Libellé écran « Administrateur du compte » pour ne pas le confondre avec l'administratif (accueil). (Décision 0028) | `Role` `ACCOUNT_ADMIN` |
| Permission | Droit nommé `domaine.objet.action`, attaché à un rôle par défaut, ajustable par utilisateur ; peut porter un paramètre (plafond). Un droit par décision sensible et par écran, jamais par champ. (Décision 0028) | `Permission` |

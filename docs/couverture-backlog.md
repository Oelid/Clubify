# Couverture du backlog par les releases

Affectation de chacune des 203 fonctionnalités de la section 7 du cahier des charges (version 1.1) à une release, ou mention « écarté » avec sa raison. Le plan de releases est décrit dans `docs/roadmap.md` ; le découpage est acté par la décision `docs/decisions/0023-decoupage-releases.md`.

Date : 2026-09-19.

## Lecture

| Colonne | Contenu |
| --- | --- |
| ID | Identifiant du backlog (section 7) |
| Domaine | Domaine du cahier (7.1 à 7.32) |
| Libellé | Libellé court du cahier |
| Cahier | Version prévue par le cahier : MVP, V2 ou V3 |
| Release | Release retenue : R1 à R9, ou « écarté » |
| Raison | Renseignée quand l'identifiant est sorti de R1 par rapport au MVP du cahier, avancé de V2, ou écarté ; « — » sinon |

Releases : R1 Comptoir · R2 Planning et coachs · R3 Parents, relances et exports comptables · R4 Pointage · R5 Rémunération et arrière-boutique · R6 Fin de saison et réinscription · R7 Camp d'été · R8 Portail parent et acquisition · R9 Deuxième club.

« Écarté » signifie hors de ce plan de releases, à reprendre si un club le demande. Rien n'est retiré du cahier des charges.

## Comptes

| Release | Identifiants | Dont MVP du cahier | Dont V2 avancés |
| --- | --- | --- | --- |
| R1 | 72 | 72 | 0 |
| R2 | 18 | 15 | 3 |
| R3 | 23 | 17 | 6 |
| R4 | 16 | 12 | 4 |
| R5 | 18 | 0 | — |
| R6 | 13 | 0 | — |
| R7 | 6 | 0 | — |
| R8 | 20 | 0 | — |
| R9 | 4 | 2 | — |
| Écartés | 13 | 0 | — |
| **Total** | **203** | **118** | **13** |

Le MVP du cahier (118) est intégralement couvert à la fin de R4, sauf ADM-02 et PLT-02 reportés en R9.

## Table

| ID | Domaine | Libellé | Cahier | Release | Raison |
| --- | --- | --- | --- | --- | --- |
| CRM-01 | CRM / Prospects | Fiche prospect | V2 | R8 | — |
| CRM-02 | CRM / Prospects | Entonnoir à étapes | V2 | R8 | — |
| CRM-03 | CRM / Prospects | Conversion en famille | V2 | R8 | — |
| CRM-04 | CRM / Prospects | Formulaire de contact public | V2 | R8 | — |
| CRM-05 | CRM / Prospects | Relances automatiques | V2 | R8 | — |
| FAM-01 | Familles / Adhérents | Compte famille | MVP | R1 | — |
| FAM-02 | Familles / Adhérents | Parents / tuteurs | MVP | R1 | — |
| FAM-03 | Familles / Adhérents | Responsable financier | MVP | R1 | — |
| FAM-04 | Familles / Adhérents | Fiche enfant | MVP | R1 | — |
| FAM-05 | Familles / Adhérents | Contacts d'urgence | MVP | R1 | — |
| FAM-06 | Familles / Adhérents | Personnes autorisées à récupérer | MVP | R1 | — |
| FAM-07 | Familles / Adhérents | Fiche santé | MVP | R1 | Hypothèse à confirmer (autorisation CNDP santé) |
| FAM-08 | Familles / Adhérents | Pièces du dossier | MVP | R1 | — |
| FAM-09 | Familles / Adhérents | Adhérent adulte | MVP | R2 | Sorti de R1 : quelques adultes (carnet boxe, coaching privé) ; arrive avec les cours privés (RES-06) |
| FAM-10 | Familles / Adhérents | Statut adhérent | MVP | R1 | — |
| FAM-11 | Familles / Adhérents | Vue 360° | MVP | R1 | — |
| FAM-12 | Familles / Adhérents | Recherche et filtres | MVP | R1 | — |
| FAM-13 | Familles / Adhérents | Import Excel | MVP | R1 | — |
| FAM-14 | Familles / Adhérents | Fusion de doublons | V2 | R6 | — |
| ONB-01 | Onboarding | Parcours d'inscription guidé | MVP | R1 | — |
| ONB-02 | Onboarding | Saisie par le parent | V2 | R8 | — |
| ONB-03 | Onboarding | Consentements | MVP | R1 | — |
| ONB-04 | Onboarding | Fiche d'inscription PDF | MVP | R1 | — |
| ONB-05 | Onboarding | Validation conditionnelle | MVP | R1 | — |
| ACT-01 | Activités | Catalogue d'activités | MVP | R1 | — |
| ACT-02 | Activités | Tranches d'âge | MVP | R1 | — |
| ACT-03 | Activités | Pièces requises par activité | MVP | R1 | — |
| ACT-04 | Activités | Lieux | MVP | R1 | — |
| NIV-01 | Niveaux | Niveaux personnalisables | MVP | R1 | — |
| NIV-02 | Niveaux | Référentiel de compétences | V2 | R6 | — |
| NIV-03 | Niveaux | Évaluation | V2 | R6 | — |
| NIV-04 | Niveaux | Critères de passage | V2 | R6 | — |
| NIV-05 | Niveaux | Passage de niveau | V2 | R6 | — |
| GRP-01 | Groupes | Groupe | MVP | R1 | — |
| GRP-02 | Groupes | Roster | MVP | R1 | — |
| GRP-03 | Groupes | Taux de remplissage | MVP | R1 | — |
| GRP-04 | Groupes | Groupes compatibles | MVP | R1 | — |
| GRP-05 | Groupes | Changement de groupe | MVP | R1 | — |
| GRP-06 | Groupes | Impression du roster | MVP | R2 | Sorti de R1 : feuille d'appel de secours, n'a de sens qu'avec des séances datées |
| PLA-01 | Planning | Saison | MVP | R1 | — |
| PLA-02 | Planning | Créneaux récurrents | MVP | R1 | — |
| PLA-03 | Planning | Génération des séances | MVP | R2 | Sorti de R1 : les séances datées ne servent pas au comptoir ; socle du planning coach |
| PLA-04 | Planning | Vues jour, semaine, mois | MVP | R2 | Sorti de R1 : requiert PLA-03 |
| PLA-05 | Planning | Fériés, vacances et indisponibilités de lieux | MVP | R1 | — |
| PLA-06 | Planning | Annulation, report | MVP | R3 | Sorti de R1 : dépend de NOT-01 (notification du groupe) |
| PLA-07 | Planning | Remplacement de coach | MVP | R2 | Sorti de R1 : requiert des séances datées |
| PLA-08 | Planning | Séance ponctuelle | MVP | R2 | Sorti de R1 : requiert des séances datées |
| PLA-09 | Planning | Horaires spéciaux | V2 | R2 | Avancé de V2 : Ramadan 2028 (fin janvier) doit être couvert avant la saison 2027-2028 |
| PLA-10 | Planning | Planning publiable | V2 | R2 | Avancé de V2 : remplace les affiches planning (D9), seul usage papier restant après R1 |
| RES-01 | Réservations | Cours d'essai | MVP | R2 | Sorti de R1 : un essai est une place sur une séance datée |
| RES-02 | Réservations | Liste d'attente par groupe | V2 | R6 | — |
| RES-03 | Réservations | Place réservée | V2 | R6 | — |
| RES-04 | Réservations | Séance à l'unité / carnet | MVP | R4 | Sorti de R1 : le débit se fait au pointage ; la vente du carnet (TAR-06) reste en R1 |
| RES-05 | Réservations | Rattrapage | V2 | R8 | — |
| RES-06 | Réservations | Cours privé | MVP | R2 | Sorti de R1 : un cours privé est une séance datée avec coach dédié |
| RES-07 | Réservations | Stages et événements | V2 | R7 | — |
| PRE-01 | Présences | Appel par le coach | MVP | R4 | Sorti de R1 : pointage introduit en début de saison 2027-2028 (pratique nouvelle, section 1.6) |
| PRE-02 | Présences | Pointage accueil | MVP | R4 | Sorti de R1 : avec PRE-01 ; recherche par nom seulement, le QR famille arrive avec APP-06 (R8) |
| PRE-03 | Présences | Statut administratif au pointage | MVP | R4 | Sorti de R1 : avec PRE-01 ; même release que IMP-05 (dépendance circulaire) |
| PRE-04 | Présences | Vue temps réel | MVP | R4 | Sorti de R1 : avec PRE-01 |
| PRE-05 | Présences | Absence déclarée | V2 | R8 | — |
| PRE-06 | Présences | Sortie de l'enfant | V2 | R4 | Avancé de V2 : « indispensable oublié » n° 1 (9.4), même écran que le pointage |
| PRE-07 | Présences | Historique et assiduité | MVP | R4 | Sorti de R1 : avec PRE-01 |
| PRE-08 | Présences | Mode hors ligne | V2 | R4 | Avancé de V2 : UC7 exige un appel qui fonctionne en réseau faible dès le premier jour (10.I) |
| INS-01 | Abonnements / Inscriptions | Inscription | MVP | R1 | — |
| INS-02 | Abonnements / Inscriptions | Multi-activités | MVP | R1 | — |
| INS-03 | Abonnements / Inscriptions | Formules | MVP | R1 | — |
| INS-04 | Abonnements / Inscriptions | Renouvellement | MVP | R3 | Sorti de R1 : dépend de NOT-03 |
| INS-05 | Abonnements / Inscriptions | Réinscription de saison | V2 | R6 | — |
| INS-06 | Abonnements / Inscriptions | Pause | V2 | R6 | — |
| INS-07 | Abonnements / Inscriptions | Résiliation | MVP | R2 | Sorti de R1 : quelques cas par saison ; l'avoir (FAC-04) existe dès R1 |
| INS-08 | Abonnements / Inscriptions | Changement d'activité | MVP | R2 | Sorti de R1 : quelques cas par saison ; le recalcul s'appuie sur FAC-04 |
| INS-09 | Abonnements / Inscriptions | Alerte d'expiration | MVP | R3 | Sorti de R1 : dépend de NOT-03 |
| TAR-01 | Tarification | Grille tarifaire | MVP | R1 | — |
| TAR-02 | Tarification | Offres par durée et tarifs saisonniers | MVP | R1 | — |
| TAR-03 | Tarification | Frais annuels | MVP | R1 | — |
| TAR-04 | Tarification | Remises automatiques | MVP | R1 | Priorité S dans le cahier, traitée comme M : UC2 est MVP |
| TAR-05 | Tarification | Remise manuelle | MVP | R1 | — |
| TAR-06 | Tarification | Packs de séances | MVP | R1 | Vente du carnet seulement ; débit au pointage = RES-04 (R4) |
| TAR-07 | Tarification | Codes promo | V2 | R5 | — |
| TAR-08 | Tarification | Tarif personnalisé | MVP | R1 | — |
| FAC-01 | Paiements | Facture famille | MVP | R1 | — |
| FAC-02 | Paiements | Échéancier | MVP | R1 | — |
| FAC-03 | Paiements | Encaissement | MVP | R1 | — |
| FAC-04 | Paiements | Avoir et remboursement | MVP | R1 | — |
| FAC-05 | Paiements | Ventilation | MVP | R1 | — |
| FAC-06 | Paiements | Solde famille | MVP | R1 | — |
| FAC-07 | Paiements | Reçu numéroté | MVP | R1 | PDF et impression ; l'envoi WhatsApp/email arrive avec NOT-01 (R3), dépendance à NOT-01 jugée fausse |
| FAC-08 | Paiements | Registre des chèques | MVP | R1 | La « tâche » sur rejet attend PER-03 (R5) ; en R1, créance réouverte et alerte dans la balance |
| FAC-09 | Paiements | Acompte / crédit famille | MVP | R1 | — |
| FAC-10 | Paiements | Lien de paiement en ligne | V2 | R8 | — |
| IMP-01 | Impayés | Balance âgée | MVP | R1 | — |
| IMP-02 | Impayés | Relances échelonnées | MVP | R3 | Sorti de R1 : les relances requièrent NOT-01 ; la balance âgée (IMP-01) reste en R1 |
| IMP-03 | Impayés | Promesse de paiement | MVP | R3 | Sorti de R1 : suspend des relances qui n'existent pas avant R3 |
| IMP-04 | Impayés | Tâche de recouvrement | V2 | R5 | — |
| IMP-05 | Impayés | Politique d'accès | MVP | R4 | Sorti de R1 : n'a d'effet qu'au pointage ; mode par défaut « alerter » |
| NOT-01 | Notifications | Moteur de notifications | MVP | R3 | Sorti de R1 : aucun message n'est nécessaire au comptoir, le parent est présent |
| NOT-02 | Notifications | Modèles | MVP | R3 | Sorti de R1 : avec NOT-01 ; délai d'approbation Meta des modèles WhatsApp à anticiper |
| NOT-03 | Notifications | Règles financières | MVP | R3 | Sorti de R1 : avec NOT-01 |
| NOT-04 | Notifications | Règles planning | MVP | R3 | Sorti de R1 : avec NOT-01 |
| NOT-05 | Notifications | Règles présence | V2 | R4 | Avancé de V2 : avec le pointage |
| NOT-06 | Notifications | Consentements et canaux | MVP | R3 | Sorti de R1 : avec NOT-01 ; le consentement lui-même est recueilli dès R1 par ONB-03 |
| NOT-07 | Notifications | Journal des envois | MVP | R3 | Sorti de R1 : avec NOT-01 |
| NOT-08 | Notifications | Autres règles | V2 | R3 | Avancé de V2 : l'alerte « certificat médical expiré » a la même mécanique que les règles financières |
| COM-01 | Communication parents | Message ciblé | MVP | R3 | Sorti de R1 : requiert NOT-01 |
| COM-02 | Communication parents | Historique par famille | MVP | R3 | Sorti de R1 : historique des messages, vide sans NOT-01 |
| COM-03 | Communication parents | Annonces | V2 | R8 | — |
| COM-04 | Communication parents | Message du coach | V2 | R8 | — |
| COA-01 | Coachs | Profil intervenant | MVP | R2 | Sorti de R1 : le coach n'intervient pas au comptoir ; créé sans PLT-02 (dépendance jugée surdimensionnée) |
| COA-02 | Coachs | Disponibilités et absences | V2 | R5 | — |
| COA-03 | Coachs | Planning du coach | MVP | R2 | Sorti de R1 : requiert PLA-04 |
| COA-04 | Coachs | Séances réalisées | MVP | R4 | Sorti de R1 : dépend de PRE-01 |
| REM-01 | Rémunération | Modèles de rémunération | V2 | R5 | — |
| REM-02 | Rémunération | État mensuel | V2 | R5 | — |
| REM-03 | Rémunération | Avances | V2 | R5 | — |
| REM-04 | Rémunération | Paiement de la rémunération | V2 | R5 | — |
| REM-05 | Rémunération | Export pour la paie | V2 | R5 | — |
| PER-01 | Personnel administratif | Fiche personnel et prestataires | V2 | R5 | — |
| PER-02 | Personnel administratif | Dépenses récurrentes | V2 | R5 | — |
| PER-03 | Personnel administratif | Tâches | V2 | R5 | — |
| POS-01 | POS | Vente comptoir | MVP | R1 | — |
| POS-02 | POS | Catalogue d'articles | MVP | R1 | — |
| POS-03 | POS | Retour et échange | V2 | R5 | — |
| POS-04 | POS | Article lié à une activité | V2 | R5 | — |
| STK-01 | Stock | Stock par article et variante | V2 | R5 | — |
| STK-02 | Stock | Entrées, sorties, ajustements | V2 | R5 | — |
| STK-03 | Stock | Seuil et alerte | V2 | R5 | — |
| STK-04 | Stock | Inventaire | V3 | écarté | 3 à 5 articles vendus (9.3) |
| STK-05 | Stock | Coût d'achat, marge, fournisseurs | V3 | écarté | 3 à 5 articles vendus (9.3) |
| FIN-01 | Finance | Session de caisse | MVP | R1 | — |
| FIN-02 | Finance | Journal des dépenses | MVP | R1 | — |
| FIN-03 | Finance | Bordereau de remise | MVP | R1 | — |
| FIN-04 | Finance | Comptes de trésorerie | MVP | R1 | — |
| FIN-05 | Finance | Résultat mensuel | MVP | R1 | — |
| FIN-06 | Finance | Rapprochement bancaire | V3 | écarté | Relève du comptable (9.2) ; à reprendre si un club le demande |
| FIN-07 | Finance | Produits constatés d'avance | V3 | écarté | Vue de gestion comptable, hors application (9.2) |
| CPT-01 | Comptabilité | Exports comptables | V2 | R3 | Avancé de V2 : premier exercice comptable tenu dans Clubify clos fin 2027 ; à confirmer avec le comptable |
| CPT-02 | Comptabilité | Taxes paramétrables | MVP | R1 | — |
| CPT-03 | Comptabilité | État des espèces encaissées | V2 | R3 | Avancé de V2 : même chantier que CPT-01 ; reste désactivable (décision 0012) |
| CPT-04 | Comptabilité | Clôture de période | V2 | R3 | Avancé de V2 : même chantier que CPT-01 |
| CPT-05 | Comptabilité | Format de facture structuré | V3 | écarté | Décret de facturation électronique non publié ; le modèle de facture de R1 (numérotation continue, taxes, avoir) suffit à préparer le raccordement |
| REP-01 | Reporting | Tableau de bord opérationnel | MVP | R4 | Sorti de R1 : dépend de PRE-04 |
| REP-02 | Reporting | Bloc inscriptions | MVP | R3 | Sorti de R1 : dépend de INS-09 |
| REP-03 | Reporting | Bloc finances | MVP | R3 | Sorti de R1 : vue de synthèse ; R1 a déjà la balance âgée et la session de caisse |
| REP-04 | Reporting | Rapports standard | V2 | R6 | — |
| REP-05 | Reporting | Rapport planifié | V2 | R3 | Avancé de V2 : coût faible une fois NOT-01 en place ; le gérant reçoit la caisse du jour |
| REP-06 | Reporting | Rétention | V3 | écarté | N'a de sens qu'après deux saisons de réinscriptions dans l'outil ; à reprendre après R6 |
| MKT-01 | Marketing | Campagnes WhatsApp / SMS / email | V2 | R8 | — |
| MKT-02 | Marketing | Parrainage | V3 | écarté | Aucun besoin observé |
| MKT-03 | Marketing | Relance des anciens | V2 | R6 | — |
| MKT-04 | Marketing | Aide à la rédaction par IA | V3 | écarté | Priorité F, section 9.3 |
| APP-01 | Application parent | Portail web mobile | V2 | R8 | — |
| APP-02 | Application parent | Enfants et planning | V2 | R8 | — |
| APP-03 | Application parent | Factures et solde | V2 | R8 | — |
| APP-04 | Application parent | Présences et progression | V2 | R8 | — |
| APP-05 | Application parent | Démarches | V2 | R8 | — |
| APP-06 | Application parent | Carte famille QR | V2 | R8 | — |
| APP-07 | Application parent | Application native et push | V3 | écarté | Section 9.1 : portail web mobile + WhatsApp suffisent |
| APC-01 | Application coach | Mes séances du jour | MVP | R2 | Sorti de R1 : requiert COA-03 |
| APC-02 | Application coach | Appel | MVP | R4 | Sorti de R1 : identifiant identique à PRE-01 (« Voir PRE-01 ») |
| APC-03 | Application coach | Fiche enfant réduite | MVP | R4 | Sorti de R1 : avec l'appel coach |
| APC-04 | Application coach | Évaluations | V2 | R6 | — |
| APC-05 | Application coach | Mes séances et ma rémunération | V2 | R5 | — |
| ADM-01 | Administration | Paramètres du club | MVP | R1 | — |
| ADM-02 | Administration | Assistant de démarrage | MVP | R9 | Sorti de R1 : le club pilote est paramétré avec nous ; utile au deuxième club |
| ADM-03 | Administration | Champs personnalisés | V2 | R9 | — |
| ADM-04 | Administration | Modèles de documents | MVP | R1 | — |
| ADM-05 | Administration | Règles d'automatisation | V2 | R5 | — |
| SEC-01 | Sécurité | Authentification | MVP | R1 | — |
| SEC-02 | Sécurité | Rôles et permissions | MVP | R1 | — |
| SEC-03 | Sécurité | Données sensibles | MVP | R1 | — |
| SEC-04 | Sécurité | Journal d'audit | MVP | R1 | — |
| SEC-05 | Sécurité | Droits des personnes | V2 | R4 | Avancé de V2 : droits des personnes (loi 09-08), dus dès la première année d'exploitation |
| SEC-06 | Sécurité | Durées de conservation | V2 | R4 | Avancé de V2 : premier anniversaire des dossiers |
| INT-01 | API / Intégrations | Messagerie | MVP | R3 | Sorti de R1 : connecteur messagerie, sans usage avant NOT-01 ; l'abstraction est posée en R1 |
| INT-02 | API / Intégrations | Paiement | V2 | R8 | — |
| INT-03 | API / Intégrations | Exports | MVP | R1 | — |
| INT-04 | API / Intégrations | API publique et webhooks | V3 | écarté | Priorité F, section 9.3 |
| INT-05 | API / Intégrations | Calendrier | V3 | écarté | Aucun besoin exprimé |
| PLT-01 | Multi-clubs / multi-sites | Multi-tenant | MVP | R1 | Porte aussi l'identifiant de site (9.6) |
| PLT-02 | Multi-clubs / multi-sites | Utilisateur multi-clubs | MVP | R9 | Sorti de R1 : un seul club ; le rôle par club est dans le modèle dès R1, l'écran arrive avec le deuxième club |
| PLT-03 | Multi-clubs / multi-sites | Sites | V3 | écarté | Écarté comme écran ; l'identifiant de site est posé en R1 (invariant) ; écrans quand un club multi-sites signe |
| PLT-04 | Multi-clubs / multi-sites | Bus d'événements métier | MVP | R1 | — |
| PLT-05 | Multi-clubs / multi-sites | Gestion documentaire | MVP | R1 | — |
| PLT-06 | Multi-clubs / multi-sites | Consolidation multi-sites | V3 | écarté | Priorité F, section 9.3 |
| PLT-07 | Multi-clubs / multi-sites | Abonnement SaaS | V2 | R9 | — |
| PLT-08 | Multi-clubs / multi-sites | Langues | V2 | R8 | Architecture i18n posée en R1 (invariant) ; arabe et anglais livrés avec le portail parent |
| OFR-01 | Paramétrage de l'offre | Formule paramétrable | MVP | R1 | — |
| OFR-02 | Paramétrage de l'offre | Forfait combiné | MVP | R1 | — |
| OFR-03 | Paramétrage de l'offre | Règles de combinaison | MVP | R1 | — |
| OFR-04 | Paramétrage de l'offre | Matrice de prix éditable | MVP | R1 | — |
| OFR-05 | Paramétrage de l'offre | Simulateur de prix | MVP | R1 | — |
| OFR-06 | Paramétrage de l'offre | Brouillon et publication | MVP | R2 | Sorti de R1 : utile quand le gérant retouche son offre, pas à la saisie initiale faite avec lui |
| OFR-07 | Paramétrage de l'offre | Contrôle de cohérence | MVP | R2 | Sorti de R1 : même raison que OFR-06 |
| OFR-08 | Paramétrage de l'offre | Duplication de saison | V2 | R2 | Avancé de V2 : indispensable pour préparer la saison 2027-2028 en juillet 2027 ; dépendance à INS-05 jugée fausse |
| EVT-01 | Camp d'été et fin d'année | Programme de camp | V2 | R7 | — |
| EVT-02 | Camp d'été et fin d'année | Emploi du temps journalier | V2 | R7 | — |
| EVT-03 | Camp d'été et fin d'année | Inscription au camp, adhérent ou non | V2 | R7 | — |
| EVT-04 | Camp d'été et fin d'année | Tarif du camp | V2 | R7 | — |
| EVT-05 | Camp d'été et fin d'année | Journée continue | V2 | R7 | — |
| EVT-06 | Camp d'été et fin d'année | Cérémonie de fin d'année | MVP | R3 | Sorti de R1 : cérémonie de juin, annoncée par WhatsApp ; première édition couverte juin 2027 si R3 est en service, sinon juin 2028 |
| EVT-07 | Camp d'été et fin d'année | Certificats et diplômes sportifs | V2 | R6 | — |
| EVT-08 | Camp d'été et fin d'année | Annonce de masse aux parents | MVP | R3 | Sorti de R1 : dépend de COM-01 et NOT-07 |

## Manques : dans aucune release, et devrait y être

1. **Solde d'ouverture famille** (reprise des reliquats à la mise en service de R1 en janvier 2027) : aucun identifiant du backlog ne le couvre, et le reste à payer n'est écrit nulle part aujourd'hui (section 1.4). Proposition : nouvel identifiant `FAC-11`, en R1, saisi par l'accueil, validé par le gérant, audité, sans facture rétroactive.
2. **Dossier d'aide CNDP et contrat de sous-traitance** (décision 0018) : livrables non logiciels, préalables à R1.
3. **Choix d'hébergement et localisation des données** (section 5, décision 0018) : préalable à R1.
4. **Frais de rejet de chèque** (UC5) : paramètre à cadrer dans la feature FAC-08, R1.
5. **Recalcul du rang fratrie après résiliation** (décision 0021) : à cadrer dans TAR-04 en R1 ; devient concret en R2 avec INS-07.
6. **Formation et conduite du changement** : accueil en R1, coachs en R4 ; hors backlog, à planifier avec le club.

## À confirmer

1. Fiche santé FAM-07 en R1 : hypothèse retenue ; à confirmer avec le juriste du club (autorisation préalable CNDP pour les données de santé).
2. Solde d'ouverture : qui saisit, sur quelle base, qui valide ?
3. Exercice comptable du club (calendaire ?) : conditionne la fenêtre de CPT-01.
4. Cérémonie de juin 2027 : couverte si R3 est en service en mai 2027, sinon papier.
5. Fournisseur WhatsApp Business et délai d'approbation des modèles : à lancer avant R3.
6. Dossier CNDP déposé avant janvier 2027 ?
7. Les 13 écartés, en particulier FIN-06 (rapprochement bancaire) et PLT-03 (écrans multi-sites).

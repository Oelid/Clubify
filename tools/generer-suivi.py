"""Génère le classeur de suivi des features de Clubify.

Source de vérité du périmètre : docs/couverture-backlog.md (identifiants et releases),
docs/decoupage-features.md (features) et docs/roadmap.md (releases et fenêtres).
Source de vérité de l'avancement : le classeur lui-même.

Le script (re)génère la structure et conserve les saisies manuelles (état, dates,
responsable, commentaire) en les recollant sur la clé de chaque ligne. Le relancer
après tout changement de périmètre ou de découpage.

    python tools/generer-suivi.py

Dépendance : openpyxl.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from openpyxl import Workbook, load_workbook
from openpyxl.formatting.rule import CellIsRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

RACINE = Path(__file__).resolve().parent.parent
COUVERTURE = RACINE / "docs" / "couverture-backlog.md"
DECOUPAGE = RACINE / "docs" / "decoupage-features.md"
ROADMAP = RACINE / "docs" / "roadmap.md"
SORTIE = RACINE / "docs" / "suivi-features.xlsx"

ETATS = ["Waiting", "Cadrage", "Dev", "In test", "Production"]

# Correspondance avec docs/processus-feature.md, rappelée dans la feuille Synthèse.
ETAPES = {
    "Waiting": "Pas commencée",
    "Cadrage": "Étapes 1 et 2 : fiche, questions, benchmark",
    "Dev": "Étapes 3 à 5 : plan, tests d'abord, implémentation",
    "In test": "Étape 5 : tests verts, recette",
    "Production": "Étape 6 : livrée, validée, en service",
}

# Colonnes saisies à la main, préservées d'une génération à l'autre.
COLONNES_SAISIES = [
    "État", "Responsable", "Début cadrage", "Fin cadrage", "Début dev",
    "Fin dev", "Début test", "Fin test", "Date production", "Commentaire",
]

ENTETES_FEATURES = [
    "Feature", "Nom", "Release", "Identifiants du backlog", "Nb ID", "État",
    "Responsable", "Début cadrage", "Fin cadrage", "Début dev", "Fin dev",
    "Début test", "Fin test", "Durée cycle (j)", "Date production",
    "Délai test → prod (j)", "Commentaire",
]

ENTETES_RELEASE = [
    "ID", "Domaine", "Libellé", "Cahier", "Feature", "État", "Date production",
]

BLEU = "1F3B4D"
GRIS = "F2F4F5"
COULEURS_ETAT = {
    "Waiting": "E8EAED",
    "Cadrage": "FDE9D3",
    "Dev": "FCE8B2",
    "In test": "CFE3EC",
    "Production": "C6E7C6",
}

BORDURE = Border(*[Side(style="thin", color="D5D9DC")] * 4)


def lire_couverture() -> list[dict]:
    """Retourne les lignes du backlog depuis docs/couverture-backlog.md."""
    motif = re.compile(r"^\|\s*([A-Z]{3}-\d{2})\s*\|")
    lignes = []
    for ligne in COUVERTURE.read_text(encoding="utf-8").splitlines():
        if not motif.match(ligne):
            continue
        cellules = [c.strip() for c in ligne.strip().strip("|").split("|")]
        lignes.append({
            "id": cellules[0],
            "domaine": cellules[1],
            "libelle": cellules[2],
            "cahier": cellules[3],
            "release": cellules[4],
        })
    return lignes


def lire_decoupage() -> list[tuple[str, str, str, list[str]]]:
    """Retourne les features depuis docs/decoupage-features.md.

    Les tableaux par release y donnent : Feature, Nom, Identifiants, Nb.
    """
    motif = re.compile(r"^\|\s*(F\d{2})\s*\|")
    titre_release = re.compile(r"^##\s+(R\d)\s+—")
    features = []
    release = None
    for ligne in DECOUPAGE.read_text(encoding="utf-8").splitlines():
        entete = titre_release.match(ligne)
        if entete:
            release = entete.group(1)
            continue
        if not motif.match(ligne) or release is None:
            continue
        cellules = [c.strip() for c in ligne.strip().strip("|").split("|")]
        identifiants = [i.strip() for i in cellules[2].split(",") if i.strip()]
        features.append((cellules[0], cellules[1], release, identifiants))
    return features


def verifier(features, backlog) -> list[str]:
    """Contrôle que chaque identifiant non écarté appartient à une feature et une seule."""
    release_par_id = {e["id"]: e["release"] for e in backlog}
    vus: dict[str, str] = {}
    anomalies = []
    for code, _, release, identifiants in features:
        for identifiant in identifiants:
            if identifiant not in release_par_id:
                anomalies.append(f"{code} : {identifiant} absent du backlog")
            elif release_par_id[identifiant] != release:
                anomalies.append(
                    f"{code} ({release}) : {identifiant} est affecté à "
                    f"{release_par_id[identifiant]} dans la couverture")
            if identifiant in vus:
                anomalies.append(
                    f"{identifiant} est dans {vus[identifiant]} et dans {code}")
            vus[identifiant] = code
    for identifiant, release in release_par_id.items():
        if release != "écarté" and identifiant not in vus:
            anomalies.append(f"{identifiant} ({release}) n'est dans aucune feature")
    return anomalies


def lire_releases() -> dict[str, dict]:
    """Retourne le nom et la fenêtre de mise en service de chaque release."""
    texte = ROADMAP.read_text(encoding="utf-8")
    releases: dict[str, dict] = {}
    courante = None
    for ligne in texte.splitlines():
        titre = re.match(r"^##\s+(R\d)\s+—\s+(.+)$", ligne)
        if titre:
            courante = titre.group(1)
            releases[courante] = {"nom": titre.group(2).strip(), "fenetre": ""}
            continue
        if courante and "Fenêtre de mise en service" in ligne:
            fenetre = ligne.split(":", 1)[1].strip()
            fenetre = re.split(r"\.\s|\sSi ratée", fenetre)[0].strip().rstrip(".")
            releases[courante]["fenetre"] = fenetre
            courante = None
    return releases


def lire_saisies(chemin: Path) -> dict[str, dict]:
    """Relit les colonnes saisies à la main du classeur existant."""
    if not chemin.exists():
        return {}
    try:
        classeur = load_workbook(chemin, data_only=False)
    except Exception as erreur:  # classeur illisible : on repart à neuf
        print(f"  ! classeur existant illisible ({erreur}), saisies non reprises")
        return {}
    if "Features" not in classeur.sheetnames:
        return {}
    feuille = classeur["Features"]
    entetes = [c.value for c in feuille[1]]
    index = {nom: i for i, nom in enumerate(entetes) if nom}
    saisies = {}
    for ligne in feuille.iter_rows(min_row=2, values_only=True):
        cle = ligne[0]
        if not cle:
            continue
        saisies[cle] = {
            nom: ligne[index[nom]]
            for nom in COLONNES_SAISIES
            if nom in index and index[nom] < len(ligne)
        }
    classeur.close()
    return saisies


def styler_entetes(feuille, nb_colonnes: int) -> None:
    for colonne in range(1, nb_colonnes + 1):
        cellule = feuille.cell(row=1, column=colonne)
        cellule.font = Font(bold=True, color="FFFFFF", size=11)
        cellule.fill = PatternFill("solid", fgColor=BLEU)
        cellule.alignment = Alignment(vertical="center", wrap_text=True)
        cellule.border = BORDURE
    feuille.row_dimensions[1].height = 30


def regle_couleurs(feuille, plage: str) -> None:
    for etat, couleur in COULEURS_ETAT.items():
        feuille.conditional_formatting.add(
            plage,
            CellIsRule(operator="equal", formula=[f'"{etat}"'],
                       fill=PatternFill("solid", fgColor=couleur)),
        )


def construire_features(classeur: Workbook, features, saisies: dict[str, dict]):
    feuille = classeur.create_sheet("Features")
    feuille.append(ENTETES_FEATURES)
    styler_entetes(feuille, len(ENTETES_FEATURES))

    lignes = [(code, nom, release, ", ".join(identifiants), len(identifiants))
              for code, nom, release, identifiants in features]

    for numero, (code, nom, release, identifiants, nb) in enumerate(lignes, start=2):
        reprise = saisies.get(code, {})
        feuille.cell(row=numero, column=1, value=code)
        feuille.cell(row=numero, column=2, value=nom)
        feuille.cell(row=numero, column=3, value=release)
        feuille.cell(row=numero, column=4, value=identifiants)
        feuille.cell(row=numero, column=5, value=nb)
        feuille.cell(row=numero, column=6, value=reprise.get("État") or "Waiting")
        feuille.cell(row=numero, column=7, value=reprise.get("Responsable"))
        for decalage, titre in enumerate(
                ["Début cadrage", "Fin cadrage", "Début dev", "Fin dev",
                 "Début test", "Fin test"]):
            cellule = feuille.cell(row=numero, column=8 + decalage,
                                   value=reprise.get(titre))
            cellule.number_format = "DD/MM/YYYY"
        feuille.cell(row=numero, column=14,
                     value=f"=IF(AND(H{numero}<>\"\",M{numero}<>\"\"),M{numero}-H{numero},\"\")")
        prod = feuille.cell(row=numero, column=15, value=reprise.get("Date production"))
        prod.number_format = "DD/MM/YYYY"
        feuille.cell(row=numero, column=16,
                     value=f"=IF(AND(M{numero}<>\"\",O{numero}<>\"\"),O{numero}-M{numero},\"\")")
        feuille.cell(row=numero, column=17, value=reprise.get("Commentaire"))
        for colonne in range(1, len(ENTETES_FEATURES) + 1):
            feuille.cell(row=numero, column=colonne).border = BORDURE

    derniere = len(lignes) + 1
    validation = DataValidation(type="list", formula1=f'"{",".join(ETATS)}"',
                                allow_blank=False)
    feuille.add_data_validation(validation)
    validation.add(f"F2:F{derniere}")
    regle_couleurs(feuille, f"F2:F{derniere}")

    feuille.freeze_panes = "C2"
    feuille.auto_filter.ref = f"A1:{get_column_letter(len(ENTETES_FEATURES))}{derniere}"
    largeurs = [9, 34, 9, 46, 7, 12, 16, 14, 14, 13, 13, 13, 13, 13, 15, 16, 40]
    for colonne, largeur in enumerate(largeurs, start=1):
        feuille.column_dimensions[get_column_letter(colonne)].width = largeur
    return derniere


def construire_release(classeur: Workbook, code: str, titre: str,
                       entrees: list[dict], feature_par_id: dict[str, str],
                       derniere_feature: int) -> None:
    feuille = classeur.create_sheet(code)
    feuille.append(ENTETES_RELEASE)
    styler_entetes(feuille, len(ENTETES_RELEASE))

    for numero, entree in enumerate(entrees, start=2):
        feature = feature_par_id.get(entree["id"], code)
        feuille.cell(row=numero, column=1, value=entree["id"])
        feuille.cell(row=numero, column=2, value=entree["domaine"])
        feuille.cell(row=numero, column=3, value=entree["libelle"])
        feuille.cell(row=numero, column=4, value=entree["cahier"])
        feuille.cell(row=numero, column=5, value=feature)
        # L'état d'un identifiant est celui de sa feature : aucune double saisie.
        feuille.cell(
            row=numero, column=6,
            value=f'=IFERROR(VLOOKUP($E{numero},Features!$A$2:$Q${derniere_feature},6,FALSE),"")')
        cellule = feuille.cell(
            row=numero, column=7,
            value=f'=IFERROR(VLOOKUP($E{numero},Features!$A$2:$Q${derniere_feature},15,FALSE),"")')
        cellule.number_format = "DD/MM/YYYY"
        for colonne in range(1, len(ENTETES_RELEASE) + 1):
            feuille.cell(row=numero, column=colonne).border = BORDURE

    derniere = len(entrees) + 1
    regle_couleurs(feuille, f"F2:F{derniere}")
    feuille.freeze_panes = "B2"
    feuille.auto_filter.ref = f"A1:{get_column_letter(len(ENTETES_RELEASE))}{derniere}"
    for colonne, largeur in enumerate([10, 26, 44, 9, 10, 12, 15], start=1):
        feuille.column_dimensions[get_column_letter(colonne)].width = largeur
    feuille.cell(row=derniere + 2, column=1, value=titre).font = Font(italic=True,
                                                                     color="6B7780")


def construire_synthese(classeur: Workbook, backlog: list[dict],
                        releases: dict[str, dict], derniere_feature: int) -> None:
    feuille = classeur.create_sheet("Synthèse", 0)
    feuille["A1"] = "Clubify — suivi des features"
    feuille["A1"].font = Font(bold=True, size=16, color=BLEU)
    feuille["A2"] = ("Généré par tools/generer-suivi.py depuis docs/couverture-backlog.md "
                     "et docs/roadmap.md. Les états et les dates se saisissent dans la "
                     "feuille Features ; les feuilles par release les reprennent.")
    feuille["A2"].font = Font(italic=True, color="6B7780", size=10)
    feuille["A2"].alignment = Alignment(wrap_text=True, vertical="top")
    feuille.merge_cells("A2:L3")

    entetes = ["Release", "Nom", "Fenêtre de mise en service", "Features",
               *ETATS, "% terminé", "Identifiants"]
    depart = 5
    for colonne, titre in enumerate(entetes, start=1):
        feuille.cell(row=depart, column=colonne, value=titre)
    styler_entetes_ligne(feuille, depart, len(entetes))

    compte_ids: dict[str, int] = {}
    for entree in backlog:
        compte_ids[entree["release"]] = compte_ids.get(entree["release"], 0) + 1

    codes = sorted(releases)
    for decalage, code in enumerate(codes):
        ligne = depart + 1 + decalage
        feuille.cell(row=ligne, column=1, value=code)
        feuille.cell(row=ligne, column=2, value=releases[code]["nom"])
        feuille.cell(row=ligne, column=3, value=releases[code]["fenetre"])
        feuille.cell(row=ligne, column=4,
                     value=f'=COUNTIF(Features!$C$2:$C${derniere_feature},$A{ligne})')
        for index, etat in enumerate(ETATS):
            colonne = 5 + index
            feuille.cell(
                row=ligne, column=colonne,
                value=(f'=COUNTIFS(Features!$C$2:$C${derniere_feature},$A{ligne},'
                       f'Features!$F$2:$F${derniere_feature},"{etat}")'))
        pourcentage = feuille.cell(
            row=ligne, column=10,
            value=f'=IF($D{ligne}=0,"",$I{ligne}/$D{ligne})')
        pourcentage.number_format = "0 %"
        feuille.cell(row=ligne, column=11, value=compte_ids.get(code, 0))
        for colonne in range(1, len(entetes) + 1):
            feuille.cell(row=ligne, column=colonne).border = BORDURE

    ligne_total = depart + 1 + len(codes)
    feuille.cell(row=ligne_total, column=1, value="Total").font = Font(bold=True)
    for colonne in [4, *range(5, 10), 11]:
        lettre = get_column_letter(colonne)
        cellule = feuille.cell(
            row=ligne_total, column=colonne,
            value=f"=SUM({lettre}{depart + 1}:{lettre}{ligne_total - 1})")
        cellule.font = Font(bold=True)
    total_pct = feuille.cell(
        row=ligne_total, column=10,
        value=f'=IF($D{ligne_total}=0,"",$I{ligne_total}/$D{ligne_total})')
    total_pct.font = Font(bold=True)
    total_pct.number_format = "0 %"
    for colonne in range(1, len(entetes) + 1):
        cellule = feuille.cell(row=ligne_total, column=colonne)
        cellule.border = BORDURE
        cellule.fill = PatternFill("solid", fgColor=GRIS)

    ecarte = compte_ids.get("écarté", 0)
    ligne_note = ligne_total + 2
    feuille.cell(row=ligne_note, column=1,
                 value=f"{ecarte} identifiants écartés, voir la feuille Écartés.")
    feuille.cell(row=ligne_note, column=1).font = Font(italic=True, color="6B7780")

    ligne_legende = ligne_note + 2
    feuille.cell(row=ligne_legende, column=1, value="États").font = Font(bold=True)
    for decalage, etat in enumerate(ETATS, start=1):
        cellule = feuille.cell(row=ligne_legende + decalage, column=1, value=etat)
        cellule.fill = PatternFill("solid", fgColor=COULEURS_ETAT[etat])
        cellule.border = BORDURE
        feuille.cell(row=ligne_legende + decalage, column=2, value=ETAPES[etat])

    ligne_dates = ligne_legende + len(ETATS) + 2
    feuille.cell(row=ligne_dates, column=1, value="Dates").font = Font(bold=True)
    feuille.cell(row=ligne_dates + 1, column=1,
                 value="Cycle de développement : de « Début cadrage » à « Fin test ».")
    feuille.cell(row=ligne_dates + 2, column=1,
                 value="Mise en production : colonne « Date production », distincte du cycle.")

    for colonne, largeur in enumerate([12, 40, 34, 10, 10, 10, 8, 10, 12, 11, 13],
                                      start=1):
        feuille.column_dimensions[get_column_letter(colonne)].width = largeur


def styler_entetes_ligne(feuille, ligne: int, nb_colonnes: int) -> None:
    for colonne in range(1, nb_colonnes + 1):
        cellule = feuille.cell(row=ligne, column=colonne)
        cellule.font = Font(bold=True, color="FFFFFF", size=11)
        cellule.fill = PatternFill("solid", fgColor=BLEU)
        cellule.alignment = Alignment(vertical="center", wrap_text=True,
                                      horizontal="center")
        cellule.border = BORDURE
    feuille.row_dimensions[ligne].height = 30


def main() -> int:
    if not all(p.exists() for p in (COUVERTURE, DECOUPAGE, ROADMAP)):
        print("un fichier source est introuvable dans docs/", file=sys.stderr)
        return 1

    backlog = lire_couverture()
    features = lire_decoupage()
    releases = lire_releases()
    saisies = lire_saisies(SORTIE)
    print(f"  {len(backlog)} identifiants, {len(features)} features, "
          f"{len(releases)} releases, {len(saisies)} lignes de saisies reprises")

    anomalies = verifier(features, backlog)
    if anomalies:
        print("  Incohérences entre le découpage et la couverture :", file=sys.stderr)
        for anomalie in anomalies:
            print(f"    - {anomalie}", file=sys.stderr)
        return 1

    feature_par_id = {
        identifiant: code
        for code, _, _, identifiants in features
        for identifiant in identifiants
    }

    classeur = Workbook()
    classeur.remove(classeur.active)

    derniere_feature = construire_features(classeur, features, saisies)

    for code in sorted(releases):
        entrees = [e for e in backlog if e["release"] == code]
        construire_release(classeur, code, f"{code} — {releases[code]['nom']}",
                           entrees, feature_par_id, derniere_feature)

    ecartes = [e for e in backlog if e["release"] == "écarté"]
    if ecartes:
        construire_release(classeur, "Écartés", "Hors du plan de releases",
                           ecartes, {}, derniere_feature)

    construire_synthese(classeur, backlog, releases, derniere_feature)
    classeur.active = 0
    try:
        classeur.save(SORTIE)
    except PermissionError:
        print(f"  {SORTIE.name} est verrouillé : ferme-le dans Excel et relance.",
              file=sys.stderr)
        return 1
    print(f"  écrit : {SORTIE.relative_to(RACINE)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

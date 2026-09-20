"""Génère le classeur de suivi des tests fonctionnels de Clubify.

Ce suivi porte sur **ce qu'on vérifie depuis les écrans** : les parcours que
l'accueil et le gérant feront réellement. Les tests unitaires, d'intégration et
d'architecture sont le filet du développement, pas la recette d'une feature ; le
classeur n'en donne que le compte, dans la synthèse.

Trois sources, et aucune saisie dans le classeur :

- les **scénarios**, décrits en langage métier dans `docs/suivi-tests.md` ;
- les **résultats**, lus dans le rapport JUnit de Playwright : un scénario est
  vert parce qu'il a été joué, jamais parce qu'on l'a déclaré tel ;
- les **critères d'acceptation**, lus dans les fiches de `docs/features/`, pour
  que la recette et la fiche ne divergent pas.

Usage :

    python tools/generer-suivi-tests.py              # lit les rapports existants
    python tools/generer-suivi-tests.py --executer   # relance la recette d'abord

Dépendance : openpyxl.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

from openpyxl import Workbook
from openpyxl.formatting.rule import CellIsRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

RACINE = Path(__file__).resolve().parent.parent
SOURCE = RACINE / "docs" / "suivi-tests.md"
FEATURES = RACINE / "docs" / "features"
RAPPORTS = RACINE / "reports"
SORTIE = RACINE / "docs" / "suivi-tests.xlsx"

COULEURS_ETAT = {
    "Vert": "C6E7C6",
    "Rouge": "F5C6C6",
    "Ignoré": "E8E8E8",
    "Non joué": "FDEBC8",
    "À écrire": "FDEBC8",
    "Manuel": "D8E4F0",
}

TITRE = Font(bold=True, color="FFFFFF")
FOND_TITRE = PatternFill("solid", fgColor="2E4057")
GRAS = Font(bold=True)
BORDURE = Border(*(Side(style="thin", color="D9D9D9"),) * 4)
HAUT = Alignment(vertical="top", wrap_text=True)

IDENTIFIANT = re.compile(r"\b(S\d{2,3})\b")


@dataclass
class Scenario:
    """Un parcours vérifié depuis les écrans."""

    identifiant: str
    feature: str
    groupe: str
    intitule: str
    prealable: str
    etapes: list[str]
    attendu: str
    criteres: list[str]
    mode: str
    etat: str = "Non joué"
    duree: float | None = None
    detail: str = ""


@dataclass
class Feature:
    code: str
    nom: str = ""
    criteres: list[str] = field(default_factory=list)


# -------------------------------------------------------------------- source

def lire_les_scenarios(texte: str) -> list[Scenario]:
    """Les scénarios, tels que le fichier les raconte.

    Le format est celui qu'on lit sans effort : un titre par scénario, puis des
    lignes « - **Champ** : valeur ». Les étapes sont numérotées.
    """
    scenarios: list[Scenario] = []
    feature = ""
    groupe = ""
    courant: dict[str, object] | None = None

    def clore() -> None:
        if courant is None:
            return
        scenarios.append(Scenario(
            identifiant=str(courant["id"]),
            feature=str(courant["feature"]),
            groupe=str(courant["groupe"]),
            intitule=str(courant["intitule"]),
            prealable=str(courant.get("Préalable", "")),
            etapes=list(courant.get("etapes", [])),
            attendu=str(courant.get("Attendu", "")),
            criteres=[c.strip() for c in str(courant.get("Critères", "")).split(",")
                      if c.strip() and c.strip() != "—"],
            mode=str(courant.get("Mode", "")),
        ))

    for ligne in texte.splitlines():
        depouillee = ligne.strip()

        if ligne.startswith("# F"):
            clore()
            courant = None
            feature = ligne[2:].split("—")[0].strip()
            continue
        if ligne.startswith("## "):
            clore()
            courant = None
            groupe = ligne[3:].strip()
            continue
        if ligne.startswith("### "):
            clore()
            titre = ligne[4:].strip()
            identifiant, _, intitule = titre.partition("—")
            courant = {"id": identifiant.strip(), "intitule": intitule.strip(),
                       "feature": feature, "groupe": groupe, "etapes": []}
            continue
        if courant is None:
            continue

        champ = re.match(r"- \*\*(.+?)\*\*\s*:?\s*(.*)$", depouillee)
        if champ:
            courant[champ.group(1)] = champ.group(2).strip()
            continue
        etape = re.match(r"\d+\.\s+(.*)$", depouillee)
        if etape:
            courant["etapes"].append(etape.group(1).strip())

    clore()
    return scenarios


def tableaux_markdown(texte: str) -> dict[str, list[dict[str, str]]]:
    """Les tableaux du fichier source, indexés par le titre qui les précède."""
    tableaux: dict[str, list[dict[str, str]]] = {}
    titre = ""
    entetes: list[str] | None = None

    for ligne in texte.splitlines():
        if ligne.startswith("## "):
            titre, entetes = ligne[3:].strip(), None
            continue
        if not ligne.strip().startswith("|"):
            entetes = None
            continue

        cellules = [c.strip() for c in ligne.strip().strip("|").split("|")]
        if entetes is None:
            entetes = cellules
            tableaux.setdefault(titre, [])
            continue
        if all(set(c) <= {"-", ":", " "} for c in cellules):
            continue
        tableaux[titre].append(dict(zip(entetes, cellules)))

    return tableaux


def criteres_de_la_fiche(chemin: Path) -> tuple[str, list[str]]:
    """Nom de la feature et critères d'acceptation, dans l'ordre de la fiche."""
    texte = chemin.read_text(encoding="utf-8")
    nom = texte.splitlines()[0].lstrip("# ").strip()
    nom = re.sub(r"^Feature\s*:\s*", "", nom)
    nom = re.sub(r"^F\d+\s*[—-]\s*", "", nom)

    dans_les_criteres = False
    criteres: list[str] = []
    for ligne in texte.splitlines():
        if ligne.startswith("## "):
            dans_les_criteres = ligne.startswith("## Critères d'acceptation")
            continue
        if dans_les_criteres:
            for reference in re.findall(r"\b(C\d+[a-z]?)\b", ligne):
                if reference not in criteres:
                    criteres.append(reference)
    return nom, criteres


# ----------------------------------------------------------------- résultats

def executer_la_recette() -> None:
    """Relance la recette fonctionnelle et écrit son rapport JUnit."""
    RAPPORTS.mkdir(exist_ok=True)
    manquantes = [nom for nom in ("E2E_ADMIN_EMAIL", "E2E_ADMIN_PASSWORD",
                                  "E2E_ADMIN_TOTP_SECRET")
                  if not __import__("os").environ.get(nom)]
    if manquantes:
        print("  recette non lancée : " + ", ".join(manquantes) + " manquent "
              "(voir docs/suivi-tests.md)", file=sys.stderr)
        return
    subprocess.run(["cmd", "/c", "npx", "playwright", "test"],
                   cwd=RACINE / "frontend", check=False)


def resultats_fonctionnels() -> tuple[dict[str, tuple[str, float, str, str]], datetime | None]:
    """Résultat par identifiant de scénario, et date du dernier passage."""
    fichier = RAPPORTS / "e2e-junit.xml"
    if not fichier.exists():
        return {}, None

    joue = datetime.fromtimestamp(fichier.stat().st_mtime)
    resultats: dict[str, tuple[str, float, str, str]] = {}

    for suite in ET.parse(fichier).getroot().iter("testsuite"):
        for cas in suite.iter("testcase"):
            trouve = IDENTIFIANT.search(cas.get("name") or "")
            if not trouve:
                continue

            if cas.find("skipped") is not None:
                etat, detail = "Ignoré", "non joué : recette non configurée"
            elif cas.find("failure") is not None or cas.find("error") is not None:
                echec = cas.find("failure")
                echec = echec if echec is not None else cas.find("error")
                message = (echec.get("message") or echec.text or "").strip()
                etat, detail = "Rouge", " ".join(message.split())[:300]
            else:
                etat, detail = "Vert", ""

            resultats[trouve.group(1)] = (
                etat, float(cas.get("time") or 0), detail, suite.get("name") or "")

    return resultats, joue


def comptes_techniques() -> dict[str, tuple[int, int]]:
    """Filet technique : (joués, en échec) par famille. Jamais détaillé ici."""
    familles: dict[str, tuple[int, int]] = {}

    def compter(fichiers) -> tuple[int, int]:
        joues = echecs = 0
        for fichier in fichiers:
            try:
                racine = ET.parse(fichier).getroot()
            except (ET.ParseError, FileNotFoundError):
                continue
            for cas in racine.iter("testcase"):
                joues += 1
                if cas.find("failure") is not None or cas.find("error") is not None:
                    echecs += 1
        return joues, echecs

    surefire = (RACINE / "backend" / "target" / "surefire-reports")
    familles["Backend (intégration, architecture)"] = compter(
        surefire.glob("TEST-*.xml") if surefire.exists() else [])
    familles["Interface (unitaires)"] = compter(
        [f for f in RAPPORTS.glob("frontend-*.xml")] if RAPPORTS.exists() else [])
    return familles


# -------------------------------------------------------------- mise en page

def entete(feuille, colonnes: list[str], largeurs: list[int], ligne: int = 1) -> None:
    for index, (nom, largeur) in enumerate(zip(colonnes, largeurs), start=1):
        cellule = feuille.cell(row=ligne, column=index, value=nom)
        cellule.font = TITRE
        cellule.fill = FOND_TITRE
        cellule.alignment = Alignment(vertical="center", wrap_text=True)
        feuille.column_dimensions[get_column_letter(index)].width = largeur
    feuille.freeze_panes = feuille.cell(row=ligne + 1, column=1)


def colorer(feuille, colonne: str, premiere: int, derniere: int) -> None:
    plage = f"{colonne}{premiere}:{colonne}{max(derniere, premiere)}"
    for etat, couleur in COULEURS_ETAT.items():
        feuille.conditional_formatting.add(plage, CellIsRule(
            operator="equal", formula=[f'"{etat}"'],
            fill=PatternFill("solid", fgColor=couleur)))


def encadrer(feuille, premiere: int, derniere: int, colonnes: int) -> None:
    for ligne in range(premiere, derniere + 1):
        for colonne in range(1, colonnes + 1):
            cellule = feuille.cell(row=ligne, column=colonne)
            cellule.border = BORDURE
            cellule.alignment = HAUT


# ------------------------------------------------------------------ feuilles

def feuille_synthese(wb, features, scenarios, anomalies, joue, techniques) -> None:
    feuille = wb.active
    feuille.title = "Synthèse"

    feuille["A1"] = "Recette fonctionnelle — Clubify"
    feuille["A1"].font = Font(bold=True, size=14)
    feuille["A2"] = ("Ce que l'accueil et le gérant font devant l'écran. Régénéré par "
                     "tools/generer-suivi-tests.py ; les scénarios viennent de "
                     "docs/suivi-tests.md et les résultats d'un passage réel. Ne rien "
                     "saisir ici.")
    feuille["A2"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A2:G2")
    feuille.row_dimensions[2].height = 30
    feuille["A3"] = ("Dernier passage : "
                     + (joue.strftime("%d/%m/%Y %H:%M") if joue else "jamais"))
    feuille["A3"].font = GRAS

    colonnes = ["Feature", "Nom", "Scénarios", "Verts", "Rouges", "Non joués",
                "Manuels", "Critères couverts", "Anomalies ouvertes"]
    entete(feuille, colonnes, [10, 30, 12, 9, 9, 12, 10, 18, 18], ligne=5)

    ligne = 6
    for feature in features.values():
        siens = [s for s in scenarios if s.feature == feature.code]
        couverts = {c for s in siens if s.etat == "Vert" for c in s.criteres}
        ouvertes = [a for a in anomalies
                    if a.get("Feature") == feature.code and a.get("État") != "Close"]

        valeurs = [
            feature.code,
            feature.nom,
            len(siens),
            sum(1 for s in siens if s.etat == "Vert"),
            sum(1 for s in siens if s.etat == "Rouge"),
            sum(1 for s in siens if s.etat in ("Non joué", "Ignoré")),
            sum(1 for s in siens if s.mode == "Manuel"),
            (f"{len(couverts & set(feature.criteres))} / {len(feature.criteres)}"
             if feature.criteres else "—"),
            len(ouvertes),
        ]
        for index, valeur in enumerate(valeurs, start=1):
            feuille.cell(row=ligne, column=index, value=valeur)
        ligne += 1

    encadrer(feuille, 5, ligne - 1, len(colonnes))

    depart = ligne + 2
    feuille.cell(row=depart, column=1, value="Filet technique").font = GRAS
    feuille.cell(row=depart, column=3,
                 value="Non suivi scénario par scénario : c'est l'outillage du "
                       "développement, pas la recette.").alignment = HAUT
    for decalage, (famille, (joues, echecs)) in enumerate(techniques.items(), start=1):
        feuille.cell(row=depart + decalage, column=1, value=famille)
        feuille.cell(row=depart + decalage, column=3, value=f"{joues} joués")
        feuille.cell(row=depart + decalage, column=4,
                     value="tous verts" if echecs == 0 else f"{echecs} en échec")

    depart += len(techniques) + 2
    feuille.cell(row=depart, column=1, value="Lecture des états").font = GRAS
    explications = {
        "Vert": "Joué et passé au dernier passage",
        "Rouge": "Joué et échoué",
        "Non joué": "Décrit, pas encore automatisé ou pas relancé",
        "Ignoré": "Écarté du passage (recette non configurée)",
        "Manuel": "Déroulé par une personne ; passage saisi dans docs/suivi-tests.md",
    }
    for decalage, (etat, texte) in enumerate(explications.items(), start=1):
        cellule = feuille.cell(row=depart + decalage, column=1, value=etat)
        cellule.fill = PatternFill("solid", fgColor=COULEURS_ETAT[etat])
        feuille.cell(row=depart + decalage, column=3, value=texte)


def feuille_feature(wb, feature, scenarios, passages) -> None:
    feuille = wb.create_sheet(feature.code)
    feuille["A1"] = f"{feature.code} — {feature.nom}"
    feuille["A1"].font = Font(bold=True, size=13)
    feuille["A2"] = ("Chaque scénario se déroule tel qu'il est écrit : le préalable, "
                     "les étapes, puis le résultat attendu. L'état vient du dernier "
                     "passage, jamais d'une saisie.")
    feuille["A2"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A2:H2")
    feuille.row_dimensions[2].height = 28

    colonnes = ["ID", "Scénario", "Préalable", "Étapes", "Résultat attendu",
                "Mode", "État", "Dernier passage"]
    entete(feuille, colonnes, [7, 40, 34, 46, 56, 12, 12, 18], ligne=4)

    ligne = 5
    groupe = None
    for scenario in sorted([s for s in scenarios if s.feature == feature.code],
                           key=lambda s: s.identifiant):
        # Un intertitre par famille : on retrouve son écran d'un coup d'œil.
        if scenario.groupe != groupe:
            groupe = scenario.groupe
            cellule = feuille.cell(row=ligne, column=1, value=groupe)
            cellule.font = GRAS
            cellule.fill = PatternFill("solid", fgColor="EDF1F5")
            for colonne in range(2, len(colonnes) + 1):
                feuille.cell(row=ligne, column=colonne).fill = PatternFill(
                    "solid", fgColor="EDF1F5")
            ligne += 1

        passage = passages.get(scenario.identifiant, {})
        valeurs = [
            scenario.identifiant,
            scenario.intitule,
            scenario.prealable,
            "\n".join(f"{rang}. {etape}"
                       for rang, etape in enumerate(scenario.etapes, 1)),
            scenario.attendu,
            scenario.mode,
            scenario.etat,
            passage.get("Date", ""),
        ]
        for index, valeur in enumerate(valeurs, start=1):
            cellule = feuille.cell(row=ligne, column=index, value=valeur)
            cellule.alignment = HAUT
        if scenario.detail:
            feuille.cell(row=ligne, column=7).comment = None
        ligne += 1

    colorer(feuille, "G", 5, ligne - 1)
    encadrer(feuille, 4, ligne - 1, len(colonnes))


def feuille_couverture(wb, features, scenarios) -> None:
    feuille = wb.create_sheet("Couverture des critères")
    feuille["A1"] = ("Un critère d'acceptation qu'aucun parcours ne vérifie est un "
                     "critère que personne ne voit passer. Les critères viennent des "
                     "fiches ; « Filet technique seul » signifie couvert par les tests "
                     "d'intégration, mais jamais depuis les écrans.")
    feuille["A1"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A1:E1")
    feuille.row_dimensions[1].height = 30

    colonnes = ["Feature", "Critère", "Vérifié par", "Nb scénarios", "État"]
    entete(feuille, colonnes, [10, 12, 70, 14, 20], ligne=3)

    ligne = 4
    for feature in features.values():
        par_critere = defaultdict(list)
        for scenario in scenarios:
            if scenario.feature == feature.code:
                for critere in scenario.criteres:
                    par_critere[critere].append(scenario)

        for critere in feature.criteres:
            joue = par_critere.get(critere, [])
            if not joue:
                etat = "Filet technique seul"
            elif any(s.etat == "Rouge" for s in joue):
                etat = "Rouge"
            elif any(s.etat == "Vert" for s in joue):
                # Un parcours vert suffit ; un scénario manuel qui reprend le même
                # critère l'approfondit, il ne le remet pas en cause.
                etat = "Vert"
            else:
                etat = "Non joué"

            valeurs = [feature.code, critere,
                       " ; ".join(f"{s.identifiant} {s.intitule}" for s in joue)
                       or "aucun parcours",
                       len(joue), etat]
            for index, valeur in enumerate(valeurs, start=1):
                feuille.cell(row=ligne, column=index, value=valeur)
            ligne += 1

        # Un scénario qui cite un critère absent de la fiche : l'un des deux a tort.
        for critere in sorted(set(par_critere) - set(feature.criteres)):
            joue = par_critere[critere]
            valeurs = [feature.code, critere,
                       " ; ".join(f"{s.identifiant} {s.intitule}" for s in joue),
                       len(joue), "Hors fiche"]
            for index, valeur in enumerate(valeurs, start=1):
                feuille.cell(row=ligne, column=index, value=valeur)
            ligne += 1

    colorer(feuille, "E", 4, ligne - 1)
    encadrer(feuille, 3, ligne - 1, len(colonnes))


def feuille_anomalies(wb, anomalies) -> None:
    feuille = wb.create_sheet("Anomalies")
    feuille["A1"] = ("Une anomalie n'est close que lorsqu'un test l'empêche de revenir. "
                     "Tenue à la main dans docs/suivi-tests.md.")
    feuille["A1"].font = GRAS

    colonnes = ["Feature", "Anomalie", "Trouvée par", "Conséquence si elle revenait",
                "Test qui la ferme", "État"]
    entete(feuille, colonnes, [10, 58, 24, 54, 30, 12], ligne=3)

    ligne = 4
    for anomalie in anomalies:
        for index, nom in enumerate(colonnes, start=1):
            feuille.cell(row=ligne, column=index, value=anomalie.get(nom, ""))
        ligne += 1

    plage = f"F4:F{max(ligne - 1, 4)}"
    feuille.conditional_formatting.add(plage, CellIsRule(
        operator="equal", formula=['"Close"'],
        fill=PatternFill("solid", fgColor=COULEURS_ETAT["Vert"])))
    feuille.conditional_formatting.add(plage, CellIsRule(
        operator="equal", formula=['"Ouverte"'],
        fill=PatternFill("solid", fgColor=COULEURS_ETAT["Rouge"])))
    encadrer(feuille, 3, ligne - 1, len(colonnes))


# ---------------------------------------------------------------------- main

def main() -> int:
    arguments = argparse.ArgumentParser(description=__doc__)
    arguments.add_argument("--executer", action="store_true",
                           help="relance la recette fonctionnelle avant de générer")
    options = arguments.parse_args()

    if not SOURCE.exists():
        print(f"Source manquante : {SOURCE}", file=sys.stderr)
        return 1

    texte = SOURCE.read_text(encoding="utf-8")
    tableaux = tableaux_markdown(texte)
    passages = {p.get("ID", ""): p for p in tableaux.get("Passages manuels", [])}
    anomalies = tableaux.get("Anomalies trouvées", [])

    if options.executer:
        print("Passage de la recette…")
        executer_la_recette()

    resultats, joue = resultats_fonctionnels()

    scenarios = lire_les_scenarios(texte)
    for scenario in scenarios:
        if scenario.mode == "Manuel":
            resultat = passages.get(scenario.identifiant, {}).get("Résultat", "")
            scenario.etat = {"OK": "Vert", "KO": "Rouge"}.get(resultat, "Manuel")
        trouve = resultats.get(scenario.identifiant)
        if trouve:
            scenario.etat, scenario.duree, scenario.detail, _ = trouve

    codes = sorted({s.feature for s in scenarios}
                   | {a.get("Feature", "") for a in anomalies})
    features: dict[str, Feature] = {}
    for code in codes:
        if not code:
            continue
        fiche = next(iter(FEATURES.glob(f"{code}-*.md")), None)
        nom, criteres = criteres_de_la_fiche(fiche) if fiche else (code, [])
        features[code] = Feature(code=code, nom=nom, criteres=criteres)

    wb = Workbook()
    feuille_synthese(wb, features, scenarios, anomalies, joue, comptes_techniques())
    for feature in features.values():
        feuille_feature(wb, feature, scenarios, passages)
    feuille_couverture(wb, features, scenarios)
    feuille_anomalies(wb, anomalies)
    wb.save(SORTIE)

    verts = sum(1 for s in scenarios if s.etat == "Vert")
    rouges = sum(1 for s in scenarios if s.etat == "Rouge")
    attente = sum(1 for s in scenarios if s.etat in ("Non joué", "Ignoré", "Manuel"))
    print(f"  {len(scenarios)} scénarios fonctionnels : {verts} verts, {rouges} rouges, "
          f"{attente} en attente")
    print(f"  {len(anomalies)} anomalies, dont "
          f"{sum(1 for a in anomalies if a.get('État') != 'Close')} ouvertes")
    orphelins = sorted(set(resultats) - {s.identifiant for s in scenarios})
    if orphelins:
        print("  Scénarios joués mais absents de docs/suivi-tests.md :",
              ", ".join(orphelins), file=sys.stderr)
    print(f"  écrit : {SORTIE.relative_to(RACINE)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

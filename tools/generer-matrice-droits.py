"""Génère la matrice des droits de Clubify.

Qui peut quoi, par défaut, pour chaque rôle. Le classeur ne se saisit pas : il
se régénère, et ses trois sources ne se recopient jamais l'une l'autre.

- **Le code** fait foi sur la liste des droits, leur attribution par défaut et
  leur caractère délégable : c'est lui que l'application applique
  (`backend/.../common/security/Permissions.java`). Une matrice tenue à la main
  finirait par décrire une application qui n'existe pas.
- **`docs/droits.md`** porte ce que le code ne peut pas dire : ce que chaque
  droit ouvre, en mots du club.
- **Les fichiers de traduction** donnent le libellé des rôles, déjà écrit une
  fois pour l'interface.

Tout écart entre ces sources est listé dans une feuille du classeur plutôt que
passé sous silence.

Usage :

    python tools/generer-matrice-droits.py

Dépendance : openpyxl.
"""

from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

RACINE = Path(__file__).resolve().parent.parent
PERMISSIONS = (RACINE / "backend" / "src" / "main" / "java" / "ma" / "clubify"
               / "common" / "security" / "Permissions.java")
ROLES = (RACINE / "backend" / "src" / "main" / "java" / "ma" / "clubify"
         / "platform" / "model" / "Role.java")
DESCRIPTIONS = RACINE / "docs" / "droits.md"
LIBELLES = (RACINE / "frontend" / "projects" / "backoffice" / "public" / "i18n" / "fr.json")
SORTIE = RACINE / "docs" / "matrice-droits.xlsx"

ACCORDE = "✓"
REFUSE = "—"

TITRE = Font(bold=True, color="FFFFFF")
FOND_TITRE = PatternFill("solid", fgColor="2E4057")
GRAS = Font(bold=True)
BORDURE = Border(*(Side(style="thin", color="D9D9D9"),) * 4)
HAUT = Alignment(vertical="top", wrap_text=True)
CENTRE = Alignment(horizontal="center", vertical="center")
VERT = PatternFill("solid", fgColor="C6E7C6")
NEUTRE = PatternFill("solid", fgColor="F4F4F4")


@dataclass
class Droit:
    code: str
    libelle: str = ""
    ouvre: str = ""
    feature: str = ""
    delegable: bool = True
    roles: set[str] = field(default_factory=set)


# ---------------------------------------------------------------- le code

def lire_le_code() -> tuple[list[Droit], dict[str, set[str]], set[str], str | None]:
    """Catalogue, jeux par défaut, droits non délégables, rôle qui détient tout."""
    texte = PERMISSIONS.read_text(encoding="utf-8")

    # Les constantes : NOM = "domaine.objet.action".
    constantes = dict(re.findall(
        r'public static final String (\w+)\s*=\s*"([^"]+)"', texte))

    def codes_de(bloc: str) -> list[str]:
        return [constantes[nom] for nom in re.findall(r"\b([A-Z][A-Z0-9_]{2,})\b", bloc)
                if nom in constantes]

    catalogue_brut = re.search(
        r"CATALOGUE\s*=\s*List\.of\((.*?)\);", texte, re.S)
    catalogue = codes_de(catalogue_brut.group(1)) if catalogue_brut else []

    non_delegables_brut = re.search(
        r"NON_DELEGABLES\s*=\s*Set\.of\((.*?)\);", texte, re.S)
    non_delegables = set(codes_de(non_delegables_brut.group(1))) if non_delegables_brut else set()

    par_role: dict[str, set[str]] = {}
    for role, bloc in re.findall(
            r"PAR_ROLE\.put\(\s*(?:Role\.)?(\w+)\s*,\s*Set\.of\((.*?)\)\s*\);", texte, re.S):
        par_role[role] = set(codes_de(bloc))

    # « detientTout » est une règle, pas une liste : on la lit là où elle est
    # écrite, au lieu de la recopier ici.
    tout = re.search(r"detientTout\(Role role\)\s*\{\s*return role\s*==\s*(?:Role\.)?(\w+)",
                     texte)

    return ([Droit(code=code) for code in catalogue],
            par_role, non_delegables, tout.group(1) if tout else None)


def lire_les_roles() -> list[str]:
    """Les rôles, dans l'ordre où l'énumération les déclare."""
    texte = ROLES.read_text(encoding="utf-8")
    corps = re.search(r"public enum Role\s*\{(.*?)(?:;|\})", texte, re.S)
    if not corps:
        return []
    return [nom for nom in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", corps.group(1))]


# ------------------------------------------------------------- les mots

def lire_les_descriptions() -> tuple[dict[str, dict[str, str]], list[dict[str, str]]]:
    """Descriptions des droits, et table des rôles, depuis docs/droits.md."""
    tableaux: dict[str, list[dict[str, str]]] = {}
    titre = ""
    entetes: list[str] | None = None

    for ligne in DESCRIPTIONS.read_text(encoding="utf-8").splitlines():
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

    droits = {d["Code"]: d for d in tableaux.get("Droits", [])}
    return droits, tableaux.get("Rôles", [])


def lire_les_libelles() -> dict[str, str]:
    """Libellés des rôles, tels que l'interface les affiche."""
    if not LIBELLES.exists():
        return {}
    return json.loads(LIBELLES.read_text(encoding="utf-8")).get("role", {})


# -------------------------------------------------------------- feuilles

def entete(feuille, colonnes, largeurs, ligne=1) -> None:
    for index, (nom, largeur) in enumerate(zip(colonnes, largeurs), start=1):
        cellule = feuille.cell(row=ligne, column=index, value=nom)
        cellule.font = TITRE
        cellule.fill = FOND_TITRE
        cellule.alignment = Alignment(vertical="center", wrap_text=True,
                                      horizontal="center" if index > 4 else "left")
        feuille.column_dimensions[get_column_letter(index)].width = largeur
    feuille.freeze_panes = feuille.cell(row=ligne + 1, column=5)


def encadrer(feuille, premiere, derniere, colonnes) -> None:
    for ligne in range(premiere, derniere + 1):
        for colonne in range(1, colonnes + 1):
            feuille.cell(row=ligne, column=colonne).border = BORDURE


def feuille_matrice(wb, droits, roles, libelles, tout) -> None:
    feuille = wb.active
    feuille.title = "Matrice"

    feuille["A1"] = "Qui peut quoi — attribution par défaut"
    feuille["A1"].font = Font(bold=True, size=14)
    feuille["A2"] = ("Régénéré par tools/generer-matrice-droits.py. La liste et "
                     "l'attribution viennent du code ; les descriptions de "
                     "docs/droits.md. Ne rien saisir ici.")
    feuille["A2"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A2:F2")
    feuille.row_dimensions[2].height = 28
    feuille["A3"] = ("Une surcharge par utilisateur peut ajouter ou retirer un droit "
                     "au-dessus de son rôle ; elle est tracée. Sauf les droits marqués "
                     "« non délégable ».")
    feuille["A3"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A3:F3")

    colonnes = ["Code", "Libellé", "Ce qu'il ouvre", "Feature"]
    colonnes += [libelles.get(role, role) for role in roles]
    colonnes += ["Délégable"]
    largeurs = [28, 32, 62, 9] + [15] * len(roles) + [12]
    entete(feuille, colonnes, largeurs, ligne=5)

    ligne = 6
    for droit in droits:
        feuille.cell(row=ligne, column=1, value=droit.code).font = Font(name="Consolas", size=10)
        feuille.cell(row=ligne, column=2, value=droit.libelle)
        feuille.cell(row=ligne, column=3, value=droit.ouvre)
        feuille.cell(row=ligne, column=4, value=droit.feature)

        for decalage, role in enumerate(roles):
            cellule = feuille.cell(row=ligne, column=5 + decalage,
                                   value=ACCORDE if role in droit.roles else REFUSE)
            cellule.alignment = CENTRE
            cellule.fill = VERT if role in droit.roles else NEUTRE

        delegable = feuille.cell(row=ligne, column=5 + len(roles),
                                 value="oui" if droit.delegable else "non")
        delegable.alignment = CENTRE
        if not droit.delegable:
            delegable.font = GRAS

        for colonne in (2, 3):
            feuille.cell(row=ligne, column=colonne).alignment = HAUT
        ligne += 1

    encadrer(feuille, 5, ligne - 1, len(colonnes))

    note = ligne + 1
    if tout:
        feuille.cell(row=note, column=1, value=(
            f"{libelles.get(tout, tout)} détient tous les droits par construction, "
            "et ils ne se retirent pas (décision 0028, critère C12b).")).font = GRAS
    feuille.cell(row=note + 1, column=1, value=(
        "Un rôle sans aucun droit n'est pas un oubli : le coach et le parent "
        "attendent leurs features (R4 et R8)."))


def feuille_roles(wb, roles, libelles, descriptions, droits) -> None:
    feuille = wb.create_sheet("Rôles")
    feuille["A1"] = "Les six rôles"
    feuille["A1"].font = Font(bold=True, size=13)

    par_code = {d.get("Rôle", ""): d for d in descriptions}
    colonnes = ["Rôle", "Libellé affiché", "Qui c'est", "Remarque", "Droits par défaut"]
    entete(feuille, colonnes, [20, 26, 52, 62, 16], ligne=3)

    ligne = 4
    for role in roles:
        decrit = par_code.get(role, {})
        nombre = sum(1 for d in droits if role in d.roles)
        valeurs = [role, libelles.get(role, ""), decrit.get("Qui c'est", ""),
                   decrit.get("Remarque", ""), nombre]
        for index, valeur in enumerate(valeurs, start=1):
            cellule = feuille.cell(row=ligne, column=index, value=valeur)
            cellule.alignment = CENTRE if index == 5 else HAUT
        ligne += 1

    encadrer(feuille, 3, ligne - 1, len(colonnes))


def feuille_ecarts(wb, ecarts) -> None:
    feuille = wb.create_sheet("Écarts")
    feuille["A1"] = ("Ce que le code et les descriptions ne disent pas pareil. "
                     "Une ligne ici est une livraison incomplète.")
    feuille["A1"].alignment = Alignment(wrap_text=True)
    feuille.merge_cells("A1:C1")

    entete(feuille, ["Quoi", "Où", "À faire"], [34, 30, 70], ligne=3)

    ligne = 4
    for quoi, ou, faire in ecarts:
        for index, valeur in enumerate((quoi, ou, faire), start=1):
            feuille.cell(row=ligne, column=index, value=valeur).alignment = HAUT
        ligne += 1

    if not ecarts:
        feuille.cell(row=4, column=1, value="Aucun écart.").font = GRAS
        ligne = 5

    encadrer(feuille, 3, ligne - 1, 3)


# ------------------------------------------------------------------ main

def main() -> int:
    for fichier in (PERMISSIONS, ROLES, DESCRIPTIONS):
        if not fichier.exists():
            print(f"Source manquante : {fichier}", file=sys.stderr)
            return 1

    droits, par_role, non_delegables, tout = lire_le_code()
    roles = lire_les_roles()
    decrits, roles_decrits = lire_les_descriptions()
    libelles = lire_les_libelles()

    for droit in droits:
        decrit = decrits.get(droit.code, {})
        droit.libelle = decrit.get("Libellé", "")
        droit.ouvre = decrit.get("Ce qu'il ouvre", "")
        droit.feature = decrit.get("Feature", "")
        droit.delegable = droit.code not in non_delegables
        droit.roles = {role for role in roles
                       if droit.code in par_role.get(role, set()) or role == tout}

    ecarts: list[tuple[str, str, str]] = []
    connus = {d.code for d in droits}
    for droit in droits:
        if not droit.libelle:
            ecarts.append((droit.code, "absent de docs/droits.md",
                           "Décrire ce que ce droit ouvre, dans les mots du club."))
    for code in sorted(set(decrits) - connus):
        ecarts.append((code, "décrit, mais absent du catalogue",
                       "Le droit n'existe pas dans le code : l'ajouter, ou retirer "
                       "sa description."))
    for role in roles:
        if role not in par_role and role != tout:
            ecarts.append((role, "absent de Permissions.java",
                           "Le rôle n'a aucun jeu par défaut déclaré."))
    for role in {r.get("Rôle", "") for r in roles_decrits} - set(roles):
        if role:
            ecarts.append((role, "décrit, mais absent de l'énumération Role",
                           "Le rôle n'existe pas dans le code."))

    wb = Workbook()
    feuille_matrice(wb, droits, roles, libelles, tout)
    feuille_roles(wb, roles, libelles, roles_decrits, droits)
    feuille_ecarts(wb, ecarts)
    wb.save(SORTIE)

    print(f"  {len(droits)} droits, {len(roles)} rôles")
    for role in roles:
        nombre = sum(1 for d in droits if role in d.roles)
        print(f"    {libelles.get(role, role):28} {nombre:2} droits"
              + ("  (tous, non retirables)" if role == tout else ""))
    print(f"  {len(ecarts)} écart(s)")
    for quoi, ou, _ in ecarts:
        print(f"    {quoi} — {ou}", file=sys.stderr)
    print(f"  écrit : {SORTIE.relative_to(RACINE)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

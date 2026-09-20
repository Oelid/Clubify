<#
.SYNOPSIS
    Affiche les accès des deux environnements, et leur état.

.DESCRIPTION
    Répond à une seule question : « avec quel compte est-ce que je me connecte,
    et où est son mot de passe ? » Rien n'est inventé — le script lit les
    fichiers d'accès et interroge chaque base pour dire ce qui existe vraiment.

    Aucun mot de passe n'est affiché sans -AvecMotsDePasse : on ne les met pas
    dans un historique de terminal par accident.

.PARAMETER AvecMotsDePasse
    Affiche aussi les mots de passe. À n'employer que devant son propre écran.
#>
[CmdletBinding()]
param([switch]$AvecMotsDePasse)

$ErrorActionPreference = 'Stop'
$racine = Split-Path -Parent $PSScriptRoot

function Titre($texte) {
    Write-Host ''
    Write-Host "  $texte" -ForegroundColor Cyan
    Write-Host ('  ' + ('-' * $texte.Length)) -ForegroundColor DarkGray
}

function Ligne($cle, $valeur) {
    Write-Host ("    {0,-22} {1}" -f $cle, $valeur)
}

function LireFichier($chemin) {
    $valeurs = @{}
    if (Test-Path $chemin) {
        foreach ($ligne in Get-Content $chemin) {
            if ($ligne -match '^([A-Z0-9_]+)=(.*)$') { $valeurs[$Matches[1]] = $Matches[2] }
        }
    }
    return $valeurs
}

function Masquer($valeur) {
    if (-not $valeur) { return '(absent)' }
    if ($AvecMotsDePasse) { return $valeur }
    return ('•' * [Math]::Min($valeur.Length, 12)) + '  (relancer avec -AvecMotsDePasse)'
}

# Interroge une base pour dire ce qui existe, plutôt que de le supposer.
function Comptes($conteneur) {
    $requete = @'
select email || ' | ' || coalesce(m.role, '-') || ' | second facteur : ' ||
       case when u.mfa_enabled then 'activé' else 'à activer' end
from user_account u
left join membership m on m.user_id = u.id
where u.deleted_at is null
order by u.created_at
'@
    try {
        $sortie = docker exec $conteneur psql -U clubify -d clubify -tAc $requete 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $sortie) { return @() }
        return @($sortie | Where-Object { $_.Trim() })
    } catch {
        return @()
    }
}

function EtatService($nom, $url) {
    try {
        $reponse = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing
        if ($reponse.StatusCode -eq 200) { return 'en marche' }
    } catch { }
    return 'arrêté'
}

# ------------------------------------------------------------ développement

Titre 'Développement — ce sur quoi je travaille'

$dev = LireFichier (Join-Path $racine '.env.dev')
Ligne 'Interface' ('http://localhost:4200   (' + (EtatService 'front' 'http://localhost:4200') + ')')
Ligne 'API' ('http://localhost:8080   (' + (EtatService 'api' 'http://localhost:8080/actuator/health') + ')')
$compteDev = if ($dev['DEV_ADMIN_EMAIL']) { $dev['DEV_ADMIN_EMAIL'] } else { '(aucun fichier .env.dev)' }
Ligne 'Compte' $compteDev
Ligne 'Mot de passe' (Masquer $dev['DEV_ADMIN_PASSWORD'])
Ligne 'Accès notés dans' '.env.dev, à la racine (non versionné)'

$comptesDev = Comptes 'backend-postgres-1'
if ($comptesDev.Count) {
    Ligne 'En base' "$($comptesDev.Count) compte(s) :"
    foreach ($compte in $comptesDev | Select-Object -First 6) { Write-Host "      $compte" }
    if ($comptesDev.Count -gt 6) { Write-Host "      … et $($comptesDev.Count - 6) autre(s)" }
} else {
    Ligne 'En base' 'base arrêtée ou vide'
}

# ----------------------------------------------------------------- recette

Titre 'Recette — ce que vous ouvrez pour tester'

$recette = LireFichier (Join-Path $racine '.env')
Ligne 'Interface' ('http://localhost:4300   (' + (EtatService 'front' 'http://localhost:4300') + ')')
Ligne 'API' ('http://localhost:8081   (' + (EtatService 'api' 'http://localhost:8081/actuator/health') + ')')
$compteRecette = if ($recette['ADMIN_EMAIL']) { $recette['ADMIN_EMAIL'] } else { '(pas encore démarrée)' }
Ligne 'Compte' $compteRecette
Ligne 'Mot de passe' (Masquer $recette['ADMIN_PASSWORD'])
Ligne 'Accès notés dans' '.env, à la racine (non versionné)'

$comptesRecette = Comptes 'clubify-recette-db-1'
if ($comptesRecette.Count) {
    Ligne 'En base' "$($comptesRecette.Count) compte(s) :"
    foreach ($compte in $comptesRecette | Select-Object -First 6) { Write-Host "      $compte" }
} else {
    Ligne 'En base' 'recette arrêtée — .\tools\demarrer-recette.ps1'
}

Titre 'Si quelque chose manque'

Write-Host @'
    Mot de passe perdu      Relisez .env (recette) ou .env.dev (développement).
    Téléphone perdu         .\tools\reinitialiser-second-facteur.ps1 <adresse>
                            puis reconnectez-vous : l'activation se redemande.
    Repartir de zéro        .\tools\demarrer-recette.ps1 -Effacer
    Le guide complet        docs/acces.md
'@
Write-Host ''

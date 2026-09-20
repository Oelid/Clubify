<#
.SYNOPSIS
    Démarre l'environnement de recette : base, backend et interface.

.DESCRIPTION
    Construit les artefacts sur la machine, fabrique les images, puis lance le
    tout. L'interface s'ouvre sur http://localhost:4300.

    Au premier lancement, un fichier .env est créé avec des secrets tirés au
    hasard et le mot de passe de l'administrateur du club de recette. Il n'est
    pas versionné (CLAUDE.md §5) : notez ce qu'il contient, ou relisez-le.

    Les modifications faites pendant que la recette tourne ne s'y voient pas :
    relancez ce script pour prendre la dernière version. C'est délibéré — une
    application qui redémarre pendant qu'on la teste ne se teste pas.

.PARAMETER SansConstruction
    Relance les conteneurs sans reconstruire. Utile pour repartir vite après
    un arrêt, quand rien n'a changé.

.PARAMETER Arreter
    Arrête l'environnement sans effacer sa base.

.PARAMETER Effacer
    Arrête et efface la base de recette : on repart d'un club neuf.
#>
[CmdletBinding()]
param(
    [switch]$SansConstruction,
    [switch]$Arreter,
    [switch]$Effacer
)

$ErrorActionPreference = 'Stop'
$racine = Split-Path -Parent $PSScriptRoot
$env = Join-Path $racine '.env'

function Ecrire($message) { Write-Host "  $message" }

if ($Arreter) {
    Push-Location $racine
    try { docker compose stop } finally { Pop-Location }
    Ecrire 'Recette arrêtée. Sa base est conservée.'
    return
}

if ($Effacer) {
    Push-Location $racine
    try { docker compose down -v } finally { Pop-Location }
    Ecrire 'Recette effacée. Le prochain démarrage amorce un club neuf.'
    return
}

# --------------------------------------------------------------- les secrets

if (-not (Test-Path $env)) {
    Ecrire 'Premier démarrage : génération des secrets de recette.'

    # RNGCryptoServiceProvider plutôt que RandomNumberGenerator.Fill : Windows
    # PowerShell 5.1 ne connaît pas la seconde.
    function NouveauSecret([int]$octets) {
        $valeur = [byte[]]::new($octets)
        $source = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
        try { $source.GetBytes($valeur) } finally { $source.Dispose() }
        [Convert]::ToBase64String($valeur)
    }

    # Le mot de passe de l'administrateur respecte la règle des douze caractères.
    $motDePasse = (NouveauSecret 12) -replace '[^A-Za-z0-9]', ''
    if ($motDePasse.Length -lt 12) { $motDePasse = $motDePasse + 'Recette2027' }

    @(
        "# Secrets de l'environnement de recette, tirés au hasard au premier",
        "# démarrage. Ce fichier n'est pas versionné (CLAUDE.md §5).",
        "# Effacez-le pour en générer de nouveaux : la base devra être effacée aussi,",
        "# les mots de passe déjà enregistrés ne correspondraient plus.",
        "DB_PASSWORD=$(NouveauSecret 18)",
        "JWT_SECRET=$(NouveauSecret 32)",
        "ENCRYPTION_KEY=$(NouveauSecret 32)",
        "CLUB_NAME=Club de recette",
        "ADMIN_EMAIL=recette@exemple.test",
        "ADMIN_PASSWORD=$motDePasse"
    ) | Set-Content -Path $env -Encoding utf8

    Ecrire "Identifiants notés dans .env : recette@exemple.test / $motDePasse"
}

# ------------------------------------------------------------ la construction

# Maven et Angular écrivent leurs avertissements sur la sortie d'erreur ; sans
# cela, PowerShell prend le premier pour un échec et arrête tout. Seul le code
# de retour dit si la construction a abouti.
function Construire($dossier, $commande, $arguments, $quoi) {
    Ecrire "Construction $quoi…"
    Push-Location (Join-Path $racine $dossier)
    try {
        $ErrorActionPreference = 'Continue'
        & $commande @arguments 2>&1 | ForEach-Object { "$_" } | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "La construction $quoi a échoué (code $LASTEXITCODE)." }
    } finally {
        Pop-Location
        $ErrorActionPreference = 'Stop'
    }
}

if (-not $SansConstruction) {
    Construire 'backend' '.\mvnw.cmd' @('-q', '-DskipTests', 'package') 'du backend'
    Construire 'frontend' 'npx' @('ng', 'build', 'backoffice') "de l'interface"
}

# ------------------------------------------------------------- le démarrage

Ecrire 'Démarrage des conteneurs…'
Push-Location $racine
try {
    $ErrorActionPreference = 'Continue'
    $arguments = if ($SansConstruction) { @('compose', 'up', '-d') }
                 else { @('compose', 'up', '-d', '--build') }
    & docker @arguments 2>&1 | ForEach-Object { "$_" } | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Le démarrage des conteneurs a échoué (code $LASTEXITCODE)." }
} finally {
    Pop-Location
    $ErrorActionPreference = 'Stop'
}

$identifiants = Get-Content $env | Where-Object { $_ -match '^(ADMIN_EMAIL|ADMIN_PASSWORD)=' }

Write-Host ''
Ecrire 'Recette démarrée.'
Ecrire '  Interface : http://localhost:4300'
Ecrire '  API       : http://localhost:8081/swagger-ui'
Ecrire '  Base      : localhost:5433'
Write-Host ''
foreach ($ligne in $identifiants) { Ecrire "  $ligne" }
Write-Host ''
Ecrire "À la première connexion, l'application demandera d'activer le second"
Ecrire "facteur : scannez le code avec votre téléphone et conservez les codes"
Ecrire 'de secours. Sans eux, un téléphone perdu ferme le club.'

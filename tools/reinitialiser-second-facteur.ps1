<#
.SYNOPSIS
    Rend l'accès à un compte dont le second facteur est perdu.

.DESCRIPTION
    Efface le second facteur d'un compte, dans une base locale. À la connexion
    suivante, l'application redemande son activation : nouveau code à scanner,
    nouveaux codes de secours.

    **Réservé aux environnements locaux.** En production, c'est l'administrateur
    du compte qui réinitialise le second facteur d'un utilisateur depuis
    l'application (droit « users.mfa.reinitialiser », critère C6c) ; l'action y
    est tracée. Ce script existe pour le cas que l'application ne couvre pas :
    l'administrateur lui-même enfermé dehors, sur une base jetable.

.PARAMETER Adresse
    L'adresse électronique du compte à débloquer.

.PARAMETER Recette
    Agit sur la base de recette plutôt que sur celle de développement.

.EXAMPLE
    .\tools\reinitialiser-second-facteur.ps1 recette@exemple.test -Recette
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Adresse,
    [switch]$Recette
)

$ErrorActionPreference = 'Stop'

if ($Recette) {
    $conteneur = 'clubify-recette-db-1'
    $environnement = 'recette'
} else {
    $conteneur = 'backend-postgres-1'
    $environnement = 'developpement'
}

Write-Host "  Base de $environnement ($conteneur)"

$lecture = "select mfa_enabled from user_account " +
           "where lower(email) = lower('$Adresse') and deleted_at is null"
$verification = docker exec $conteneur psql -U clubify -d clubify -tAc $lecture

if ($LASTEXITCODE -ne 0) {
    throw "Base injoignable. L'environnement de $environnement est-il demarre ?"
}
if (-not $verification) {
    throw "Aucun compte pour $Adresse. Verifiez l'adresse avec .\tools\afficher-acces.ps1."
}

# Les codes de secours et les appareils de confiance partent avec le secret :
# ils ouvraient le compte au meme titre que le telephone.
$requete = "update user_account set mfa_enabled = false, mfa_secret = null " +
           " where lower(email) = lower('$Adresse');" +
           " delete from recovery_code where user_id = (select id from user_account" +
           " where lower(email) = lower('$Adresse'));" +
           " update trusted_device set revoked_at = now() where user_id = (select id" +
           " from user_account where lower(email) = lower('$Adresse'))" +
           "   and revoked_at is null;"

docker exec $conteneur psql -U clubify -d clubify -c $requete | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'La reinitialisation a echoue.' }

Write-Host ''
Write-Host "  Second facteur efface pour $Adresse." -ForegroundColor Green
Write-Host "  Reconnectez-vous : l'application redemandera son activation."
Write-Host '  Conservez les nouveaux codes de secours.'
Write-Host ''

if (-not $Recette) {
    Write-Host '  Le secret note dans .env.dev ne vaut plus rien : la recette' -ForegroundColor Yellow
    Write-Host "  fonctionnelle echouera tant qu'il n'aura pas ete remplace." -ForegroundColor Yellow
    Write-Host '  Reactivez le second facteur, puis notez le nouveau secret.'
    Write-Host ''
}

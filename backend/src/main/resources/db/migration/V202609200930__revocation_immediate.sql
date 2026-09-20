-- Révocation immédiate des jetons d'accès.
--
-- Un jeton d'accès est signé et sans état : fermer les sessions d'un
-- utilisateur ne l'invalidait donc pas avant son échéance. Pour un téléphone
-- volé ou un départ, quinze minutes de sursis sont quinze minutes de trop
-- (critères C8b et C9).
--
-- Cette borne règle le problème sans rendre chaque jeton consultable en base :
-- tout jeton émis avant elle est refusé.

alter table user_account
    add column sessions_valid_from timestamptz not null default now();

comment on column user_account.sessions_valid_from is
    'Tout jeton d''accès émis avant cette borne est refusé (critères C8b, C9).';

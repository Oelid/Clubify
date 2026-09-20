-- Défi de second facteur : entre le mot de passe accepté et le code vérifié.
--
-- Il est stocké plutôt que signé et rendu au client, pour deux raisons : un défi
-- doit être à usage unique, et il doit pouvoir être révoqué. Sa purge est la
-- première règle automatique du système, et elle s'inscrit au journal sous
-- l'auteur « système » (critère C16b).

create table mfa_challenge (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    user_id             uuid          not null references user_account (id),
    purpose             varchar(32)   not null,
    expires_at          timestamptz   not null,
    consumed_at         timestamptz,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz,
    constraint mfa_challenge_purpose_known check (purpose in ('LOGIN', 'ENROLLMENT'))
);

create index mfa_challenge_user_idx on mfa_challenge (user_id);
create index mfa_challenge_club_idx on mfa_challenge (club_id);

alter table mfa_challenge enable row level security;
create policy mfa_challenge_par_club on mfa_challenge
    using (current_club_id() is null or club_id = current_club_id())
    with check (current_club_id() is null or club_id = current_club_id());

grant select, insert, update, delete, truncate on mfa_challenge to clubify_app;

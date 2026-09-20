-- F01 — Garde-fous portés par la base, et non par l'application seule.
--
-- Deux protections indépendantes :
--   1. le journal d'audit est en ajout seul, y compris pour un superutilisateur ;
--   2. l'isolation par club dispose d'une seconde ligne (Row-Level Security)
--      derrière le discriminant Hibernate (décision 0029, point M7).

-- ------------------------------------------- 1. journal en ajout seul (C17)
--
-- Les droits seuls ne suffisent pas : un superutilisateur les contourne. Le
-- déclencheur, lui, s'applique à tout le monde. Les deux sont posés.

create or replace function audit_log_append_only() returns trigger
    language plpgsql as $$
begin
    raise exception
        'Le journal est en ajout seul : ni modification ni suppression sur audit_log (SEC-04).'
        using errcode = 'restrict_violation';
end;
$$;

comment on function audit_log_append_only() is
    'Refuse toute modification et toute suppression sur audit_log (SEC-04, 9.4 point 12).';

create trigger audit_log_no_update
    before update on audit_log
    for each statement execute function audit_log_append_only();

create trigger audit_log_no_delete
    before delete on audit_log
    for each statement execute function audit_log_append_only();

create trigger audit_log_no_truncate
    before truncate on audit_log
    for each statement execute function audit_log_append_only();

-- ------------------------------------------------- 2. rôle applicatif (SEC-04)
--
-- En exploitation, l'application se connecte avec ce rôle : il ne peut ni
-- modifier ni supprimer une ligne d'audit. Le déclencheur ci-dessus couvre le
-- cas où la connexion serait plus privilégiée.

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'clubify_app') then
        create role clubify_app nologin;
    end if;
end;
$$;

grant usage on schema public to clubify_app;

grant select, insert, update, delete, truncate
    on club, site, user_account, membership, user_permission_override,
       refresh_token, trusted_device, recovery_code, club_setting,
       stored_file, file_link, outbox_event
    to clubify_app;

-- Le journal : lecture et ajout, rien d'autre.
grant select, insert on audit_log to clubify_app;

-- --------------------------------------------- 3. isolation de second rideau
--
-- Le filtre premier est le discriminant Hibernate, qui s'applique à chaque
-- requête. La Row-Level Security rattrape le cas d'une requête écrite à la
-- main dans un contexte de club.
--
-- La politique laisse passer quand aucun contexte n'est posé : l'amorçage d'un
-- club et les traitements de fond n'appartiennent à aucun club. Toute requête
-- servie dans le cadre d'une demande, elle, pose app.club_id ; l'isolation y
-- est donc doublée.

create or replace function current_club_id() returns uuid
    language plpgsql stable as $$
declare
    valeur text := current_setting('app.club_id', true);
begin
    if valeur is null or valeur = '' then
        return null;
    end if;
    return valeur::uuid;
end;
$$;

comment on function current_club_id() is
    'Club du contexte courant, posé par l''application à chaque transaction servie.';

do $$
declare
    nom text;
begin
    foreach nom in array array[
        'site', 'membership', 'user_permission_override', 'refresh_token',
        'trusted_device', 'recovery_code', 'club_setting', 'stored_file',
        'file_link', 'outbox_event'
    ] loop
        execute format('alter table %I enable row level security', nom);
        execute format($f$
            create policy %I_par_club on %I
                using (current_club_id() is null or club_id = current_club_id())
                with check (current_club_id() is null or club_id = current_club_id())
        $f$, nom, nom);
    end loop;
end;
$$;

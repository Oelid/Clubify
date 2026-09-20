-- F01 — Socle plateforme.
--
-- Invariants portés par ce schéma (cahier 9.6, CLAUDE.md §3, décision 0029) :
--   * identifiants UUID, jamais d'auto-incrément exposé ;
--   * club_id sur chaque table, sauf club (elle est le club), user_account
--     (un compte peut appartenir à plusieurs clubs) et audit_log (les
--     événements d'authentification n'ont pas de club) ;
--   * colonnes d'audit et deleted_at partout : suppression logique seule ;
--   * instants en timestamptz, donc en UTC ;
--   * unicité compatible avec la suppression logique par index partiel.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------- club

create table club (
    id                  uuid primary key,
    name                varchar(200)  not null,
    legal_form          varchar(100),
    ice                 varchar(15),
    tax_id              varchar(50),
    trade_register      varchar(50),
    address             text,
    phone               varchar(20),
    email               varchar(320),
    timezone            varchar(64)   not null default 'Africa/Casablanca',
    currency            varchar(3)    not null default 'MAD',
    default_language    varchar(8)    not null default 'fr',
    logo_file_id        uuid,
    version             bigint        not null default 0,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table club is 'Le tenant. Ne porte pas de club_id : elle est le club.';

create table site (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    name                varchar(200)  not null,
    address             text,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

create index site_club_idx on site (club_id);

-- ------------------------------------------------------- comptes et accès

create table user_account (
    id                  uuid primary key,
    email               varchar(320)  not null,
    first_name          varchar(100)  not null,
    last_name           varchar(100)  not null,
    phone               varchar(20),
    password_hash       varchar(255)  not null,
    language            varchar(8)    not null default 'fr',
    active              boolean       not null default true,
    mfa_enabled         boolean       not null default false,
    mfa_secret          text,
    failed_attempts     integer       not null default 0,
    locked_until        timestamptz,
    last_login_at       timestamptz,
    version             bigint        not null default 0,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table user_account is
    'Compte global : un même utilisateur peut appartenir à plusieurs clubs (PLT-02, décision 0029).';
comment on column user_account.mfa_secret is
    'Secret TOTP chiffré par l''application (SEC-03) : le support ne voit jamais le clair.';

create unique index user_account_email_uk on user_account (lower(email))
    where deleted_at is null;

create table membership (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    user_id             uuid          not null references user_account (id),
    role                varchar(32)   not null,
    active              boolean       not null default true,
    version             bigint        not null default 0,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz,
    constraint membership_role_known check (role in
        ('ACCOUNT_ADMIN', 'MANAGER', 'FRONT_DESK', 'COACH', 'ACCOUNTANT', 'PARENT'))
);

comment on table membership is
    'Appartenance d''un utilisateur à un club, avec son rôle (SEC-02, décision 0028).';

create unique index membership_user_club_uk on membership (club_id, user_id)
    where deleted_at is null;
create index membership_club_idx on membership (club_id);

create table user_permission_override (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    membership_id       uuid          not null references membership (id),
    permission_code     varchar(120)  not null,
    granted             boolean       not null,
    parameter           jsonb,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table user_permission_override is
    'Surcharge de permission : le rôle reste la règle, ceci est l''exception (décision 0028).';

create unique index user_permission_override_uk
    on user_permission_override (membership_id, permission_code)
    where deleted_at is null;
create index user_permission_override_club_idx on user_permission_override (club_id);

create table refresh_token (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    user_id             uuid          not null references user_account (id),
    token_hash          varchar(128)  not null,
    device_label        varchar(200),
    expires_at          timestamptz   not null,
    revoked_at          timestamptz,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

create unique index refresh_token_hash_uk on refresh_token (token_hash)
    where deleted_at is null;
create index refresh_token_user_idx on refresh_token (user_id);
create index refresh_token_club_idx on refresh_token (club_id);

create table trusted_device (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    user_id             uuid          not null references user_account (id),
    token_hash          varchar(128)  not null,
    label               varchar(200)  not null,
    last_used_at        timestamptz,
    expires_at          timestamptz   not null,
    revoked_at          timestamptz,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table trusted_device is
    'Appareil de confiance : le second facteur n''y est plus demandé pendant la durée '
    'paramétrée par le club (benchmark B4).';

create unique index trusted_device_hash_uk on trusted_device (token_hash)
    where deleted_at is null;
create index trusted_device_user_idx on trusted_device (user_id);
create index trusted_device_club_idx on trusted_device (club_id);

create table recovery_code (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    user_id             uuid          not null references user_account (id),
    code_hash           varchar(255)  not null,
    used_at             timestamptz,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table recovery_code is
    'Code de secours à usage unique, remis à l''activation du second facteur (benchmark B5).';

create index recovery_code_user_idx on recovery_code (user_id);
create index recovery_code_club_idx on recovery_code (club_id);

-- --------------------------------------------------- paramètres du club

create table club_setting (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    setting_key         varchar(120)  not null,
    value               jsonb         not null,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table club_setting is
    'Valeur d''une règle configurable (section 9.8). Le défaut vit en code, jamais ici.';

create unique index club_setting_uk on club_setting (club_id, setting_key)
    where deleted_at is null;

-- ------------------------------------------------------------- fichiers

create table stored_file (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    purpose             varchar(64)   not null,
    filename            varchar(255),
    content_type        varchar(160)  not null,
    size_bytes          bigint        not null,
    checksum            varchar(128),
    storage_key         varchar(512)  not null,
    encryption_key_id   varchar(64)   not null,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table stored_file is
    'Fichier privé, chiffré par l''application avant d''atteindre le support (SEC-03).';

create index stored_file_club_idx on stored_file (club_id);

create table file_link (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    file_id             uuid          not null references stored_file (id),
    token_hash          varchar(128)  not null,
    expires_at          timestamptz   not null,
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table file_link is
    'Lien signé et expirant vers un fichier (PLT-05). Sa durée est paramétrable par club.';

create unique index file_link_hash_uk on file_link (token_hash) where deleted_at is null;
create index file_link_club_idx on file_link (club_id);

alter table club add constraint club_logo_fk foreign key (logo_file_id) references stored_file (id);

-- ------------------------------------------------- événements et journal

create table outbox_event (
    id                  uuid primary key,
    club_id             uuid          not null references club (id),
    event_type          varchar(160)  not null,
    payload             jsonb         not null,
    attempts            integer       not null default 0,
    last_error          text,
    processed_at        timestamptz,
    available_at        timestamptz   not null default now(),
    created_at          timestamptz   not null default now(),
    created_by          uuid,
    updated_at          timestamptz   not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz
);

comment on table outbox_event is
    'Effets externes : écrits dans la transaction, traités ensuite, rejoués en cas d''échec. '
    'L''audit, lui, est synchrone et bloquant (décision 0029).';

create index outbox_event_pending_idx on outbox_event (available_at)
    where processed_at is null;

create table audit_log (
    id                  uuid primary key,
    club_id             uuid          references club (id),
    occurred_at         timestamptz   not null default now(),
    actor_type          varchar(16)   not null,
    actor_id            uuid,
    actor_label         varchar(200),
    action              varchar(160)  not null,
    entity_type         varchar(120),
    entity_id           uuid,
    before_state        jsonb,
    after_state         jsonb,
    reason              text,
    request_id          varchar(64),
    constraint audit_log_actor_type_known check (actor_type in ('USER', 'PARENT', 'SYSTEM')),
    -- Seuls les événements d'authentification peuvent survenir hors d'un club :
    -- une tentative sur un identifiant inconnu n'en a pas (décision 0029).
    constraint audit_log_club_id_required check (club_id is not null or action like 'auth.%')
);

comment on table audit_log is
    'Journal en ajout seul. Jamais purgé en R1 ; aucune colonne de modification, '
    'aucun deleted_at : une ligne d''audit ne se corrige pas (SEC-04, benchmark B2).';

create index audit_log_club_time_idx on audit_log (club_id, occurred_at desc);
create index audit_log_action_idx on audit_log (action);
create index audit_log_actor_idx on audit_log (actor_id);
create index audit_log_entity_idx on audit_log (entity_type, entity_id);

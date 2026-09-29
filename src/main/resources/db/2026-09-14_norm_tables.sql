-- Norms: the tables, created by hand before the first start.
--
-- ddl-auto creates the same tables on its own, but lays the columns out alphabetically - both
-- column ordering strategies of Hibernate 7 ignore the order of declaration - so "nummer" and
-- "buchstabe" end up far apart. Created here first, ddl-auto=update finds them and only adds
-- what a later entity change brings, at the end of the table.
--
-- Run on an empty database, before 2026-09-04_norms.sql: that one indexes these tables.
--
-- Types, lengths and sequence increments are those Hibernate generates for the entities. The
-- unique constraint on docnumber and both foreign keys keep Hibernate's generated names, so an
-- update run recognises them instead of adding a second copy.


CREATE SEQUENCE IF NOT EXISTS norm_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS norm_document_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS norm_abbreviation_seq START WITH 1 INCREMENT BY 50;


CREATE TABLE IF NOT EXISTS norm (
    id                           bigint        NOT NULL,

    -- identity
    jurisdiction                 varchar(8)    NOT NULL,
    source                       varchar(32)   NOT NULL,
    identifier                   varchar(64)   NOT NULL,

    -- names and kind
    kurztitel                    text,
    langtitel                    text,
    typ                          text,
    kundmachungsorgan            varchar(512),
    eli                          varchar(512),
    gesamte_rechtsvorschrift_url varchar(1024),
    indizes                      text,

    -- import bookkeeping
    title_source_inkrafttreten   date,
    structure_loaded_for         date,
    last_change_check            timestamp(6),
    metadata                     json,

    created                      timestamp(6),
    updated                      timestamp(6),

    CONSTRAINT norm_pkey PRIMARY KEY (id),
    CONSTRAINT norm_identity UNIQUE (jurisdiction, source, identifier)
);

CREATE INDEX IF NOT EXISTS norm_identifier_index ON norm (identifier);


CREATE TABLE IF NOT EXISTS norm_document (
    id                           bigint        NOT NULL,
    norm_id                      bigint        NOT NULL,
    docnumber                    varchar(64)   NOT NULL,
    language                     varchar(8)    NOT NULL,
    sort_index                   integer,
    eli_provision                varchar(512),

    -- designation
    abschnitt_typ                varchar(32),
    artikel_paragraph_anlage     varchar(256),
    nummer                       varchar(32),
    buchstabe                    varchar(16),
    teil                         varchar(32),
    uebergangsrecht              varchar(256),

    -- validity and origin
    inkrafttreten                date,
    ausserkrafttreten            date,
    stammnorm_publikationsorgan  varchar(256),
    stammnorm_bgblnummer         varchar(128),
    novellen_publikationsorgan   varchar(256),
    novellen_bgblnummer          varchar(128),
    novellen_beziehung           varchar(256),
    beachte                      text,
    anmerkung                    text,

    -- content
    geaendert                    date,
    html_url                     varchar(1024),
    attachments                  json,
    html                         text,
    full_text                    text,
    word_count                   bigint,
    text_conversion_version      integer,
    metadata                     json,

    created                      timestamp(6),
    updated                      timestamp(6),
    deleted_at                   timestamp(6),

    CONSTRAINT norm_document_pkey PRIMARY KEY (id),
    CONSTRAINT ukqmsx6nyrcjqdlluf5bk58ilvj UNIQUE (docnumber),
    CONSTRAINT fkes3e1gubfaqi1bdmtatdckdan FOREIGN KEY (norm_id) REFERENCES norm (id)
);

CREATE INDEX IF NOT EXISTS norm_document_norm_index ON norm_document (norm_id);
CREATE INDEX IF NOT EXISTS norm_document_range_index ON norm_document (norm_id, nummer, buchstabe);
CREATE INDEX IF NOT EXISTS norm_document_sort_index ON norm_document (norm_id, sort_index);
CREATE INDEX IF NOT EXISTS norm_document_eli_index ON norm_document (eli_provision);


CREATE TABLE IF NOT EXISTS norm_abbreviation (
    id                           bigint        NOT NULL,
    norm_id                      bigint        NOT NULL,
    abkuerzung                   text          NOT NULL,
    normalized                   varchar(256)  NOT NULL,
    quelle                       varchar(32)   NOT NULL,
    first_seen                   timestamp(6),
    last_seen                    timestamp(6),

    CONSTRAINT norm_abbreviation_pkey PRIMARY KEY (id),
    CONSTRAINT norm_abbreviation_identity UNIQUE (norm_id, normalized),
    CONSTRAINT fksxfyj1d3mjayyyju7068wfmtx FOREIGN KEY (norm_id) REFERENCES norm (id)
);

CREATE INDEX IF NOT EXISTS norm_abbreviation_normalized_index ON norm_abbreviation (normalized);
CREATE INDEX IF NOT EXISTS norm_abbreviation_norm_index ON norm_abbreviation (norm_id);

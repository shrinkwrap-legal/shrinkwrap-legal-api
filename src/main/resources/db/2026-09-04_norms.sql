-- Norms: everything the JPA mapping cannot express.
--
-- The tables norm, norm_document and norm_abbreviation, their columns, their unique
-- constraints and their plain btree indexes are created by ddl-auto from the entity
-- annotations. Only what is left goes here.
--
-- Deliberately NOT part of this: a search_vector on norm_document. Full text search over
-- norm text is postponed and the gap is accepted knowingly - see section 1 of
-- weiterentwicklung-normen.md. Adding it later means one column, one trigger and one GIN
-- index, in the shape of 2026-05-24_tsvector.sql.


-- Searching a law by its title has to tolerate how people actually write it - "Strassen-
-- verkehrsordnung" for "Straßenverkehrsordnung", or just a fragment. Trigram indexes make
-- ILIKE '%…%' and similarity() usable; a btree index cannot serve a leading wildcard.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS norm_kurztitel_trgm_index
    ON norm USING gin (kurztitel gin_trgm_ops);

CREATE INDEX IF NOT EXISTS norm_langtitel_trgm_index
    ON norm USING gin (langtitel gin_trgm_ops);


-- Abbreviation lookup goes through the normalised form and is the hot path of every
-- citation resolution. ddl-auto creates a plain index on it; this one additionally covers
-- the abbreviation being ambiguous across laws, which is answered by norm_id.
CREATE INDEX IF NOT EXISTS norm_abbreviation_lookup_index
    ON norm_abbreviation (normalized, norm_id);


-- Almost every read asks for the provisions of one law that are in force on a given day.
-- Withdrawn rows are kept for old citations but are never part of that answer, so they are
-- kept out of the index rather than filtered out of the result.
CREATE INDEX IF NOT EXISTS norm_document_in_force_index
    ON norm_document (norm_id, inkrafttreten, ausserkrafttreten)
    WHERE deleted_at IS NULL;

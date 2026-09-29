-- Norms: corrects data written before two import fixes. Run once, in any order; both statements
-- only touch rows that still carry the old value, so running them again changes nothing.


-- 1. State law provision paths kept their document number.
--
-- eliProvisionPath only removed a trailing /NOR…, but state law numbers its documents LOO…, LKT…,
-- LWI… and so on - three letters and eight digits throughout. The number stayed in the path, and
-- the risUrl built from path and number named the document twice and answered 404:
--   /eli/lgbl/OB/1993/114/P1/LOO12005088/LOO12005088
-- Affected: all of state law, 268.571 of 678.303 documents in the local mirror.

UPDATE norm_document
SET eli_provision = regexp_replace(eli_provision, '/[A-Z]{3}[0-9]{8}$', '')
WHERE eli_provision ~ '/[A-Z]{3}[0-9]{8}$';


-- 2. Repealed laws had neither long title nor ELI.
--
-- Both are taken from the head document (§ 0), and only from the one in force today - which a
-- repealed law does not have. The import now falls back to the head that came into force last,
-- for laws that have no long title yet; this does the same for the laws already imported.
-- Mirrors NormImporter.applyHead: head = NORM document, not withdrawn, already in force.
-- Affected: some 13.700 laws in the local mirror, the Kesselgesetz and TKG 2003 among them.

UPDATE norm n
SET langtitel = regexp_replace(h.titel, '^\s+|\s+$', '', 'g'),
    eli       = h.eli
FROM (
    SELECT DISTINCT ON (d.norm_id)
           d.norm_id,
           coalesce(d.metadata -> 'bundesrecht' ->> 'titel',
                    d.metadata -> 'landesrecht' ->> 'titel')             AS titel,
           coalesce(d.metadata -> 'bundesrecht' ->> 'eli',
                    d.metadata -> 'landesrecht' -> 'lrKons' ->> 'eli')   AS eli
    FROM norm_document d
    WHERE d.abschnitt_typ = 'NORM'
      AND d.deleted_at IS NULL
      AND d.inkrafttreten <= current_date
    ORDER BY d.norm_id, d.inkrafttreten DESC
) h
WHERE h.norm_id = n.id
  AND n.langtitel IS NULL;

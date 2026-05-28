ALTER TABLE caselaw_analysis ADD COLUMN search_vector tsvector;

UPDATE caselaw_analysis
SET search_vector = CASE
                        WHEN identical_to_id IS NOT NULL THEN NULL
                        WHEN full_text IS NOT NULL THEN to_tsvector('german', full_text)
                        WHEN analysis IS NOT NULL THEN jsonb_to_tsvector('german', analysis::jsonb, '["string"]')
                        ELSE NULL
    END
where search_vector is null;
and id < 633452;



-- Run this repeatedly (you can script it or execute it in a loop)
UPDATE caselaw_analysis
SET search_vector = CASE
                        WHEN identical_to_id IS NOT NULL THEN NULL
                        WHEN full_text IS NOT NULL THEN to_tsvector('german', full_text)
                        WHEN analysis IS NOT NULL THEN jsonb_to_tsvector('german', analysis::jsonb, '["string"]')
                        ELSE NULL
    END
WHERE id IN (
    SELECT id FROM caselaw_analysis
    WHERE search_vector IS NULL and identical_to_id is null
    LIMIT 100000
    );


-- add trigger
CREATE OR REPLACE FUNCTION refresh_caselaw_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := CASE
        WHEN NEW.identical_to_id IS NOT NULL THEN NULL
        WHEN NEW.full_text IS NOT NULL THEN to_tsvector('german', NEW.full_text)
        WHEN NEW.analysis IS NOT NULL THEN jsonb_to_tsvector('german', NEW.analysis::jsonb, '["string"]')
        ELSE NULL
END;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach it to the table
CREATE TRIGGER tsvector_update_trigger
    BEFORE INSERT OR UPDATE ON caselaw_analysis
                         FOR EACH ROW EXECUTE FUNCTION refresh_caselaw_search_vector();
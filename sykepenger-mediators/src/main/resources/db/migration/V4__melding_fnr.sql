ALTER TABLE melding ADD COLUMN fnr VARCHAR(32) NOT NULL DEFAULT '';
CREATE INDEX "index_melding_fnr" ON melding USING btree (fnr);

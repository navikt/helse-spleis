DELETE FROM melding a USING melding b WHERE a.id > b.id AND a.melding_id = b.melding_id;
ALTER TABLE melding ADD CONSTRAINT unique_melding_id UNIQUE (melding_id);

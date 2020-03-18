ALTER TABLE person ALTER COLUMN data SET DATA TYPE json USING data::json;
ALTER TABLE melding ALTER COLUMN data SET DATA TYPE json USING data::json;

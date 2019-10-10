CREATE TABLE person
(
  aktor_id VARCHAR(32) NOT NULL,
  data JSONB NOT NULL,
  opprettet TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
  PRIMARY KEY (aktor_id)
);

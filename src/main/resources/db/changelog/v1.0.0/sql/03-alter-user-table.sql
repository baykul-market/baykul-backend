ALTER TABLE users ADD COLUMN IF NOT EXISTS localization CHARACTER VARYING(255);
ALTER TABLE users ADD CONSTRAINT users_localization_check CHECK (((localization)::text = ANY ((ARRAY['RUS'::character varying, 'ENG'::character varying])::text[])));
UPDATE users SET localization = 'RUS' WHERE localization IS NULL;
ALTER TABLE users ALTER COLUMN localization SET NOT NULL;
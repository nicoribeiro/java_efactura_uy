-- Campos de texto de emails y adjuntos recibidos: varchar(255) -> text
--
-- Un asunto, remitente, Message-ID o nombre de adjunto de mas de 255 caracteres hacia fallar el commit
-- ("value too long for type character varying(255)"). Las entidades ahora declaran estas columnas como TEXT, pero
-- hibernate.hbm2ddl.auto=update no modifica columnas existentes: hay que correr este script en cada base
-- (desarrollo, test y produccion) antes o despues de desplegar.
--
-- En PostgreSQL varchar -> text no reescribe la tabla ni pierde datos. Se puede correr mas de una vez.

BEGIN;

ALTER TABLE email_message
    ALTER COLUMN message_id TYPE text,
    ALTER COLUMN from_email TYPE text,
    ALTER COLUMN subject    TYPE text;

ALTER TABLE attachment
    ALTER COLUMN name       TYPE text,
    ALTER COLUMN url        TYPE text,
    ALTER COLUMN created_by TYPE text;

COMMIT;

-- Verificacion: las 6 columnas deben quedar con data_type = 'text'
-- SELECT table_name, column_name, data_type, character_maximum_length
-- FROM information_schema.columns
-- WHERE (table_name = 'email_message' AND column_name IN ('message_id', 'from_email', 'subject'))
--    OR (table_name = 'attachment' AND column_name IN ('name', 'url', 'created_by'));

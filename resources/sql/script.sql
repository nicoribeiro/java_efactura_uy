INSERT INTO empresa_emails_recibido("empresa_id", "emailsrecibido_id") 
	select <empresaID>, id from email_message;

CREATE OR REPLACE FUNCTION "UpdateTable"() RETURNS boolean
    LANGUAGE plpgsql
AS
$$
DECLARE
    TABLE_RECORD RECORD;
    a_id NUMERIC;
    e_id NUMERIC;
  BEGIN
    
FOR TABLE_RECORD IN SELECT * FROM email_message_attachment -- can select required fields only
    LOOP
        SELECT "attachment_id", "emailmessage_id" INTO a_id, e_id
        FROM email_message_attachment WHERE "attachment_id" = TABLE_RECORD."attachment_id";

        UPDATE attachment SET emailmessage_id = e_id WHERE id = a_id; 
    END LOOP;
    RETURN TRUE;
END
$$;

SELECT public."UpdateTable"();
-- Script de psaje de version 0.76 a 1.0
-- MOP: 
-- 1 Levantar java_efactura_uy
-- 2 ejecutar un metodo de consulta (para que levante hibernate)
-- 3 ejecutar este script SQL (se debe reemplazar <idEmpresa> por el id de la/s empresa/s emisora/s)


insert into sucursal(id, ciudad, departamento, domicilio_fiscal, empresa_id, codigo_sucursal, codigo_postal, telefono)
select id, localidad, departamento, direccion, id, codigo_sucursal, codigo_postal, telefono from empresa where localidad is not null;

ALTER TABLE "public"."empresa"
DROP COLUMN "codigo_postal",
DROP COLUMN "codigo_sucursal",
DROP COLUMN "departamento",
DROP COLUMN "direccion",
DROP COLUMN "localidad",
DROP COLUMN "telefono";

update cfe set sucursal_id = <idEmpresa> where empresaemisora_id = <idEmpresa>;

INSERT INTO empresa_emails_recibidos_error("empresa_id", "emailsrecibidoserror_id") 
	select <idEmpresa>, id from email_message;

CREATE OR REPLACE FUNCTION "UpdateTable"() RETURNS boolean
    LANGUAGE plpgsql
AS
$$
DECLARE
    TABLE_RECORD RECORD;
    a_id NUMERIC;
    e_id NUMERIC;
  BEGIN
    
FOR TABLE_RECORD IN SELECT * FROM email_message_attachment
    LOOP
        SELECT "attachment_id", "emailmessage_id" INTO a_id, e_id
        FROM email_message_attachment WHERE "attachment_id" = TABLE_RECORD."attachment_id";

        UPDATE attachment SET emailmessage_id = e_id WHERE id = a_id; 
    END LOOP;
    RETURN TRUE;
END
$$;

SELECT public."UpdateTable"();
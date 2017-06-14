package com.bluedot.efactura.pool;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;

import com.bluedot.efactura.interceptors.InterceptorContextHolder;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;

public class CustomMerlin extends Merlin{

	
	/*
	 * (non-Javadoc)
	 * 
	 * Esta clase se usa para poder firmar los Sobres, CFE y Reportes Diarios con el certificado de cada Empresa.
	 * 
	 * Lo que hacemos es tomar a la empresa del ThreadLocal y pedirle el keystore para setearlo en la clase Merlin
	 * 
	 * Luego se setea el alias del cert y se vuelve a hacer la llamada a super.getX509Certificates. 
	 * 
	 * Es decir, hacemos un override de este metodo pero al final lo volvemos a llamar al del padre.
	 * 
	 * @see org.apache.wss4j.common.crypto.Merlin#getX509Certificates(org.apache.wss4j.common.crypto.CryptoType)
	 */
	@Override
	 public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (cryptoType == null) {
            return null;
        }
        CryptoType.TYPE type = cryptoType.getType();
        X509Certificate[] certs = null;
        switch (type) {
        case ISSUER_SERIAL:
        case THUMBPRINT_SHA1:
        case SKI_BYTES:
        case SUBJECT_DN:
        case ENDPOINT:
            certs =  super.getX509Certificates(cryptoType);
            break;
        case ALIAS:
        	try {
				Empresa empresa = InterceptorContextHolder.getEmpresa();
				setKeyStore(empresa.getFirmaDigital().getKeyStore());        	
				cryptoType.setAlias(FirmaDigital.KEY_ALIAS);
				certs = super.getX509Certificates(cryptoType);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
        	break;
        }
        return certs;
    }
	
	
	
}

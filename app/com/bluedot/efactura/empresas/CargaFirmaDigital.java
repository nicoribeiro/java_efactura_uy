package com.bluedot.efactura.empresas;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;

/**
 * Arma y valida la firma digital de una empresa a partir de lo que se carga: el certificado y la clave privada en PEM, o
 * el archivo PKCS#12 (.p12 / .pfx) que entrega la autoridad certificadora, con su contrasena. Se guarda siempre en PEM,
 * que es lo que usa FirmaDigital.getKeyStore().
 *
 * Valida que la clave privada corresponda al certificado y que el certificado este vigente. No guarda nada.
 */
public class CargaFirmaDigital {

	/** Firma armada, sin guardar, con los datos del certificado para informar */
	public static class Resultado {
		public final FirmaDigital firma;
		public final X509Certificate certificado;
		/** El RUT de la empresa aparece en el titular del certificado */
		public final boolean rutEnCertificado;

		Resultado(FirmaDigital firma, X509Certificate certificado, boolean rutEnCertificado) {
			this.firma = firma;
			this.certificado = certificado;
			this.rutEnCertificado = rutEnCertificado;
		}
	}

	/** Certificado (uno o varios, la cadena) y clave privada en PEM (PKCS#1 o PKCS#8) */
	public static Resultado desdePem(Empresa empresa, String certificadoPem, String clavePem) throws APIException {
		FirmaDigital firma = new FirmaDigital();
		firma.setCertificate(certificadoPem);
		firma.setPrivateKey(clavePem);
		return validar(empresa, firma);
	}

	/** Archivo PKCS#12 (.p12 / .pfx) en base64 y su contrasena */
	public static Resultado desdePkcs12(Empresa empresa, String pkcs12Base64, String password) throws APIException {
		char[] pass = password != null ? password.toCharArray() : new char[0];
		KeyStore p12;
		try {
			p12 = KeyStore.getInstance("PKCS12");
			p12.load(new ByteArrayInputStream(Base64.getMimeDecoder().decode(pkcs12Base64)), pass);
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("pkcs12", "archivo")
					.setDetailMessage("No se pudo abrir el archivo .p12/.pfx: contraseña incorrecta o archivo inválido");
		}

		try {
			Enumeration<String> alias = p12.aliases();
			while (alias.hasMoreElements()) {
				String a = alias.nextElement();
				if (!p12.isKeyEntry(a))
					continue;
				PrivateKey clave = (PrivateKey) p12.getKey(a, pass);
				Certificate[] cadena = p12.getCertificateChain(a);
				if (clave == null || cadena == null || cadena.length == 0)
					continue;

				StringBuilder certificadoPem = new StringBuilder();
				for (Certificate c : cadena)
					certificadoPem.append(pem("CERTIFICATE", c.getEncoded()));

				// PKCS#8, que FirmaDigital sabe leer
				return desdePem(empresa, certificadoPem.toString(), pem("PRIVATE KEY", clave.getEncoded()));
			}
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("pkcs12", "archivo")
					.setDetailMessage("No se pudo leer la clave del archivo .p12/.pfx: " + e.getMessage());
		}
		throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("pkcs12", "archivo")
				.setDetailMessage("El archivo .p12/.pfx no tiene una clave privada con su certificado");
	}

	private static String pem(String tipo, byte[] contenido) {
		return "-----BEGIN " + tipo + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(contenido)
				+ "\n-----END " + tipo + "-----\n";
	}

	private static Resultado validar(Empresa empresa, FirmaDigital firma) throws APIException {
		X509Certificate certificado;
		PrivateKey clave;
		try {
			KeyStore keyStore = firma.getKeyStore();
			certificado = (X509Certificate) keyStore.getCertificate(FirmaDigital.KEY_ALIAS);
			clave = (PrivateKey) keyStore.getKey(FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD.toCharArray());
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("firmaDigital", "certificado")
					.setDetailMessage("Certificado o clave privada inválidos");
		}

		// La clave privada tiene que ser la del certificado: se firma algo y se verifica con la clave publica
		try {
			String algoritmo = "EC".equals(clave.getAlgorithm()) ? "SHA256withECDSA" : "SHA256with" + clave.getAlgorithm();
			byte[] prueba = "efactura".getBytes("UTF-8");
			Signature firmar = Signature.getInstance(algoritmo);
			firmar.initSign(clave);
			firmar.update(prueba);
			byte[] firmado = firmar.sign();
			Signature verificar = Signature.getInstance(algoritmo);
			verificar.initVerify(certificado.getPublicKey());
			verificar.update(prueba);
			if (!verificar.verify(firmado))
				throw new IllegalStateException();
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("firmaDigital", "clave")
					.setDetailMessage("La clave privada no corresponde al certificado");
		}

		Date ahora = new Date();
		if (certificado.getNotAfter().before(ahora))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("firmaDigital", "certificado")
					.setDetailMessage("El certificado está vencido");
		if (certificado.getNotBefore().after(ahora))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("firmaDigital", "certificado")
					.setDetailMessage("El certificado todavía no es válido");

		firma.setValidaDesde(certificado.getNotBefore());
		firma.setValidaHasta(certificado.getNotAfter());

		boolean rutEnCertificado = certificado.getSubjectX500Principal().getName().contains(empresa.getRut());
		return new Resultado(firma, certificado, rutEnCertificado);
	}
}

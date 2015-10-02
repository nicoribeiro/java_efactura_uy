package com.bluedot.efactura.commons;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bluedot.commons.Settings;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.KeyPasswordCallback;

public class Commons
{
	private static String securityPrefixName = "org.apache.ws.security.crypto.merlin.keystore.";

	public enum DgiService {
		Recepcion, Consulta, Rut
	}

	public static String getURL(DgiService service) throws FileNotFoundException, IOException
	{

		Settings settings = Settings.getInstance();

		String environment = settings.getString(Constants.ENVIRONMENT);

		switch (service)
		{
		case Consulta:
			return settings.getString(Constants.SERVICE_CONSULTA_PREFIX + "." + environment);
		case Recepcion:
			return settings.getString(Constants.SERVICE_RECEPCION_URL_PREFIX + "." + environment);
		case Rut:
			return settings.getString(Constants.SERVICE_RUT_URL_PREFIX + "." + environment);
		}
		return null;

	}

	

	public static KeyPasswordCallback getPasswordCallback() throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException
	{

		Map<String, String> keystorePasswords = new HashMap<>();
		keystorePasswords.put(getCetificateAlias(), getCertificatePassword());
		return new KeyPasswordCallback(keystorePasswords);

	}

	public static KeyStore getKeyStore() throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		KeyStore keystore = KeyStore.getInstance(securityProperties.getProperty(securityPrefixName + "type"));
		FileInputStream fIn = new FileInputStream(securityProperties.getProperty(securityPrefixName + "file"));
		keystore.load(fIn, securityProperties.getProperty(securityPrefixName + "password").toCharArray());

		return keystore;
	}

	public static String getCertificatePassword() throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		return securityProperties.getProperty("certificate.password");

	}

	public static String getCetificateAlias() throws FileNotFoundException, IOException
	{
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		return securityProperties.getProperty("certificate.alias");
	}

	public static JSONObject safeGetJSONObject(JSONObject object, String key) throws EFacturaException
	{
		if (object.optJSONObject(key) == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage(key);
		return object.getJSONObject(key);
	}

}

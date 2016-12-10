package com.bluedot.efactura.pool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.bluedot.commons.utils.ObjectPool;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.Commons.DgiService;
import com.bluedot.efactura.interceptors.CDataWriterInterceptor;
import com.bluedot.efactura.interceptors.NamespacesInterceptor;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import dgi.soap.recepcion.WSEFacturaSoapPort;
import play.Application;

@Singleton
public class WSRecepcionPool extends ObjectPool<WSEFacturaSoapPortWrapper> {

	private Provider<Application> application;
	
	private Commons commons;
	
	@Inject
	public WSRecepcionPool(Provider<Application> application, Commons commons){
		this.application = application;
		this.commons = commons;
	}
	

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private String securityPropertiesPath;
	private String certificateAlias;
	private CallbackHandler passwordCallback;
	private String serviceURL;


	/**
	 * Configures security settings and in/out interceptors and builds a client
	 * service proxy
	 */
	@Override
	protected WSEFacturaSoapPortWrapper create() {
		try {
			JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
			factory.setAddress(serviceURL);
			WSEFacturaSoapPort port = factory.create(WSEFacturaSoapPort.class);

			Endpoint cxfEndpoint = ClientProxy.getClient(port).getEndpoint();

			Map<String, Object> outProps = new HashMap<>();
			outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
			outProps.put(WSHandlerConstants.USER, getCertificateAlias());
			outProps.put(WSHandlerConstants.SIG_PROP_FILE, getSecurityPropertiesPath());
			outProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
			outProps.put(WSHandlerConstants.PW_CALLBACK_REF, getPasswordCallback());

			WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
			cxfEndpoint.getOutInterceptors().add(wssOut);

			// Note: uncomment this if you need to log outgoing soap signed request
			//cxfEndpoint.getOutInterceptors().add(new SoapEnvelopeLoggingOutInterceptor());

			SignatureInterceptor signatureInterceptor = new SignatureInterceptor();
			cxfEndpoint.getOutInterceptors().add(signatureInterceptor);

			NamespacesInterceptor interceptor = new NamespacesInterceptor();
			cxfEndpoint.getOutInterceptors().add(interceptor);

			CDataWriterInterceptor cdataInterceptor = new CDataWriterInterceptor();
			cxfEndpoint.getOutInterceptors().add(cdataInterceptor);
			
			return new WSEFacturaSoapPortWrapper(port);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}
		return null; 
	}

	@Override
	public boolean validate(WSEFacturaSoapPortWrapper o) {
		return true;
	}

	@Override
	public void expire(WSEFacturaSoapPortWrapper o) {
	}
	
	public String getSecurityPropertiesPath() {
		this.securityPropertiesPath = Objects.requireNonNull(application.get().configuration().getString(Constants.SECURITY_FILE), "Security properties path is required");
		return securityPropertiesPath;
	}

	public String getCertificateAlias() throws FileNotFoundException, IOException {
		this.certificateAlias = Objects.requireNonNull(commons.getCetificateAlias(), "Certificate alias is required");
		return certificateAlias;
	}

	public CallbackHandler getPasswordCallback() throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		this.passwordCallback = Objects.requireNonNull(commons.getPasswordCallback(), "Password callback is required");
		return passwordCallback;
	}

	public String getServiceURL() {
		this.serviceURL = commons.getURL(DgiService.Rut);
		return serviceURL;
	}

}

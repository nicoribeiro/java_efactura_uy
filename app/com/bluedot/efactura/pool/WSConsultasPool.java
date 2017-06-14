package com.bluedot.efactura.pool;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.ObjectPool;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.Commons.DgiService;
import com.bluedot.efactura.interceptors.CDataWriterInterceptor;
import com.bluedot.efactura.interceptors.NamespacesInterceptor;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.pool.wrappers.WSEFacturaConsultasSoapPortWrapper;

import dgi.soap.consultas.WSEFacturaConsultasSoapPort;

public class WSConsultasPool extends ObjectPool<WSEFacturaConsultasSoapPortWrapper> {
	private static WSConsultasPool instance = null;

	public static synchronized WSConsultasPool getInstance()
			throws IOException, APIException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		if (instance == null) {
			CallbackHandler passwordCallback = FirmaDigital.getPasswordCallback();
			instance = new WSConsultasPool(FirmaDigital.KEY_ALIAS,
					passwordCallback, Commons.getURL(DgiService.Consulta));
		}

		return instance;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private final String certificateAlias;
	private final CallbackHandler passwordCallback;
	private final String serviceURL;

	private WSConsultasPool(String keystoreAlias, CallbackHandler passwordCallback,
			String serviceURL) {
		this.certificateAlias = Objects.requireNonNull(keystoreAlias, "Certificate alias is required");
		this.passwordCallback = Objects.requireNonNull(passwordCallback, "Password callback is required");
		this.serviceURL = serviceURL;
	}

	/**
	 * Configures security settings and in/out interceptors and builds a client
	 * service proxy
	 */
	@Override
	protected WSEFacturaConsultasSoapPortWrapper create() {
		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		factory.setAddress(serviceURL);
		WSEFacturaConsultasSoapPort port = factory.create(WSEFacturaConsultasSoapPort.class);

		Endpoint cxfEndpoint = ClientProxy.getClient(port).getEndpoint();

		Crypto customMerlin = new CustomMerlin();
		
		Map<String, Object> outProps = new HashMap<>();
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
		outProps.put(WSHandlerConstants.USER, certificateAlias);
		outProps.put(WSHandlerConstants.SIG_PROP_REF_ID, "CustomMerlin");
		outProps.put("CustomMerlin", customMerlin);
		outProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
		outProps.put(WSHandlerConstants.PW_CALLBACK_REF, passwordCallback);

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
		
		return new WSEFacturaConsultasSoapPortWrapper(port);
	}

	@Override
	public boolean validate(WSEFacturaConsultasSoapPortWrapper o) {
		return true;
	}

	@Override
	public void expire(WSEFacturaConsultasSoapPortWrapper o) {
	}

}

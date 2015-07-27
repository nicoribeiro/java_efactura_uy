package com.bluedot.efactura.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.bluedot.efactura.CompanyDataService;

import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialExecute;
import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialExecuteResponse;
import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialSoapPort;

public class CompanyDataServiceImpl implements CompanyDataService {

    private static final String SERVICE_URL = "https://efactura.dgi.gub.uy:6470/ePrueba/ws_personaGetActEmpresarialPrueba";

    private final String securityPropertiesPath;
    private final String keystoreAlias;
    private final CallbackHandler passwordCallback;

    public CompanyDataServiceImpl(String securityPropertiesPath, String keystoreAlias, CallbackHandler passwordCallback) {
        this.securityPropertiesPath = Objects.requireNonNull(securityPropertiesPath, "Security properties path is required");
        this.keystoreAlias = Objects.requireNonNull(keystoreAlias, "Keystore alias is required");
        this.passwordCallback = Objects.requireNonNull(passwordCallback, "Password callback is required");
    }

    @Override
    public String call(String rut) {
        Objects.requireNonNull(rut, "Parameter RUT is required");

        WSPersonaGetActEmpresarialSoapPort port = buildClient(passwordCallback);

        WSPersonaGetActEmpresarialExecute input = new WSPersonaGetActEmpresarialExecute();
        input.setRut(rut);
        WSPersonaGetActEmpresarialExecuteResponse output = port.execute(input);

        return output.getData();
    }

    /**
     * Configures security settings and in/out interceptors and builds a client service proxy
     * @param callbackHandler private key password callback handler
     * @return
     * @see http://cxf.apache.org/faq.html#FAQ-AreJAX-WSclientproxiesthreadsafe? for comments on thread safety of created client proxy
     */
    private WSPersonaGetActEmpresarialSoapPort buildClient(CallbackHandler callbackHandler) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(SERVICE_URL);
        WSPersonaGetActEmpresarialSoapPort port = factory.create(WSPersonaGetActEmpresarialSoapPort.class);

        Endpoint cxfEndpoint = ClientProxy.getClient(port).getEndpoint();

        Map<String, Object> outProps = new HashMap<>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProps.put(WSHandlerConstants.USER, keystoreAlias);
        outProps.put(WSHandlerConstants.SIG_PROP_FILE, securityPropertiesPath);
        outProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProps.put(WSHandlerConstants.PW_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(wssOut);
        // Note: uncomment this if you need to log outgoing signed request
//        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        return port;
    }

}

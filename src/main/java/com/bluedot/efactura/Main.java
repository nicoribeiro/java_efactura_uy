package com.bluedot.efactura;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.xml.security.c14n.helper.C14nHelper;

import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialExecute;
import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialExecuteResponse;
import dgi.modernizacion.consolidado.WSPersonaGetActEmpresarialSoapPort;

/**
 * Test class showcasing the setup required to call the service.
 * <p>
 * For it to work the following are required:<br>
 * 1. A password for keystore entry provided in main method args<br>
 * 2. "security" folder located under this project's resources folder. The security folder contains the keystore and security properties file.<br>
 * 3. Unlimited strength JCE policy installed (see http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)<br>
 * 4. A hack to work around the issue on relative namespaces used in the service. This is achieved via a modified {@link C14nHelper} class in the
 * project's classpath (giving it higher precedence and being loaded instead of the default one). The only modification there is a hardcoded
 * short-circuit in namespaceIsAbsolute method.
 *
 */
public class Main {

    public static final String KEYSTORE_ALIAS = "lerand";
    private static final String SERVICE_URL = "https://efactura.dgi.gub.uy:6470/ePrueba/ws_personaGetActEmpresarialPrueba";
    private static final String SECURITY_PROPERTIES_PATH = "security/security.properties";

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("One argument - keystore password - must be provided");
        }

        // This should come from the environment, some config file, etc.
        String keystorePassword = args[0];

        // Build a client (which is not thread safe!)
        Map<String, String> keystorePasswords = new HashMap<>();
        keystorePasswords.put(KEYSTORE_ALIAS, keystorePassword);
        CallbackHandler passwordCallback = new KeyPasswordCallback(keystorePasswords);
        WSPersonaGetActEmpresarialSoapPort port = buildClient(passwordCallback);

        // Call the service
        WSPersonaGetActEmpresarialExecute input = new WSPersonaGetActEmpresarialExecute();
        input.setRut("219000090011");
        WSPersonaGetActEmpresarialExecuteResponse output = port.execute(input);

        System.out.println("Output data:\n" + output.getData());
    }

    /**
     * Configures security settings and in/out interceptors
     * @param callbackHandler private key password callback handler
     * @return
     */
    public static WSPersonaGetActEmpresarialSoapPort buildClient(CallbackHandler callbackHandler) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(SERVICE_URL);
        WSPersonaGetActEmpresarialSoapPort port = factory.create(WSPersonaGetActEmpresarialSoapPort.class);

        Endpoint cxfEndpoint = ClientProxy.getClient(port).getEndpoint();

        Map<String, Object> outProps = new HashMap<>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProps.put(WSHandlerConstants.USER, KEYSTORE_ALIAS);
        outProps.put(WSHandlerConstants.SIG_PROP_FILE, SECURITY_PROPERTIES_PATH);
        outProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProps.put(WSHandlerConstants.PW_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        cxfEndpoint.getOutInterceptors().add(wssOut);
        return port;
    }
}

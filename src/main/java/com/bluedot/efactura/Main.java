package com.bluedot.efactura;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.CallbackHandler;

import org.apache.xml.security.c14n.helper.C14nHelper;

import com.bluedot.efactura.impl.CompanyDataServiceImpl;
import com.bluedot.efactura.impl.KeyPasswordCallback;

/**
 * Test class showcasing the setup required to call the service.
 * <p>
 * For it to work the following are required:<br>
 * 1. A password for keystore entry provided as "keystorePass" VM argument<br>
 * 2. "security" folder located under this project's resources folder. The security folder contains the keystore and security properties file.<br>
 * 3. Unlimited strength JCE policy installed (see http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)<br>
 * 4. A hack to work around the issue on relative namespaces used in the service. This is achieved via a modified {@link C14nHelper} class in the
 * project's classpath (giving it higher precedence and being loaded instead of the default one). The only modification there is a hardcoded
 * short-circuit in namespaceIsAbsolute method.
 *
 */
public class Main {

    private static final String SECURITY_PROPERTIES_PATH = "security/security.properties";
    public static final String KEYSTORE_ALIAS = "lerand";

    public static void main(String[] args) {

        // This can be set through VM argument -DkeystorePass=password
        String keystorePassword = System.getProperty("keystorePass");
        Objects.requireNonNull(keystorePassword, "Password must be set");

        // Build a client (which is not thread safe!)
        Map<String, String> keystorePasswords = new HashMap<>();
        keystorePasswords.put(KEYSTORE_ALIAS, keystorePassword);
        CallbackHandler passwordCallback = new KeyPasswordCallback(keystorePasswords);

        CompanyDataService service = new CompanyDataServiceImpl(SECURITY_PROPERTIES_PATH, KEYSTORE_ALIAS, passwordCallback);

        String response = service.call("219000090011");

        // Call the service
        System.out.println("Output data:\n" + response);
    }
}

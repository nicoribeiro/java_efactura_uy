package com.bluedot.efactura;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.CallbackHandler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.bluedot.efactura.impl.CompanyDataServiceImpl;
import com.bluedot.efactura.impl.KeyPasswordCallback;

public class CompanyDataServiceImplTest {

    private static final String SECURITY_PROPERTIES_PATH = "security/security.properties";
    private static final String KEYSTORE_ALIAS = "lerand";

    private CompanyDataService service;

    @Before
    public void setUp() {

        // This can be set through VM argument -DkeystorePass=password
        String keystorePassword = System.getProperty("keystorePass");
        Objects.requireNonNull(keystorePassword, "Password must be set");

        // Build a client (which is not thread safe!)
        Map<String, String> keystorePasswords = new HashMap<>();
        keystorePasswords.put(KEYSTORE_ALIAS, keystorePassword);
        CallbackHandler passwordCallback = new KeyPasswordCallback(keystorePasswords);

        service = new CompanyDataServiceImpl(SECURITY_PROPERTIES_PATH, KEYSTORE_ALIAS, passwordCallback);
    }

    @Test
    public void testService() throws Exception {
        String response = service.call("219000090011");
        Assert.assertTrue(response.contains("WS_PersonaActEmpresarial"));
    }
}

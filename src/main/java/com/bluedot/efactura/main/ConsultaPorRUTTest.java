package com.bluedot.efactura.main;

import org.apache.xml.security.c14n.helper.C14nHelper;

import com.bluedot.efactura.services.ConsultaRutService;
import com.bluedot.efactura.services.impl.ConsultaRutServiceImpl;

/**
 * Test class showcasing the setup required to call the service RUT.
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
public class ConsultaPorRUTTest {

    

    public static void main(String[] args) {

        ConsultaRutService service = new ConsultaRutServiceImpl();
     
        // Call the service
        String response = service.getRutData("219000090011");

        
        System.out.println("Output data:\n" + response);
    }
}

package dgi.soap.consultas;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.0.4
 * 2017-05-17T18:18:40.180-03:00
 * Generated source version: 3.0.4
 * 
 */
@WebServiceClient(name = "WS_eFactura_Consultas", 
                  wsdlLocation = "https://efactura.dgi.gub.uy:6460/ePrueba/ws_consultasPrueba?wsdl",
                  targetNamespace = "http://dgi.gub.uy") 
public class WSEFacturaConsultas extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://dgi.gub.uy", "WS_eFactura_Consultas");
    public final static QName WSEFacturaConsultasSoapPort = new QName("http://dgi.gub.uy", "WS_eFactura_ConsultasSoapPort");
    static {
        URL url = null;
        try {
            url = new URL("https://efactura.dgi.gub.uy:6460/ePrueba/ws_consultasPrueba?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(WSEFacturaConsultas.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "https://efactura.dgi.gub.uy:6460/ePrueba/ws_consultasPrueba?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public WSEFacturaConsultas(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public WSEFacturaConsultas(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WSEFacturaConsultas() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public WSEFacturaConsultas(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public WSEFacturaConsultas(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public WSEFacturaConsultas(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    

    /**
     *
     * @return
     *     returns WSEFacturaConsultasSoapPort
     */
    @WebEndpoint(name = "WS_eFactura_ConsultasSoapPort")
    public WSEFacturaConsultasSoapPort getWSEFacturaConsultasSoapPort() {
        return super.getPort(WSEFacturaConsultasSoapPort, WSEFacturaConsultasSoapPort.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WSEFacturaConsultasSoapPort
     */
    @WebEndpoint(name = "WS_eFactura_ConsultasSoapPort")
    public WSEFacturaConsultasSoapPort getWSEFacturaConsultasSoapPort(WebServiceFeature... features) {
        return super.getPort(WSEFacturaConsultasSoapPort, WSEFacturaConsultasSoapPort.class, features);
    }

}
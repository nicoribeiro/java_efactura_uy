package dgi.soap.recepcion;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 3.0.4
 * 2015-07-08T07:30:54.823-03:00
 * Generated source version: 3.0.4
 * 
 */
@WebService(targetNamespace = "http://dgi.gub.uy", name = "WS_eFacturaSoapPort")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface WSEFacturaSoapPort {

    @WebResult(name = "WS_eFactura.EFACRECEPCIONREPORTEResponse", targetNamespace = "http://dgi.gub.uy", partName = "parameters")
    @WebMethod(operationName = "EFACRECEPCIONREPORTE", action = "http://dgi.gub.uyaction/AWS_EFACTURA.EFACRECEPCIONREPORTE")
    public WSEFacturaEFACRECEPCIONREPORTEResponse efacrecepcionreporte(
        @WebParam(partName = "parameters", name = "WS_eFactura.EFACRECEPCIONREPORTE", targetNamespace = "http://dgi.gub.uy")
        WSEFacturaEFACRECEPCIONREPORTE parameters
    );

    @WebResult(name = "WS_eFactura.EFACCONSULTARESTADOENVIOResponse", targetNamespace = "http://dgi.gub.uy", partName = "parameters")
    @WebMethod(operationName = "EFACCONSULTARESTADOENVIO", action = "http://dgi.gub.uyaction/AWS_EFACTURA.EFACCONSULTARESTADOENVIO")
    public WSEFacturaEFACCONSULTARESTADOENVIOResponse efacconsultarestadoenvio(
        @WebParam(partName = "parameters", name = "WS_eFactura.EFACCONSULTARESTADOENVIO", targetNamespace = "http://dgi.gub.uy")
        WSEFacturaEFACCONSULTARESTADOENVIO parameters
    );

    @WebResult(name = "WS_eFactura.EFACRECEPCIONSOBREResponse", targetNamespace = "http://dgi.gub.uy", partName = "parameters")
    @WebMethod(operationName = "EFACRECEPCIONSOBRE", action = "http://dgi.gub.uyaction/AWS_EFACTURA.EFACRECEPCIONSOBRE")
    public WSEFacturaEFACRECEPCIONSOBREResponse efacrecepcionsobre(
        @WebParam(partName = "parameters", name = "WS_eFactura.EFACRECEPCIONSOBRE", targetNamespace = "http://dgi.gub.uy")
        WSEFacturaEFACRECEPCIONSOBRE parameters
    );
}

package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Ackconsultaestadocfe" type="{http://dgi.gub.uy}ACKConsultaEstadoCFE"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "ackconsultaestadocfe"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARESTADOCFEResponse")
public class WSEFacturaConsultasEFACCONSULTARESTADOCFEResponse {

    @XmlElement(name = "Ackconsultaestadocfe", required = true)
    protected ACKConsultaEstadoCFE ackconsultaestadocfe;

    /**
     * Gets the value of the ackconsultaestadocfe property.
     * 
     * @return
     *     possible object is
     *     {@link ACKConsultaEstadoCFE }
     *     
     */
    public ACKConsultaEstadoCFE getAckconsultaestadocfe() {
        return ackconsultaestadocfe;
    }

    /**
     * Sets the value of the ackconsultaestadocfe property.
     * 
     * @param value
     *     allowed object is
     *     {@link ACKConsultaEstadoCFE }
     *     
     */
    public void setAckconsultaestadocfe(ACKConsultaEstadoCFE value) {
        this.ackconsultaestadocfe = value;
    }

}


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
 *         &lt;element name="Ackconsultaenvioscfe" type="{http://dgi.gub.uy}ACKConsultaEnviosCFE"/&gt;
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
    "ackconsultaenvioscfe"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARENVIOSCFEResponse")
public class WSEFacturaConsultasEFACCONSULTARENVIOSCFEResponse {

    @XmlElement(name = "Ackconsultaenvioscfe", required = true)
    protected ACKConsultaEnviosCFE ackconsultaenvioscfe;

    /**
     * Gets the value of the ackconsultaenvioscfe property.
     * 
     * @return
     *     possible object is
     *     {@link ACKConsultaEnviosCFE }
     *     
     */
    public ACKConsultaEnviosCFE getAckconsultaenvioscfe() {
        return ackconsultaenvioscfe;
    }

    /**
     * Sets the value of the ackconsultaenvioscfe property.
     * 
     * @param value
     *     allowed object is
     *     {@link ACKConsultaEnviosCFE }
     *     
     */
    public void setAckconsultaenvioscfe(ACKConsultaEnviosCFE value) {
        this.ackconsultaenvioscfe = value;
    }

}

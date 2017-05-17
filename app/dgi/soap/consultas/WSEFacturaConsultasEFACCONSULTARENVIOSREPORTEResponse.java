
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
 *         &lt;element name="Ackconsultaenviosreporte" type="{http://dgi.gub.uy}ACKConsultaEnviosReporte"/&gt;
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
    "ackconsultaenviosreporte"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARENVIOSREPORTEResponse")
public class WSEFacturaConsultasEFACCONSULTARENVIOSREPORTEResponse {

    @XmlElement(name = "Ackconsultaenviosreporte", required = true)
    protected ACKConsultaEnviosReporte ackconsultaenviosreporte;

    /**
     * Gets the value of the ackconsultaenviosreporte property.
     * 
     * @return
     *     possible object is
     *     {@link ACKConsultaEnviosReporte }
     *     
     */
    public ACKConsultaEnviosReporte getAckconsultaenviosreporte() {
        return ackconsultaenviosreporte;
    }

    /**
     * Sets the value of the ackconsultaenviosreporte property.
     * 
     * @param value
     *     allowed object is
     *     {@link ACKConsultaEnviosReporte }
     *     
     */
    public void setAckconsultaenviosreporte(ACKConsultaEnviosReporte value) {
        this.ackconsultaenviosreporte = value;
    }

}

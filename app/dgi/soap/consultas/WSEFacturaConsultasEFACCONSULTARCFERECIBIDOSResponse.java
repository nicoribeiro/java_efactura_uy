
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
 *         &lt;element name="Ackconsultacferecibidos" type="{http://dgi.gub.uy}ACKConsultaCFERecibidos"/&gt;
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
    "ackconsultacferecibidos"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARCFERECIBIDOSResponse")
public class WSEFacturaConsultasEFACCONSULTARCFERECIBIDOSResponse {

    @XmlElement(name = "Ackconsultacferecibidos", required = true)
    protected ACKConsultaCFERecibidos ackconsultacferecibidos;

    /**
     * Gets the value of the ackconsultacferecibidos property.
     * 
     * @return
     *     possible object is
     *     {@link ACKConsultaCFERecibidos }
     *     
     */
    public ACKConsultaCFERecibidos getAckconsultacferecibidos() {
        return ackconsultacferecibidos;
    }

    /**
     * Sets the value of the ackconsultacferecibidos property.
     * 
     * @param value
     *     allowed object is
     *     {@link ACKConsultaCFERecibidos }
     *     
     */
    public void setAckconsultacferecibidos(ACKConsultaCFERecibidos value) {
        this.ackconsultacferecibidos = value;
    }

}


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
 *         &lt;element name="Consultacferecibidos" type="{http://dgi.gub.uy}ConsultaCFERecibidos"/&gt;
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
    "consultacferecibidos"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARCFERECIBIDOS")
public class WSEFacturaConsultasEFACCONSULTARCFERECIBIDOS {

    @XmlElement(name = "Consultacferecibidos", required = true)
    protected ConsultaCFERecibidos consultacferecibidos;

    /**
     * Gets the value of the consultacferecibidos property.
     * 
     * @return
     *     possible object is
     *     {@link ConsultaCFERecibidos }
     *     
     */
    public ConsultaCFERecibidos getConsultacferecibidos() {
        return consultacferecibidos;
    }

    /**
     * Sets the value of the consultacferecibidos property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConsultaCFERecibidos }
     *     
     */
    public void setConsultacferecibidos(ConsultaCFERecibidos value) {
        this.consultacferecibidos = value;
    }

}

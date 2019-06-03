
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
 *         &lt;element name="Consultarucemisores" type="{http://dgi.gub.uy}ConsultaRUCEmisores"/&gt;
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
    "consultarucemisores"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARRUCEMISORES")
public class WSEFacturaConsultasEFACCONSULTARRUCEMISORES {

    @XmlElement(name = "Consultarucemisores", required = true)
    protected ConsultaRUCEmisores consultarucemisores;

    /**
     * Gets the value of the consultarucemisores property.
     * 
     * @return
     *     possible object is
     *     {@link ConsultaRUCEmisores }
     *     
     */
    public ConsultaRUCEmisores getConsultarucemisores() {
        return consultarucemisores;
    }

    /**
     * Sets the value of the consultarucemisores property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConsultaRUCEmisores }
     *     
     */
    public void setConsultarucemisores(ConsultaRUCEmisores value) {
        this.consultarucemisores = value;
    }

}

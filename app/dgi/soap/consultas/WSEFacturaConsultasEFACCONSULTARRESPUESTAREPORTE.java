
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
 *         &lt;element name="Consultarespuestareporte" type="{http://dgi.gub.uy}ConsultaRespuestaReporte"/&gt;
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
    "consultarespuestareporte"
})
@XmlRootElement(name = "WS_eFactura_Consultas.EFACCONSULTARRESPUESTAREPORTE")
public class WSEFacturaConsultasEFACCONSULTARRESPUESTAREPORTE {

    @XmlElement(name = "Consultarespuestareporte", required = true)
    protected ConsultaRespuestaReporte consultarespuestareporte;

    /**
     * Gets the value of the consultarespuestareporte property.
     * 
     * @return
     *     possible object is
     *     {@link ConsultaRespuestaReporte }
     *     
     */
    public ConsultaRespuestaReporte getConsultarespuestareporte() {
        return consultarespuestareporte;
    }

    /**
     * Sets the value of the consultarespuestareporte property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConsultaRespuestaReporte }
     *     
     */
    public void setConsultarespuestareporte(ConsultaRespuestaReporte value) {
        this.consultarespuestareporte = value;
    }

}


package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ACKConsultaEnviosReporte complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ACKConsultaEnviosReporte"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="ColeccionDatosReporte" type="{http://dgi.gub.uy}ArrayOfDatosReporte"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ACKConsultaEnviosReporte", propOrder = {

})
public class ACKConsultaEnviosReporte {

    @XmlElement(name = "ColeccionDatosReporte", required = true)
    protected ArrayOfDatosReporte coleccionDatosReporte;

    /**
     * Gets the value of the coleccionDatosReporte property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDatosReporte }
     *     
     */
    public ArrayOfDatosReporte getColeccionDatosReporte() {
        return coleccionDatosReporte;
    }

    /**
     * Sets the value of the coleccionDatosReporte property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDatosReporte }
     *     
     */
    public void setColeccionDatosReporte(ArrayOfDatosReporte value) {
        this.coleccionDatosReporte = value;
    }

}

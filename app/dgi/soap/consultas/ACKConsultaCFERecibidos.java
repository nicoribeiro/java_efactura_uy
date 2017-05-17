
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ACKConsultaCFERecibidos complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ACKConsultaCFERecibidos"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="ColeccionDatosCFE" type="{http://dgi.gub.uy}ArrayOfDatosCFE"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ACKConsultaCFERecibidos", propOrder = {

})
public class ACKConsultaCFERecibidos {

    @XmlElement(name = "ColeccionDatosCFE", required = true)
    protected ArrayOfDatosCFE coleccionDatosCFE;

    /**
     * Gets the value of the coleccionDatosCFE property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDatosCFE }
     *     
     */
    public ArrayOfDatosCFE getColeccionDatosCFE() {
        return coleccionDatosCFE;
    }

    /**
     * Sets the value of the coleccionDatosCFE property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDatosCFE }
     *     
     */
    public void setColeccionDatosCFE(ArrayOfDatosCFE value) {
        this.coleccionDatosCFE = value;
    }

}

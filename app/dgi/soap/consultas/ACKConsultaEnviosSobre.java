
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ACKConsultaEnviosSobre complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ACKConsultaEnviosSobre"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="ColeccionDatosSobre" type="{http://dgi.gub.uy}ArrayOfDatosSobre"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ACKConsultaEnviosSobre", propOrder = {

})
public class ACKConsultaEnviosSobre {

    @XmlElement(name = "ColeccionDatosSobre", required = true)
    protected ArrayOfDatosSobre coleccionDatosSobre;

    /**
     * Gets the value of the coleccionDatosSobre property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDatosSobre }
     *     
     */
    public ArrayOfDatosSobre getColeccionDatosSobre() {
        return coleccionDatosSobre;
    }

    /**
     * Sets the value of the coleccionDatosSobre property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDatosSobre }
     *     
     */
    public void setColeccionDatosSobre(ArrayOfDatosSobre value) {
        this.coleccionDatosSobre = value;
    }

}

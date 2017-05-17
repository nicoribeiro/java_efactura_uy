
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ACKConsultaEnviosCFE complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ACKConsultaEnviosCFE"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="ColeccionDatosSobres" type="{http://dgi.gub.uy}ArrayOfDatosSobre"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ACKConsultaEnviosCFE", propOrder = {

})
public class ACKConsultaEnviosCFE {

    @XmlElement(name = "ColeccionDatosSobres", required = true)
    protected ArrayOfDatosSobre coleccionDatosSobres;

    /**
     * Gets the value of the coleccionDatosSobres property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDatosSobre }
     *     
     */
    public ArrayOfDatosSobre getColeccionDatosSobres() {
        return coleccionDatosSobres;
    }

    /**
     * Sets the value of the coleccionDatosSobres property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDatosSobre }
     *     
     */
    public void setColeccionDatosSobres(ArrayOfDatosSobre value) {
        this.coleccionDatosSobres = value;
    }

}

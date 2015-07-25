
package dgi.modernizacion.consolidado;

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
 *         &lt;element name="Rut" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
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
    "rut"
})
@XmlRootElement(name = "WS_PersonaGetActEmpresarial.Execute")
public class WSPersonaGetActEmpresarialExecute {

    @XmlElement(name = "Rut", required = true)
    protected String rut;

    /**
     * Gets the value of the rut property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRut() {
        return rut;
    }

    /**
     * Sets the value of the rut property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRut(String value) {
        this.rut = value;
    }

}

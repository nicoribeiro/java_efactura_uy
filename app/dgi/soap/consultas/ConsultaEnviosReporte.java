
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for ConsultaEnviosReporte complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConsultaEnviosReporte"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="FechaResumen" type="{http://www.w3.org/2001/XMLSchema}date"/&gt;
 *         &lt;element name="Secuencia" type="{http://www.w3.org/2001/XMLSchema}byte" minOccurs="0"/&gt;
 *         &lt;element name="IdEmisor" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConsultaEnviosReporte", propOrder = {

})
public class ConsultaEnviosReporte {

    @XmlElement(name = "FechaResumen", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar fechaResumen;
    @XmlElement(name = "Secuencia", defaultValue = "0")
    protected Byte secuencia;
    @XmlElement(name = "IdEmisor", defaultValue = "0")
    protected Long idEmisor;

    /**
     * Gets the value of the fechaResumen property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFechaResumen() {
        return fechaResumen;
    }

    /**
     * Sets the value of the fechaResumen property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFechaResumen(XMLGregorianCalendar value) {
        this.fechaResumen = value;
    }

    /**
     * Gets the value of the secuencia property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getSecuencia() {
        return secuencia;
    }

    /**
     * Sets the value of the secuencia property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setSecuencia(Byte value) {
        this.secuencia = value;
    }

    /**
     * Gets the value of the idEmisor property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getIdEmisor() {
        return idEmisor;
    }

    /**
     * Sets the value of the idEmisor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setIdEmisor(Long value) {
        this.idEmisor = value;
    }

}

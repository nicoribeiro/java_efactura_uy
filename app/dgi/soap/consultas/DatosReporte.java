
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for DatosReporte complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DatosReporte"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="IdEmisor" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="IdReceptor" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="Estado" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="FechaHoraRecepcion" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatosReporte", propOrder = {

})
public class DatosReporte {

    @XmlElement(name = "IdEmisor")
    protected long idEmisor;
    @XmlElement(name = "IdReceptor")
    protected long idReceptor;
    @XmlElement(name = "Estado", required = true)
    protected String estado;
    @XmlElement(name = "FechaHoraRecepcion", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar fechaHoraRecepcion;

    /**
     * Gets the value of the idEmisor property.
     * 
     */
    public long getIdEmisor() {
        return idEmisor;
    }

    /**
     * Sets the value of the idEmisor property.
     * 
     */
    public void setIdEmisor(long value) {
        this.idEmisor = value;
    }

    /**
     * Gets the value of the idReceptor property.
     * 
     */
    public long getIdReceptor() {
        return idReceptor;
    }

    /**
     * Sets the value of the idReceptor property.
     * 
     */
    public void setIdReceptor(long value) {
        this.idReceptor = value;
    }

    /**
     * Gets the value of the estado property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Sets the value of the estado property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEstado(String value) {
        this.estado = value;
    }

    /**
     * Gets the value of the fechaHoraRecepcion property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFechaHoraRecepcion() {
        return fechaHoraRecepcion;
    }

    /**
     * Sets the value of the fechaHoraRecepcion property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFechaHoraRecepcion(XMLGregorianCalendar value) {
        this.fechaHoraRecepcion = value;
    }

}

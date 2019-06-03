
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for DatosSobre complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DatosSobre"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="EstadoSobre" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="FechaHoraSobre" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="IdEmisor" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="IdReceptor" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="ParamConsulta" type="{http://dgi.gub.uy}eFacParamConsulta"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatosSobre", propOrder = {

})
public class DatosSobre {

    @XmlElement(name = "EstadoSobre", required = true)
    protected String estadoSobre;
    @XmlElement(name = "FechaHoraSobre", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar fechaHoraSobre;
    @XmlElement(name = "IdEmisor")
    protected long idEmisor;
    @XmlElement(name = "IdReceptor")
    protected long idReceptor;
    @XmlElement(name = "ParamConsulta", required = true)
    protected EFacParamConsulta paramConsulta;

    /**
     * Gets the value of the estadoSobre property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEstadoSobre() {
        return estadoSobre;
    }

    /**
     * Sets the value of the estadoSobre property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEstadoSobre(String value) {
        this.estadoSobre = value;
    }

    /**
     * Gets the value of the fechaHoraSobre property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFechaHoraSobre() {
        return fechaHoraSobre;
    }

    /**
     * Sets the value of the fechaHoraSobre property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFechaHoraSobre(XMLGregorianCalendar value) {
        this.fechaHoraSobre = value;
    }

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
     * Gets the value of the paramConsulta property.
     * 
     * @return
     *     possible object is
     *     {@link EFacParamConsulta }
     *     
     */
    public EFacParamConsulta getParamConsulta() {
        return paramConsulta;
    }

    /**
     * Sets the value of the paramConsulta property.
     * 
     * @param value
     *     allowed object is
     *     {@link EFacParamConsulta }
     *     
     */
    public void setParamConsulta(EFacParamConsulta value) {
        this.paramConsulta = value;
    }

}

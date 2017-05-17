
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for RucEmisoresMail.RucEmisoresMailItem complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RucEmisoresMail.RucEmisoresMailItem"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="RUC" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="DENOMINACION" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="NOMBRE_FANTASIA" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="FECHA_INICIO" type="{http://www.w3.org/2001/XMLSchema}date"/&gt;
 *         &lt;element name="FECHA_FIN" type="{http://www.w3.org/2001/XMLSchema}date"/&gt;
 *         &lt;element name="MAIL" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RucEmisoresMail.RucEmisoresMailItem", propOrder = {

})
public class RucEmisoresMailRucEmisoresMailItem {

    @XmlElement(name = "RUC", required = true)
    protected String ruc;
    @XmlElement(name = "DENOMINACION", required = true)
    protected String denominacion;
    @XmlElement(name = "NOMBRE_FANTASIA", required = true)
    protected String nombrefantasia;
    @XmlElement(name = "FECHA_INICIO", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar fechainicio;
    @XmlElement(name = "FECHA_FIN", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar fechafin;
    @XmlElement(name = "MAIL", required = true)
    protected String mail;

    /**
     * Gets the value of the ruc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRUC() {
        return ruc;
    }

    /**
     * Sets the value of the ruc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRUC(String value) {
        this.ruc = value;
    }

    /**
     * Gets the value of the denominacion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDENOMINACION() {
        return denominacion;
    }

    /**
     * Sets the value of the denominacion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDENOMINACION(String value) {
        this.denominacion = value;
    }

    /**
     * Gets the value of the nombrefantasia property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNOMBREFANTASIA() {
        return nombrefantasia;
    }

    /**
     * Sets the value of the nombrefantasia property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNOMBREFANTASIA(String value) {
        this.nombrefantasia = value;
    }

    /**
     * Gets the value of the fechainicio property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFECHAINICIO() {
        return fechainicio;
    }

    /**
     * Sets the value of the fechainicio property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFECHAINICIO(XMLGregorianCalendar value) {
        this.fechainicio = value;
    }

    /**
     * Gets the value of the fechafin property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFECHAFIN() {
        return fechafin;
    }

    /**
     * Sets the value of the fechafin property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFECHAFIN(XMLGregorianCalendar value) {
        this.fechafin = value;
    }

    /**
     * Gets the value of the mail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMAIL() {
        return mail;
    }

    /**
     * Sets the value of the mail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMAIL(String value) {
        this.mail = value;
    }

}


package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for DatosCFE complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DatosCFE"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="TipoCFE" type="{http://www.w3.org/2001/XMLSchema}short"/&gt;
 *         &lt;element name="Serie" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="Nro" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Estado" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="FechaHora" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="RucEmisor" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="Moneda" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="TotalNeto" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="TotalIVA" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="MontoTotal" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="TotalRetenido" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatosCFE", propOrder = {

})
public class DatosCFE {

    @XmlElement(name = "TipoCFE")
    protected short tipoCFE;
    @XmlElement(name = "Serie", required = true)
    protected String serie;
    @XmlElement(name = "Nro")
    protected int nro;
    @XmlElement(name = "Estado", required = true)
    protected String estado;
    @XmlElement(name = "FechaHora", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar fechaHora;
    @XmlElement(name = "RucEmisor", required = true)
    protected String rucEmisor;
    @XmlElement(name = "Moneda", required = true)
    protected String moneda;
    @XmlElement(name = "TotalNeto")
    protected double totalNeto;
    @XmlElement(name = "TotalIVA")
    protected double totalIVA;
    @XmlElement(name = "MontoTotal")
    protected double montoTotal;
    @XmlElement(name = "TotalRetenido")
    protected double totalRetenido;

    /**
     * Gets the value of the tipoCFE property.
     * 
     */
    public short getTipoCFE() {
        return tipoCFE;
    }

    /**
     * Sets the value of the tipoCFE property.
     * 
     */
    public void setTipoCFE(short value) {
        this.tipoCFE = value;
    }

    /**
     * Gets the value of the serie property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSerie() {
        return serie;
    }

    /**
     * Sets the value of the serie property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSerie(String value) {
        this.serie = value;
    }

    /**
     * Gets the value of the nro property.
     * 
     */
    public int getNro() {
        return nro;
    }

    /**
     * Sets the value of the nro property.
     * 
     */
    public void setNro(int value) {
        this.nro = value;
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
     * Gets the value of the fechaHora property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFechaHora() {
        return fechaHora;
    }

    /**
     * Sets the value of the fechaHora property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFechaHora(XMLGregorianCalendar value) {
        this.fechaHora = value;
    }

    /**
     * Gets the value of the rucEmisor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRucEmisor() {
        return rucEmisor;
    }

    /**
     * Sets the value of the rucEmisor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRucEmisor(String value) {
        this.rucEmisor = value;
    }

    /**
     * Gets the value of the moneda property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMoneda() {
        return moneda;
    }

    /**
     * Sets the value of the moneda property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMoneda(String value) {
        this.moneda = value;
    }

    /**
     * Gets the value of the totalNeto property.
     * 
     */
    public double getTotalNeto() {
        return totalNeto;
    }

    /**
     * Sets the value of the totalNeto property.
     * 
     */
    public void setTotalNeto(double value) {
        this.totalNeto = value;
    }

    /**
     * Gets the value of the totalIVA property.
     * 
     */
    public double getTotalIVA() {
        return totalIVA;
    }

    /**
     * Sets the value of the totalIVA property.
     * 
     */
    public void setTotalIVA(double value) {
        this.totalIVA = value;
    }

    /**
     * Gets the value of the montoTotal property.
     * 
     */
    public double getMontoTotal() {
        return montoTotal;
    }

    /**
     * Sets the value of the montoTotal property.
     * 
     */
    public void setMontoTotal(double value) {
        this.montoTotal = value;
    }

    /**
     * Gets the value of the totalRetenido property.
     * 
     */
    public double getTotalRetenido() {
        return totalRetenido;
    }

    /**
     * Sets the value of the totalRetenido property.
     * 
     */
    public void setTotalRetenido(double value) {
        this.totalRetenido = value;
    }

}

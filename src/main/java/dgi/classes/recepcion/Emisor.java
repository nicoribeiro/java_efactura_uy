//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.13 at 01:48:11 AM UYT 
//


package dgi.classes.recepcion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Emisor complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Emisor">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RUCEmisor" type="{http://cfe.dgi.gub.uy}RUCType"/>
 *         &lt;element name="RznSoc" type="{http://cfe.dgi.gub.uy}RznSocType"/>
 *         &lt;element name="NomComercial" type="{http://cfe.dgi.gub.uy}NomComercialType" minOccurs="0"/>
 *         &lt;element name="GiroEmis" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="60"/>
 *               &lt;minLength value="0"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="Telefono" maxOccurs="2" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://cfe.dgi.gub.uy}FonoType">
 *               &lt;maxLength value="20"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="CorreoEmisor" type="{http://cfe.dgi.gub.uy}MailType" minOccurs="0"/>
 *         &lt;element name="EmiSucursal" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="20"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="CdgDGISucur">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *               &lt;totalDigits value="4"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="DomFiscal">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="70"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="Ciudad" type="{http://cfe.dgi.gub.uy}CiudadType"/>
 *         &lt;element name="Departamento" type="{http://cfe.dgi.gub.uy}DeptoType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Emisor", propOrder = {
    "rucEmisor",
    "rznSoc",
    "nomComercial",
    "giroEmis",
    "telefono",
    "correoEmisor",
    "emiSucursal",
    "cdgDGISucur",
    "domFiscal",
    "ciudad",
    "departamento"
})
public class Emisor {

    @XmlElement(name = "RUCEmisor", required = true)
    protected String rucEmisor;
    @XmlElement(name = "RznSoc", required = true)
    protected String rznSoc;
    @XmlElement(name = "NomComercial")
    protected String nomComercial;
    @XmlElement(name = "GiroEmis")
    protected String giroEmis;
    @XmlElement(name = "Telefono")
    protected List<String> telefono;
    @XmlElement(name = "CorreoEmisor")
    protected String correoEmisor;
    @XmlElement(name = "EmiSucursal")
    protected String emiSucursal;
    @XmlElement(name = "CdgDGISucur", required = true)
    protected BigInteger cdgDGISucur;
    @XmlElement(name = "DomFiscal", required = true)
    protected String domFiscal;
    @XmlElement(name = "Ciudad", required = true)
    protected String ciudad;
    @XmlElement(name = "Departamento", required = true)
    protected String departamento;

    /**
     * Gets the value of the rucEmisor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRUCEmisor() {
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
    public void setRUCEmisor(String value) {
        this.rucEmisor = value;
    }

    /**
     * Gets the value of the rznSoc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRznSoc() {
        return rznSoc;
    }

    /**
     * Sets the value of the rznSoc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRznSoc(String value) {
        this.rznSoc = value;
    }

    /**
     * Gets the value of the nomComercial property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNomComercial() {
        return nomComercial;
    }

    /**
     * Sets the value of the nomComercial property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNomComercial(String value) {
        this.nomComercial = value;
    }

    /**
     * Gets the value of the giroEmis property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGiroEmis() {
        return giroEmis;
    }

    /**
     * Sets the value of the giroEmis property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGiroEmis(String value) {
        this.giroEmis = value;
    }

    /**
     * Gets the value of the telefono property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the telefono property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTelefono().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getTelefono() {
        if (telefono == null) {
            telefono = new ArrayList<String>();
        }
        return this.telefono;
    }

    /**
     * Gets the value of the correoEmisor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorreoEmisor() {
        return correoEmisor;
    }

    /**
     * Sets the value of the correoEmisor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorreoEmisor(String value) {
        this.correoEmisor = value;
    }

    /**
     * Gets the value of the emiSucursal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmiSucursal() {
        return emiSucursal;
    }

    /**
     * Sets the value of the emiSucursal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmiSucursal(String value) {
        this.emiSucursal = value;
    }

    /**
     * Gets the value of the cdgDGISucur property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCdgDGISucur() {
        return cdgDGISucur;
    }

    /**
     * Sets the value of the cdgDGISucur property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCdgDGISucur(BigInteger value) {
        this.cdgDGISucur = value;
    }

    /**
     * Gets the value of the domFiscal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDomFiscal() {
        return domFiscal;
    }

    /**
     * Sets the value of the domFiscal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDomFiscal(String value) {
        this.domFiscal = value;
    }

    /**
     * Gets the value of the ciudad property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCiudad() {
        return ciudad;
    }

    /**
     * Sets the value of the ciudad property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCiudad(String value) {
        this.ciudad = value;
    }

    /**
     * Gets the value of the departamento property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDepartamento() {
        return departamento;
    }

    /**
     * Sets the value of the departamento property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDepartamento(String value) {
        this.departamento = value;
    }

}

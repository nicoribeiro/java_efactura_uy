//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.06.01 at 06:36:07 PM UYT 
//


package dgi.classes.recepcion;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RetPerc_Resg complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RetPerc_Resg">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CodRet" type="{http://cfe.dgi.gub.uy}CodRetType"/>
 *         &lt;element name="Tasa" type="{http://cfe.dgi.gub.uy}PctType" minOccurs="0"/>
 *         &lt;element name="MntSujetoaRet" type="{http://cfe.dgi.gub.uy}MontoType"/>
 *         &lt;element name="ValRetPerc" type="{http://cfe.dgi.gub.uy}MontoType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetPerc_Resg", namespace = "http://cfe.dgi.gub.uy", propOrder = {
    "codRet",
    "tasa",
    "mntSujetoaRet",
    "valRetPerc"
})
public class RetPercResg {

    @XmlElement(name = "CodRet", required = true)
    protected String codRet;
    @XmlElement(name = "Tasa")
    protected BigDecimal tasa;
    @XmlElement(name = "MntSujetoaRet", required = true)
    protected BigDecimal mntSujetoaRet;
    @XmlElement(name = "ValRetPerc", required = true)
    protected BigDecimal valRetPerc;

    /**
     * Gets the value of the codRet property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodRet() {
        return codRet;
    }

    /**
     * Sets the value of the codRet property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodRet(String value) {
        this.codRet = value;
    }

    /**
     * Gets the value of the tasa property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTasa() {
        return tasa;
    }

    /**
     * Sets the value of the tasa property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTasa(BigDecimal value) {
        this.tasa = value;
    }

    /**
     * Gets the value of the mntSujetoaRet property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMntSujetoaRet() {
        return mntSujetoaRet;
    }

    /**
     * Sets the value of the mntSujetoaRet property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMntSujetoaRet(BigDecimal value) {
        this.mntSujetoaRet = value;
    }

    /**
     * Gets the value of the valRetPerc property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getValRetPerc() {
        return valRetPerc;
    }

    /**
     * Sets the value of the valRetPerc property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setValRetPerc(BigDecimal value) {
        this.valRetPerc = value;
    }

}

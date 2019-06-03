
package dgi.soap.consultas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ACKConsultaEstadoCFE complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ACKConsultaEstadoCFE"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="EstadoCFE" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
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
@XmlType(name = "ACKConsultaEstadoCFE", propOrder = {

})
public class ACKConsultaEstadoCFE {

    @XmlElement(name = "EstadoCFE", required = true)
    protected String estadoCFE;
    @XmlElement(name = "IdEmisor")
    protected long idEmisor;
    @XmlElement(name = "IdReceptor")
    protected long idReceptor;
    @XmlElement(name = "ParamConsulta", required = true)
    protected EFacParamConsulta paramConsulta;

    /**
     * Gets the value of the estadoCFE property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEstadoCFE() {
        return estadoCFE;
    }

    /**
     * Sets the value of the estadoCFE property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEstadoCFE(String value) {
        this.estadoCFE = value;
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

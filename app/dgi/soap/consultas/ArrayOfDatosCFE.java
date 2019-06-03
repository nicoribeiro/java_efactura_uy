
package dgi.soap.consultas;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfDatosCFE complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfDatosCFE"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DatosCFE" type="{http://dgi.gub.uy}DatosCFE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfDatosCFE", propOrder = {
    "datosCFE"
})
public class ArrayOfDatosCFE {

    @XmlElement(name = "DatosCFE")
    protected List<DatosCFE> datosCFE;

    /**
     * Gets the value of the datosCFE property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the datosCFE property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatosCFE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DatosCFE }
     * 
     * 
     */
    public List<DatosCFE> getDatosCFE() {
        if (datosCFE == null) {
            datosCFE = new ArrayList<DatosCFE>();
        }
        return this.datosCFE;
    }

}


package dgi.soap.consultas;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RucEmisoresMail complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RucEmisoresMail"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="RucEmisoresMail.RucEmisoresMailItem" type="{http://dgi.gub.uy}RucEmisoresMail.RucEmisoresMailItem" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RucEmisoresMail", propOrder = {
    "rucEmisoresMailRucEmisoresMailItem"
})
public class RucEmisoresMail {

    @XmlElement(name = "RucEmisoresMail.RucEmisoresMailItem")
    protected List<RucEmisoresMailRucEmisoresMailItem> rucEmisoresMailRucEmisoresMailItem;

    /**
     * Gets the value of the rucEmisoresMailRucEmisoresMailItem property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rucEmisoresMailRucEmisoresMailItem property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRucEmisoresMailRucEmisoresMailItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RucEmisoresMailRucEmisoresMailItem }
     * 
     * 
     */
    public List<RucEmisoresMailRucEmisoresMailItem> getRucEmisoresMailRucEmisoresMailItem() {
        if (rucEmisoresMailRucEmisoresMailItem == null) {
            rucEmisoresMailRucEmisoresMailItem = new ArrayList<RucEmisoresMailRucEmisoresMailItem>();
        }
        return this.rucEmisoresMailRucEmisoresMailItem;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.13 at 01:48:11 AM UYT 
//


package dgi.classes.recepcion;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EstadoACKCFEType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="EstadoACKCFEType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="2"/>
 *     &lt;enumeration value="AE"/>
 *     &lt;enumeration value="BE"/>
 *     &lt;enumeration value="CE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "EstadoACKCFEType")
@XmlEnum
public enum EstadoACKCFEType {

    AE,
    BE,
    CE;

    public String value() {
        return name();
    }

    public static EstadoACKCFEType fromValue(String v) {
        return valueOf(v);
    }

}

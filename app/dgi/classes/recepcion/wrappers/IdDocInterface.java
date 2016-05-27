package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

public interface IdDocInterface {

	/**
	 * Gets the value of the tipoCFE property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	BigInteger getTipoCFE();

	/**
	 * Gets the value of the serie property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	String getSerie();

	/**
	 * Gets the value of the nro property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	BigInteger getNro();

	/**
	 * Gets the value of the fchEmis property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link XMLGregorianCalendar }
	 *     
	 */
	XMLGregorianCalendar getFchEmis();

	/**
	 * Gets the value of the periodoDesde property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link XMLGregorianCalendar }
	 *     
	 */
	XMLGregorianCalendar getPeriodoDesde();

	/**
	 * Gets the value of the periodoHasta property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link XMLGregorianCalendar }
	 *     
	 */
	XMLGregorianCalendar getPeriodoHasta();

	/**
	 * Gets the value of the mntBruto property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	BigInteger getMntBruto();

	/**
	 * Gets the value of the fmaPago property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	BigInteger getFmaPago();

	/**
	 * Gets the value of the fchVenc property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link XMLGregorianCalendar }
	 *     
	 */
	XMLGregorianCalendar getFchVenc();

}
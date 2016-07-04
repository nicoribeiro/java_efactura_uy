package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.util.List;

import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.Totales;

public interface TotalesInterface {

	/**
	 * Gets the value of the tpoMoneda property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link TipMonType }
	 *     
	 */
	TipMonType getTpoMoneda();

	/**
	 * Sets the value of the tpoMoneda property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link TipMonType }
	 *     
	 */
	void setTpoMoneda(TipMonType value);

	/**
	 * Gets the value of the tpoCambio property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getTpoCambio();

	/**
	 * Sets the value of the tpoCambio property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setTpoCambio(BigDecimal value);

	/**
	 * Gets the value of the mntNoGrv property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntNoGrv();

	/**
	 * Sets the value of the mntNoGrv property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntNoGrv(BigDecimal value);

	/**
	 * Gets the value of the mntExpoyAsim property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntExpoyAsim();

	/**
	 * Sets the value of the mntExpoyAsim property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntExpoyAsim(BigDecimal value);

	/**
	 * Gets the value of the mntImpuestoPerc property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntImpuestoPerc();

	/**
	 * Sets the value of the mntImpuestoPerc property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntImpuestoPerc(BigDecimal value);

	/**
	 * Gets the value of the mntIVaenSusp property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntIVaenSusp();

	/**
	 * Sets the value of the mntIVaenSusp property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntIVaenSusp(BigDecimal value);

	/**
	 * Gets the value of the mntNetoIvaTasaMin property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntNetoIvaTasaMin();

	/**
	 * Sets the value of the mntNetoIvaTasaMin property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntNetoIvaTasaMin(BigDecimal value);

	/**
	 * Gets the value of the mntNetoIVATasaBasica property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntNetoIVATasaBasica();

	/**
	 * Sets the value of the mntNetoIVATasaBasica property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntNetoIVATasaBasica(BigDecimal value);

	/**
	 * Gets the value of the mntNetoIVAOtra property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntNetoIVAOtra();

	/**
	 * Sets the value of the mntNetoIVAOtra property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntNetoIVAOtra(BigDecimal value);

	/**
	 * Gets the value of the ivaTasaMin property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getIVATasaMin();

	/**
	 * Sets the value of the ivaTasaMin property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setIVATasaMin(BigDecimal value);

	/**
	 * Gets the value of the ivaTasaBasica property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getIVATasaBasica();

	/**
	 * Sets the value of the ivaTasaBasica property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setIVATasaBasica(BigDecimal value);

	/**
	 * Gets the value of the mntIVATasaMin property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntIVATasaMin();

	/**
	 * Sets the value of the mntIVATasaMin property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntIVATasaMin(BigDecimal value);

	/**
	 * Gets the value of the mntIVATasaBasica property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntIVATasaBasica();

	/**
	 * Sets the value of the mntIVATasaBasica property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntIVATasaBasica(BigDecimal value);

	/**
	 * Gets the value of the mntIVAOtra property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntIVAOtra();

	/**
	 * Sets the value of the mntIVAOtra property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntIVAOtra(BigDecimal value);

	/**
	 * Gets the value of the mntTotal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntTotal();

	/**
	 * Sets the value of the mntTotal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntTotal(BigDecimal value);

	/**
	 * Gets the value of the mntTotRetenido property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntTotRetenido();

	/**
	 * Sets the value of the mntTotRetenido property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntTotRetenido(BigDecimal value);

	/**
	 * Gets the value of the cantLinDet property.
	 * 
	 */
	int getCantLinDet();

	/**
	 * Sets the value of the cantLinDet property.
	 * 
	 */
	void setCantLinDet(int value);

	/**
	 * Gets the value of the retencPercep property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the retencPercep property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getRetencPercep().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link Totales2.RetencPercep }
	 * 
	 * 
	 */
	List<TotalesRetencPercepInterface> getRetencPerceps();

	/**
	 * Gets the value of the montoNF property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMontoNF();

	/**
	 * Sets the value of the montoNF property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMontoNF(BigDecimal value);

	/**
	 * Gets the value of the mntPagar property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMntPagar();

	/**
	 * Sets the value of the mntPagar property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMntPagar(BigDecimal value);

	
	void setRetencPercep(List<TotalesRetencPercepInterface> list);
}

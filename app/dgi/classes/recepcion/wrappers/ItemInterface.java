package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact.CodItem;
import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.RetPerc;

public interface ItemInterface {

	/**
	 * Gets the value of the nroLinDet property.
	 * 
	 */
	int getNroLinDet();

	/**
	 * Sets the value of the nroLinDet property.
	 * 
	 */
	void setNroLinDet(int value);

	/**
	 * Gets the value of the codItem property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the codItem property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getCodItem().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link ItemDetFact2.CodItem }
	 * 
	 * 
	 */
//	List<CodItem> getCodItems();

	/**
	 * Gets the value of the indFact property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	BigInteger getIndFact();

	/**
	 * Sets the value of the indFact property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigInteger }
	 *     
	 */
	void setIndFact(BigInteger value);

	/**
	 * Gets the value of the indAgenteResp property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	String getIndAgenteResp();

	/**
	 * Sets the value of the indAgenteResp property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	void setIndAgenteResp(String value);

	/**
	 * Gets the value of the nomItem property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	String getNomItem();

	/**
	 * Sets the value of the nomItem property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	void setNomItem(String value);

	/**
	 * Gets the value of the dscItem property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	String getDscItem();

	/**
	 * Sets the value of the dscItem property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	void setDscItem(String value);

	/**
	 * Gets the value of the cantidad property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getCantidad();

	/**
	 * Sets the value of the cantidad property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setCantidad(BigDecimal value);

	/**
	 * Gets the value of the uniMed property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	String getUniMed();

	/**
	 * Sets the value of the uniMed property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	void setUniMed(String value);

	/**
	 * Gets the value of the precioUnitario property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getPrecioUnitario();

	/**
	 * Sets the value of the precioUnitario property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setPrecioUnitario(BigDecimal value);

	/**
	 * Gets the value of the descuentoPct property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getDescuentoPct();

	/**
	 * Sets the value of the descuentoPct property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setDescuentoPct(BigDecimal value);

	/**
	 * Gets the value of the descuentoMonto property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getDescuentoMonto();

	/**
	 * Sets the value of the descuentoMonto property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setDescuentoMonto(BigDecimal value);

	/**
	 * Gets the value of the subDescuento property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the subDescuento property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getSubDescuento().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link ItemDetFact2.SubDescuento }
	 * 
	 * 
	 */
	List<SubDescuento> getSubDescuentos();

	/**
	 * Gets the value of the recargoPct property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getRecargoPct();

	/**
	 * Sets the value of the recargoPct property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setRecargoPct(BigDecimal value);

	/**
	 * Gets the value of the recargoMnt property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getRecargoMnt();

	/**
	 * Sets the value of the recargoMnt property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setRecargoMnt(BigDecimal value);

	/**
	 * Gets the value of the subRecargo property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the subRecargo property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getSubRecargo().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link ItemDetFact2.SubRecargo }
	 * 
	 * 
	 */
	List<SubRecargo> getSubRecargos();

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
	 * {@link RetPerc }
	 * 
	 * 
	 */
	List<RetPercInterface> getRetencPerceps();

	/**
	 * Gets the value of the montoItem property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigDecimal }
	 *     
	 */
	BigDecimal getMontoItem();

	/**
	 * Sets the value of the montoItem property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigDecimal }
	 *     
	 */
	void setMontoItem(BigDecimal value);


	List<CodItemInterface> getGenericCodItem();
	
	void addCodItem(String TpoCod, String Cod);
	
	void setRetencPerceps(List<RetPercInterface> list);

}
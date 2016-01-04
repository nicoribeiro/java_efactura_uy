package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;

import dgi.classes.recepcion.RetPercResg;

public interface RetPercInterface {

	/**
	 * Gets the value of the codRet property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getCodRet();

	/**
	 * Sets the value of the codRet property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setCodRet(String value);

	/**
	 * Gets the value of the tasa property.
	 * 
	 * @return possible object is {@link BigDecimal }
	 * 
	 */
	public BigDecimal getTasa();

	/**
	 * Sets the value of the tasa property.
	 * 
	 * @param value
	 *            allowed object is {@link BigDecimal }
	 * 
	 */
	public void setTasa(BigDecimal value);

	/**
	 * Gets the value of the mntSujetoaRet property.
	 * 
	 * @return possible object is {@link BigDecimal }
	 * 
	 */
	public BigDecimal getMntSujetoaRet();

	/**
	 * Sets the value of the mntSujetoaRet property.
	 * 
	 * @param value
	 *            allowed object is {@link BigDecimal }
	 * 
	 */
	public void setMntSujetoaRet(BigDecimal value);

	/**
	 * Gets the value of the valRetPerc property.
	 * 
	 * @return possible object is {@link BigDecimal }
	 * 
	 */
	public BigDecimal getValRetPerc();

	/**
	 * Sets the value of the valRetPerc property.
	 * 
	 * @param value
	 *            allowed object is {@link BigDecimal }
	 * 
	 */
	public void setValRetPerc(BigDecimal value);

	public Object getDelegate();

}

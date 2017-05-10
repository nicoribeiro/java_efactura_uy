package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;

public interface TotalesRetencPercepInterface {

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

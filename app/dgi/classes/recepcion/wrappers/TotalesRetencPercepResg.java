package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;

import dgi.classes.recepcion.TotalesResg.RetencPercep;

public class TotalesRetencPercepResg implements TotalesRetencPercepInterface {

	private RetencPercep delegate; 
	
	public TotalesRetencPercepResg(RetencPercep retPerc) {
		this.delegate = retPerc;
	}

	@Override
	public String getCodRet() {
		return delegate.getCodRet();
	}

	@Override
	public void setCodRet(String value) {
		delegate.setCodRet(value);

	}

	@Override
	public BigDecimal getValRetPerc() {
		return delegate.getValRetPerc();
	}

	@Override
	public void setValRetPerc(BigDecimal value) {
		delegate.setValRetPerc(value);
	}
	
	@Override
	public Object getDelegate() {
		return delegate;
	}

}

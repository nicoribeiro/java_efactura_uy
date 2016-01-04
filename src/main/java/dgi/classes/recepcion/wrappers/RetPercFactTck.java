package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;

import dgi.classes.recepcion.RetPerc;

public class RetPercFactTck implements RetPercInterface {

	private RetPerc delegate;
	
	public RetPercFactTck(RetPerc retencion) {
		this.delegate = retencion;
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
	public BigDecimal getTasa() {
		return delegate.getTasa();
	}

	@Override
	public void setTasa(BigDecimal value) {
		delegate.setTasa(value);
	}

	@Override
	public BigDecimal getMntSujetoaRet() {
		return delegate.getMntSujetoaRet();
	}

	@Override
	public void setMntSujetoaRet(BigDecimal value) {
		delegate.setMntSujetoaRet(value);
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

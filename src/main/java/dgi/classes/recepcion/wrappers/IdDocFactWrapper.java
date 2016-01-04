package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import dgi.classes.recepcion.IdDocFact;

public class IdDocFactWrapper implements IdDocInterface {

	private IdDocFact delegate;

	public IdDocFactWrapper(IdDocFact idDoc) {
		this.delegate = idDoc;
	}

	@Override
	public BigInteger getTipoCFE() {
		return delegate.getTipoCFE();
	}

	@Override
	public String getSerie() {
		return delegate.getSerie();
	}

	@Override
	public BigInteger getNro() {
		return delegate.getNro();
	}

	@Override
	public XMLGregorianCalendar getFchEmis() {
		return delegate.getFchEmis();
	}

	@Override
	public XMLGregorianCalendar getPeriodoDesde() {
		return delegate.getPeriodoDesde();
	}

	@Override
	public XMLGregorianCalendar getPeriodoHasta() {
		return delegate.getPeriodoHasta();
	}

	@Override
	public BigInteger getMntBruto() {
		return delegate.getMntBruto();
	}

	@Override
	public BigInteger getFmaPago() {
		return delegate.getFmaPago();
	}

	@Override
	public XMLGregorianCalendar getFchVenc() {
		return delegate.getFchVenc();
	}

}

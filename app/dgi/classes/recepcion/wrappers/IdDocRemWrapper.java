package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import dgi.classes.recepcion.IdDocRem;

public class IdDocRemWrapper implements IdDocInterface {

	private IdDocRem delegate;

	public IdDocRemWrapper(IdDocRem idDoc) {
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
		return null;
	}

	@Override
	public XMLGregorianCalendar getPeriodoHasta() {
		return null;
	}

	@Override
	public BigInteger getMntBruto() {
		return null;
	}

	@Override
	public BigInteger getFmaPago() {
		return null;
	}

	@Override
	public XMLGregorianCalendar getFchVenc() {
		return null;
	}

}

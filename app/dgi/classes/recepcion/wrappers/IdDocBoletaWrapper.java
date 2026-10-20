package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import dgi.classes.recepcion.IdDocBoleta;

/**
 * Wrapper del IdDoc de la e-Boleta de entrada (151-153, 251-253)
 */
public class IdDocBoletaWrapper implements IdDocInterface {

	private IdDocBoleta delegate;

	public IdDocBoletaWrapper(IdDocBoleta idDoc) {
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

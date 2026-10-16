package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import dgi.classes.recepcion.IdDocFactExp;

/**
 * Wrapper del IdDoc de la e-Factura de Exportacion (121-123, 221-223)
 */
public class IdDocFactExpWrapper implements IdDocInterface {

	private IdDocFactExp delegate;

	public IdDocFactExpWrapper(IdDocFactExp idDoc) {
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

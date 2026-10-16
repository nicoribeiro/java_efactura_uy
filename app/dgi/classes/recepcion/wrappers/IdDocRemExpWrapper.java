package dgi.classes.recepcion.wrappers;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import dgi.classes.recepcion.IdDocRemExp;

/**
 * Wrapper del IdDoc del e-Remito de Exportacion (124, 224): no tiene forma de pago, montos brutos ni periodo
 */
public class IdDocRemExpWrapper implements IdDocInterface {

	private IdDocRemExp delegate;

	public IdDocRemExpWrapper(IdDocRemExp idDoc) {
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

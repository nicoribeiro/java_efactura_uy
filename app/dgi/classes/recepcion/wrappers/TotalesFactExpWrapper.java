package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.util.List;

import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.TotalesFactExp;

/**
 * Wrapper de los totales de la e-Factura de Exportacion. La exportacion no lleva IVA ni retenciones: esos campos no
 * existen en Totales_Fact_Exp, los setters no hacen nada y los getters devuelven cero (los totales los arma
 * CFEBuilderExportacion)
 */
public class TotalesFactExpWrapper implements TotalesInterface {

	private TotalesFactExp delegate;

	public TotalesFactExpWrapper(TotalesFactExp totales) {
		this.delegate = totales;
	}

	@Override
	public TipMonType getTpoMoneda() {
		return delegate.getTpoMoneda();
	}

	@Override
	public void setTpoMoneda(TipMonType value) {
		delegate.setTpoMoneda(value);
	}

	@Override
	public BigDecimal getTpoCambio() {
		return delegate.getTpoCambio();
	}

	@Override
	public void setTpoCambio(BigDecimal value) {
		delegate.setTpoCambio(value);
	}

	@Override
	public BigDecimal getMntNoGrv() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntNoGrv(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntExpoyAsim() {
		return delegate.getMntExpoyAsim();
	}

	@Override
	public void setMntExpoyAsim(BigDecimal value) {
		delegate.setMntExpoyAsim(value);
	}

	@Override
	public BigDecimal getMntImpuestoPerc() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntImpuestoPerc(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVaenSusp() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntIVaenSusp(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVATasaMin() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntNetoIvaTasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVATasaBasica() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntNetoIVATasaBasica(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVAOtra() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntNetoIVAOtra(BigDecimal value) {
	}

	@Override
	public BigDecimal getIVATasaMin() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setIVATasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getIVATasaBasica() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setIVATasaBasica(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVATasaMin() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntIVATasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVATasaBasica() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntIVATasaBasica(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVAOtra() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntIVAOtra(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntTotal() {
		return delegate.getMntTotal();
	}

	@Override
	public void setMntTotal(BigDecimal value) {
		delegate.setMntTotal(value);
	}

	@Override
	public BigDecimal getMntTotRetenido() {
		return BigDecimal.ZERO;
	}

	@Override
	public void setMntTotRetenido(BigDecimal value) {
	}

	@Override
	public int getCantLinDet() {
		return delegate.getCantLinDet();
	}

	@Override
	public void setCantLinDet(int value) {
		delegate.setCantLinDet(value);
	}

	@Override
	public List<TotalesRetencPercepInterface> getRetencPerceps() {
		return null;
	}

	@Override
	public BigDecimal getMontoNF() {
		return delegate.getMontoNF();
	}

	@Override
	public void setMontoNF(BigDecimal value) {
		delegate.setMontoNF(value);
	}

	@Override
	public BigDecimal getMntPagar() {
		return delegate.getMntPagar();
	}

	@Override
	public void setMntPagar(BigDecimal value) {
		delegate.setMntPagar(value);
	}

	@Override
	public void setRetencPercep(List<TotalesRetencPercepInterface> list) {
	}

}

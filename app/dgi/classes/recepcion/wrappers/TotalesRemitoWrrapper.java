package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.util.List;

import dgi.classes.recepcion.CFEDefType.ERem.Encabezado.Totales;
import dgi.classes.recepcion.TipMonType;

public class TotalesRemitoWrrapper implements TotalesInterface {

	private Totales delegate;

	public TotalesRemitoWrrapper(Totales totales) {
		this.delegate = totales;
	}

	@Override
	public TipMonType getTpoMoneda() {
		return null;
	}

	@Override
	public void setTpoMoneda(TipMonType value) {
	}

	@Override
	public BigDecimal getTpoCambio() {
		return null;
	}

	@Override
	public void setTpoCambio(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNoGrv() {
		return null;
	}

	@Override
	public void setMntNoGrv(BigDecimal value) {

	}

	@Override
	public BigDecimal getMntExpoyAsim() {
		return null;
	}

	@Override
	public void setMntExpoyAsim(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntImpuestoPerc() {
		return null;
	}

	@Override
	public void setMntImpuestoPerc(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVaenSusp() {
		return null;
	}

	@Override
	public void setMntIVaenSusp(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVATasaMin() {
		return null;
	}

	@Override
	public void setMntNetoIvaTasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVATasaBasica() {
		return null;
	}

	@Override
	public void setMntNetoIVATasaBasica(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntNetoIVAOtra() {
		return null;
	}

	@Override
	public void setMntNetoIVAOtra(BigDecimal value) {
	}

	@Override
	public BigDecimal getIVATasaMin() {
		return null;
	}

	@Override
	public void setIVATasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getIVATasaBasica() {
		return null;
	}

	@Override
	public void setIVATasaBasica(BigDecimal value) {

	}

	@Override
	public BigDecimal getMntIVATasaMin() {
		return null;
	}

	@Override
	public void setMntIVATasaMin(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVATasaBasica() {
		return null;
	}

	@Override
	public void setMntIVATasaBasica(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntIVAOtra() {
		return null;
	}

	@Override
	public void setMntIVAOtra(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntTotal() {
		return null;
	}

	@Override
	public void setMntTotal(BigDecimal value) {

	}

	@Override
	public BigDecimal getMntTotRetenido() {
		return null;
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
		return null;
	}

	@Override
	public void setMontoNF(BigDecimal value) {
	}

	@Override
	public BigDecimal getMntPagar() {
		return null;
	}

	@Override
	public void setMntPagar(BigDecimal value) {
	}

	@Override
	public void setRetencPercep(List<TotalesRetencPercepInterface> list) {
	}

}

package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.Totales;
import dgi.classes.recepcion.Totales.RetencPercep;

public class TotalesFactTickWrapper implements TotalesInterface {

	private Totales delegate;

	public TotalesFactTickWrapper(Totales totales) {
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
		return delegate.getMntNoGrv();
	}

	@Override
	public void setMntNoGrv(BigDecimal value) {
		delegate.setMntNoGrv(value);

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
		return delegate.getMntImpuestoPerc();
	}

	@Override
	public void setMntImpuestoPerc(BigDecimal value) {
		delegate.setMntImpuestoPerc(value);
	}

	@Override
	public BigDecimal getMntIVaenSusp() {
		return delegate.getMntIVaenSusp();
	}

	@Override
	public void setMntIVaenSusp(BigDecimal value) {
		delegate.setMntIVaenSusp(value);
	}

	@Override
	public BigDecimal getMntNetoIvaTasaMin() {
		return delegate.getMntNetoIvaTasaMin();
	}

	@Override
	public void setMntNetoIvaTasaMin(BigDecimal value) {
		delegate.setMntNetoIvaTasaMin(value);
	}

	@Override
	public BigDecimal getMntNetoIVATasaBasica() {
		return delegate.getMntNetoIVATasaBasica();
	}

	@Override
	public void setMntNetoIVATasaBasica(BigDecimal value) {
		delegate.setMntNetoIVATasaBasica(value);
	}

	@Override
	public BigDecimal getMntNetoIVAOtra() {
		return delegate.getMntNetoIVAOtra();
	}

	@Override
	public void setMntNetoIVAOtra(BigDecimal value) {
		delegate.setMntNetoIVAOtra(value);
	}

	@Override
	public BigDecimal getIVATasaMin() {
		return delegate.getIVATasaMin();
	}

	@Override
	public void setIVATasaMin(BigDecimal value) {
		delegate.setIVATasaMin(value);
	}

	@Override
	public BigDecimal getIVATasaBasica() {
		return delegate.getIVATasaBasica();
	}

	@Override
	public void setIVATasaBasica(BigDecimal value) {
		delegate.setIVATasaBasica(value);

	}

	@Override
	public BigDecimal getMntIVATasaMin() {
		return delegate.getMntIVATasaMin();
	}

	@Override
	public void setMntIVATasaMin(BigDecimal value) {
		delegate.setMntIVATasaMin(value);
	}

	@Override
	public BigDecimal getMntIVATasaBasica() {
		return delegate.getMntIVATasaBasica();
	}

	@Override
	public void setMntIVATasaBasica(BigDecimal value) {
		delegate.setMntIVATasaBasica(value);
	}

	@Override
	public BigDecimal getMntIVAOtra() {
		return delegate.getMntIVAOtra();
	}

	@Override
	public void setMntIVAOtra(BigDecimal value) {
		delegate.setMntIVAOtra(value);
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
		return delegate.getMntTotRetenido();
	}

	@Override
	public void setMntTotRetenido(BigDecimal value) {
		delegate.setMntTotRetenido(value);
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
		ArrayList<TotalesRetencPercepInterface> list = new ArrayList<TotalesRetencPercepInterface>();
		for (Iterator<RetencPercep> iterator = delegate.getRetencPerceps().iterator(); iterator.hasNext();) {
			RetencPercep retPerc = iterator.next();
			list.add(new TotalesRetencPercepFactTck(retPerc));
		}
		
		return list;
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
		for (Iterator<TotalesRetencPercepInterface> iterator = list.iterator(); iterator.hasNext();) {
			TotalesRetencPercepInterface retencion = iterator.next();
			delegate.getRetencPerceps().add((RetencPercep) retencion.getDelegate());
		}
	}

}

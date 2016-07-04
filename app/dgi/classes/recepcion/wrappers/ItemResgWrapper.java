package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact.CodItem;
import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.ItemResg;
import dgi.classes.recepcion.RetPerc;
import dgi.classes.recepcion.RetPercResg;

public class ItemResgWrapper implements ItemInterface {

	private ItemResg delegate;

	public ItemResgWrapper(ItemResg item) {
		this.delegate = item;
	}

	@Override
	public List<CodItemInterface> getGenericCodItem() {
		return new ArrayList<CodItemInterface>();
	}

	@Override
	public int getNroLinDet() {
		return delegate.getNroLinDet();
	}

	@Override
	public BigInteger getIndFact() {
		return delegate.getIndFact();
	}

	@Override
	public String getIndAgenteResp() {
		return null;
	}

	@Override
	public String getNomItem() {
		return null;
	}

	@Override
	public String getDscItem() {
		return null;
	}

	@Override
	public BigDecimal getCantidad() {
		return null;
	}

	@Override
	public String getUniMed() {
		return null;
	}

	@Override
	public BigDecimal getPrecioUnitario() {
		return null;
	}

	@Override
	public BigDecimal getDescuentoPct() {
		return null;
	}

	@Override
	public BigDecimal getDescuentoMonto() {
		return null;
	}

	@Override
	public BigDecimal getRecargoPct() {
		return null;
	}

	@Override
	public BigDecimal getRecargoMnt() {
		return null;
	}

	@Override
	public List<RetPercInterface> getRetencPerceps() {
		ArrayList<RetPercInterface> list = new ArrayList<RetPercInterface>();
		for (Iterator<RetPercResg> iterator = delegate.getRetencPerceps().iterator(); iterator.hasNext();) {
			RetPercResg retPerc = iterator.next();
			list.add(new RetPercResgWrapper(retPerc));
		}
		
		return list;
	}

	@Override
	public BigDecimal getMontoItem() {
		return null;
	}

	@Override
	public void setNroLinDet(int value) {
		delegate.setNroLinDet(value);

	}

	@Override
	public List<CodItem> getCodItems() {
		return null;
	}

	@Override
	public void setIndFact(BigInteger value) {
		delegate.setIndFact(value);

	}

	@Override
	public void setIndAgenteResp(String value) {
	}

	@Override
	public void setNomItem(String value) {
	}

	@Override
	public void setDscItem(String value) {
	}

	@Override
	public void setCantidad(BigDecimal value) {
	}

	@Override
	public void setUniMed(String value) {
	}

	@Override
	public void setPrecioUnitario(BigDecimal value) {
	}

	@Override
	public void setDescuentoPct(BigDecimal value) {
	}

	@Override
	public void setDescuentoMonto(BigDecimal value) {
	}

	@Override
	public List<SubDescuento> getSubDescuentos() {
		return null;
	}

	@Override
	public void setRecargoPct(BigDecimal value) {
	}

	@Override
	public void setRecargoMnt(BigDecimal value) {
	}

	@Override
	public List<SubRecargo> getSubRecargos() {
		return null;
	}

	@Override
	public void setMontoItem(BigDecimal value) {
	}

	@Override
	public void setRetencPerceps(List<RetPercInterface> list) {
		for (Iterator<RetPercInterface> iterator = list.iterator(); iterator.hasNext();) {
			RetPercInterface retencion = iterator.next();
			delegate.getRetencPerceps().add((RetPercResg) retencion.getDelegate());
		}
	}

}

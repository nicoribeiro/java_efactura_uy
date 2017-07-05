package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.ItemRem;
import dgi.classes.recepcion.ItemRem.CodItem;
import dgi.classes.recepcion.RetPerc;

public class ItemRemWrapper implements ItemInterface {

	private ItemRem delegate;
	
	private ArrayList<CodItemInterface> codItems;

	public ItemRemWrapper(ItemRem item) {
		this.delegate = item;
	}

	@Override
	public List<CodItemInterface> getGenericCodItem() {
		if (codItems == null) {
			codItems = new ArrayList<CodItemInterface>();
			if (delegate.getCodItems() != null) {
				for (CodItem codItem : delegate.getCodItems()) {
					codItems.add(new ItemRemCodItemWrapper(codItem));
				}
			}

		}
		return this.codItems;
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
		return delegate.getNomItem();
	}

	@Override
	public String getDscItem() {
		return delegate.getDscItem();
	}

	@Override
	public BigDecimal getCantidad() {
		return delegate.getCantidad();
	}

	@Override
	public String getUniMed() {
		return delegate.getUniMed();
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
		return null;
	}

	@Override
	public BigDecimal getMontoItem() {
		return null;
	}

	@Override
	public void setNroLinDet(int value) {
		delegate.setNroLinDet(value);

	}

//	@Override
//	public List<CodItemInterface> getCodItems() {
//		return delegate.getCodItems();
//	}

	@Override
	public void setIndFact(BigInteger value) {
		delegate.setIndFact(value);

	}

	@Override
	public void setIndAgenteResp(String value) {
	}

	@Override
	public void setNomItem(String value) {
		delegate.setNomItem(value);
	}

	@Override
	public void setDscItem(String value) {
		delegate.setDscItem(value);
	}

	@Override
	public void setCantidad(BigDecimal value) {
		delegate.setCantidad(value);
	}

	@Override
	public void setUniMed(String value) {
		delegate.setUniMed(value);
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
	}

	@Override
	public void addCodItem(String tpoCod, String cod) {
		CodItem codItem = new CodItem();
		codItem.setCod(cod);
		codItem.setTpoCod(tpoCod);
		delegate.getCodItems().add(codItem);
		
	}

}

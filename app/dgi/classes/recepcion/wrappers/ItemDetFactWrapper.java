package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ItemDetFact.CodItem;
import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.RetPerc;

/**
 * Wrapper for the ItemDetFact class
 * 
 * This class is used for PDF generation
 * 
 * @author nicolasribeiro
 *
 */
public class ItemDetFactWrapper implements ItemInterface {

	private ItemDetFact delegate;

	private ArrayList<CodItemInterface> codItems;

	public ItemDetFactWrapper(ItemDetFact itemDetFact) {
		this.delegate = itemDetFact;
	}

	@Override
	public List<CodItemInterface> getGenericCodItem() {
		if (codItems == null) {
			codItems = new ArrayList<CodItemInterface>();
			if (delegate.getCodItems() != null) {
				for (CodItem codItem : delegate.getCodItems()) {
					codItems.add(new ItemDetFactCodItemWrapper(codItem));
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
		return delegate.getIndAgenteResp();
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
		return delegate.getPrecioUnitario();
	}

	@Override
	public BigDecimal getDescuentoPct() {
		return delegate.getDescuentoMonto();
	}

	@Override
	public BigDecimal getDescuentoMonto() {
		return delegate.getDescuentoMonto();
	}

	@Override
	public BigDecimal getRecargoPct() {
		return delegate.getRecargoPct();
	}

	@Override
	public BigDecimal getRecargoMnt() {
		return delegate.getRecargoMnt();
	}

	@Override
	public List<RetPercInterface> getRetencPerceps() {
		ArrayList<RetPercInterface> list = new ArrayList<RetPercInterface>();
		for (Iterator<RetPerc> iterator = delegate.getRetencPerceps().iterator(); iterator.hasNext();) {
			RetPerc retPerc = iterator.next();
			list.add(new RetPercFactTck(retPerc));
		}
		
		return list;
	}

	@Override
	public BigDecimal getMontoItem() {
		return delegate.getMontoItem();
	}

	@Override
	public void setNroLinDet(int value) {
		delegate.setNroLinDet(value);

	}

//	@Override
//	public List<CodItem> getCodItems() {
//		return delegate.getCodItems();
//	}

	@Override
	public void setIndFact(BigInteger value) {
		delegate.setIndFact(value);

	}

	@Override
	public void setIndAgenteResp(String value) {
		delegate.setIndAgenteResp(value);
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
		delegate.setPrecioUnitario(value);
	}

	@Override
	public void setDescuentoPct(BigDecimal value) {
		delegate.setDescuentoPct(value);
	}

	@Override
	public void setDescuentoMonto(BigDecimal value) {
		delegate.setDescuentoMonto(value);
	}

	@Override
	public List<SubDescuento> getSubDescuentos() {
		return delegate.getSubDescuentos();
	}

	@Override
	public void setRecargoPct(BigDecimal value) {
		delegate.setRecargoPct(value);
	}

	@Override
	public void setRecargoMnt(BigDecimal value) {
		delegate.setRecargoMnt(value);
	}

	@Override
	public List<SubRecargo> getSubRecargos() {
		return delegate.getSubRecargos();
	}

	@Override
	public void setMontoItem(BigDecimal value) {
		delegate.setMontoItem(value);
	}

	@Override
	public void setRetencPerceps(List<RetPercInterface> list) {
		for (Iterator<RetPercInterface> iterator = list.iterator(); iterator.hasNext();) {
			RetPercInterface retencion = iterator.next();
			delegate.getRetencPerceps().add((RetPerc) retencion.getDelegate());
		}
	}

	@Override
	public void addCodItem(String tpoCod, String cod) {
		CodItem codItem = new CodItem();
		codItem.setCod(cod);
		codItem.setTpoCod(tpoCod);
		delegate.getCodItems().add(codItem);
		
	}

}

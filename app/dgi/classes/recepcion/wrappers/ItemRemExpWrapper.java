package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.ItemRemExp;
import dgi.classes.recepcion.ItemRemExp.CodItem;

/**
 * Wrapper del item del e-Remito de Exportacion (Item_Rem_Exp): tiene precio unitario y monto pero no descuentos,
 * recargos, indicador de agente responsable ni retenciones/percepciones (esos setters no hacen nada y los getters
 * devuelven null)
 */
public class ItemRemExpWrapper implements ItemInterface {

	private ItemRemExp delegate;

	private ArrayList<CodItemInterface> codItems;

	public ItemRemExpWrapper(ItemRemExp item) {
		this.delegate = item;
	}

	public ItemRemExp getDelegate() {
		return delegate;
	}

	@Override
	public List<CodItemInterface> getGenericCodItem() {
		if (codItems == null) {
			codItems = new ArrayList<CodItemInterface>();
			if (delegate.getCodItems() != null) {
				for (CodItem codItem : delegate.getCodItems()) {
					codItems.add(new ItemRemExpCodItemWrapper(codItem));
				}
			}
		}
		return this.codItems;
	}

	@Override
	public void addCodItem(String tpoCod, String cod) {
		CodItem codItem = new CodItem();
		codItem.setCod(cod);
		codItem.setTpoCod(tpoCod);
		delegate.getCodItems().add(codItem);
	}

	@Override
	public int getNroLinDet() {
		return delegate.getNroLinDet();
	}

	@Override
	public void setNroLinDet(int value) {
		delegate.setNroLinDet(value);
	}

	@Override
	public BigInteger getIndFact() {
		return delegate.getIndFact();
	}

	@Override
	public void setIndFact(BigInteger value) {
		delegate.setIndFact(value);
	}

	@Override
	public String getIndAgenteResp() {
		return null;
	}

	@Override
	public void setIndAgenteResp(String value) {
	}

	@Override
	public String getNomItem() {
		return delegate.getNomItem();
	}

	@Override
	public void setNomItem(String value) {
		delegate.setNomItem(value);
	}

	@Override
	public String getDscItem() {
		return delegate.getDscItem();
	}

	@Override
	public void setDscItem(String value) {
		delegate.setDscItem(value);
	}

	@Override
	public BigDecimal getCantidad() {
		return delegate.getCantidad();
	}

	@Override
	public void setCantidad(BigDecimal value) {
		delegate.setCantidad(value);
	}

	@Override
	public String getUniMed() {
		return delegate.getUniMed();
	}

	@Override
	public void setUniMed(String value) {
		delegate.setUniMed(value);
	}

	@Override
	public BigDecimal getPrecioUnitario() {
		return delegate.getPrecioUnitario();
	}

	@Override
	public void setPrecioUnitario(BigDecimal value) {
		delegate.setPrecioUnitario(value);
	}

	@Override
	public BigDecimal getDescuentoPct() {
		return null;
	}

	@Override
	public void setDescuentoPct(BigDecimal value) {
	}

	@Override
	public BigDecimal getDescuentoMonto() {
		return null;
	}

	@Override
	public void setDescuentoMonto(BigDecimal value) {
	}

	@Override
	public List<SubDescuento> getSubDescuentos() {
		return null;
	}

	@Override
	public BigDecimal getRecargoPct() {
		return null;
	}

	@Override
	public void setRecargoPct(BigDecimal value) {
	}

	@Override
	public BigDecimal getRecargoMnt() {
		return null;
	}

	@Override
	public void setRecargoMnt(BigDecimal value) {
	}

	@Override
	public List<SubRecargo> getSubRecargos() {
		return null;
	}

	@Override
	public List<RetPercInterface> getRetencPerceps() {
		return null;
	}

	@Override
	public void setRetencPerceps(List<RetPercInterface> list) {
	}

	@Override
	public BigDecimal getMontoItem() {
		return delegate.getMontoItem();
	}

	@Override
	public void setMontoItem(BigDecimal value) {
		delegate.setMontoItem(value);
	}

}

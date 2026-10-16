package dgi.classes.recepcion.wrappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import dgi.classes.recepcion.ItemDetFact.SubDescuento;
import dgi.classes.recepcion.ItemDetFact.SubRecargo;
import dgi.classes.recepcion.ItemDetFactExp;
import dgi.classes.recepcion.ItemDetFactExp.CodItem;

/**
 * Wrapper del item de la e-Factura de Exportacion. No tiene indicador de agente responsable ni retenciones/percepciones;
 * los subdescuentos y subrecargos son de otro tipo de JAXB y no se exponen (como en ItemRemWrapper)
 */
public class ItemDetFactExpWrapper implements ItemInterface {

	private ItemDetFactExp delegate;

	private ArrayList<CodItemInterface> codItems;

	public ItemDetFactExpWrapper(ItemDetFactExp item) {
		this.delegate = item;
	}

	public ItemDetFactExp getDelegate() {
		return delegate;
	}

	@Override
	public List<CodItemInterface> getGenericCodItem() {
		if (codItems == null) {
			codItems = new ArrayList<CodItemInterface>();
			if (delegate.getCodItems() != null) {
				for (CodItem codItem : delegate.getCodItems()) {
					codItems.add(new ItemDetFactExpCodItemWrapper(codItem));
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
		return delegate.getDescuentoPct();
	}

	@Override
	public void setDescuentoPct(BigDecimal value) {
		delegate.setDescuentoPct(value);
	}

	@Override
	public BigDecimal getDescuentoMonto() {
		return delegate.getDescuentoMonto();
	}

	@Override
	public void setDescuentoMonto(BigDecimal value) {
		delegate.setDescuentoMonto(value);
	}

	@Override
	public List<SubDescuento> getSubDescuentos() {
		return null;
	}

	@Override
	public BigDecimal getRecargoPct() {
		return delegate.getRecargoPct();
	}

	@Override
	public void setRecargoPct(BigDecimal value) {
		delegate.setRecargoPct(value);
	}

	@Override
	public BigDecimal getRecargoMnt() {
		return delegate.getRecargoMnt();
	}

	@Override
	public void setRecargoMnt(BigDecimal value) {
		delegate.setRecargoMnt(value);
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

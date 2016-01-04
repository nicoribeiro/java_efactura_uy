package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ItemDetFact.CodItem;

public class ItemDetFactCodItemWrapper implements CodItemInterface {

	private CodItem delegate;
	
	public ItemDetFactCodItemWrapper(CodItem codItem) {
		this.delegate = codItem;
	}

	@Override
	public String getTpoCod() {
		return delegate.getTpoCod();
	}

	@Override
	public String getCod() {
		return delegate.getCod();
	}

}

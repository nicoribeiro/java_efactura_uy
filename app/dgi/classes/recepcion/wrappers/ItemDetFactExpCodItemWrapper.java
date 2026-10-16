package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ItemDetFactExp.CodItem;

public class ItemDetFactExpCodItemWrapper implements CodItemInterface {

	private CodItem delegate;

	public ItemDetFactExpCodItemWrapper(CodItem codItem) {
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

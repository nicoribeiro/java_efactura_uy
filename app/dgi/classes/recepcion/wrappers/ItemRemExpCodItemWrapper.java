package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ItemRemExp.CodItem;

public class ItemRemExpCodItemWrapper implements CodItemInterface {

	private CodItem delegate;

	public ItemRemExpCodItemWrapper(CodItem codItem) {
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

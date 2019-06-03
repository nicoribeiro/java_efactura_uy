package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ItemRem.CodItem;

public class ItemRemCodItemWrapper implements CodItemInterface {

	private CodItem delegate;
	
	public ItemRemCodItemWrapper(CodItem codItem) {
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

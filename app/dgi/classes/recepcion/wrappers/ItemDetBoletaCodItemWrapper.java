package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ItemDetBoleta.CodItem;

public class ItemDetBoletaCodItemWrapper implements CodItemInterface {

	private CodItem delegate;

	public ItemDetBoletaCodItemWrapper(CodItem codItem) {
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

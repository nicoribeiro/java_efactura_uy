package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ReceptorFact;

public class ReceptorFactWrapper implements ReceptorInterface {

	private ReceptorFact delegate;

	public ReceptorFactWrapper(ReceptorFact receptor) {
		this.delegate = receptor;
	}

	@Override
	public int getTipoDocRecep() {
		return delegate.getTipoDocRecep();
	}

	@Override
	public String getCodPaisRecep() {
		return delegate.getCodPaisRecep();
	}

	@Override
	public String getDocRecep() {
		return delegate.getDocRecep();
	}

	@Override
	public String getRznSocRecep() {
		return delegate.getRznSocRecep();
	}

	@Override
	public String getDirRecep() {
		return delegate.getDirRecep();
	}

	@Override
	public String getCiudadRecep() {
		return delegate.getCiudadRecep();
	}

	@Override
	public String getDeptoRecep() {
		return delegate.getDeptoRecep();
	}

	@Override
	public String getPaisRecep() {
		return delegate.getPaisRecep();
	}

	@Override
	public Integer getCP() {
		return delegate.getCP();
	}

	@Override
	public String getInfoAdicional() {
		return delegate.getInfoAdicional();
	}

	@Override
	public String getLugarDestEnt() {
		return delegate.getLugarDestEnt();
	}

	@Override
	public String getCompraID() {
		return delegate.getCompraID();
	}

	@Override
	public void setTipoDocRecep(int value) {
		delegate.setTipoDocRecep(value);

	}

	@Override
	public void setCodPaisRecep(String value) {
		delegate.setCodPaisRecep(value);
	}

	@Override
	public void setDocRecep(String value) {
		delegate.setDocRecep(value);
	}

	@Override
	public void setRznSocRecep(String value) {
		delegate.setRznSocRecep(value);
	}

	@Override
	public void setDirRecep(String value) {
		delegate.setDirRecep(value);
	}

	@Override
	public void setCiudadRecep(String value) {
		delegate.setCiudadRecep(value);
	}

	@Override
	public void setDeptoRecep(String value) {
		delegate.setDeptoRecep(value);
	}

	@Override
	public void setPaisRecep(String value) {
		delegate.setPaisRecep(value);
	}

	@Override
	public void setCP(Integer value) {
		delegate.setCP(value);
	}

	@Override
	public void setInfoAdicional(String value) {
		delegate.setInfoAdicional(value);
	}

	@Override
	public void setLugarDestEnt(String value) {
		delegate.setLugarDestEnt(value);
	}

	@Override
	public void setCompraID(String value) {
		delegate.setCompraID(value);
	}

}

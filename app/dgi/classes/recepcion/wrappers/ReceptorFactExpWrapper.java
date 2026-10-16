package dgi.classes.recepcion.wrappers;

import dgi.classes.recepcion.ReceptorFactExp;

/**
 * Wrapper del receptor de la e-Factura de Exportacion: receptor extranjero, admite DocRecepExt y TipoDocRecep es
 * opcional (Integer en el tipo de JAXB)
 */
public class ReceptorFactExpWrapper implements ReceptorInterface {

	private ReceptorFactExp delegate;

	public ReceptorFactExpWrapper(ReceptorFactExp receptor) {
		this.delegate = receptor;
	}

	@Override
	public int getTipoDocRecep() {
		return delegate.getTipoDocRecep() != null ? delegate.getTipoDocRecep() : 0;
	}

	@Override
	public void setTipoDocRecep(int value) {
		delegate.setTipoDocRecep(value);
	}

	@Override
	public String getCodPaisRecep() {
		return delegate.getCodPaisRecep();
	}

	@Override
	public void setCodPaisRecep(String value) {
		delegate.setCodPaisRecep(value);
	}

	@Override
	public String getDocRecep() {
		return delegate.getDocRecep();
	}

	@Override
	public void setDocRecep(String value) {
		delegate.setDocRecep(value);
	}

	@Override
	public String getDocRecepExt() {
		return delegate.getDocRecepExt();
	}

	@Override
	public void setDocRecepExt(String value) {
		delegate.setDocRecepExt(value);
	}

	@Override
	public String getRznSocRecep() {
		return delegate.getRznSocRecep();
	}

	@Override
	public void setRznSocRecep(String value) {
		delegate.setRznSocRecep(value);
	}

	@Override
	public String getDirRecep() {
		return delegate.getDirRecep();
	}

	@Override
	public void setDirRecep(String value) {
		delegate.setDirRecep(value);
	}

	@Override
	public String getCiudadRecep() {
		return delegate.getCiudadRecep();
	}

	@Override
	public void setCiudadRecep(String value) {
		delegate.setCiudadRecep(value);
	}

	@Override
	public String getDeptoRecep() {
		return delegate.getDeptoRecep();
	}

	@Override
	public void setDeptoRecep(String value) {
		delegate.setDeptoRecep(value);
	}

	@Override
	public String getPaisRecep() {
		return delegate.getPaisRecep();
	}

	@Override
	public void setPaisRecep(String value) {
		delegate.setPaisRecep(value);
	}

	@Override
	public Integer getCP() {
		return delegate.getCP();
	}

	@Override
	public void setCP(Integer value) {
		delegate.setCP(value);
	}

	@Override
	public String getInfoAdicional() {
		return delegate.getInfoAdicional();
	}

	@Override
	public void setInfoAdicional(String value) {
		delegate.setInfoAdicional(value);
	}

	@Override
	public String getLugarDestEnt() {
		return delegate.getLugarDestEnt();
	}

	@Override
	public void setLugarDestEnt(String value) {
		delegate.setLugarDestEnt(value);
	}

	@Override
	public String getCompraID() {
		return delegate.getCompraID();
	}

	@Override
	public void setCompraID(String value) {
		delegate.setCompraID(value);
	}

}

package com.bluedot.efactura.strategy.builder;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONException;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroControllerFactory;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.EResg.Detalle;
import dgi.classes.recepcion.CFEDefType.EResg.Encabezado;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.ItemResg;
import dgi.classes.recepcion.ReceptorResg;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.TotalesResg;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocResgWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemResgWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorResgWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TotalesResguardoWrrapper;
import play.db.jpa.JPAApi;

public class EResguardoStrategy extends CommonStrategy implements CFEStrategy {

	@Inject
	public EResguardoStrategy(@Assisted CFE cfe, @Assisted Empresa empresa, JPAApi jpaApi, CAEMicroControllerFactory caeMicroControllerFactory) throws APIException {
		super(cfe, caeMicroControllerFactory.create(empresa), jpaApi);
		if (cfe.getEresguardo() == null)
			this.cfe.setEresguardo(new EResg());

		switch (cfe.getTipo()) {
		case eResguardo:
		case eResguardo_Contingencia:
			break;
		default:
			throw APIException.raise(APIErrors.NOT_SUPPORTED)
					.setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
		}

	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	private ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorResg());
		return new ReceptorResgWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEresguardo().getEncabezado() == null)
			cfe.getEresguardo().setEncabezado(new Encabezado());
		return cfe.getEresguardo().getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesResg());
		return new TotalesResguardoWrrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (cfe.getEresguardo().getCAEData() == null)
			cfe.getEresguardo().setCAEData(new CAEDataType());
		return cfe.getEresguardo().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();

		for (ItemResg itemResg : cfe.getEresguardo().getDetalle().getItems()) {
			list.add(new ItemResgWrapper(itemResg));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocResg());
		return new IdDocResgWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocResg idDocResg = caeMicroController.getIdDocResg(cfe.getTipo());
			getEncabezado().setIdDoc(idDocResg);
			cfe.setSerie(idDocResg.getSerie());
			cfe.setNro(idDocResg.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			APIException.raise(e);
		}
	}

	@Override
	public void setCAEData() throws APIException {
		try {
			cfe.getEresguardo().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			APIException.raise(e);
		}

	}

	private Detalle getDetalle() {
		if (cfe.getEresguardo().getDetalle() == null)
			cfe.getEresguardo().setDetalle(new Detalle());
		return cfe.getEresguardo().getDetalle();
	}

	@Override
	public ItemInterface createItem() {
		ItemResg item = new ItemResg();
		getDetalle().getItems().add(item);
		return new ItemResgWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (cfe.getEresguardo().getReferencia() == null)
			cfe.getEresguardo().setReferencia(new ReferenciaTipo());
		return cfe.getEresguardo().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		cfe.getEresguardo().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String rznSocRecep,
			String dirRecep, String ciudadRecep, String deptoRecep) throws APIException {
		ReceptorInterface receptor = getReceptor();

		if (tipoDocRecep != null)
			receptor.setTipoDocRecep(tipoDocRecep.getId());
		if (codPaisRecep != null)
			receptor.setCodPaisRecep(codPaisRecep);
		if (docRecep != null)
			receptor.setDocRecep(docRecep);
		if (dirRecep != null)
			receptor.setDirRecep(dirRecep);
		if (rznSocRecep != null)
			receptor.setRznSocRecep(rznSocRecep);
		if (ciudadRecep != null)
			receptor.setCiudadRecep(ciudadRecep);
		if (deptoRecep != null)
			receptor.setDeptoRecep(deptoRecep);

		if (codPaisRecep != null && docRecep != null && codPaisRecep != null)
			if (tipoDocRecep == TipoDocumento.CI) {
				Pais pais = Pais.findByCodigo(jpaApi, codPaisRecep, true);
				cfe.setTitular(getOrCreateTitular(pais, tipoDocRecep, docRecep));
			} else {
				if (rznSocRecep != null && dirRecep != null && ciudadRecep != null && deptoRecep != null)
					cfe.setEmpresaReceptora(
							getOrCreateEmpresa(docRecep, rznSocRecep, dirRecep, ciudadRecep, deptoRecep));
			}

	}
}

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
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.CFEDefType.ERem.Detalle;
import dgi.classes.recepcion.CFEDefType.ERem.Encabezado;
import dgi.classes.recepcion.CFEDefType.ERem.Encabezado.Totales;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocRem;
import dgi.classes.recepcion.ItemRem;
import dgi.classes.recepcion.ReceptorRem;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocRemWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemRemWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorRemitoWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TotalesRemitoWrrapper;

public class ERemitoStrategy extends CommonStrategy implements CFEStrategy {

	public ERemitoStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEremito() == null)
			this.cfe.setEremito(new ERem());

		switch (cfe.getTipo()) {
		case eRemito:
		case eRemito_Contingencia:
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
			getEncabezado().setReceptor(new ReceptorRem());
		return new ReceptorRemitoWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEremito().getEncabezado() == null)
			cfe.getEremito().setEncabezado(new Encabezado());
		return cfe.getEremito().getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new Totales());
		return new TotalesRemitoWrrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (cfe.getEremito().getCAEData() == null)
			cfe.getEremito().setCAEData(new CAEDataType());
		return cfe.getEremito().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();

		for (ItemRem itemRem : cfe.getEremito().getDetalle().getItems()) {
			list.add(new ItemRemWrapper(itemRem));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocRem());
		return new IdDocRemWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocRem idDocResg = caeMicroController.getIdDocRem(cfe.getTipo(), cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
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
			cfe.getEremito().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			APIException.raise(e);
		}

	}

	private Detalle getDetalle() {
		if (cfe.getEremito().getDetalle() == null)
			cfe.getEremito().setDetalle(new Detalle());
		return cfe.getEremito().getDetalle();
	}

	@Override
	public ItemInterface createItem() {
		ItemRem item = new ItemRem();
		getDetalle().getItems().add(item);
		return new ItemRemWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (cfe.getEremito().getReferencia() == null)
			cfe.getEremito().setReferencia(new ReferenciaTipo());
		return cfe.getEremito().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		cfe.getEremito().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String rznSocRecep,
			String dirRecep, String ciudadRecep, String deptoRecep, String pdfMailAddress) throws APIException {
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
				Pais pais = Pais.findByCodigo(codPaisRecep, true);
				cfe.setTitular(getOrCreateTitular(pais, tipoDocRecep, docRecep));
			} else {
				if (rznSocRecep != null && dirRecep != null && ciudadRecep != null)
					cfe.setEmpresaReceptora(
							Empresa.getOrCreateEmpresa(docRecep, rznSocRecep, null));
			}
		
		cfe.setPdfMailAddress(pdfMailAddress);

	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		if (cfe.getEfactura().getComplFiscal() == null)
			cfe.getEfactura().setComplFiscal(new ComplFiscalType());
		return cfe.getEfactura().getComplFiscal();
	}
}

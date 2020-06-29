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
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EFact.Detalle;
import dgi.classes.recepcion.CFEDefType.EFact.Encabezado;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ReceptorFact;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.Totales;
import dgi.classes.recepcion.wrappers.IdDocFactWrapper;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemDetFactWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorFactWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesFactTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public class EfactStrategy extends CommonStrategy implements CFEStrategy {

	public EfactStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEfactura()==null)
			this.cfe.setEfactura(new EFact());
		
		switch (cfe.getTipo()) {
			case eFactura:
			case eFactura_Contingencia:
			case Nota_de_Credito_de_eFactura:
			case Nota_de_Credito_de_eFactura_Contingencia:
			case Nota_de_Debito_de_eFactura:
			case Nota_de_Debito_de_eFactura_Contingencia:
			case eFactura_Venta_por_Cuenta_Ajena:
			case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
				break;
			default:
				throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
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
			getEncabezado().setReceptor(new ReceptorFact());
		return new ReceptorFactWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEfactura().getEncabezado() == null)
			cfe.getEfactura().setEncabezado(new Encabezado());
		return cfe.getEfactura().getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new Totales());
		return new TotalesFactTickWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (cfe.getEfactura().getCAEData() == null)
			cfe.getEfactura().setCAEData(new CAEDataType());
		return cfe.getEfactura().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();

		for (ItemDetFact itemDetFact : cfe.getEfactura().getDetalle().getItems()) {
			list.add(new ItemDetFactWrapper(itemDetFact));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocFact());
		return new IdDocFactWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException{
		try {
			IdDocFact idDocFact = caeMicroController.getIdDocFact(cfe.getTipo(), cfe.isIndMontoBruto(), cfe.getFormaDePago().value, cfe.getFechaEmision());
			getEncabezado().setIdDoc(idDocFact);
			cfe.setSerie(idDocFact.getSerie());
			cfe.setNro(idDocFact.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			APIException.raise(e);
		}
	}

	@Override
	public void setCAEData() throws APIException{
		try {
			cfe.getEfactura().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			APIException.raise(e);
		}

	}

	private Detalle getDetalle() {
		if (cfe.getEfactura().getDetalle() == null)
			cfe.getEfactura().setDetalle(new Detalle());
		return cfe.getEfactura().getDetalle();
	}

	@Override
	public ItemInterface createItem() {
		ItemDetFact item = new ItemDetFact();
		getDetalle().getItems().add(item);
		return new ItemDetFactWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (cfe.getEfactura().getReferencia() == null)
			cfe.getEfactura().setReferencia(new ReferenciaTipo());
		return cfe.getEfactura().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		cfe.getEfactura().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, boolean update) throws APIException {
		
		ReceptorInterface receptor = getReceptor();
		
		if (tipoDocRecep==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("tipoDocRecep");
		
		if (tipoDocRecep != TipoDocumento.RUC)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("tipoDocRecep").setDetailMessage("Deberia ser 2 (RUC)");
		
		if (codPaisRecep ==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("codPaisRecep");
		
		if (!codPaisRecep.equals("UY"))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("codPaisRecep").setDetailMessage("Deberia ser UY");
		
		if (docRecep ==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("docRecep");
		
		if (dirRecep ==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("dirRecep");
		
		if (rznSocRecep ==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("rznSocRecep");
		
		if (ciudadRecep ==null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("ciudadRecep");
		
		receptor.setTipoDocRecep(tipoDocRecep.getId());
		receptor.setCodPaisRecep(codPaisRecep);
		receptor.setDocRecep(docRecep);
		receptor.setDirRecep(dirRecep);
		receptor.setRznSocRecep(rznSocRecep);
		receptor.setCiudadRecep(ciudadRecep);
		
		if (deptoRecep != null)
			receptor.setDeptoRecep(deptoRecep);
		
		cfe.setEmpresaReceptora(Empresa.getOrCreateEmpresa(docRecep, rznSocRecep, dirRecep, ciudadRecep, deptoRecep, update));
		
	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		if (cfe.getEfactura().getComplFiscal() == null)
			cfe.getEfactura().setComplFiscal(new ComplFiscalType());
		return cfe.getEfactura().getComplFiscal();
	}
}

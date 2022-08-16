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
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.CFEDefType.ETck.Detalle;
import dgi.classes.recepcion.CFEDefType.ETck.Encabezado;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocTck;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ReceptorTck;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.Totales;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocTickWrapper;
import dgi.classes.recepcion.wrappers.ItemDetFactWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesFactTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public class EticketStrategy extends CommonStrategy implements CFEStrategy {

	public EticketStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEticket()==null)
			this.cfe.setEticket(new ETck());

		switch (cfe.getTipo()) {
			case eTicket:
			case eTicket_Contingencia:
			case Nota_de_Credito_de_eTicket:
			case Nota_de_Credito_de_eTicket_Contingencia:
			case Nota_de_Debito_de_eTicket:
			case Nota_de_Debito_de_eTicket_Contingencia:
			case eTicket_Venta_por_Cuenta_Ajena:
			case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
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
			getEncabezado().setReceptor(new ReceptorTck());
		return new ReceptorTickWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEticket().getEncabezado() == null)
			cfe.getEticket().setEncabezado(new Encabezado());
		return cfe.getEticket().getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new Totales());
		return new TotalesFactTickWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (cfe.getEticket().getCAEData() == null)
			cfe.getEticket().setCAEData(new CAEDataType());
		return cfe.getEticket().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();

		for (ItemDetFact itemDetFact : cfe.getEticket().getDetalle().getItems()) {
			list.add(new ItemDetFactWrapper(itemDetFact));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocTck());
		return new IdDocTickWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocTck idDocTck = caeMicroController.getIdDocTick(cfe.getTipo(), cfe.isIndMontoBruto(), cfe.getFormaDePago().value, cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
			getEncabezado().setIdDoc(idDocTck);
			cfe.setSerie(idDocTck.getSerie());
			cfe.setNro(idDocTck.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			APIException.raise(e);
		}
	}

	@Override
	public void setCAEData() throws APIException {
		try {
			cfe.getEticket().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			APIException.raise(e);
		}

	}

	private Detalle getDetalle() {
		if (cfe.getEticket().getDetalle() == null)
			cfe.getEticket().setDetalle(new Detalle());
		return cfe.getEticket().getDetalle();
	}

	@Override
	public ItemInterface createItem() {
		ItemDetFact item = new ItemDetFact();
		getDetalle().getItems().add(item);
		return new ItemDetFactWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (cfe.getEticket().getReferencia() == null)
			cfe.getEticket().setReferencia(new ReferenciaTipo());
		return cfe.getEticket().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		cfe.getEticket().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String pdfMailAddress, String CompraID) throws APIException {
		ReceptorInterface receptor = getReceptor();

		if (supera10000UI()) {
			
			if (tipoDocRecep==null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("tipoDocRecep");
			
			if (tipoDocRecep == TipoDocumento.RUC)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("tipoDocRecep").setDetailMessage("Deberia ser distinto de 2 (RUC)");

			if (codPaisRecep == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("codPaisRecep");

			if ((tipoDocRecep==TipoDocumento.RUC || tipoDocRecep==TipoDocumento.CI)  && !codPaisRecep.equals("UY"))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("codPaisRecep")
						.setDetailMessage("Debe ser UY");

			if (tipoDocRecep==TipoDocumento.DNI  && (codPaisRecep.equals("AR") || codPaisRecep.equals("BR") || codPaisRecep.equals("CL") || codPaisRecep.equals("PY"))   )
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("codPaisRecep")
						.setDetailMessage("Debe ser UY");
			
			if (docRecep == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("docRecep");


		}

		if (tipoDocRecep!=null)
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
		
		if (codPaisRecep != null && docRecep != null && codPaisRecep!=null){
			Pais pais = Pais.findByCodigo(codPaisRecep, true);
			cfe.setTitular(getOrCreateTitular(pais,tipoDocRecep,docRecep));
		}
		
		receptor.setCompraID(CompraID);
		
		cfe.setPdfMailAddress(pdfMailAddress);
	}
	
	@Override
	public ComplFiscalType getComplementoFiscal() {
		if (cfe.getEfactura().getComplFiscal() == null)
			cfe.getEfactura().setComplFiscal(new ComplFiscalType());
		return cfe.getEfactura().getComplFiscal();
	}
	
}

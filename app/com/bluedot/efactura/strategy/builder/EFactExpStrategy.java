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
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.CFEDefType.EFactExp.Detalle;
import dgi.classes.recepcion.CFEDefType.EFactExp.Encabezado;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocFactExp;
import dgi.classes.recepcion.ItemDetFactExp;
import dgi.classes.recepcion.ReceptorFactExp;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.TotalesFactExp;
import dgi.classes.recepcion.wrappers.IdDocFactExpWrapper;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemDetFactExpWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorFactExpWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesFactExpWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * e-Factura de Exportacion y sus notas de credito y debito (121, 122, 123 y contingencia 221, 222, 223). El e-Remito
 * de exportacion (124/224) es ERemExpStrategy.
 */
public class EFactExpStrategy extends CommonStrategy implements EstrategiaExportacion {

	public EFactExpStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEfacturaExportacion() == null)
			this.cfe.setEfacturaExportacion(new EFactExp());

		switch (cfe.getTipo()) {
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
				break;
			default:
				throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
		}
	}

	private EFactExp getEFactExp() {
		return cfe.getEfacturaExportacion();
	}

	private Encabezado getEncabezado() {
		if (getEFactExp().getEncabezado() == null)
			getEFactExp().setEncabezado(new Encabezado());
		return getEFactExp().getEncabezado();
	}

	private Detalle getDetalle() {
		if (getEFactExp().getDetalle() == null)
			getEFactExp().setDetalle(new Detalle());
		return getEFactExp().getDetalle();
	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	private ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorFactExp());
		return new ReceptorFactExpWrapper(getEncabezado().getReceptor());
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesFactExp());
		return new TotalesFactExpWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (getEFactExp().getCAEData() == null)
			getEFactExp().setCAEData(new CAEDataType());
		return getEFactExp().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();
		for (ItemDetFactExp item : getDetalle().getItems()) {
			list.add(new ItemDetFactExpWrapper(item));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocFactExp());
		return new IdDocFactExpWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocFactExp idDoc = caeMicroController.getIdDocFactExp(cfe.getTipo(), cfe.getFormaDePago().value, cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
			idDoc.setNroInterno(cfe.getNroInterno());
			// ClauVenta, ModVenta, ViaTransp y los opcionales (validados en DatosIdDoc.leer al crear el CFE)
			DatosIdDoc.aplicar(cfe.getDatosIdDoc(), idDoc);
			getEncabezado().setIdDoc(idDoc);
			cfe.setSerie(idDoc.getSerie());
			cfe.setNro(idDoc.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			throw APIException.raise(e);
		}
	}

	@Override
	public void setCAEData() throws APIException {
		try {
			getEFactExp().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			throw APIException.raise(e);
		}
	}

	@Override
	public ItemInterface createItem() {
		ItemDetFactExp item = new ItemDetFactExp();
		getDetalle().getItems().add(item);
		return new ItemDetFactExpWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (getEFactExp().getReferencia() == null)
			getEFactExp().setReferencia(new ReferenciaTipo());
		return getEFactExp().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		getEFactExp().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String pdfMailAddress, String compraID) throws APIException {
		buildReceptor(tipoDocRecep, codPaisRecep, docRecep, docRecepExt, rznSocRecep, dirRecep, ciudadRecep, deptoRecep, null, null,
				null, null, pdfMailAddress, compraID, false);
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String paisRecep, Integer cp,
			String infoAdicional, String lugarDestEnt, String pdfMailAddress, String compraID, boolean secretoProfesional) throws APIException {
		ReceptorExportacion.armar(this, cfe, this::getReceptor, "la e-Factura de exportacion y sus notas", tipoDocRecep, codPaisRecep,
				docRecep, docRecepExt, rznSocRecep, dirRecep, ciudadRecep, deptoRecep, paisRecep, cp, infoAdicional, lugarDestEnt,
				pdfMailAddress, compraID, secretoProfesional);
	}

	@Override
	public void setNCM(int indice, String ncm) {
		getDetalle().getItems().get(indice).setNCM(ncm);
	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		// La e-Factura de exportacion no tiene complemento fiscal
		return null;
	}

}

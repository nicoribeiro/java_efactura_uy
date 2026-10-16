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
import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.CFEDefType.ERemExp.Detalle;
import dgi.classes.recepcion.CFEDefType.ERemExp.Encabezado;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocRemExp;
import dgi.classes.recepcion.ItemRemExp;
import dgi.classes.recepcion.ReceptorRemExp;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.TotalesRemExp;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocRemExpWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemRemExpWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorRemExpWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TotalesRemExpWrapper;

/**
 * e-Remito de Exportacion (124 y contingencia 224): documenta el traslado de la mercaderia que se exporta. Lleva
 * TipoTraslado, ModVenta y ViaTransp en el IdDoc, receptor del exterior como la e-Factura de exportacion, items con
 * precio y totales sin IVA (MntExpoyAsim y MntTotal). No tiene forma de pago, descuentos, recargos, medios de pago ni
 * complemento fiscal.
 */
public class ERemExpStrategy extends CommonStrategy implements EstrategiaExportacion {

	public ERemExpStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEremitoExportacion() == null)
			this.cfe.setEremitoExportacion(new ERemExp());

		switch (cfe.getTipo()) {
			case eRemito_de_Exportacion:
			case eRemito_de_Exportacion_Contingencia:
				break;
			default:
				throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
		}
	}

	private ERemExp getERemExp() {
		return cfe.getEremitoExportacion();
	}

	private Encabezado getEncabezado() {
		if (getERemExp().getEncabezado() == null)
			getERemExp().setEncabezado(new Encabezado());
		return getERemExp().getEncabezado();
	}

	private Detalle getDetalle() {
		if (getERemExp().getDetalle() == null)
			getERemExp().setDetalle(new Detalle());
		return getERemExp().getDetalle();
	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	private ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorRemExp());
		return new ReceptorRemExpWrapper(getEncabezado().getReceptor());
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesRemExp());
		return new TotalesRemExpWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (getERemExp().getCAEData() == null)
			getERemExp().setCAEData(new CAEDataType());
		return getERemExp().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();
		for (ItemRemExp item : getDetalle().getItems()) {
			list.add(new ItemRemExpWrapper(item));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocRemExp());
		return new IdDocRemExpWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocRemExp idDoc = caeMicroController.getIdDocRemExp(cfe.getTipo(), cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
			idDoc.setNroInterno(cfe.getNroInterno());
			// TipoTraslado, ModVenta, ViaTransp y los opcionales (validados en DatosIdDoc.leer al crear el CFE)
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
			getERemExp().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			throw APIException.raise(e);
		}
	}

	@Override
	public ItemInterface createItem() {
		ItemRemExp item = new ItemRemExp();
		getDetalle().getItems().add(item);
		return new ItemRemExpWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (getERemExp().getReferencia() == null)
			getERemExp().setReferencia(new ReferenciaTipo());
		return getERemExp().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		getERemExp().setTmstFirma(newXMLGregorianCalendar);
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
		ReceptorExportacion.armar(this, cfe, this::getReceptor, "el e-Remito de exportacion", tipoDocRecep, codPaisRecep, docRecep,
				docRecepExt, rznSocRecep, dirRecep, ciudadRecep, deptoRecep, paisRecep, cp, infoAdicional, lugarDestEnt, pdfMailAddress,
				compraID, secretoProfesional);
	}

	@Override
	public void setNCM(int indice, String ncm) {
		getDetalle().getItems().get(indice).setNCM(ncm);
	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		// El e-Remito de exportacion no tiene complemento fiscal
		return null;
	}

}

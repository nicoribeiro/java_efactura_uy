package com.bluedot.efactura.impl;

import org.json.JSONObject;

import com.bluedot.efactura.CFEController;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;

public class CFEControllerImpl implements CFEController {

	private Object buildTemplate(JSONObject docJSON, CFEBuiderInterface cfeBuilder, JSONObject referencia)
			throws EFacturaException {

		cfeBuilder.buildTimestampFirma();

		/*
		 * Detalle (lineas de factura)
		 */
		if (docJSON.optJSONArray("Detalle") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Detalle");
		cfeBuilder.buildDetalle(docJSON.getJSONArray("Detalle"));

		/*
		 * Encabezado
		 */
		if (docJSON.optJSONObject("Encabezado") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Encabezado");
		JSONObject encabezadoJSON = docJSON.getJSONObject("Encabezado");

		cfeBuilder.buildEmisor(Commons.safeGetJSONObject(encabezadoJSON, "Emisor"));

		if (encabezadoJSON.optJSONObject("Receptor") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Receptor");
		cfeBuilder.buildReceptor(encabezadoJSON.optJSONObject(("Receptor")));

		if (encabezadoJSON.optJSONObject("Totales") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Totales");
		cfeBuilder.buildTotales(encabezadoJSON.getJSONObject("Totales"));

		cfeBuilder.buildIdDoc();

		if (referencia != null)
			cfeBuilder.buildReferencia(referencia);

		/*
		 * CAEData
		 */
		cfeBuilder.buildCAEData();

		return cfeBuilder.getCFE();
	}

	@Override
	public EFact createEfactura(JSONObject factura) throws EFacturaException {

		return (EFact) buildTemplate(factura, CFEBuilderFactory.getCFEBuilder(TipoDoc.eFactura), null);

	}

	@Override
	public ETck createETicket(JSONObject ticket) throws EFacturaException {

		return (ETck) buildTemplate(ticket, CFEBuilderFactory.getCFEBuilder(TipoDoc.eTicket), null);

	}

	@Override
	public ERem createERemito(JSONObject remito) throws EFacturaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EResg createEResguardo(JSONObject resguardo) throws EFacturaException {

		return (EResg) buildTemplate(resguardo, CFEBuilderFactory.getCFEBuilder(TipoDoc.eResguardo), null);

	}

	@Override
	public ERemExp createERemitoExportacion(JSONObject remito) throws EFacturaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EFactExp createEFacturaExportacion(JSONObject factura) throws EFacturaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EFact createNotaCreditoEfactura(JSONObject notaCredito, JSONObject referencia) throws EFacturaException {
		if (referencia == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Referencia");

		return (EFact) buildTemplate(notaCredito, CFEBuilderFactory.getCFEBuilder(TipoDoc.Nota_de_Credito_de_eFactura),
				referencia);

	}

	@Override
	public EFact createNotaDebitoEfactura(JSONObject notaDebito, JSONObject referencia) throws EFacturaException {
		if (referencia == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Referencia");

		return (EFact) buildTemplate(notaDebito, CFEBuilderFactory.getCFEBuilder(TipoDoc.Nota_de_Debito_de_eFactura),
				referencia);

	}

	@Override
	public ETck createNotaCreditoETicket(JSONObject notaCredito, JSONObject referencia) throws EFacturaException {
		if (referencia == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Referencia");

		return (ETck) buildTemplate(notaCredito, CFEBuilderFactory.getCFEBuilder(TipoDoc.Nota_de_Credito_de_eTicket),
				referencia);

	}

	@Override
	public ETck createNotaDebitoETicket(JSONObject notoDebito, JSONObject referencia) throws EFacturaException {
		if (referencia == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Referencia");

		return (ETck) buildTemplate(notoDebito, CFEBuilderFactory.getCFEBuilder(TipoDoc.Nota_de_Debito_de_eTicket),
				referencia);

	}

}

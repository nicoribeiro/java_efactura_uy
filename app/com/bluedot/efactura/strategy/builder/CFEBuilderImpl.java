package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FormaDePago;
import com.bluedot.efactura.model.IVA;
import com.bluedot.efactura.model.IndicadorFacturacion;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.ReferenciaTipo.Referencia;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TpoCod;

public class CFEBuilderImpl implements CFEBuiderInterface {

	protected CFEStrategy strategy;
	protected CAEMicroController caeMicroController;

	public CFEBuilderImpl(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		this.caeMicroController = caeMicroController;
		this.strategy = strategy;
	}

	protected CFEBuilderImpl() {

	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		strategy.getCFE().setCantLineas(detalleJson.length());
		
		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemInterface item = strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);
			
			
			item.setNroLinDet(i);

			item.setNomItem(Commons.safeGetString(itemJson,"NomItem"));

			item.setIndFact(new BigInteger(String.valueOf(Commons.safeGetInteger(itemJson,"IndFact"))));

			item.setCantidad(new BigDecimal(Commons.safeGetString(itemJson,"Cantidad")));

			item.setUniMed(Commons.safeGetString(itemJson,"UniMed"));

			item.setPrecioUnitario(new BigDecimal(Commons.safeGetString(itemJson,"PrecioUnitario")));

			if (!itemJson.has("MontoItem"))
				item.setMontoItem(item.getPrecioUnitario().multiply(item.getCantidad()));
			else
				item.setMontoItem(new BigDecimal(itemJson.getString("MontoItem")));
			
			Detalle detalle = new Detalle(strategy.getCFE(), i,  item.getNomItem(), item.getCantidad().doubleValue(), item.getUniMed(), item.getPrecioUnitario().doubleValue(), item.getMontoItem().doubleValue());
			
			if (itemJson.has("CodItem")){
				item.addCodItem(TpoCod.INT1, itemJson.getString("CodItem"));
				detalle.setCodItem(itemJson.getString("CodItem"));
			}
			
			strategy.getCFE().getDetalle().add(detalle);
		}
	}

	@Override
	public void buildReceptor(JSONObject receptorJson) throws APIException {
		
		TipoDocumento TipoDocRecep = receptorJson.has("TipoDocRecep")? TipoDocumento.fromInt(receptorJson.getInt("TipoDocRecep")): null;

		String CodPaisRecep = receptorJson.has("CodPaisRecep") ? receptorJson.getString("CodPaisRecep") : null;

		String DocRecep = receptorJson.has("DocRecep") ? receptorJson.getString("DocRecep") : null;

		String RznSocRecep = receptorJson.has("RznSocRecep") ? receptorJson.getString("RznSocRecep") : null;

		String DirRecep = receptorJson.has("DirRecep") ? receptorJson.getString("DirRecep") : null;

		String CiudadRecep = receptorJson.has("CiudadRecep") ? receptorJson.getString("CiudadRecep") : null;
		
		String DeptoRecep = receptorJson.has("DeptoRecep") ? receptorJson.getString("DeptoRecep") : null;
		
		strategy.buildReceptor(TipoDocRecep, CodPaisRecep, DocRecep, RznSocRecep, DirRecep, CiudadRecep, DeptoRecep);
		
	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException {
		List<ItemInterface> items = strategy.getItem();

		TotalesInterface totales = strategy.getTotales();

		/*
		 * Moneda
		 */
		TipMonType moneda = TipMonType.fromValue(totalesJson.getString("TpoMoneda"));

		if (moneda == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("TpoMoneda"))
					.setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);
		strategy.getCFE().setMoneda(moneda);

		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio")){
				DecimalFormat df = new DecimalFormat("####0.000");
				totales.setTpoCambio(new BigDecimal(df.format(totalesJson.getDouble("TpoCambio"))));
				strategy.getCFE().setTipoCambio(totales.getTpoCambio().doubleValue());
			}else
				throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("totales.TpoCambio"));

		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(items.size());

		/*
		 * IVA
		 */
		BigDecimal[] iva = new BigDecimal[IndicadorFacturacion.maxIndice + 1];

		for (int i = 0; i <= IndicadorFacturacion.maxIndice; i++) {
			iva[i] = BigDecimal.ZERO;
		}

		for (Iterator<ItemInterface> iterator = items.iterator(); iterator.hasNext();) {
			ItemInterface item = iterator.next();
			BigDecimal itemMonto = item.getPrecioUnitario().multiply(item.getCantidad());
			iva[item.getIndFact().intValue()] = itemMonto.add(iva[item.getIndFact().intValue()]);

		}

		for (IndicadorFacturacion indiceIva : IndicadorFacturacion.values()) {
			if (iva[indiceIva.getIndice()] != BigDecimal.ZERO && !indiceIva.isSoportado())
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE)
				.setDetailMessage("El indicador de facturacion (IndFact=" + indiceIva.getIndice() + ") no esta soportado");
		}
		
		/*
		 * id:119
		 */
		totales.setIVATasaMin(new BigDecimal(String.valueOf(IVA.findByIndicadorFacturacion(IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA).getPorcentajeIVA())));
		strategy.getCFE().setIvaTasaMin(totales.getIVATasaMin().doubleValue());

		/*
		 * id:120
		 */
		totales.setIVATasaBasica(new BigDecimal(String.valueOf(IVA.findByIndicadorFacturacion(IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA).getPorcentajeIVA())));
		strategy.getCFE().setIvaTasaBas(totales.getIVATasaBasica().doubleValue());

		/*
		 * id:112
		 */
		totales.setMntNoGrv(iva[IndicadorFacturacion.INDICADOR_FACTURACION_EXCENTO_IVA.getIndice()].setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setTotMntNoGrv(totales.getMntNoGrv().doubleValue());

		/*
		 * id:113
		 */
		totales.setMntExpoyAsim(new BigDecimal("0"));
		strategy.getCFE().setTotMntExpyAsim(totales.getMntExpoyAsim().doubleValue());

		/*
		 * id:114
		 */
		totales.setMntImpuestoPerc(new BigDecimal("0"));
		strategy.getCFE().setTotMntImpPerc(totales.getMntImpuestoPerc().doubleValue());

		/*
		 * id:115
		 */
		totales.setMntIVaenSusp(new BigDecimal("0"));
		strategy.getCFE().setTotMntIVAenSusp(totales.getMntIVaenSusp().doubleValue());

		/*
		 * id:116
		 */
		if (montosIncluyenIva){
			BigDecimal divisor = (new BigDecimal(1)).add(totales.getIVATasaMin().divide(new BigDecimal("100")));
			totales.setMntNetoIvaTasaMin((iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA.getIndice()].divide(divisor, 2, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP));
		}else
			totales.setMntNetoIvaTasaMin(iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA.getIndice()].setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setTotMntIVATasaMin(totales.getMntNetoIVATasaMin().doubleValue());
		
		/*
		 * id:117
		 */
		if (montosIncluyenIva){
			BigDecimal divisor = (new BigDecimal(1)).add(totales.getIVATasaBasica().divide(new BigDecimal("100")));
			totales.setMntNetoIVATasaBasica(
					(iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA.getIndice()].divide(divisor, 2, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP));
		}else
			totales.setMntNetoIVATasaBasica(
				iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA.getIndice()].setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setTotMntIVATasaBas(totales.getMntNetoIVATasaBasica().doubleValue());
		
		/*
		 * id:118
		 */
		//TODO otra tasa no se sabe cual es para dividir como en id:117 o id:116
		if (montosIncluyenIva)
			totales.setMntNetoIVAOtra(iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_OTRA_TASA.getIndice()].setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			totales.setMntNetoIVAOtra(iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_OTRA_TASA.getIndice()].setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setTotMntIVAOtra(totales.getMntNetoIVAOtra().doubleValue());
		

		/*
		 * id:121
		 */
		if (montosIncluyenIva)
			totales.setMntIVATasaMin(
					iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA.getIndice()].subtract(totales.getMntNetoIVATasaMin()).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			totales.setMntIVATasaMin(
					totales.getIVATasaMin().multiply((totales.getMntNetoIVATasaMin().divide(new BigDecimal("100"))))
							.setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setMntIVATasaMin(totales.getMntIVATasaMin().doubleValue());

		/*
		 * id:122
		 */
		if (montosIncluyenIva)
			totales.setMntIVATasaBasica(
					iva[IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA.getIndice()].subtract(totales.getMntNetoIVATasaBasica()).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
		totales.setMntIVATasaBasica(
				totales.getIVATasaBasica().multiply((totales.getMntNetoIVATasaBasica().divide(new BigDecimal("100"))))
						.setScale(2, BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setMntIVATasaBas(totales.getMntIVATasaBasica().doubleValue());

		/*
		 * id:123
		 */
		//TODO ver que no se toma en cuenta si los montosIncluyenIva
		totales.setMntIVAOtra(
				(new BigDecimal("1")).multiply((totales.getMntNetoIVAOtra().divide(new BigDecimal("100")))).setScale(2,
						BigDecimal.ROUND_HALF_UP));
		strategy.getCFE().setMntIVAOtra(totales.getMntIVAOtra().doubleValue());
		

		/*
		 * id:124
		 */
		BigDecimal mntTotal = (new BigDecimal("0")).add(totales.getMntNoGrv()).add(totales.getMntExpoyAsim())
				.add(totales.getMntImpuestoPerc()).add(totales.getMntIVaenSusp()).add(totales.getMntNetoIVATasaMin())
				.add(totales.getMntNetoIVATasaBasica()).add(totales.getMntNetoIVAOtra());

		mntTotal = mntTotal.add(totales.getMntIVATasaMin()).add(totales.getMntIVATasaBasica())
				.add(totales.getMntIVAOtra());
		mntTotal.setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMntTotal(mntTotal);
		strategy.getCFE().setTotMntTotal(mntTotal.doubleValue());
		
		/*
		 * id:125 
		 */
		//TODO esto es distinto de cero cuando se hace una retencion (resguardo no?)
		BigDecimal montoTotalRetenido = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMntTotRetenido(montoTotalRetenido);
		strategy.getCFE().setTotMntRetenido(montoTotalRetenido.doubleValue());
		
		/*
		 * id:129
		 */
		BigDecimal montoNF = iva[IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE.getIndice()].subtract(iva[IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO.getIndice()]).setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMontoNF(montoNF);

		/*
		 * id:130
		 */
		BigDecimal mntPagar = mntTotal.add(totales.getMontoNF()).add(totales.getMntTotRetenido()).setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMntPagar(mntPagar);
		
	}

	@Override
	public void buildIdDoc(boolean montosIncluyenIva, int formaPago) throws APIException {
		strategy.getCFE().setFormaDePago(FormaDePago.fromInt(formaPago));
		strategy.getCFE().setIndMontoBruto(montosIncluyenIva);
		strategy.getCFE().setCae(caeMicroController.getCAE(strategy.getCFE().getTipo()));
	}

	@Override
	public void buildCAEData() throws APIException {
		strategy.setCAEData();
	}

	@Override
	public void buildEmisor(Empresa empresaEmisora) throws APIException {
		Emisor emisor = strategy.getEmisor();

		emisor.setRUCEmisor(empresaEmisora.getRut());

		emisor.setRznSoc(empresaEmisora.getRazon());

		emisor.setCdgDGISucur(new BigInteger(String.valueOf(empresaEmisora.getCodigoSucursal())));

		emisor.setDomFiscal(empresaEmisora.getDireccion());

		emisor.setCiudad(empresaEmisora.getLocalidad());

		emisor.setDepartamento(empresaEmisora.getDepartamento());

		strategy.getCFE().setEmpresaEmisora(empresaEmisora);
	}

	@Override
	public void buildReferencia(Empresa empresaEmisora, JSONObject referenciaJSON) throws APIException {

		try {
			if (strategy.getCFE().isObligatorioReferencia()) {

				if (referenciaJSON == null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("referencia"));

				ReferenciaTipo referenciaType = strategy.getReferenciaTipo();

				Referencia referencia = new Referencia();

				/*
				 * Campo Opcional
				 */
				if (referenciaJSON.has("FechaCFEref")) {
					SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date parsedDate = new SimpleDateFormat("yyyyMMdd").parse(referenciaJSON.getString("FechaCFEref"));
					referencia.setFechaCFEref(
							DatatypeFactory.newInstance().newXMLGregorianCalendar(outputFormat.format(parsedDate)));
				}

				/*
				 * Se utiliza cuando no se puede identificar los CFE de
				 * referencia. Por ejemplo: -cuando el CFE afecta a un número
				 * de más de 40 CFE de referencia, -cuando se referencia a un
				 * documento no codificado, etc.
				 * 
				 * Se debe explicitar el motivo en "Razón Referencia" (C6)
				 */
				String generadorId = Commons.safeGetString(referenciaJSON, "NroRef");
				CFE cfeReferencia = CFE.findByGeneradorId(empresaEmisora, generadorId);

				if (cfeReferencia == null) {
					referencia.setIndGlobal(new BigInteger("1"));
					referencia.setRazonRef(Commons.safeGetString(referenciaJSON, "RazonRef"));
					strategy.getCFE().setRazonReferencia(Commons.safeGetString(referenciaJSON, "RazonRef"));
				} else {
					referencia.setNroCFERef(new BigInteger(String.valueOf(cfeReferencia.getNro())));
					referencia.setSerie(cfeReferencia.getSerie());
					referencia.setTpoDocRef(new BigInteger(String.valueOf(cfeReferencia.getTipo().value)));
					strategy.getCFE().setReferencia(cfeReferencia);
				}

				referencia.setNroLinRef(1);

				referenciaType.getReferencias().add(referencia);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void buildTimestampFirma() throws APIException {
		try {
			GregorianCalendar cal = new GregorianCalendar();
			XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
			strategy.setTimestampFirma(xmlCal);
			strategy.getCFE().setFecha(cal.getTime());
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}

	}

	@Override
	public CFE getCFE() {
		return strategy.getCFE();
	}

	@Override
	public void asignarId() throws APIException {
		strategy.setIdDoc();
	}

}

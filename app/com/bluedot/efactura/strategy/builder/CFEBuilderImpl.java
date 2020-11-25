package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
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
import com.bluedot.efactura.model.DireccionDocumento;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FormaDePago;
import com.bluedot.efactura.model.IVA;
import com.bluedot.efactura.model.IndicadorFacturacion;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.ComplFiscalDataType;
import dgi.classes.recepcion.ComplFiscalType;
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

	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

	
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
			
			if (itemJson.has("DscItem") && !itemJson.getString("DscItem").equalsIgnoreCase("null") && !itemJson.getString("DscItem").equalsIgnoreCase(""))
				item.setDscItem(itemJson.getString("DscItem"));
 
			item.setPrecioUnitario(new BigDecimal(Commons.safeGetString(itemJson,"PrecioUnitario")));

			if (!itemJson.has("MontoItem"))
				item.setMontoItem(item.getPrecioUnitario().multiply(item.getCantidad()));
			else
				item.setMontoItem(new BigDecimal(itemJson.getString("MontoItem")));
			
			Detalle detalle = new Detalle(strategy.getCFE(), i,  item.getNomItem(), item.getCantidad().doubleValue(), item.getUniMed(), item.getPrecioUnitario().doubleValue(), item.getMontoItem().doubleValue());
			
			if (itemJson.has("DscItem"))
				detalle.setDescripcionItem(item.getDscItem());
			
			if (itemJson.has("CodItem")){
				String tpoCod = TpoCod.INT1.name();
				if (itemJson.has("TpoCod"))
					tpoCod = itemJson.getString("TpoCod");
				
				item.addCodItem(tpoCod, itemJson.getString("CodItem"));
				
				detalle.setCodItem(itemJson.getString("CodItem"));
				detalle.setTpoCod(tpoCod);
			}
			
			strategy.getCFE().getDetalle().add(detalle);
		}
	}

	@Override
	public void buildReceptor(JSONObject receptorJson, boolean esCfeEmitido) throws APIException {
		
		TipoDocumento TipoDocRecep = receptorJson.has("TipoDocRecep")? TipoDocumento.fromInt(receptorJson.getInt("TipoDocRecep")): null;

		String CodPaisRecep = receptorJson.has("CodPaisRecep") ? receptorJson.getString("CodPaisRecep") : null;

		String DocRecep = receptorJson.has("DocRecep") ? receptorJson.getString("DocRecep") : null;

		String RznSocRecep = receptorJson.has("RznSocRecep") ? receptorJson.getString("RznSocRecep") : null;

		String DirRecep = receptorJson.has("DirRecep") ? receptorJson.getString("DirRecep") : null;

		String CiudadRecep = receptorJson.has("CiudadRecep") ? receptorJson.getString("CiudadRecep") : null;
		
		String DeptoRecep = receptorJson.has("DeptoRecep") ? receptorJson.getString("DeptoRecep") : null;
		
		String mailPdfAddress = receptorJson.has("MailPdfAddress") ? receptorJson.getString("MailPdfAddress") : null;
		
		//Validar y extraeer direcciones de correos validos
	    Pattern pattern = Pattern.compile(regex);
		if (mailPdfAddress != null) {
			String[] tokens = mailPdfAddress.split("[\\s,]+");
			StringBuilder result = new StringBuilder();
			for (String string : tokens) {
				Matcher matcher = pattern.matcher(string);
				if (matcher.matches()) {
					result.append(string);
					result.append(",");
				}
			}
			mailPdfAddress = result.length() > 0 ? result.substring(0, result.length() - 1) : "";
		}
		
		boolean update = esCfeEmitido;
		
		strategy.buildReceptor(TipoDocRecep, CodPaisRecep, DocRecep, RznSocRecep, DirRecep, CiudadRecep, DeptoRecep, update, mailPdfAddress);
		
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
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TpoMoneda")
					.setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);
		strategy.getCFE().setMoneda(moneda);

		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio")){
				DecimalFormat df = new DecimalFormat("####0.000", DecimalFormatSymbols.getInstance(Locale.US));
				totales.setTpoCambio(new BigDecimal(df.format(totalesJson.getDouble("TpoCambio"))));
				strategy.getCFE().setTipoCambio(totales.getTpoCambio().doubleValue());
			}else
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("totales.TpoCambio");

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
		 * id:128 
		 */
		//TODO esto es un campo que no se bien para que sirve...
		strategy.getCFE().setTotValRetPerc(0d);
		
		
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
	public void buildIdDoc(boolean montosIncluyenIva, Integer formaPago, JSONObject idDocJson) throws APIException {
		
		if (idDocJson.has("Nro"))
			strategy.getCFE().setNro(idDocJson.getLong("Nro"));
		if (idDocJson.has("Serie"))
			strategy.getCFE().setSerie(idDocJson.getString("Serie"));
		
		if (formaPago!=null)
			strategy.getCFE().setFormaDePago(FormaDePago.fromInt(formaPago));
		
		strategy.getCFE().setIndMontoBruto(montosIncluyenIva);
		
		try {
			if (idDocJson.has("FchEmis"))
				strategy.getCFE().setFechaEmision(simpleDateFormat.parse(idDocJson.getString("FchEmis")));
			else
				strategy.getCFE().setFechaEmision(new Date());
		} catch (JSONException | ParseException e) {
			throw APIException.raise(e);
		}
	}

	@Override
	public void buildCAEData() throws APIException {
		strategy.setCAEData();
		strategy.getCFE().setCae(caeMicroController.getCAE(strategy.getCFE().getTipo()));
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
	public void buildEmisor(JSONObject emisorJson) throws APIException {
		Emisor emisor = strategy.getEmisor();
		
		emisor.setRUCEmisor(Commons.safeGetString(emisorJson, "RUCEmisor"));
		
		/*
		 * Campo Opcional
		 */
		if (emisorJson.has("NomComercial"))
			emisor.setNomComercial(Commons.safeGetString(emisorJson, "NomComercial"));

		emisor.setRznSoc(Commons.safeGetString(emisorJson, "RznSoc"));

		emisor.setCdgDGISucur(new BigInteger(Commons.safeGetString(emisorJson, "CdgDGISucur")));

		emisor.setDomFiscal(Commons.safeGetString(emisorJson, "DomFiscal"));

		emisor.setCiudad(Commons.safeGetString(emisorJson, "Ciudad"));

		emisor.setDepartamento(Commons.safeGetString(emisorJson, "Departamento"));

		Empresa empresaEmisora = Empresa.getOrCreateEmpresa(emisor.getRUCEmisor(), emisor.getRznSoc(), emisor.getDomFiscal(), emisor.getCiudad(), emisor.getDepartamento(), false); 
		
		strategy.getCFE().setEmpresaEmisora(empresaEmisora);
		
	}

	@Override
	public void buildReferencia(Empresa empresaEmisora, JSONArray referenciasJSON) throws APIException {

		try {
			if (strategy.getCFE().isObligatorioReferencia()) {

				for (int i = 0; i< referenciasJSON.length(); i++ ) {
					
					JSONObject referenciaJSON = referenciasJSON.getJSONObject(i);
				
				if (referenciaJSON == null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Referencia");

				ReferenciaTipo referenciaType = strategy.getReferenciaTipo();

				Referencia referencia = new Referencia();

				/*
				 * Campo Opcional
				 */
				if (referenciaJSON.has("FechaCFEref")) {
					Date parsedDate = simpleDateFormat.parse(referenciaJSON.getString("FechaCFEref"));
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime(parsedDate);
					referencia.setFechaCFEref(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
				}

				/*
				 * Se utiliza cuando no se puede identificar los CFE de
				 * referencia. Por ejemplo: -cuando el CFE afecta a un número
				 * de más de 40 CFE de referencia, -cuando se referencia a un
				 * documento no codificado, etc.
				 * 
				 * Se debe explicitar el motivo en "Razón Referencia" (C6)
				 */
				if (referenciaJSON.has("IndGlobal") && referenciaJSON.getInt("IndGlobal")==1){
					referencia.setIndGlobal(new BigInteger("1"));
					
					String RazonRef = "";
					if (referenciaJSON.has("RazonRef"))
						RazonRef = referenciaJSON.getString("RazonRef");
					referencia.setRazonRef(RazonRef);
					strategy.getCFE().setRazonReferencia(RazonRef);
				} else {
					referencia.setNroCFERef(new BigInteger(Commons.safeGetString(referenciaJSON, "NroCFERef")));
					referencia.setSerie(Commons.safeGetString(referenciaJSON,"Serie"));
					referencia.setTpoDocRef(new BigInteger(Commons.safeGetString(referenciaJSON,"TpoDocRef")));
					referencia.setNroLinRef(Commons.safeGetInteger(referenciaJSON, "NroLinRef"));
					
					List<CFE> cfes = CFE.findById(empresaEmisora, TipoDoc.fromInt(Commons.safeGetInteger(referenciaJSON,"TpoDocRef")), Commons.safeGetString(referenciaJSON,"Serie"), Commons.safeGetLong(referenciaJSON, "NroCFERef"), null, DireccionDocumento.EMITIDO, false);
					
					if (cfes.size()>1)
						throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",empresaEmisora.getRut()+"-"+Commons.safeGetLong(referenciaJSON, "NroCFERef")+"-"+Commons.safeGetString(referenciaJSON,"Serie")+"-"+TipoDoc.fromInt(Commons.safeGetInteger(referenciaJSON,"TpoDocRef"))).setDetailMessage("No identifica a un unico cfe");
					
					if (cfes.size()==1){
						CFE cfeReferencia = cfes.get(0);
						strategy.getCFE().setReferencia(cfeReferencia);
					}
				}
				referencia.setNroLinRef(Commons.safeGetInteger(referenciaJSON, "NroLinRef"));
				

				referenciaType.getReferencias().add(referencia);
				}
			}
		} catch (JSONException | ParseException | DatatypeConfigurationException e) {
			throw APIException.raise(e);
		} 
	}

	@Override
	public void buildTimestampFirma(Long timestamp) throws APIException {
		try {
			
			GregorianCalendar cal = new GregorianCalendar();
			if (timestamp!=null)
				cal.setTime(new Date(timestamp));
			XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
			strategy.setTimestampFirma(xmlCal);
				
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

	@Override
	public void buildComplementoFiscal(JSONObject complementoFiscalJson) throws APIException {
		
	}

}

package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ReferenciaType;
import dgi.classes.recepcion.ReferenciaType.Referencia;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public class CFEBuiderImpl implements CFEBuiderInterface {

	protected CFEStrategy strategy;

	public CFEBuiderImpl(CFEDefType.EFact efactura, TipoDoc tipo) throws EFacturaException {
		this.strategy = (new CFEStrategy.Builder()).withEfact(efactura).withTipo(tipo).build();
	}

	public CFEBuiderImpl(ETck eticket, TipoDoc tipo) throws EFacturaException {
		this.strategy = (new CFEStrategy.Builder()).withEtick(eticket).withTipo(tipo).build();
	}
	
	protected CFEBuiderImpl(){
		
	}

	@Override
	public void buildDetalle(JSONArray detalleJson) throws EFacturaException {

		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemInterface item = strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);

			item.setNroLinDet(i);

			if (itemJson.optString("NomItem") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("NomItem");
			item.setNomItem(itemJson.getString("NomItem"));

			if (itemJson.optString("IndFact") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("IndFact");
			item.setIndFact(new BigInteger(itemJson.getString("IndFact")));

			if (itemJson.optString("Cantidad") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Cantidad");
			item.setCantidad(new BigDecimal(itemJson.getString("Cantidad")));

			if (itemJson.optString("UniMed") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("UniMed");
			item.setUniMed(itemJson.getString("UniMed"));

			if (itemJson.optString("PrecioUnitario") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("PrecioUnitario");
			item.setPrecioUnitario(new BigDecimal(itemJson.getString("PrecioUnitario")));

			item.setMontoItem(new BigDecimal(itemJson.getString("MontoItem")));
		}
	}

	@Override
	public void buildReceptor(JSONObject receptorJson) throws EFacturaException {
		ReceptorInterface receptor = strategy.getReceptor();

		if (receptorJson.optInt("TipoDocRecep") == 0)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("TipoDocRecep");
		receptor.setTipoDocRecep(receptorJson.getInt("TipoDocRecep"));

		if (receptorJson.optString("CodPaisRecep") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CodPaisRecep");
		receptor.setCodPaisRecep(receptorJson.getString("CodPaisRecep"));

		if (receptorJson.optString("DocRecep") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DocRecep");
		receptor.setDocRecep(receptorJson.getString("DocRecep"));

		if (receptorJson.optString("RznSocRecep") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSocRecep");
		receptor.setRznSocRecep(receptorJson.getString("RznSocRecep"));

		if (receptorJson.optString("DirRecep") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DirRecep");
		receptor.setDirRecep(receptorJson.getString("DirRecep"));

		if (receptorJson.optString("CiudadRecep") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CiudadRecep");
		receptor.setCiudadRecep(receptorJson.getString("CiudadRecep"));

	}

	@Override
	public void buildTotales(JSONObject totalesJson) throws EFacturaException {
		List<ItemInterface> items = strategy.getItem();

		TotalesInterface totales = strategy.getTotales();

		/*
		 * Moneda
		 */
		TipMonType moneda = TipMonType.fromValue(totalesJson.getString("TpoMoneda"));

		if (moneda == null)
			throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
					.setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);

		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio"))
				totales.setTpoCambio(new BigDecimal(totalesJson.getString("TpoCambio")));
			else
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER)
						.setDetailMessage("Falta el parametro totales.TpoCambio");

		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(items.size());

		/*
		 * IVA
		 */
		BigDecimal[] iva = new BigDecimal[13];

		for (int i = 0; i < 13; i++) {
			iva[i] = BigDecimal.ZERO;
		}

		for (Iterator<ItemInterface> iterator = items.iterator(); iterator.hasNext();) {
			ItemInterface item = iterator.next();
			BigDecimal itemMonto = item.getPrecioUnitario().multiply(item.getCantidad());
			iva[item.getIndFact().intValue()] = itemMonto.add(iva[item.getIndFact().intValue()]);

		}

		for (int i = 0; i < 13; i++) {
			if (iva[i] != BigDecimal.ZERO && i != INDICADOR_FACTURACION_EXCENTO_IVA
					&& i != INDICADOR_FACTURACION_IVA_OTRA_TASA && i != INDICADOR_FACTURACION_IVA_TASA_BASICA
					&& i != INDICADOR_FACTURACION_IVA_TASA_MINIMA)
				throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
						.setDetailMessage("El indicador de facturacion (IndFact=" + i + ") no esta soportado");
		}

		/*
		 * id:112
		 */
		totales.setMntNoGrv(iva[INDICADOR_FACTURACION_EXCENTO_IVA]);

		/*
		 * id:113
		 */
		totales.setMntExpoyAsim(new BigDecimal("0"));

		/*
		 * id:114
		 */
		totales.setMntImpuestoPerc(new BigDecimal("0"));

		/*
		 * id:115
		 */
		totales.setMntIVaenSusp(new BigDecimal("0"));

		/*
		 * id:116
		 */
		totales.setMntNetoIvaTasaMin(iva[INDICADOR_FACTURACION_IVA_TASA_MINIMA]);

		/*
		 * id:117
		 */
		totales.setMntNetoIVATasaBasica(iva[INDICADOR_FACTURACION_IVA_TASA_BASICA]);

		/*
		 * id:118
		 */
		totales.setMntNetoIVAOtra(iva[INDICADOR_FACTURACION_IVA_OTRA_TASA]);

		/*
		 * id:119
		 */
		totales.setIVATasaMin(new BigDecimal("10"));

		/*
		 * id:120
		 */
		totales.setIVATasaBasica(new BigDecimal("22"));

		/*
		 * id:121
		 */
		totales.setMntIVATasaMin(
				totales.getIVATasaMin().multiply(totales.getMntNetoIvaTasaMin().divide(new BigDecimal("100"))));

		/*
		 * id:122
		 */
		totales.setMntIVATasaBasica(
				totales.getIVATasaBasica().multiply(totales.getMntNetoIVATasaBasica().divide(new BigDecimal("100"))));

		/*
		 * id:123
		 */
		totales.setMntIVAOtra(
				(new BigDecimal("1")).multiply(totales.getMntNetoIVAOtra().divide(new BigDecimal("100"))));

		BigDecimal mntTotal = (new BigDecimal("0")).add(totales.getMntNoGrv()).add(totales.getMntExpoyAsim())
				.add(totales.getMntImpuestoPerc()).add(totales.getMntIVaenSusp()).add(totales.getMntNetoIvaTasaMin())
				.add(totales.getMntNetoIVATasaBasica()).add(totales.getMntNetoIVAOtra());

		mntTotal = mntTotal.add(totales.getMntIVATasaMin()).add(totales.getMntIVATasaBasica())
				.add(totales.getMntIVAOtra());

		totales.setMntTotal(mntTotal);

		totales.setMntPagar(mntTotal);

	}

	@Override
	public void buildIdDoc() throws EFacturaException {
		strategy.setIdDoc();
	}

	@Override
	public void buildCAEData() throws EFacturaException {
		strategy.setCAEData();
	}

	@Override
	public void buildEmisor(JSONObject emisorJson) throws EFacturaException {
		Emisor emisor = strategy.getEmisor();

		if (emisorJson.optString("RUCEmisor") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RUCEmisor");
		emisor.setRUCEmisor(emisorJson.getString("RUCEmisor"));

		if (emisorJson.optString("RznSoc") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSoc");
		emisor.setRznSoc(emisorJson.getString("RznSoc"));

		if (emisorJson.optString("CdgDGISucur") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CdgDGISucur");
		emisor.setCdgDGISucur(new BigInteger(emisorJson.getString("CdgDGISucur")));

		if (emisorJson.optString("DomFiscal") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DomFiscal");
		emisor.setDomFiscal(emisorJson.getString("DomFiscal"));

		if (emisorJson.optString("Ciudad") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Ciudad");
		emisor.setCiudad(emisorJson.getString("Ciudad"));

		if (emisorJson.optString("Departamento") == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Departamento");
		emisor.setDepartamento(emisorJson.getString("Departamento"));

	}

	@Override
	public void buildReferencia(JSONObject referenciaJSON) throws EFacturaException {
		
		try {
			ReferenciaType referenciaType = strategy.getReferenciaType();
			
			Referencia referencia = new Referencia();
			
			if (referenciaJSON.optString("FechaCFEref") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("FechaCFEref");
			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date parsedDate =  new SimpleDateFormat("yyyyMMdd").parse(referenciaJSON.getString("FechaCFEref"));
			referencia.setFechaCFEref(DatatypeFactory.newInstance().newXMLGregorianCalendar(outputFormat.format(parsedDate)));			
			
			
			/*
			 * Se utiliza cuando no se puede identificar los CFE de referencia.
			 * Por ejemplo:
			 * -cuando el CFE afecta a un número de más de 40 CFE de referencia, 
			 * -cuando se referencia a un documento no codificado, etc. 
			 * 
			 * Se debe explicitar el motivo en "Razón Referencia" (C6) 
			 */
			//referencia.setIndGlobal(new BigInteger("1"));
			//referencia.setRazonRef("razon");
			
			if (referenciaJSON.optString("NroCFERef") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("NroCFERef");
			referencia.setNroCFERef(new BigInteger(referenciaJSON.getString("NroCFERef")));
			
			referencia.setNroLinRef(1);
			
			if (referenciaJSON.optString("Serie") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Serie");
			referencia.setSerie(referenciaJSON.getString("Serie"));
			
			if (referenciaJSON.optString("TpoDocRef") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("TpoDocRef");
			referencia.setTpoDocRef(new BigInteger(referenciaJSON.getString("TpoDocRef")));
			
			referenciaType.getReferencia().add(referencia);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void buildTimestampFirma() throws EFacturaException {
		try {
			strategy.setTimestampFirma(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		} catch (DatatypeConfigurationException e) {
			throw EFacturaException.raise(e);
		}
		
	}

	@Override
	public Object getCFE() {
		
		return strategy.getCFE();
	}

}

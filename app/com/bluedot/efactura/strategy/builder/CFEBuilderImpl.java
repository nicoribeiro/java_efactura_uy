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

import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
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

public class CFEBuilderImpl implements CFEBuiderInterface {

	protected CFEStrategy strategy;

	public CFEBuilderImpl(CFEDefType.EFact efactura, TipoDoc tipo) throws EFacturaException {
		this.strategy = (new CFEStrategy.Builder()).withEfact(efactura).withTipo(tipo).build();
	}

	public CFEBuilderImpl(ETck eticket, TipoDoc tipo) throws EFacturaException {
		this.strategy = (new CFEStrategy.Builder()).withEtick(eticket).withTipo(tipo).build();
	}

	protected CFEBuilderImpl() {

	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws EFacturaException {

		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemInterface item = strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);

			item.setNroLinDet(i);

			if (!itemJson.has("NomItem"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("NomItem");
			item.setNomItem(itemJson.getString("NomItem"));

			if (!itemJson.has("IndFact"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("IndFact");
			item.setIndFact(new BigInteger(itemJson.getString("IndFact")));

			if (!itemJson.has("Cantidad"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Cantidad");
			item.setCantidad(new BigDecimal(itemJson.getString("Cantidad")));

			if (!itemJson.has("UniMed"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("UniMed");
			item.setUniMed(itemJson.getString("UniMed"));

			if (!itemJson.has("PrecioUnitario"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("PrecioUnitario");
			item.setPrecioUnitario(new BigDecimal(itemJson.getString("PrecioUnitario")));

			if (!itemJson.has("MontoItem"))
				item.setMontoItem(item.getPrecioUnitario().multiply(item.getCantidad()));
			else
				item.setMontoItem(new BigDecimal(itemJson.getString("MontoItem")));
		}
	}

	@Override
	public void buildReceptor(JSONObject receptorJson) throws EFacturaException {
		ReceptorInterface receptor = strategy.getReceptor();

		if (!receptorJson.has("TipoDocRecep"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("TipoDocRecep");
		receptor.setTipoDocRecep(receptorJson.getInt("TipoDocRecep"));

		if (!receptorJson.has("CodPaisRecep"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CodPaisRecep");
		receptor.setCodPaisRecep(receptorJson.getString("CodPaisRecep"));

		if (!receptorJson.has("DocRecep"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DocRecep");
		receptor.setDocRecep(receptorJson.getString("DocRecep"));

		if (!receptorJson.has("RznSocRecep"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSocRecep");
		receptor.setRznSocRecep(receptorJson.getString("RznSocRecep"));

		if (strategy.esMandatoriaDirRecep()){
			if (!receptorJson.has("DirRecep"))
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DirRecep");
			receptor.setDirRecep(receptorJson.getString("DirRecep"));
		}
		
		if (receptorJson.has("CiudadRecep"))
			//throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CiudadRecep");
			receptor.setCiudadRecep(receptorJson.getString("CiudadRecep"));

	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws EFacturaException {
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
					&& i != INDICADOR_FACTURACION_IVA_TASA_MINIMA && i != INDICADOR_FACTURACION_NO_FACTURABLE
					&& i != INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO)
				throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
						.setDetailMessage("El indicador de facturacion (IndFact=" + i + ") no esta soportado");
		}
		
		/*
		 * id:119
		 */
		totales.setIVATasaMin(new BigDecimal("10"));

		/*
		 * id:120
		 */
		totales.setIVATasaBasica(new BigDecimal("22"));
		

		/*
		 * id:112
		 */
		totales.setMntNoGrv(iva[INDICADOR_FACTURACION_EXCENTO_IVA].setScale(2, BigDecimal.ROUND_HALF_UP));

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
		if (montosIncluyenIva){
			BigDecimal divisor = (new BigDecimal(1)).add(totales.getIVATasaMin().divide(new BigDecimal("100")));
			totales.setMntNetoIvaTasaMin((iva[INDICADOR_FACTURACION_IVA_TASA_MINIMA].divide(divisor, 2, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP));
		}else
			totales.setMntNetoIvaTasaMin(iva[INDICADOR_FACTURACION_IVA_TASA_MINIMA].setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * id:117
		 */
		if (montosIncluyenIva){
			BigDecimal divisor = (new BigDecimal(1)).add(totales.getIVATasaBasica().divide(new BigDecimal("100")));
			totales.setMntNetoIVATasaBasica(
					(iva[INDICADOR_FACTURACION_IVA_TASA_BASICA].divide(divisor, 2, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP));
		}else
			totales.setMntNetoIVATasaBasica(
				iva[INDICADOR_FACTURACION_IVA_TASA_BASICA].setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * id:118
		 */
		//TODO otra tasa no se sabe cual es para dividir como en id:117 o id:116
		if (montosIncluyenIva)
			totales.setMntNetoIVAOtra(iva[INDICADOR_FACTURACION_IVA_OTRA_TASA].setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			totales.setMntNetoIVAOtra(iva[INDICADOR_FACTURACION_IVA_OTRA_TASA].setScale(2, BigDecimal.ROUND_HALF_UP));

		

		/*
		 * id:121
		 */
		if (montosIncluyenIva)
			totales.setMntIVATasaMin(
					iva[INDICADOR_FACTURACION_IVA_TASA_MINIMA].subtract(totales.getMntNetoIvaTasaMin()).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			totales.setMntIVATasaMin(
					totales.getIVATasaMin().multiply((totales.getMntNetoIvaTasaMin().divide(new BigDecimal("100"))))
							.setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * id:122
		 */
		if (montosIncluyenIva)
			totales.setMntIVATasaBasica(
					iva[INDICADOR_FACTURACION_IVA_TASA_BASICA].subtract(totales.getMntNetoIVATasaBasica()).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
		totales.setMntIVATasaBasica(
				totales.getIVATasaBasica().multiply((totales.getMntNetoIVATasaBasica().divide(new BigDecimal("100"))))
						.setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * id:123
		 */
		//TODO ver que no se toma en cuenta si los montosIncluyenIva
		totales.setMntIVAOtra(
				(new BigDecimal("1")).multiply((totales.getMntNetoIVAOtra().divide(new BigDecimal("100")))).setScale(2,
						BigDecimal.ROUND_HALF_UP));

		

		/*
		 * id:124
		 */
		BigDecimal mntTotal = (new BigDecimal("0")).add(totales.getMntNoGrv()).add(totales.getMntExpoyAsim())
				.add(totales.getMntImpuestoPerc()).add(totales.getMntIVaenSusp()).add(totales.getMntNetoIvaTasaMin())
				.add(totales.getMntNetoIVATasaBasica()).add(totales.getMntNetoIVAOtra());

		mntTotal = mntTotal.add(totales.getMntIVATasaMin()).add(totales.getMntIVATasaBasica())
				.add(totales.getMntIVAOtra());
		totales.setMntTotal(mntTotal.setScale(2, BigDecimal.ROUND_HALF_UP));
		
		/*
		 * id:125 
		 */
		//TODO esto es distinto de cero cuando se hace una retencion (resguardo no?)
		totales.setMntTotRetenido(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP));
		
		/*
		 * id:129
		 */
		totales.setMontoNF(iva[INDICADOR_FACTURACION_NO_FACTURABLE].subtract(iva[INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO]).setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * id:130
		 */
		totales.setMntPagar(mntTotal.add(totales.getMontoNF()).add(totales.getMntTotRetenido()).setScale(2, BigDecimal.ROUND_HALF_UP));

	}

	@Override
	public void buildIdDoc(boolean montosIncluyenIva, int formaPago) throws EFacturaException {
		strategy.setIdDoc(montosIncluyenIva, formaPago);
	}

	@Override
	public void buildCAEData() throws EFacturaException {
		strategy.setCAEData();
	}

	@Override
	public void buildEmisor(JSONObject emisorJson) throws EFacturaException {
		Emisor emisor = strategy.getEmisor();

		if (!emisorJson.has("RUCEmisor"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RUCEmisor");
		emisor.setRUCEmisor(emisorJson.getString("RUCEmisor"));

		if (!emisorJson.has("RznSoc"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSoc");
		emisor.setRznSoc(emisorJson.getString("RznSoc"));

		if (!emisorJson.has("CdgDGISucur"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CdgDGISucur");
		emisor.setCdgDGISucur(new BigInteger(emisorJson.getString("CdgDGISucur")));

		if (!emisorJson.has("DomFiscal"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DomFiscal");
		emisor.setDomFiscal(emisorJson.getString("DomFiscal"));

		if (!emisorJson.has("Ciudad"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Ciudad");
		emisor.setCiudad(emisorJson.getString("Ciudad"));

		if (!emisorJson.has("Departamento"))
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Departamento");
		emisor.setDepartamento(emisorJson.getString("Departamento"));

	}

	@Override
	public void buildReferencia(JSONObject referenciaJSON) throws EFacturaException {

		try {
			ReferenciaType referenciaType = strategy.getReferenciaType();

			Referencia referencia = new Referencia();

			// Campo Opcional
			if (referenciaJSON.has("FechaCFEref")) {
				SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date parsedDate = new SimpleDateFormat("yyyyMMdd").parse(referenciaJSON.getString("FechaCFEref"));
				referencia.setFechaCFEref(
						DatatypeFactory.newInstance().newXMLGregorianCalendar(outputFormat.format(parsedDate)));
			}

			/*
			 * Se utiliza cuando no se puede identificar los CFE de referencia.
			 * Por ejemplo: -cuando el CFE afecta a un número de más de 40 CFE
			 * de referencia, -cuando se referencia a un documento no
			 * codificado, etc.
			 * 
			 * Se debe explicitar el motivo en "Razón Referencia" (C6)
			 */
			// referencia.setIndGlobal(new BigInteger("1"));
			// referencia.setRazonRef("razon");

			if (referenciaJSON.has("IndGlobal")) {
				if (!referenciaJSON.has("RazonRef"))
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RazonRef");
				referencia.setRazonRef(referenciaJSON.getString("RazonRef"));

				if (referenciaJSON.getInt("IndGlobal") != 1)
					throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("IndGlobal");
				referencia.setIndGlobal(new BigInteger(referenciaJSON.getString("IndGlobal")));

			} else {

				if (!referenciaJSON.has("NroCFERef"))
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("NroCFERef");
				referencia.setNroCFERef(new BigInteger(referenciaJSON.getString("NroCFERef")));

				if (!referenciaJSON.has("Serie"))
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Serie");
				referencia.setSerie(referenciaJSON.getString("Serie"));

				if (!referenciaJSON.has("TpoDocRef"))
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("TpoDocRef");
				referencia.setTpoDocRef(new BigInteger(referenciaJSON.getString("TpoDocRef")));
			}

			referencia.setNroLinRef(1);

			referenciaType.getReferencia().add(referencia);
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

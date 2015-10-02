package com.bluedot.efactura.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.efactura.CFEController;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.CFEDefType.EFact.Detalle;
import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ReceptorFact;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.Totales;

public class CFEControllerImpl implements CFEController
{

	public static int INDICADOR_FACTURACION_EXCENTO_IVA = 1;
	public static int INDICADOR_FACTURACION_IVA_TASA_MINIMA = 2;
	public static int INDICADOR_FACTURACION_IVA_TASA_BASICA = 3;
	public static int INDICADOR_FACTURACION_IVA_OTRA_TASA = 4;
	public static int INDICADOR_FACTURACION_ENTREGA_GRATUITA = 5;
	public static int INDICADOR_FACTURACION_NO_FACTURABLE = 6;
	public static int INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO = 7;
	public static int INDICADOR_FACTURACION_REBAJAR_EN_REMITOS = 8;
	public static int INDICADOR_FACTURACION_ANULAR_EN_RESGUARDOS = 9;
	public static int INDICADOR_FACTURACION_EXPORTACION_Y_ASIMILADAS = 10;
	public static int INDICADOR_FACTURACION_IMPUESTO_PERCIBIDO = 11;
	public static int INDICADOR_FACTURACION_IVA_SUSPENSO = 12;

	/* (non-Javadoc)
	 * @see com.bluedot.efactura.EFacturaController#createEfactura(org.json.JSONObject)
	 */
	@Override
	public CFEDefType.EFact createEfactura(JSONObject factura) throws EFacturaException
	{

		try
		{
			CFEDefType.EFact efact = new EFact();
			
			efact.setTmstFirma(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			

			/*
			 * Detalle (lineas de factura)
			 */
			if (factura.optJSONArray("Detalle")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Detalle");
			
			CFEDefType.EFact.Detalle detalle = getDetalle(factura.getJSONArray("Detalle"));
			efact.setDetalle(detalle);
			
			/*
			 * Encabezado
			 */
			if (factura.optJSONObject("Encabezado")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Encabezado");
			JSONObject encabezadoJSON = factura.getJSONObject("Encabezado");
			
			
			CFEDefType.EFact.Encabezado encabezado = new CFEDefType.EFact.Encabezado();
			encabezado.setEmisor(getEmisor(Commons.safeGetJSONObject(encabezadoJSON,"Emisor")));
			
			if (encabezadoJSON.optJSONObject("Receptor")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Receptor");
			encabezado.setReceptor(getReceptor(encabezadoJSON.optJSONObject(("Receptor"))));
			
			if (encabezadoJSON.optJSONObject("Totales")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Totales");
			encabezado.setTotales(getTotales(encabezadoJSON.getJSONObject("Totales"), detalle));
			
			encabezado.setIdDoc(CAEManagerImpl.getInstance().getIdDocFact());
			efact.setEncabezado(encabezado);
			
			
			/*
			 * CAEData
			 */
			efact.setCAEData(CAEManagerImpl.getInstance().getCaeData(TipoDoc.eFactura));
			
			
			
			return efact;
			
		} catch (JSONException e)
		{
			e.printStackTrace();
			throw  EFacturaException.raise(e);
		} catch (DatatypeConfigurationException e)
		{
			e.printStackTrace();
			throw  EFacturaException.raise(e);
		} catch (IOException e)
		{
			e.printStackTrace();
			throw  EFacturaException.raise(e);
		}

	}

	private Totales getTotales(JSONObject totalesJson, CFEDefType.EFact.Detalle detalle) throws EFacturaException
	{
		Totales totales = new Totales();

		// <ns0:TpoMoneda>UYU</ns0:TpoMoneda>
		// <ns0:MntNoGrv>5280</ns0:MntNoGrv>
		// <ns0:MntTotal>5280</ns0:MntTotal>
		// <ns0:CantLinDet>3</ns0:CantLinDet>
		// <ns0:MntPagar>5280</ns0:MntPagar>
		
		/*
		 * Moneda
		 */
		TipMonType moneda = TipMonType.fromValue(totalesJson.getString("TpoMoneda"));

		if (moneda == null)
			throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);

		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio"))
				totales.setTpoCambio(new BigDecimal(totalesJson.getString("TpoCambio")));
			else
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Falta el parametro totales.TpoCambio");
		
		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(detalle.getItem().size());
		
		
		/*
		 * IVA
		 */
		BigDecimal[] iva = new BigDecimal[13];

		for (int i = 0; i < 13; i++)
		{
			iva[i] = BigDecimal.ZERO;
		}

		for (Iterator<ItemDetFact> iterator = detalle.getItem().iterator(); iterator.hasNext();)
		{
			ItemDetFact item = iterator.next();
			iva[item.getIndFact().intValue()] = (item.getPrecioUnitario().multiply(item.getCantidad())).add(iva[item.getNroLinDet()]);

		}

		for (int i = 0; i < 13; i++)
		{
			if (iva[i] != BigDecimal.ZERO && i != INDICADOR_FACTURACION_EXCENTO_IVA && i != INDICADOR_FACTURACION_IVA_OTRA_TASA && i != INDICADOR_FACTURACION_IVA_TASA_BASICA
					&& i != INDICADOR_FACTURACION_IVA_TASA_MINIMA)
				throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("El indicador de facturacion (IndFact=" + i + ") no esta soportado");
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
		totales.setMntIVATasaMin(totales.getIVATasaMin().multiply(totales.getMntNetoIvaTasaMin()));
		
		/*
		 * id:122
		 */
		totales.setMntIVATasaBasica(totales.getIVATasaBasica().multiply(totales.getMntNetoIVATasaBasica()));
		
		/*
		 * id:123
		 */
		totales.setMntIVAOtra((new BigDecimal("1")).multiply(totales.getMntNetoIVAOtra()));
		
		
		BigDecimal mntTotal = (new BigDecimal("0")).add(totales.getMntNoGrv()).add(totales.getMntExpoyAsim()).add(totales.getMntImpuestoPerc()).add(totales.getMntIVaenSusp()).add(totales.getMntNetoIvaTasaMin()).add(totales.getMntNetoIVATasaBasica()).add(totales.getMntNetoIVAOtra());
		
		mntTotal.add(totales.getMntIVATasaMin()).add(totales.getMntIVATasaBasica()).add(totales.getMntIVAOtra());
		
		totales.setMntTotal(mntTotal);
		
		totales.setMntPagar(mntTotal);
		
		return totales;
	}

	private Detalle getDetalle(JSONArray detalleJson) throws EFacturaException
	{
		CFEDefType.EFact.Detalle detalle = new CFEDefType.EFact.Detalle();

		for (int i = 1; i <= detalleJson.length(); i++)
		{
			ItemDetFact item = new ItemDetFact();
			JSONObject itemJson = detalleJson.getJSONObject(i-1);

			item.setNroLinDet(i);
			
			if (itemJson.optString("NomItem")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("NomItem");
			item.setNomItem(itemJson.getString("NomItem"));
			
			if (itemJson.optString("IndFact")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("IndFact");
			item.setIndFact(new BigInteger(itemJson.getString("IndFact")));
			
			if (itemJson.optString("Cantidad")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Cantidad");
			item.setCantidad(new BigDecimal(itemJson.getString("Cantidad")));
			
			if (itemJson.optString("UniMed")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("UniMed");
			item.setUniMed(itemJson.getString("UniMed"));
			
			if (itemJson.optString("PrecioUnitario")==null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("PrecioUnitario");
			item.setPrecioUnitario(new BigDecimal(itemJson.getString("PrecioUnitario")));
			
			item.setMontoItem(new BigDecimal(itemJson.getString("MontoItem")));
			
			detalle.getItem().add(item);
		}

		return detalle;
	}

	private ReceptorFact getReceptor(JSONObject receptorJson) throws EFacturaException
	{
		ReceptorFact receptor = new ReceptorFact();

		if (receptorJson.optInt("TipoDocRecep")==0)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("TipoDocRecep");
		receptor.setTipoDocRecep(receptorJson.getInt("TipoDocRecep"));
		
		if (receptorJson.optString("CodPaisRecep")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CodPaisRecep");
		receptor.setCodPaisRecep(receptorJson.getString("CodPaisRecep"));
		
		if (receptorJson.optString("DocRecep")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DocRecep");
		receptor.setDocRecep(receptorJson.getString("DocRecep"));
		
		if (receptorJson.optString("RznSocRecep")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSocRecep");
		receptor.setRznSocRecep(receptorJson.getString("RznSocRecep"));
		
		if (receptorJson.optString("DirRecep")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DirRecep");
		receptor.setDirRecep(receptorJson.getString("DirRecep"));
		
		if (receptorJson.optString("CiudadRecep")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CiudadRecep");
		receptor.setCiudadRecep(receptorJson.getString("CiudadRecep"));

		return receptor;
	}

	private Emisor getEmisor(JSONObject emisorJson) throws EFacturaException
	{
		Emisor emisor = new Emisor();
		
		if (emisorJson.optString("RUCEmisor")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RUCEmisor");
		emisor.setRUCEmisor(emisorJson.getString("RUCEmisor"));
		
		if (emisorJson.optString("RznSoc")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("RznSoc");
		emisor.setRznSoc(emisorJson.getString("RznSoc"));
		
		if (emisorJson.optString("CdgDGISucur")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CdgDGISucur");
		emisor.setCdgDGISucur(new BigInteger(emisorJson.getString("CdgDGISucur")));
		
		if (emisorJson.optString("DomFiscal")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("DomFiscal");
		emisor.setDomFiscal(emisorJson.getString("DomFiscal"));
		
		if (emisorJson.optString("Ciudad")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Ciudad");
		emisor.setCiudad(emisorJson.getString("Ciudad"));
		
		if (emisorJson.optString("Departamento")==null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Departamento");
		emisor.setDepartamento(emisorJson.getString("Departamento"));
		return emisor;
	}

	@Override
	public ETck createETicket(JSONObject ticket) throws EFacturaException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ERem createERemito(JSONObject remito) throws EFacturaException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EResg createEResguardo(JSONObject resguardo) throws EFacturaException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.bluedot.efactura.EFacturaController#createERemitoExportacion(org.json.JSONObject)
	 */
	@Override
	public ERemExp createERemitoExportacion(JSONObject remito) throws EFacturaException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.bluedot.efactura.EFacturaController#createEFacturaExportacion(org.json.JSONObject)
	 */
	@Override
	public EFactExp createEFacturaExportacion(JSONObject factura) throws EFacturaException
	{
		// TODO Auto-generated method stub
		return null;
	}

}

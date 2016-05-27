package com.bluedot.efactura.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.DateHandler;
import com.bluedot.commons.IO;
import com.bluedot.commons.Settings;
import com.bluedot.efactura.CAEManager;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

public class CAEManagerImpl implements CAEManager
{
	private static CAEManager instance = null;

	private HashMap<Integer, JSONObject> caesMap;

	private CAEManagerImpl() throws IOException, EFacturaException {
		refreshMap();
	}
	
	public synchronized void refreshMap()throws IOException, EFacturaException {
		caesMap = new HashMap<Integer, JSONObject>();
		String filepath = Settings.getInstance().getString(Constants.CAEs_FILE);
		if (filepath != null)
		{
			String data = IO.readFile(filepath, Charset.forName("UTF-8"));
			JSONObject caesObject = new JSONObject(data);
			if (caesObject.optJSONArray("data") == null)
			{
				throw EFacturaException.raise(EFacturaErrors.ERROR_IN_CAE_FILE);
			} else
			{
				JSONArray caesArray = caesObject.optJSONArray("data");
				for (int i = 0; i < caesArray.length(); i++)
				{
					JSONObject cae = caesArray.getJSONObject(i);
					caesMap.put(cae.getInt("TipoDoc"), cae);
				}
			}
		}
	}

	public static synchronized CAEManager getInstance() throws IOException, EFacturaException
	{
		if (instance == null)
		{
			instance = new CAEManagerImpl();
		}

		return instance;
	}

	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}

	public enum TipoDoc {
		eTicket(101, "eTicket"), Nota_de_Credito_de_eTicket(102,"Nota de Credito de eTicket"), Nota_de_Debito_de_eTicket(103,"Nota de Debito de eTicket"), eFactura(111,"eFactura"), Nota_de_Credito_de_eFactura(112,"Nota de Credito de eFactura"), Nota_de_Debito_de_eFactura(113,"Nota de Debito de eFactura"), eFactura_Exportacion(121,"eFactura Exportacion"), Nota_de_Credito_de_eFactura_Exportacion(
				122,"Nota de Credito de eFactura Exportacion"), Nota_de_Debito_de_eFactura_Exportacion(123,"Nota de Debito de eFactura Exportacion"), eRemito_de_Exportacion(124,"eRemito de Exportacion"), eTicket_Venta_por_Cuenta_Ajena(131,"eTicket Venta por Cuenta Ajena"), Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena(132,"Nota de Credito de eTicket Venta por Cuenta Ajena"), Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena(
				133,"Nota de Debito de eTicket Venta por Cuenta Ajena"), eFactura_Venta_por_Cuenta_Ajena(141, "eFactura Venta por Cuenta Ajena"), Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena(142,"Nota de Credito de eFactura Venta por Cuenta Ajena"), Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena(143,"Nota de Debito de eFactura Venta por Cuenta Ajena"), eRemito(181,"eRemito"), eResguardo(
				182,"eResguardo"),

		eTicket_Contingencia(201,"eTicket Contingencia"), Nota_de_Credito_de_eTicket_Contingencia(202,"Nota de Credito de eTicket Contingencia"), Nota_de_Debito_de_eTicket_Contingencia(203,"Nota de Debito de eTicket Contingencia"), eFactura_Contingencia(211,"eFactura Contingencia"), Nota_de_Credito_de_eFactura_Contingencia(212,"Nota de Credito de eFactura Contingencia"), Nota_de_Debito_de_eFactura_Contingencia(
				213,"Nota de Debito de eFactura Contingencia"), eFactura_Exportacion_Contingencia(221,"eFactura Exportacion Contingencia"), Nota_de_Credito_de_eFactura_Exportacion_Contingencia(222,"Nota de Credito de eFactura Exportacion Contingencia"), Nota_de_Debito_de_eFactura_Exportacion_Contingencia(223,"Nota de Debito de eFactura Exportacion Contingencia"), eRemito_de_Exportacion_Contingencia(
				224,"eRemito de Exportacion Contingencia"), eTicket_Venta_por_Cuenta_Ajena_Contingencia(231,"eTicket Venta por Cuenta Ajena Contingencia"), Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(232,"Nota de Credito de eTicket Venta por Cuenta Ajena Contingencia"), Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(
				233,"Nota de Debito de eTicket Venta por Cuenta Ajena Contingencia"), eFactura_Venta_por_Cuenta_Ajena_Contingencia(241,"eFactura Venta por Cuenta Ajena Contingencia"), Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(242,"Nota de Credito de eFactura Venta por Cuenta Ajena Contingencia"), Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(
				243,"Nota de Debito de eFactura Venta por Cuenta Ajena Contingencia"), eRemito_Contingencia(281,"eRemito Contingencia"), eResguardo_Contingencia(282,"eResguardo Contingencia");

		public int value;
		public String friendlyName;

		private static Map<Integer, TipoDoc> idMap = new TreeMap<Integer,TipoDoc>();
		
		static {
	        for (TipoDoc tipoDoc : values()) {
	        	//values()[i].value = START_VALUE + i;
	            idMap.put(tipoDoc.value, tipoDoc);
	        }
	    }
		
		 public static TipoDoc fromInt(int i) {
		        return idMap.get(i);
		 }
		
		TipoDoc(int value, String friendlyName) {
			this.value = value;
			this.friendlyName = friendlyName;
		}
		

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bluedot.efactura.CAEManagers#getIdDoc()
	 */

	

	private synchronized String consumeId(JSONObject cae) throws IOException, JSONException, EFacturaException
	{
		String idString = cae.getString("nextId");
		String HNroString = cae.getString("HNro");

		Integer idInt = Integer.valueOf(idString);
		Integer HNroInt = Integer.valueOf(HNroString);

		if (HNroInt == idInt)
			throw EFacturaException.raise(EFacturaErrors.CAE_NOT_AVAILABLE_ID).setDetailMessage("CAEID:" + cae.getString("CAEID") + " TipoDoc:" + cae.getString("TipoDoc"));

		cae.put("nextId", String.valueOf(++idInt));
		flushCaes();
		return idString;
	}

	private synchronized void flushCaes() throws IOException
	{
		String filepath = Settings.getInstance().getString(Constants.CAEs_FILE);

		JSONArray caes = new JSONArray();

		for (JSONObject cae : caesMap.values())
		{
			caes.put(cae);
		}

		JSONObject data = new JSONObject();

		data.put("data", caes);

		IO.writeFile(filepath, data.toString());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.bluedot.efactura.CAEManagers#getCaeData(com.bluedot.efactura.CAEManager
	 * .TipoDoc)
	 */
	@Override
	public synchronized CAEDataType getCaeData(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException, JSONException, ParseException
	{

		CAEDataType cae = new CAEDataType();
		if (!caesMap.containsKey(tipoDoc.value))
			throw EFacturaException.raise(EFacturaErrors.CAE_DATA_NOT_FOUND).setDetailMessage("Tipo de documento: " + tipoDoc.name() + " id:" + tipoDoc.value);
		JSONObject caeJson = caesMap.get(tipoDoc.value);

		cae.setCAEID(new BigInteger(caeJson.getString("CAEID")));
		cae.setDNro(new BigInteger(caeJson.getString("DNro")));
		cae.setHNro(new BigInteger(caeJson.getString("HNro")));

		/*
		 * Fecha
		 */
		GregorianCalendar c = new GregorianCalendar();
		c.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		c.setTime(formatter.parse(caeJson.getString("FecVenc")));
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		cae.setFecVenc(date);

		return cae;

	}
	
	@Override
	public synchronized JSONObject getCaeJson(TipoDoc tipoDoc) throws EFacturaException
	{

		if (!caesMap.containsKey(tipoDoc.value))
			throw EFacturaException.raise(EFacturaErrors.CAE_DATA_NOT_FOUND).setDetailMessage("Tipo de documento: " + tipoDoc.name() + " id:" + tipoDoc.value);
		JSONObject caeJson = caesMap.get(tipoDoc.value);

		return caeJson;

	}

	@Override
	public synchronized IdDocTck getIdDocTick(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago) throws EFacturaException, DatatypeConfigurationException, IOException {
		IdDocTck iddoc = new IdDocTck();

		JSONObject cae = caesMap.get(tipoDoc.value);

		if (cae==null)
			throw EFacturaException.raise(EFacturaErrors.ERROR_IN_CAE_FILE).setDetailMessage("No se encuentra cae para TipoDoc:" + tipoDoc.value);
		
		if (montosIncluyenIva)
			iddoc.setMntBruto(new BigInteger("1"));
		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getString("serie"));
		iddoc.setNro(new BigInteger(consumeId(cae)));
		/*
		 * 1 = Contado
		 * 
		 * 2 = Credito
		 */
		iddoc.setFmaPago(new BigInteger(String.valueOf(formaPago)));
		/*
		 * Fecha
		 */
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		iddoc.setFchEmis(date);

		return iddoc;
	}
	
	@Override
	public synchronized IdDocFact getIdDocFact(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago) throws DatatypeConfigurationException, IOException, JSONException, EFacturaException
	{
		IdDocFact iddoc = new IdDocFact();

		JSONObject cae = caesMap.get(tipoDoc.value);

		if (montosIncluyenIva)
			iddoc.setMntBruto(new BigInteger("1"));
		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getString("serie"));
		iddoc.setNro(new BigInteger(consumeId(cae)));
		/*
		 * 1 = Contado
		 * 
		 * 2 = Credito
		 */
		iddoc.setFmaPago(new BigInteger(String.valueOf(formaPago)));
		/*
		 * Fecha
		 */
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		iddoc.setFchEmis(date);

		return iddoc;
	}

	@Override
	public synchronized IdDocResg getIdDocResg(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException, IOException {
		IdDocResg iddoc = new IdDocResg();

		JSONObject cae = caesMap.get(tipoDoc.value);

		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getString("serie"));
		iddoc.setNro(new BigInteger(consumeId(cae)));
		
		/*
		 * Fecha
		 */
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		iddoc.setFchEmis(date);

		return iddoc;
	}

}

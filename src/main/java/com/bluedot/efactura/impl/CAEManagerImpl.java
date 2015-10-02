package com.bluedot.efactura.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

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
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;

public class CAEManagerImpl implements CAEManager
{
	private static CAEManager instance = null;

	private HashMap<Integer, JSONObject> caesMap;

	private CAEManagerImpl() throws IOException, EFacturaException {
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
		eTicket(101), Nota_de_Credito_de_eTicket(102), Nota_de_Debito_de_eTicket(103), eFactura(111), Nota_de_Credito_de_eFactura(112), Nota_de_Debito_de_eFactura(113), eFactura_Exportacion(121), Nota_de_Credito_de_eFactura_Exportacion(
				122), Nota_de_Debito_de_eFactura_Exportacion(123), eRemito_de_Exportacion(124), eTicket_Venta_por_Cuenta_Ajena(131), Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena(132), Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena(
				133), eFactura_Venta_por_Cuenta_Ajena(141), Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena(142), Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena(143), eRemito(181), eResguardo(
				182),

		eTicket_Contingencia(201), Nota_de_Credito_de_eTicket_Contingencia(202), Nota_de_Debito_de_eTicket_Contingencia(203), eFactura_Contingencia(211), Nota_de_Credito_de_eFactura_Contingencia(212), Nota_de_Debito_de_eFactura_Contingencia(
				213), eFactura_Exportacion_Contingencia(221), Nota_de_Credito_de_eFactura_Exportacion_Contingencia(222), Nota_de_Debito_de_eFactura_Exportacion_Contingencia(223), eRemito_de_Exportacion_Contingencia(
				224), eTicket_Venta_por_Cuenta_Ajena_Contingencia(231), Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(232), Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(
				233), eFactura_Venta_por_Cuenta_Ajena_Contingencia(241), Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(242), Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(
				243), eRemito_Contingencia(281), eResguardo_Contingencia(282);

		int value;

		TipoDoc(int value) {
			this.value = value;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bluedot.efactura.CAEManagers#getIdDoc()
	 */

	@Override
	public synchronized IdDocFact getIdDocFact() throws DatatypeConfigurationException, IOException, JSONException, EFacturaException
	{
		IdDocFact iddoc = new IdDocFact();

		JSONObject cae = caesMap.get(TipoDoc.eFactura.value);

		iddoc.setTipoCFE(new BigInteger(String.valueOf(TipoDoc.eFactura.value)));
		iddoc.setSerie(cae.getString("serie"));
		iddoc.setNro(new BigInteger(consumeId(cae)));
		/*
		 * 1 = Contado
		 * 
		 * 2 = Credito
		 */
		iddoc.setFmaPago(new BigInteger("2"));
		/*
		 * Fecha
		 */
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		iddoc.setFchEmis(date);

		return iddoc;
	}

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
	public CAEDataType getCaeData(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException
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
		c.setTime(DateHandler.fromStringToDate(caeJson.getString("FecVenc"), new SimpleDateFormat("YYYY-MM-DD")));
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		cae.setFecVenc(date);

		return cae;

	}

}

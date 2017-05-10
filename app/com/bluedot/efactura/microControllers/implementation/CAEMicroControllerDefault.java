package com.bluedot.efactura.microControllers.implementation;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONException;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

public class CAEMicroControllerDefault extends MicroControllerDefault implements CAEMicroController 
{
	

	//TODO se podria hacer un cache de esta info en hazelcast
	
	private HashMap<TipoDoc, CAE> caesMap;
	
	/**
	 * Crea un handler de CAEs para la empresa dada
	 * 
	 * Debe de existir un solo Handler de CAEs por empresa para cualquier instante del sistema.
	 * Esto se logra haciendo mutex de la operacion que contiene este constructor.
	 * 
	 * @param empresa Empresa poseedora de los CAE a manipular
	 */
	
	//TODO revisar que todas las llamadas a este constructor eesten con mutex
	public CAEMicroControllerDefault(Empresa empresa){
		super(empresa);
		caesMap = new HashMap<TipoDoc, CAE>();
		for (Iterator<CAE> iterator = empresa.getCAEs().iterator(); iterator.hasNext();) {
			CAE cae = iterator.next();
			if (cae.getFechaAnulado()==null && DateHandler.diff( new Date(), cae.getFechaVencimiento())>1 && cae.getSiguiente()<cae.getFin())
				caesMap.put(cae.getTipo(), cae);
		}
	}
	

	private synchronized long consumeId(CAE cae) throws IOException, JSONException, APIException
	{
		if (cae.getSiguiente() == cae.getFin())
			throw APIException.raise(APIErrors.CAE_NOT_AVAILABLE_ID.withParams(cae.getNro(), cae.getTipo()));

		long siguiente = cae.getSiguiente();
		cae.setSiguiente(siguiente+1);
		cae.update();
		return siguiente;
	}

	@Override
	public synchronized CAEDataType getCAEDataType(TipoDoc tipoDoc) throws APIException, DatatypeConfigurationException, JSONException, ParseException
	{

		CAEDataType cae = new CAEDataType();
		if (!caesMap.containsKey(tipoDoc))
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND.withParams(tipoDoc.friendlyName, tipoDoc.value));
		CAE caeJson = caesMap.get(tipoDoc);

		cae.setCAEID(new BigInteger(String.valueOf(caeJson.getNro())));
		cae.setDNro(new BigInteger(String.valueOf(caeJson.getInicial())));
		cae.setHNro(new BigInteger(String.valueOf(caeJson.getFin())));

		/*
		 * Fecha
		 */
		GregorianCalendar c = new GregorianCalendar();
		c.setTimeZone(TimeZone.getTimeZone("UTC"));
		c.setTime(caeJson.getFechaVencimiento());
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		cae.setFecVenc(date);

		return cae;

	}
	
	@Override
	public synchronized CAE getCAE(TipoDoc tipoDoc) throws APIException
	{
		if (!caesMap.containsKey(tipoDoc))
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND.withParams(tipoDoc.friendlyName, tipoDoc.value));
		return caesMap.get(tipoDoc);
	}

	@Override
	public synchronized IdDocTck getIdDocTick(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago) throws APIException, DatatypeConfigurationException, IOException {
		IdDocTck iddoc = new IdDocTck();

		CAE cae = caesMap.get(tipoDoc);

		if (cae==null)
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND.withParams(tipoDoc.friendlyName, tipoDoc.value));
		
		if (montosIncluyenIva)
			iddoc.setMntBruto(new BigInteger("1"));
		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getSerie());
		iddoc.setNro(new BigInteger(String.valueOf(consumeId(cae))));
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
	public synchronized IdDocFact getIdDocFact(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago) throws DatatypeConfigurationException, IOException, JSONException, APIException
	{
		IdDocFact iddoc = new IdDocFact();

		CAE cae = caesMap.get(tipoDoc);

		if (montosIncluyenIva)
			iddoc.setMntBruto(new BigInteger("1"));
		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getSerie());
		iddoc.setNro(new BigInteger(String.valueOf(consumeId(cae))));
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
	public synchronized IdDocResg getIdDocResg(TipoDoc tipoDoc) throws APIException, DatatypeConfigurationException, IOException {
		IdDocResg iddoc = new IdDocResg();

		CAE cae = caesMap.get(tipoDoc);

		iddoc.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));
		iddoc.setSerie(cae.getSerie());
		iddoc.setNro(new BigInteger(String.valueOf(consumeId(cae))));
		
		/*
		 * Fecha
		 */
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		iddoc.setFchEmis(date);

		return iddoc;
	}


	@Override
	public void addCAE(CAE cae) throws APIException {
		
		if (cae.getFechaAnulado()!=null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("FechaAnulado")).setDetailMessage("Fecha anulado debe ser == null");
		
		//TODO agregar todas las validaciones pertinentes del nuevo CAE
		
		if (caesMap.containsKey(cae.getTipo())){
			anularCAE(caesMap.get(cae.getTipo()));
		}
		
		cae.save();
		empresa.getCAEs().add(cae);
		caesMap.put(cae.getTipo(), cae);
		
	}


	private void anularCAE(CAE cae) {
		
		//TODO ver como marco para notificar a la DGI de los ids anulados
		cae.setFechaAnulado(new Date());
		cae.update();
		caesMap.remove(cae.getTipo());
		
	}

}

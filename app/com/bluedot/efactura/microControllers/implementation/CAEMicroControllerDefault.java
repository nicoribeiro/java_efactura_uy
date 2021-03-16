package com.bluedot.efactura.microControllers.implementation;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocRem;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

public class CAEMicroControllerDefault extends MicroControllerDefault implements CAEMicroController 
{
	

	//TODO se podria hacer un cache de esta info en hazelcast
	
	private HashMap<TipoDoc, List<CAE>> caesMap;
	
	/**
	 * Crea un handler de CAEs para la empresa dada
	 * 
	 * Debe de existir un solo Handler de CAEs por empresa para cualquier instante del sistema.
	 * Esto se logra haciendo mutex de la operacion que contiene este constructor.
	 * 
	 * @param empresa Empresa poseedora de los CAE a manipular
	 */
	//TODO como se crea cada vez no hay problema cuando se acaban los numeros de un CAE, pero si hacemos cache esto hay que atenderlo
	//TODO revisar que todas las llamadas a este constructor eesten con mutex
	public CAEMicroControllerDefault(Empresa empresa){
		super(empresa);
		caesMap = new HashMap<TipoDoc, List<CAE>>();
		for (Iterator<CAE> iterator = empresa.getCaes().iterator(); iterator.hasNext();) {
			CAE cae = iterator.next();
			if (cae.getFechaAnulado() == null && (new Date()).before(cae.getFechaVencimiento()) && cae.getSiguiente() <= cae.getFin()){
				/*
				 * Es un CAE valido y tiene numeros disponibles
				 */
				addCAEtoMap(cae);
			}
		}

		sortCAEs();
	}

	/**
	 * @param cae
	 */
	private void addCAEtoMap(CAE cae) {
		if (caesMap.get(cae.getTipo()) == null)
			caesMap.put(cae.getTipo(), new LinkedList<CAE>());
		
		caesMap.get(cae.getTipo()).add(cae);
	}
	
	
	private void sortCAEs(){
		for (List<CAE> caeList : caesMap.values()) {
			Collections.sort(caeList, new Comparator<CAE>() {
		         @Override
		         public int compare(CAE o1, CAE o2) {
		             return o1.getFechaVencimiento().compareTo(o2.getFechaVencimiento());
		         }
		     });
		}
	}
	
	private synchronized long consumeId(CAE cae) throws IOException, JSONException, APIException
	{
		if (cae.getSiguiente()  > cae.getFin())
			throw APIException.raise(APIErrors.CAE_NOT_AVAILABLE_ID).withParams(cae.getNro(), cae.getTipo());

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
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND).withParams(tipoDoc.friendlyName, tipoDoc.value);
		CAE caeJson = caesMap.get(tipoDoc).get(0);

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
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND).withParams(tipoDoc.friendlyName, tipoDoc.value);
		return caesMap.get(tipoDoc).get(0);
	}

	@Override
	public synchronized IdDocTck getIdDocTick(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago) throws APIException, DatatypeConfigurationException, IOException {
		
		if (!caesMap.containsKey(tipoDoc))
			throw APIException.raise(APIErrors.CAE_DATA_NOT_FOUND).withParams(tipoDoc.friendlyName, tipoDoc.value);
		
		IdDocTck iddoc = new IdDocTck();

		CAE cae = caesMap.get(tipoDoc).get(0);

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
	public synchronized IdDocFact getIdDocFact(TipoDoc tipoDoc, boolean montosIncluyenIva, int formaPago, Date fchEmis) throws DatatypeConfigurationException, IOException, JSONException, APIException
	{
		IdDocFact iddoc = new IdDocFact();

		CAE cae = caesMap.get(tipoDoc).get(0);

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
		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(fchEmis));
		iddoc.setFchEmis(date);

		return iddoc;
	}

	@Override
	public synchronized IdDocResg getIdDocResg(TipoDoc tipoDoc) throws APIException, DatatypeConfigurationException, IOException {
		IdDocResg iddoc = new IdDocResg();

		CAE cae = caesMap.get(tipoDoc).get(0);

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
	public IdDocRem getIdDocRem(TipoDoc tipoDoc) throws APIException, DatatypeConfigurationException, IOException {
		IdDocRem iddoc = new IdDocRem();

		CAE cae = caesMap.get(tipoDoc).get(0);

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
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("FechaAnulado").setDetailMessage("Fecha anulado debe ser == null");
		
		if ((new Date().after(cae.getFechaVencimiento())))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("FechaVencimiento").setDetailMessage("Fecha de vencimeinto en el pasado");		
		
		if (DateHandler.diff(new Date(), cae.getFechaVencimiento()) <= 7)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("FechaVencimiento").setDetailMessage("CAE muy proximo a vencer");
		
		if (cae.getInicial() == 0)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Inicial").setDetailMessage("Inicial no puede ser 0");
		
		if (cae.getFin() == 0)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Fin").setDetailMessage("Fin no puede ser 0");		
		/*
		if (cae.getSiguiente() != cae.getInicial())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Siguiente").setDetailMessage("Siguiente debe ser = a Inicial");
		*/
		if (cae.getTipo() == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Tipo").setDetailMessage("El CAE debe tener Tipo");
		
		if (cae.getSerie() == null || cae.getSerie().equals(""))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Serie").setDetailMessage("La Serie no puede ser vacia");
		
		cae.save();
		this.getEmpresa().getCaes().add(cae);
		addCAEtoMap(cae);
		
	}

	public Set<TipoDoc> getTipoDoc() {
		return caesMap.keySet();
	}

	public int getCantCAEValidos(TipoDoc tipoDoc) {
		if (caesMap.get(tipoDoc)==null)
			return 0;
		else
			return caesMap.get(tipoDoc).size();
		
	}

	public double getPorcentajeUsoCAE(TipoDoc tipoDoc) {
		
		long usados = 0;
		
		long totales = 0;
		
		for (CAE cae : caesMap.get(tipoDoc)) {
			totales =+ cae.getFin()-cae.getInicial()+1;
			usados =+ cae.getSiguiente()-cae.getInicial();
		}
		
		
		return Math.floor(((float)usados/totales)*100);
	}

	@Override
	public CAE getCAEfromJson(JSONObject caeJson) {
		
		Date fechaVencimiento = DateHandler.fromStringToDate(caeJson.getString("FVD"), new SimpleDateFormat("yyyy-MM-dd"));
		
		final long dNro = caeJson.getLong("DNro");
		long siguiente = dNro;
		if (caeJson.has("Siguiente")) {
			siguiente = caeJson.getLong("Siguiente");
		}
		
		CAE cae = new CAE(this.getEmpresa(), caeJson.getLong("NA"), TipoDoc.fromInt(caeJson.getInt("TCFE")), caeJson.getString("Serie"), dNro, caeJson.getLong("HNro"), fechaVencimiento, siguiente);
		
		return cae;
	}

	@Override
	public void anularCAEs(TipoDoc tipo) {
		if (caesMap.get(tipo)!=null)
			for (Iterator<CAE> iterator = caesMap.get(tipo).iterator(); iterator.hasNext();) {
				CAE cae =  iterator.next();
				cae.setFechaAnulado(new Date());
				cae.update();
			}
	}
}

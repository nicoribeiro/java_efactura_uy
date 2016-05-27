package com.bluedot.efactura;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.datatype.DatatypeConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

public interface CAEManager {

	/**
	 * Retorn la informacion de CAE vigente para dicho tipoDoc
	 * 
	 * @param tipoDoc
	 * @return
	 * @throws EFacturaException
	 * @throws DatatypeConfigurationException
	 */
	public abstract CAEDataType getCaeData(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException, JSONException, ParseException;

	
	JSONObject getCaeJson(TipoDoc tipoDoc) throws EFacturaException;
	
	/**
	 * Obtiene un nuevo identificador de Factura
	 * 
	 * @return IdDocFact
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 */
	public abstract IdDocFact getIdDocFact(TipoDoc tipo, boolean montosIncluyenIva, int formaPago)
			throws EFacturaException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Ticket
	 * 
	 * @return
	 */
	public abstract IdDocTck getIdDocTick(TipoDoc tipo, boolean montosIncluyenIva, int formaPago)
			throws EFacturaException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Resguardo
	 * 
	 * @return
	 */
	public abstract IdDocResg getIdDocResg(TipoDoc tipo)
			throws EFacturaException, DatatypeConfigurationException, IOException;


	public abstract void refreshMap()throws IOException, EFacturaException ;

	

}
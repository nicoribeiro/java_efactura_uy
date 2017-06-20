package com.bluedot.efactura.microControllers.interfaces;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.datatype.DatatypeConfigurationException;

import org.json.JSONException;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocRem;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

public interface CAEMicroController {

	/**
	 * Retorna la informacion de CAE vigente para dicho tipoDoc
	 * 
	 * @param tipoDoc
	 * @return
	 * @throws APIException
	 * @throws DatatypeConfigurationException
	 */
	CAEDataType getCAEDataType(TipoDoc tipoDoc) throws APIException, DatatypeConfigurationException, JSONException, ParseException;

	/**
	 * Retorna la informacion de CAE vigente para dicho tipoDoc
	 * 
	 * @param tipoDoc
	 * @return
	 * @throws APIException
	 */
	public CAE getCAE(TipoDoc tipoDoc) throws APIException;
	
	/**
	 * Obtiene un nuevo identificador de Factura
	 * 
	 * @return IdDocFact
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 */
	IdDocFact getIdDocFact(TipoDoc tipo, boolean montosIncluyenIva, int formaPago)
			throws APIException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Ticket
	 * 
	 * @return
	 */
	 IdDocTck getIdDocTick(TipoDoc tipo, boolean montosIncluyenIva, int formaPago)
			throws APIException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Resguardo
	 * 
	 * @return
	 */
	IdDocResg getIdDocResg(TipoDoc tipo)
			throws APIException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Remito
	 * 
	 * @return
	 */
	IdDocRem getIdDocRem(TipoDoc tipo)
			throws APIException, DatatypeConfigurationException, IOException;
	
	void addCAE(CAE cae) throws APIException;


}
package com.bluedot.efactura;

import java.io.IOException;

import javax.xml.datatype.DatatypeConfigurationException;

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
	public abstract CAEDataType getCaeData(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException;

	/**
	 * Obtiene un nuevo identificador de Factura
	 * 
	 * @return IdDocFact
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 */
	public abstract IdDocFact getIdDocFact(TipoDoc tipo)
			throws EFacturaException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Ticket
	 * 
	 * @return
	 */
	public abstract IdDocTck getIdDocTick(TipoDoc tipo)
			throws EFacturaException, DatatypeConfigurationException, IOException;

	/**
	 * Obtiene un nuevo identificador de Resguardo
	 * 
	 * @return
	 */
	public abstract IdDocResg getIdDocResg(TipoDoc tipo)
			throws EFacturaException, DatatypeConfigurationException, IOException;

}
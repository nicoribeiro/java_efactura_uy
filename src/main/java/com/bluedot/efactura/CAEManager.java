package com.bluedot.efactura;

import java.io.IOException;

import javax.xml.datatype.DatatypeConfigurationException;

import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.IdDocFact;

public interface CAEManager
{

	/**
	 * Obtiene un nuevo identificador de Factura
	 * @return IdDocFact
	 * @throws DatatypeConfigurationException
	 * @throws IOException 
	 */
	public abstract IdDocFact getIdDocFact() throws EFacturaException, DatatypeConfigurationException, IOException;

	/**
	 * Retorn la informacion de CAE vigente para dicho tipoDoc
	 * @param tipoDoc
	 * @return
	 * @throws EFacturaException
	 * @throws DatatypeConfigurationException
	 */
	public abstract CAEDataType getCaeData(TipoDoc tipoDoc) throws EFacturaException, DatatypeConfigurationException;

}
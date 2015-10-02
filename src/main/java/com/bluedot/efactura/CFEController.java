package com.bluedot.efactura;

import org.json.JSONObject;

import dgi.classes.recepcion.CFEDefType;

public interface CFEController
{
	/**
	 * Crea una nueva factura electronica a partir de un objeto JSON.  
	 * @param factura
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EFact createEfactura(JSONObject factura) throws EFacturaException;

	/**
	 * Crea una nueva ticket electronico a partir de un objeto JSON.  
	 * @param ticket
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ETck createETicket(JSONObject ticket)throws EFacturaException;
	
	/**
	 * Crea una nueva remito electronico a partir de un objeto JSON.  
	 * @param remito
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ERem createERemito(JSONObject remito)throws EFacturaException;
	
	/**
	 * Crea una nueva resguardo electronica a partir de un objeto JSON.  
	 * @param resguardo
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EResg createEResguardo(JSONObject resguardo)throws EFacturaException;
	
	/**
	 * Crea una nuevo remito electronico a partir de un objeto JSON.  
	 * @param remito
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ERemExp createERemitoExportacion(JSONObject remito)throws EFacturaException;
	
	/**
	 * Crea una nueva factura electronica de exportacion a partir de un objeto JSON.  
	 * @param factura
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EFactExp createEFacturaExportacion(JSONObject factura)throws EFacturaException;
	
}
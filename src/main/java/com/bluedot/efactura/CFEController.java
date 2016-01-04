package com.bluedot.efactura;

import org.json.JSONObject;

import dgi.classes.recepcion.CFEDefType;

public interface CFEController
{
	/**
	 * Crea una nueva efactura/NC-efactura/ND-efactura a partir de un objeto JSON.  
	 * @param factura
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EFact createEfactura(JSONObject factura) throws EFacturaException;
	public CFEDefType.EFact createNotaCreditoEfactura(JSONObject notaCredito, JSONObject referencia) throws EFacturaException;
	public CFEDefType.EFact createNotaDebitoEfactura(JSONObject notaDebito, JSONObject referencia) throws EFacturaException;

	/**
	 * Crea un nuevo eticket/NC-eticket/ND-eticket a partir de un objeto JSON.  
	 * @param ticket
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ETck createETicket(JSONObject ticket)throws EFacturaException;
	public CFEDefType.ETck createNotaCreditoETicket(JSONObject notaCredito, JSONObject referencia)throws EFacturaException;
	public CFEDefType.ETck createNotaDebitoETicket(JSONObject notoDebito, JSONObject referencia)throws EFacturaException;
	
	/**
	 * Crea un nuevo eremito a partir de un objeto JSON.  
	 * @param remito
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ERem createERemito(JSONObject remito)throws EFacturaException;
	
	/**
	 * Crea un nuevo eresguardo a partir de un objeto JSON.  
	 * @param resguardo
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EResg createEResguardo(JSONObject resguardo)throws EFacturaException;
	
	/**
	 * Crea un nuevo eremito de exportacion a partir de un objeto JSON.  
	 * @param remito
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.ERemExp createERemitoExportacion(JSONObject remito)throws EFacturaException;
	
	/**
	 * Crea una nueva efactura de exportacion a partir de un objeto JSON.  
	 * @param factura
	 * @return
	 * @throws EFacturaException 
	 */
	public CFEDefType.EFactExp createEFacturaExportacion(JSONObject factura)throws EFacturaException;
	
}
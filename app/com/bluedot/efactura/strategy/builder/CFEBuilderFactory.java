package com.bluedot.efactura.strategy.builder;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public class CFEBuilderFactory {

	
	public static CFEBuiderInterface getCFEBuilder(TipoDoc tipo, CAEMicroController caeMicroController) throws APIException {
		
		CFEStrategy strategy = (new CFEStrategy.Builder()).withTipo(tipo).withCAEMicroController(caeMicroController).build();
		
		return getInterface(tipo, strategy, caeMicroController);
	}
	
	public static CFEBuiderInterface getCFEBuilder(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		
		CFEStrategy strategy = (new CFEStrategy.Builder()).withCFE(cfe).withCAEMicroController(caeMicroController).build();
		
		return getInterface(cfe.getTipo(), strategy, caeMicroController);
		
	}
	
	public static CFEBuiderInterface getCFEBuilder(CFEEmpresasType cfe) throws APIException {
		
		TipoDoc tipoDoc = null;
		
		if (cfe.getCFE().getEFact()!=null)
			tipoDoc = TipoDoc.fromInt(cfe.getCFE().getEFact().getEncabezado().getIdDoc().getTipoCFE().intValue());
		
		if (cfe.getCFE().getERem()!=null)
			tipoDoc = TipoDoc.fromInt(cfe.getCFE().getERem().getEncabezado().getIdDoc().getTipoCFE().intValue());
		
		if (cfe.getCFE().getEResg()!=null)
			tipoDoc = TipoDoc.fromInt(cfe.getCFE().getEResg().getEncabezado().getIdDoc().getTipoCFE().intValue());
		
		if (cfe.getCFE().getETck()!=null)
			tipoDoc = TipoDoc.fromInt(cfe.getCFE().getETck().getEncabezado().getIdDoc().getTipoCFE().intValue());
		
		CFEStrategy strategy = (new CFEStrategy.Builder()).withTipo(tipoDoc).build();
		
		return getInterface(tipoDoc, strategy, null);
		
	}

	
	private static CFEBuiderInterface getInterface(TipoDoc tipoDoc, CFEStrategy strategy,
			CAEMicroController caeMicroController) throws APIException{
		switch (tipoDoc) {
		
		
		
		case eFactura:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Credito_de_eFactura:
		case eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Contingencia:
			return new CFEBuilderImpl(caeMicroController, strategy);
		case eTicket:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eTicket:
		case eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Contingencia:
			return new CFEBuilderImpl(caeMicroController, strategy);
		case eResguardo:
		case eResguardo_Contingencia:
			return new CFEBuiderResguardo(caeMicroController, strategy);
		case eRemito:
		case eRemito_Contingencia:
			return new CFEBuiderRemito(caeMicroController, strategy);
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("No existe Builder para tipoDoc:" + tipoDoc.value);
		}
		
		return null;
	}

}

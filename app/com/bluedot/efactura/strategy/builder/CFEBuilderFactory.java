package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;

public class CFEBuilderFactory {

	
	public static CFEBuiderInterface getCFEBuilder(TipoDoc tipo) throws EFacturaException {
		switch (tipo) {
		case eFactura:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Credito_de_eFactura:
		case eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Contingencia:
			EFact eFactura = new EFact();
			return new CFEBuilderImpl(eFactura, tipo);
		case eTicket:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eTicket:
		case eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Contingencia:
			ETck eTicket = new ETck();
			return new CFEBuilderImpl(eTicket, tipo);
		case eResguardo:
			EResg eResguardo = new EResg();
			return new CFEBuiderResguardo(eResguardo, tipo);
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
		case eRemito:
		case eRemito_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
		case eResguardo_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			throw EFacturaException.raise(EFacturaErrors.NOT_SUPPORTED).setDetailMessage("No existe Builder para tipoDoc:" + tipo.value);
		}
		
		return null;
	}
	
}

package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CFEStrategyProvider implements Provider<CFEStrategy> {

	private EfactStrategyFactory efactStrategyFactory;
	private EResguardoStrategyFactory eResguardoStrategyFactory;
	private EticketStrategyFactory eticketStrategyFactory; 
	
	private CFE cfe;
	private TipoDoc tipoDoc;
	private Empresa empresa;
	
	@Inject
	public CFEStrategyProvider(EfactStrategyFactory efactStrategyFactory, EResguardoStrategyFactory eResguardoStrategyFactory, EticketStrategyFactory eticketStrategyFactory) {
		this.efactStrategyFactory = efactStrategyFactory;
		this.eResguardoStrategyFactory = eResguardoStrategyFactory;
		this.eticketStrategyFactory = eticketStrategyFactory;
	}

	
	@Override
	public CFEStrategy get() {
		if (cfe==null){
			cfe = new CFE();
			cfe.setTipo(tipoDoc);
		}else{
			tipoDoc = cfe.getTipo();
		}
		
		
		
		switch (tipoDoc) {

		case Nota_de_Credito_de_eFactura:
		case eFactura:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case eFactura_Contingencia:
			return efactStrategyFactory.create(cfe, empresa);
		case eTicket:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eTicket:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case eTicket_Contingencia:
			return eticketStrategyFactory.create(cfe, empresa);
		case eResguardo:
		case eResguardo_Contingencia:
			return eResguardoStrategyFactory.create(cfe, empresa);

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

		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			return null;
		}
		return null;
		
	}


	public void setCfe(CFE cfe) {
		this.cfe = cfe;
	}


	public void setTipoDoc(TipoDoc tipoDoc) {
		this.tipoDoc = tipoDoc;
	}


	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

}

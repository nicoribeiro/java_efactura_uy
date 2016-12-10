package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.microControllers.interfaces.CAEMicroControllerFactory;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CFEBuilderProvider implements Provider<CFEBuilder> {

	private CFEStrategyProvider cfeStrategyProvider;
	private CFEBuilderImplFactory cfeBuilderImplFactory;
	private CFEBuilderResguardoFactory cfeBuilderResguardoFactory;
	private CAEMicroControllerFactory caeMicroControllerFactory;
	
	private CFE cfe;
	private TipoDoc tipoDoc;
	private Empresa empresa;
	
	@Inject
	public CFEBuilderProvider(CFEStrategyProvider cfeStrategyProvider, CFEBuilderImplFactory cfeBuilderImplFactory, CFEBuilderResguardoFactory cfeBuilderResguardoFactory, CAEMicroControllerFactory caeMicroControllerFactory){
		this.cfeStrategyProvider = cfeStrategyProvider;
	}

	
	@Override
	public CFEBuilder get() {
		
		cfeStrategyProvider.setTipoDoc(tipoDoc);
		cfeStrategyProvider.setCfe(cfe);
		cfeStrategyProvider.setEmpresa(empresa);
		
		switch (tipoDoc) {
		
	case eFactura:
	case Nota_de_Debito_de_eFactura:
	case Nota_de_Credito_de_eFactura:
	case eFactura_Contingencia:
	case Nota_de_Debito_de_eFactura_Contingencia:
	case Nota_de_Credito_de_eFactura_Contingencia:
	case eTicket:
	case Nota_de_Credito_de_eTicket:
	case Nota_de_Debito_de_eTicket:
	case eTicket_Contingencia:
	case Nota_de_Debito_de_eTicket_Contingencia:
	case Nota_de_Credito_de_eTicket_Contingencia:
		return cfeBuilderImplFactory.create(cfeStrategyProvider.get(),caeMicroControllerFactory.create(empresa)); 
	case eResguardo:
		return cfeBuilderResguardoFactory.create(cfeStrategyProvider.get(),caeMicroControllerFactory.create(empresa)); 
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

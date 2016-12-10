package com.bluedot.efactura.strategy.builder;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.model.TipoDocumento;
import com.bluedot.efactura.model.Titular;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public interface CFEStrategy {

//	public class Builder {
//
//		private TipoDoc tipoDoc;
//		private CFE cfe;
//		private CAEMicroController caeMicroController;
//
//		public TipoDoc getTipo() {
//			return tipoDoc;
//		}
//
//		public Builder withCAEMicroController(CAEMicroController caeMicroController) {
//			this.caeMicroController = caeMicroController;
//			return this;
//		}
//
//		public Builder withTipo(TipoDoc tipo) {
//			this.tipoDoc = tipo;
//			return this;
//		}
//
//		public Builder withCFE(CFE cfe) {
//			this.cfe = cfe;
//			return this;
//		}
//
//		public CFEStrategy build() throws APIException {
//			if (cfe==null){
//				cfe = new CFE();
//				cfe.setTipo(tipoDoc);
//			}else{
//				tipoDoc = cfe.getTipo();
//			}
//			
//			
//			
//			switch (tipoDoc) {
//
//			case Nota_de_Credito_de_eFactura:
//			case eFactura:
//			case Nota_de_Debito_de_eFactura:
//			case Nota_de_Credito_de_eFactura_Contingencia:
//			case Nota_de_Debito_de_eFactura_Contingencia:
//			case eFactura_Contingencia:
//				return new EfactStrategy(cfe, caeMicroController);
//			case eTicket:
//			case Nota_de_Credito_de_eTicket:
//			case Nota_de_Debito_de_eTicket:
//			case Nota_de_Credito_de_eTicket_Contingencia:
//			case Nota_de_Debito_de_eTicket_Contingencia:
//			case eTicket_Contingencia:
//				return new EticketStrategy(cfe, caeMicroController);
//			case eResguardo:
//			case eResguardo_Contingencia:
//				return new EResguardoStrategy(cfe, caeMicroController);
//
//			case Nota_de_Credito_de_eFactura_Exportacion:
//			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
//			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
//			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
//
//			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
//			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
//
//			case Nota_de_Debito_de_eFactura_Exportacion:
//			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
//			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
//			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
//
//			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
//			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
//
//			case eFactura_Exportacion:
//			case eFactura_Exportacion_Contingencia:
//			case eFactura_Venta_por_Cuenta_Ajena:
//			case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
//
//			case eRemito:
//			case eRemito_Contingencia:
//			case eRemito_de_Exportacion:
//			case eRemito_de_Exportacion_Contingencia:
//
//			case eTicket_Venta_por_Cuenta_Ajena:
//			case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
//				throw APIException.raise(APIErrors.NOT_SUPPORTED)
//						.setDetailMessage("Estrategia para el tipo: " + tipoDoc.friendlyName);
//			}
//			return null;
//
//		}
//
//	}

	Emisor getEmisor();

	TotalesInterface getTotales();

	CAEDataType getCAEData();

	List<ItemInterface> getItem();

	IdDocInterface getIdDoc();

	void setIdDoc() throws APIException;

	void setCAEData() throws APIException;

	ItemInterface createItem();

	ReferenciaTipo getReferenciaTipo();

	void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar);

	CFE getCFE();

	void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep) throws APIException;

}

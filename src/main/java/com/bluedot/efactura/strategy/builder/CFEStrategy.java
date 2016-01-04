package com.bluedot.efactura.strategy.builder;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ReferenciaType;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public interface CFEStrategy {

	public class Builder {

		private TipoDoc tipoDoc;
		private EFact eFactura;
		private ETck eTicket;
		private EResg eResguardo;

		public TipoDoc getTipo() {
			return tipoDoc;
		}

		public Builder withTipo(TipoDoc tipo) {
			this.tipoDoc = tipo;
			return this;
		}

		public Builder withEfact(EFact efact) {
			this.eFactura = efact;
			return this;
		}

		public Builder withEtick(ETck etick) {
			this.eTicket = etick;
			return this;
		}

		public Builder withEResg(EResg eResguardo) {
			this.eResguardo = eResguardo;
			return this;
		}

		public CFEStrategy build() throws EFacturaException {
			switch (tipoDoc) {

			case Nota_de_Credito_de_eFactura:
			case eFactura:
			case Nota_de_Debito_de_eFactura:
				return new EfactStrategy(eFactura, tipoDoc);
			case eTicket:
			case Nota_de_Credito_de_eTicket:
			case Nota_de_Debito_de_eTicket:
				return new EticketStrategy(eTicket, tipoDoc);
			case eResguardo:
				return new EResguardoStrategy(eResguardo, tipoDoc);

			case Nota_de_Credito_de_eFactura_Contingencia:
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Credito_de_eTicket_Contingencia:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Debito_de_eFactura_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Debito_de_eTicket_Contingencia:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:

			case eFactura_Contingencia:
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case eFactura_Venta_por_Cuenta_Ajena:
			case eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case eRemito:
			case eRemito_Contingencia:
			case eRemito_de_Exportacion:
			case eRemito_de_Exportacion_Contingencia:

			case eResguardo_Contingencia:

			case eTicket_Contingencia:
			case eTicket_Venta_por_Cuenta_Ajena:
			case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
				throw EFacturaException.raise(EFacturaErrors.NOT_SUPPORTED)
						.setDetailMessage("Estrategia para el tipo: " + tipoDoc.friendlyName);
			}
			return null;

		}

	}

	Emisor getEmisor();

	ReceptorInterface getReceptor();

	TotalesInterface getTotales();

	CAEDataType getCAEData();

	List<ItemInterface> getItem();

	IdDocInterface getIdDoc();

	void setIdDoc();

	void setCAEData();

	ItemInterface createItem();

	ReferenciaType getReferenciaType();

	void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar);

	Object getCFE();

}

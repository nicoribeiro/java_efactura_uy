package com.bluedot.efactura.strategy.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.CFEDefType.ETck.Detalle;
import dgi.classes.recepcion.CFEDefType.ETck.Encabezado;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocTck;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ReceptorTck;
import dgi.classes.recepcion.ReferenciaType;
import dgi.classes.recepcion.Totales;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocTickWrapper;
import dgi.classes.recepcion.wrappers.ItemDetFactWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesFactTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

public class EticketStrategy implements CFEStrategy {

	private ETck cfe;
	private TipoDoc tipo;

	public EticketStrategy(ETck eticket, TipoDoc tipo) throws EFacturaException {
		this.cfe = eticket;
		
		if (tipo!=TipoDoc.eTicket && tipo!=TipoDoc.Nota_de_Credito_de_eTicket && tipo!=TipoDoc.Nota_de_Debito_de_eTicket)
			throw EFacturaException.raise(EFacturaErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + tipo.friendlyName);
		
		this.tipo = tipo;
	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	@Override
	public ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorTck());
		return new ReceptorTickWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEncabezado() == null)
			cfe.setEncabezado(new Encabezado());
		return cfe.getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new Totales());
		return new TotalesFactTickWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (cfe.getCAEData() == null)
			cfe.setCAEData(new CAEDataType());
		return cfe.getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();

		for (ItemDetFact itemDetFact : cfe.getDetalle().getItem()) {
			list.add(new ItemDetFactWrapper(itemDetFact));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocTck());
		return new IdDocTickWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() {
		try {
			getEncabezado().setIdDoc(CAEManagerImpl.getInstance().getIdDocTick(tipo));
		} catch (DatatypeConfigurationException | IOException | EFacturaException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setCAEData() {
		try {
			cfe.setCAEData(CAEManagerImpl.getInstance().getCaeData(tipo));
		} catch (DatatypeConfigurationException | IOException | EFacturaException e) {
			e.printStackTrace();
		}

	}

	private Detalle getDetalle() {
		if (cfe.getDetalle() == null)
			cfe.setDetalle(new Detalle());
		return cfe.getDetalle();
	}

	@Override
	public ItemInterface createItem() {
		ItemDetFact item = new ItemDetFact();
		getDetalle().getItem().add(item);
		return new ItemDetFactWrapper(item);
	}

	@Override
	public ReferenciaType getReferenciaType() {
		if (cfe.getReferencia() == null)
			cfe.setReferencia(new ReferenciaType());
		return cfe.getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		cfe.setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public Object getCFE() {
		return cfe;
	}

}

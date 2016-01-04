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
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.EResg.Detalle;
import dgi.classes.recepcion.CFEDefType.EResg.Encabezado;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.ItemResg;
import dgi.classes.recepcion.ReceptorResg;
import dgi.classes.recepcion.ReferenciaType;
import dgi.classes.recepcion.TotalesResg;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.IdDocResgWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemResgWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.ReceptorResgWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TotalesResguardoWrrapper;

public class EResguardoStrategy implements CFEStrategy {

	private EResg cfe;
	protected TipoDoc tipo;
	
	public EResguardoStrategy(EResg eResguardo, TipoDoc tipo) throws EFacturaException {
		this.cfe = eResguardo;
		
		if (tipo!=TipoDoc.eResguardo)
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
			getEncabezado().setReceptor(new ReceptorResg());
		return new ReceptorResgWrapper(getEncabezado().getReceptor());
	}

	private Encabezado getEncabezado() {
		if (cfe.getEncabezado() == null)
			cfe.setEncabezado(new Encabezado());
		return cfe.getEncabezado();
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesResg());
		return new TotalesResguardoWrrapper(getEncabezado().getTotales());
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

		for (ItemResg itemResg : cfe.getDetalle().getItem()) {
			list.add(new ItemResgWrapper(itemResg));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocResg());
		return new IdDocResgWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() {
		try {
			getEncabezado().setIdDoc(CAEManagerImpl.getInstance().getIdDocResg(tipo));
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
		ItemResg item = new ItemResg();
		getDetalle().getItem().add(item);
		return new ItemResgWrapper(item);
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

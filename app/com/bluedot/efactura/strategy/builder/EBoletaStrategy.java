package com.bluedot.efactura.strategy.builder;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.json.JSONException;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.IndicadorFacturacion;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EBoleta;
import dgi.classes.recepcion.CFEDefType.EBoleta.Detalle;
import dgi.classes.recepcion.CFEDefType.EBoleta.Encabezado;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocBoleta;
import dgi.classes.recepcion.ItemDetBoleta;
import dgi.classes.recepcion.ReceptorBoleta;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.TotalesBoleta;
import dgi.classes.recepcion.wrappers.IdDocBoletaWrapper;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemDetBoletaWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorBoletaWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesBoletaWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * e-Boleta de entrada y sus notas de credito y debito (151, 152, 153 y contingencia 251, 252, 253). La emite el
 * COMPRADOR cuando le compra a quien no factura (no contribuyente, IVA minimo o Monotributo, IMEBA, o compra de moneda
 * extranjera para su reventa): el "receptor" del CFE es el VENDEDOR.
 */
public class EBoletaStrategy extends CommonStrategy implements CFEStrategy {

	public EBoletaStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEboleta() == null)
			this.cfe.setEboleta(new EBoleta());

		switch (cfe.getTipo()) {
			case eBoleta_de_entrada:
			case eBoleta_de_entrada_Contingencia:
			case Nota_de_Credito_de_eBoleta_de_entrada:
			case Nota_de_Credito_de_eBoleta_de_entrada_Contingencia:
			case Nota_de_Debito_de_eBoleta_de_entrada:
			case Nota_de_Debito_de_eBoleta_de_entrada_Contingencia:
				break;
			default:
				throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
		}
	}

	private EBoleta getEBoleta() {
		return cfe.getEboleta();
	}

	private Encabezado getEncabezado() {
		if (getEBoleta().getEncabezado() == null)
			getEBoleta().setEncabezado(new Encabezado());
		return getEBoleta().getEncabezado();
	}

	private Detalle getDetalle() {
		if (getEBoleta().getDetalle() == null)
			getEBoleta().setDetalle(new Detalle());
		return getEBoleta().getDetalle();
	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	private ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorBoleta());
		return new ReceptorBoletaWrapper(getEncabezado().getReceptor());
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesBoleta());
		return new TotalesBoletaWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (getEBoleta().getCAEData() == null)
			getEBoleta().setCAEData(new CAEDataType());
		return getEBoleta().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();
		for (ItemDetBoleta item : getDetalle().getItems()) {
			list.add(new ItemDetBoletaWrapper(item));
		}
		return list;
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocBoleta());
		return new IdDocBoletaWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocBoleta idDoc = caeMicroController.getIdDocBoleta(cfe.getTipo(), cfe.getFormaDePago().value, cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
			idDoc.setNroInterno(cfe.getNroInterno());
			// IndCompraMEReventa, SecProf e InfoAdicionalDoc (validados en DatosIdDoc.leer al crear el CFE)
			DatosIdDoc.aplicar(cfe.getDatosIdDoc(), idDoc);
			/*
			 * A-C10: si hay IVA en suspenso (A-C115) tiene que ser 2 (lineas con IMEBA y adicionales incluidos)
			 */
			if (cfe.getTotMntIVAenSusp() != 0)
				idDoc.setMntBruto(BigInteger.valueOf(2));
			getEncabezado().setIdDoc(idDoc);
			cfe.setSerie(idDoc.getSerie());
			cfe.setNro(idDoc.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			throw APIException.raise(e);
		}
	}

	/**
	 * Compra de moneda extranjera para su reventa (A-C21 en el IdDoc del JSON): lo fija el builder antes del receptor,
	 * porque el IdDoc se arma recien al enviar
	 */
	private boolean compraMEReventa = false;

	public void setCompraMEReventa(boolean compraMEReventa) {
		this.compraMEReventa = compraMEReventa;
	}

	public boolean esCompraMEReventa() {
		return compraMEReventa;
	}

	@Override
	public void setCAEData() throws APIException {
		try {
			getEBoleta().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			throw APIException.raise(e);
		}
	}

	@Override
	public ItemInterface createItem() {
		ItemDetBoleta item = new ItemDetBoleta();
		getDetalle().getItems().add(item);
		return new ItemDetBoletaWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (getEBoleta().getReferencia() == null)
			getEBoleta().setReferencia(new ReferenciaTipo());
		return getEBoleta().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		getEBoleta().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	/**
	 * Vendedor (receptor del CFE, A-C60 a A-C69). Formato CFE v25-2:
	 * <ul>
	 * <li>A-C60 siempre, salvo compra de moneda extranjera para su reventa (A-C21) a quien no tiene RUC por menos del
	 * tope de UI</li>
	 * <li>RUC, NIE y CI: pais UY y documento; DNI: AR, BR, CL o PY; otros, pasaporte y NIFE: documento del exterior</li>
	 * <li>Con RUC (A-C60=2) el vendedor no puede ser emisor electronico, salvo en la compra de moneda extranjera para su
	 * reventa; los items 13 (no contribuyente) no admiten RUC y los 14 (IVA minimo, Monotributo) y 15 (IMEBA) lo exigen</li>
	 * </ul>
	 */
	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String pdfMailAddress, String compraID) throws APIException {
		ReceptorInterface receptor = getReceptor();

		boolean hayIdentificacion = tipoDocRecep != null || !vacio(codPaisRecep) || !vacio(docRecep) || !vacio(docRecepExt);
		boolean exceptuado = esCompraMEReventa() && tipoDocRecep != TipoDocumento.RUC && !superaTopeUI();
		if (!hayIdentificacion && !exceptuado)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Receptor.TipoDocRecep")
					.setDetailMessage("En la e-Boleta de entrada hay que identificar al vendedor (receptor del CFE)");

		if (hayIdentificacion) {
			if (tipoDocRecep == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("TipoDocRecep");
			if (vacio(codPaisRecep))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("CodPaisRecep");
			switch (tipoDocRecep) {
			case RUC:
			case NIE:
			case CI:
				if (!codPaisRecep.equals("UY"))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CodPaisRecep").setDetailMessage("Debe ser UY");
				if (vacio(docRecep))
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("DocRecep");
				break;
			case DNI:
				if (!codPaisRecep.equals("AR") && !codPaisRecep.equals("BR") && !codPaisRecep.equals("CL") && !codPaisRecep.equals("PY"))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CodPaisRecep").setDetailMessage("Debe ser AR o BR o CL o PY");
				if (vacio(docRecepExt))
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("DocRecepExt");
				break;
			default:
				if (vacio(docRecepExt))
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("DocRecepExt");
				break;
			}
		}

		/*
		 * Indicadores de facturacion segun el vendedor (B-C4)
		 */
		boolean conRuc = tipoDocRecep == TipoDocumento.RUC;
		for (ItemInterface item : getItem()) {
			int indFact = item.getIndFact().intValue();
			if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_NO_CONTRIBUYENTE.getIndice() && conRuc)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + item.getNroLinDet() + ": IndFact 13 (vendido por un no contribuyente) no admite un vendedor con RUC");
			if ((indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_IVA_MINIMO.getIndice()
					|| indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_IMEBA.getIndice()) && !conRuc)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + item.getNroLinDet() + ": IndFact " + indFact + " exige identificar al vendedor con su RUC (TipoDocRecep 2)");
		}

		if (conRuc) {
			Empresa vendedor = Empresa.findByRUT(docRecep);
			if (vendedor != null && vendedor.isEmisorElectronico() && !esCompraMEReventa())
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("DocRecep", docRecep)
						.setDetailMessage("El vendedor es emisor electronico: tiene que emitir su propio CFE (la e-Boleta de entrada no corresponde)");
			// El vendedor queda como empresa receptora (como en la e-Factura); a un emisor electronico se le envia el XML
			cfe.setEmpresaReceptora(Empresa.getOrCreateEmpresa(docRecep, rznSocRecep, null));
		}

		if (tipoDocRecep != null)
			receptor.setTipoDocRecep(tipoDocRecep.getId());
		if (codPaisRecep != null)
			receptor.setCodPaisRecep(codPaisRecep);
		if (docRecep != null)
			receptor.setDocRecep(docRecep);
		if (docRecepExt != null)
			receptor.setDocRecepExt(docRecepExt);
		if (rznSocRecep != null)
			receptor.setRznSocRecep(rznSocRecep);
		if (dirRecep != null)
			receptor.setDirRecep(dirRecep);
		if (ciudadRecep != null)
			receptor.setCiudadRecep(ciudadRecep);
		if (deptoRecep != null)
			receptor.setDeptoRecep(deptoRecep);

		if (codPaisRecep != null && docRecep != null) {
			Pais pais = Pais.findByCodigo(codPaisRecep, true);
			cfe.setTitular(getOrCreateTitular(pais, tipoDocRecep, docRecep));
		}

		receptor.setCompraID(compraID);

		cfe.setPdfMailAddress(pdfMailAddress);
	}

	private static boolean vacio(String valor) {
		return valor == null || valor.trim().isEmpty();
	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		// Solo con IndCompraMEReventa=2 (compra por cuenta ajena), que no se admite todavia
		return null;
	}

}

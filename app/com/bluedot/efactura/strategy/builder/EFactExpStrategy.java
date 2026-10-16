package com.bluedot.efactura.strategy.builder;

import java.io.IOException;
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
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.CFEDefType.EFactExp.Detalle;
import dgi.classes.recepcion.CFEDefType.EFactExp.Encabezado;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.IdDocFactExp;
import dgi.classes.recepcion.ItemDetFactExp;
import dgi.classes.recepcion.ReceptorFactExp;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.TotalesFactExp;
import dgi.classes.recepcion.wrappers.IdDocFactExpWrapper;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemDetFactExpWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorFactExpWrapper;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesFactExpWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * e-Factura de Exportacion y sus notas de credito y debito (121, 122, 123 y contingencia 221, 222, 223). El e-Remito
 * de exportacion (124/224) no esta soportado.
 */
public class EFactExpStrategy extends CommonStrategy implements CFEStrategy {

	public EFactExpStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		super(cfe, caeMicroController);
		if (cfe.getEfacturaExportacion() == null)
			this.cfe.setEfacturaExportacion(new EFactExp());

		switch (cfe.getTipo()) {
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
				break;
			default:
				throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Estrategia para el tipo: " + cfe.getTipo().friendlyName);
		}
	}

	private EFactExp getEFactExp() {
		return cfe.getEfacturaExportacion();
	}

	private Encabezado getEncabezado() {
		if (getEFactExp().getEncabezado() == null)
			getEFactExp().setEncabezado(new Encabezado());
		return getEFactExp().getEncabezado();
	}

	private Detalle getDetalle() {
		if (getEFactExp().getDetalle() == null)
			getEFactExp().setDetalle(new Detalle());
		return getEFactExp().getDetalle();
	}

	@Override
	public Emisor getEmisor() {
		if (getEncabezado().getEmisor() == null)
			getEncabezado().setEmisor(new Emisor());
		return getEncabezado().getEmisor();
	}

	private ReceptorInterface getReceptor() {
		if (getEncabezado().getReceptor() == null)
			getEncabezado().setReceptor(new ReceptorFactExp());
		return new ReceptorFactExpWrapper(getEncabezado().getReceptor());
	}

	@Override
	public TotalesInterface getTotales() {
		if (getEncabezado().getTotales() == null)
			getEncabezado().setTotales(new TotalesFactExp());
		return new TotalesFactExpWrapper(getEncabezado().getTotales());
	}

	@Override
	public CAEDataType getCAEData() {
		if (getEFactExp().getCAEData() == null)
			getEFactExp().setCAEData(new CAEDataType());
		return getEFactExp().getCAEData();
	}

	@Override
	public List<ItemInterface> getItem() {
		ArrayList<ItemInterface> list = new ArrayList<ItemInterface>();
		for (ItemDetFactExp item : getDetalle().getItems()) {
			list.add(new ItemDetFactExpWrapper(item));
		}
		return list;
	}

	/** Los items de JAXB, para los campos que no estan en ItemInterface (NCM) */
	public List<ItemDetFactExp> getItemsExportacion() {
		return getDetalle().getItems();
	}

	@Override
	public IdDocInterface getIdDoc() {
		if (getEncabezado().getIdDoc() == null)
			getEncabezado().setIdDoc(new IdDocFactExp());
		return new IdDocFactExpWrapper(getEncabezado().getIdDoc());
	}

	@Override
	public void setIdDoc() throws APIException {
		try {
			IdDocFactExp idDoc = caeMicroController.getIdDocFactExp(cfe.getTipo(), cfe.getFormaDePago().value, cfe.getFechaEmision(), cfe.getEstrategiaNumeracion());
			idDoc.setNroInterno(cfe.getNroInterno());
			// ClauVenta, ModVenta, ViaTransp y los opcionales (validados en DatosIdDoc.leer al crear el CFE)
			DatosIdDoc.aplicar(cfe.getDatosIdDoc(), idDoc);
			getEncabezado().setIdDoc(idDoc);
			cfe.setSerie(idDoc.getSerie());
			cfe.setNro(idDoc.getNro().intValue());
		} catch (DatatypeConfigurationException | IOException e) {
			APIException.raise(e);
		}
	}

	@Override
	public void setCAEData() throws APIException {
		try {
			getEFactExp().setCAEData(caeMicroController.getCAEDataType(cfe.getTipo()));
		} catch (DatatypeConfigurationException | JSONException | ParseException e) {
			APIException.raise(e);
		}
	}

	@Override
	public ItemInterface createItem() {
		ItemDetFactExp item = new ItemDetFactExp();
		getDetalle().getItems().add(item);
		return new ItemDetFactExpWrapper(item);
	}

	@Override
	public ReferenciaTipo getReferenciaTipo() {
		if (getEFactExp().getReferencia() == null)
			getEFactExp().setReferencia(new ReferenciaTipo());
		return getEFactExp().getReferencia();
	}

	@Override
	public void setTimestampFirma(XMLGregorianCalendar newXMLGregorianCalendar) {
		getEFactExp().setTmstFirma(newXMLGregorianCalendar);
	}

	@Override
	public CFE getCFE() {
		return cfe;
	}

	@Override
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String pdfMailAddress, String compraID) throws APIException {
		buildReceptor(tipoDocRecep, codPaisRecep, docRecep, docRecepExt, rznSocRecep, dirRecep, ciudadRecep, deptoRecep, null, null,
				null, null, pdfMailAddress, compraID, false);
	}

	private static boolean vacio(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static void largoMaximo(String campo, String valor, int largo) throws APIException {
		if (valor != null && valor.length() > largo)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
					.setDetailMessage(campo + " admite hasta " + largo + " caracteres");
	}

	private static void obligatorio(String campo, String valor) throws APIException {
		if (vacio(valor))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams(campo)
					.setDetailMessage("Falta " + campo + ", obligatorio en la e-Factura de exportacion y sus notas");
	}

	/**
	 * Receptor de la e-Factura de exportacion (Formato CFE, area receptor A-C60 a A-C70): en general un comprador del
	 * exterior. A diferencia de la e-Factura no se exige RUC ni pais UY.
	 * <ul>
	 * <li>Nombre (A-C63), direccion (A-C64), ciudad (A-C65), departamento/provincia/estado (A-C66) y pais (A-C66.1) son
	 * obligatorios, salvo con secreto profesional (A-C18=1)</li>
	 * <li>La identificacion (A-C60 a A-C62.1) es opcional, pero si viene tiene que ser completa y coherente: tipo de
	 * documento, codigo de pais ISO 3166-1 alfa-2 (99 si no esta en la tabla) y DocRecep (RUC, CI, NIE; pais UY) o
	 * DocRecepExt (otros, pasaporte, DNI de AR/BR/CL/PY, NIFE), nunca los dos</li>
	 * </ul>
	 */
	public void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt,
			String rznSocRecep, String dirRecep, String ciudadRecep, String deptoRecep, String paisRecep, Integer cp,
			String infoAdicional, String lugarDestEnt, String pdfMailAddress, String compraID, boolean secretoProfesional) throws APIException {

		ReceptorInterface receptor = getReceptor();

		if (!secretoProfesional) {
			obligatorio("RznSocRecep", rznSocRecep);
			obligatorio("DirRecep", dirRecep);
			obligatorio("CiudadRecep", ciudadRecep);
			obligatorio("DeptoRecep", deptoRecep);
			obligatorio("PaisRecep", paisRecep);
		}

		largoMaximo("RznSocRecep", rznSocRecep, 150);
		largoMaximo("DirRecep", dirRecep, 70);
		largoMaximo("CiudadRecep", ciudadRecep, 30);
		largoMaximo("DeptoRecep", deptoRecep, 30);
		largoMaximo("PaisRecep", paisRecep, 30);
		largoMaximo("InfoAdicional", infoAdicional, 150);
		largoMaximo("LugarDestEnt", lugarDestEnt, 100);
		largoMaximo("CompraID", compraID, 50);

		if (cp != null && (cp < 0 || cp > 99999))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CP", cp).setDetailMessage("CP admite hasta 5 digitos");

		/*
		 * Identificacion (A-C60, A-C61, A-C62 / A-C62.1): si viene alguno de los datos tienen que venir todos
		 */
		boolean hayIdentificacion = tipoDocRecep != null || !vacio(codPaisRecep) || !vacio(docRecep) || !vacio(docRecepExt);
		if (hayIdentificacion) {
			if (tipoDocRecep == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("TipoDocRecep")
						.setDetailMessage("Si se identifica al receptor hay que mandar TipoDocRecep (2 RUC, 3 CI, 4 otros, 5 pasaporte, 6 DNI, 7 NIFE)");
			if (vacio(codPaisRecep))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("CodPaisRecep")
						.setDetailMessage("Si se identifica al receptor hay que mandar CodPaisRecep (ISO 3166-1 alfa-2, o 99 si no esta en la tabla)");
			codPaisRecep = codPaisRecep.trim().toUpperCase();
			if (!codPaisRecep.matches("[A-Z]{2}") && !codPaisRecep.equals("99"))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CodPaisRecep", codPaisRecep)
						.setDetailMessage("CodPaisRecep tiene que ser el codigo ISO 3166-1 alfa-2 del pais (ej. AR, BR, US) o 99 si no esta en la tabla");
			if (!vacio(docRecep) && !vacio(docRecepExt))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("DocRecepExt")
						.setDetailMessage("Va DocRecep (RUC, CI, NIE) o DocRecepExt (otros, pasaporte, DNI, NIFE), no los dos");

			switch (tipoDocRecep) {
			case RUC:
			case NIE:
			case CI:
				if (!codPaisRecep.equals("UY"))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CodPaisRecep", codPaisRecep)
							.setDetailMessage("Con TipoDocRecep " + tipoDocRecep.getId() + " el pais tiene que ser UY");
				if (vacio(docRecep))
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("DocRecep")
							.setDetailMessage("Con TipoDocRecep " + tipoDocRecep.getId() + " va el documento uruguayo en DocRecep");
				largoMaximo("DocRecep", docRecep, 12);
				break;
			case DNI:
				if (!codPaisRecep.equals("AR") && !codPaisRecep.equals("BR") && !codPaisRecep.equals("CL") && !codPaisRecep.equals("PY"))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CodPaisRecep", codPaisRecep)
							.setDetailMessage("Con DNI (TipoDocRecep 6) el pais tiene que ser AR, BR, CL o PY");
				// sigue: el documento va en DocRecepExt
			case OTROS:
			case PASAPORTE:
			case NIFE:
				if (vacio(docRecepExt))
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("DocRecepExt")
							.setDetailMessage("Con TipoDocRecep " + tipoDocRecep.getId() + " va el documento extranjero en DocRecepExt");
				largoMaximo("DocRecepExt", docRecepExt, 20);
				break;
			}

			receptor.setTipoDocRecep(tipoDocRecep.getId());
			receptor.setCodPaisRecep(codPaisRecep);
			if (!vacio(docRecep))
				receptor.setDocRecep(docRecep.trim());
			if (!vacio(docRecepExt))
				receptor.setDocRecepExt(docRecepExt.trim());
		}

		if (!vacio(rznSocRecep))
			receptor.setRznSocRecep(rznSocRecep);
		if (!vacio(dirRecep))
			receptor.setDirRecep(dirRecep);
		if (!vacio(ciudadRecep))
			receptor.setCiudadRecep(ciudadRecep);
		if (!vacio(deptoRecep))
			receptor.setDeptoRecep(deptoRecep);
		if (!vacio(paisRecep))
			receptor.setPaisRecep(paisRecep);
		if (cp != null)
			receptor.setCP(cp);
		if (!vacio(infoAdicional))
			receptor.setInfoAdicional(infoAdicional);
		if (!vacio(lugarDestEnt))
			receptor.setLugarDestEnt(lugarDestEnt);
		if (!vacio(compraID))
			receptor.setCompraID(compraID);

		if (tipoDocRecep == TipoDocumento.RUC) {
			// Receptor uruguayo con RUC (ej. usuario de zona franca): como en la e-Factura, queda como empresa receptora
			cfe.setEmpresaReceptora(Empresa.getOrCreateEmpresa(docRecep.trim(), rznSocRecep, null));
		} else if (tipoDocRecep != null) {
			// Comprador extranjero: queda como titular si el pais esta en la tabla (como en el e-Ticket)
			Pais pais = Pais.findByCodigo(codPaisRecep, false);
			if (pais != null)
				cfe.setTitular(getOrCreateTitular(pais, tipoDocRecep, !vacio(docRecep) ? docRecep.trim() : docRecepExt.trim()));
		}

		cfe.setPdfMailAddress(pdfMailAddress);
	}

	@Override
	public ComplFiscalType getComplementoFiscal() {
		// La e-Factura de exportacion no tiene complemento fiscal
		return null;
	}

}

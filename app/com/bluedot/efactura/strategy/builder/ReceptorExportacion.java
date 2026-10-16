package com.bluedot.efactura.strategy.builder;

import java.util.function.Supplier;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.wrappers.ReceptorInterface;

/**
 * Receptor de la e-Factura de exportacion, sus notas y el e-Remito de exportacion (Formato CFE v25-2, area receptor
 * A-C60 a A-C70; mismos codigos de obligatoriedad para 121, 122, 123 y 124): en general un comprador del exterior. A
 * diferencia de la e-Factura no se exige RUC ni pais UY.
 * <ul>
 * <li>Nombre (A-C63), direccion (A-C64), ciudad (A-C65), departamento/provincia/estado (A-C66) y pais (A-C66.1) son
 * obligatorios, salvo con secreto profesional (A-C18=1)</li>
 * <li>La identificacion (A-C60 a A-C62.1) es opcional, pero si viene tiene que ser completa y coherente: tipo de
 * documento, codigo de pais ISO 3166-1 alfa-2 (99 si no esta en la tabla) y DocRecep (RUC, CI, NIE; pais UY) o
 * DocRecepExt (otros, pasaporte, DNI de AR/BR/CL/PY, NIFE), nunca los dos</li>
 * </ul>
 */
class ReceptorExportacion {

	private ReceptorExportacion() {
	}

	private static boolean vacio(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static void largoMaximo(String campo, String valor, int largo) throws APIException {
		if (valor != null && valor.length() > largo)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
					.setDetailMessage(campo + " admite hasta " + largo + " caracteres");
	}

	private static void obligatorio(String campo, String valor, String comprobante) throws APIException {
		if (vacio(valor))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams(campo)
					.setDetailMessage("Falta " + campo + ", obligatorio en " + comprobante);
	}

	/**
	 * Valida y arma el receptor.
	 *
	 * @param estrategia
	 *            para crear el titular (comprador extranjero identificado)
	 * @param receptorJaxb
	 *            el receptor del CFE; solo se pide (y se crea en el XML) si hay algun dato para poner
	 * @param comprobante
	 *            para los mensajes, ej. "la e-Factura de exportacion y sus notas"
	 */
	static void armar(CommonStrategy estrategia, CFE cfe, Supplier<ReceptorInterface> receptorJaxb, String comprobante,
			TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt, String rznSocRecep, String dirRecep,
			String ciudadRecep, String deptoRecep, String paisRecep, Integer cp, String infoAdicional, String lugarDestEnt,
			String pdfMailAddress, String compraID, boolean secretoProfesional) throws APIException {

		if (!secretoProfesional) {
			obligatorio("RznSocRecep", rznSocRecep, comprobante);
			obligatorio("DirRecep", dirRecep, comprobante);
			obligatorio("CiudadRecep", ciudadRecep, comprobante);
			obligatorio("DeptoRecep", deptoRecep, comprobante);
			obligatorio("PaisRecep", paisRecep, comprobante);
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

			ReceptorInterface receptor = receptorJaxb.get();
			receptor.setTipoDocRecep(tipoDocRecep.getId());
			receptor.setCodPaisRecep(codPaisRecep);
			if (!vacio(docRecep))
				receptor.setDocRecep(docRecep.trim());
			if (!vacio(docRecepExt))
				receptor.setDocRecepExt(docRecepExt.trim());
		}

		if (!vacio(rznSocRecep))
			receptorJaxb.get().setRznSocRecep(rznSocRecep);
		if (!vacio(dirRecep))
			receptorJaxb.get().setDirRecep(dirRecep);
		if (!vacio(ciudadRecep))
			receptorJaxb.get().setCiudadRecep(ciudadRecep);
		if (!vacio(deptoRecep))
			receptorJaxb.get().setDeptoRecep(deptoRecep);
		if (!vacio(paisRecep))
			receptorJaxb.get().setPaisRecep(paisRecep);
		if (cp != null)
			receptorJaxb.get().setCP(cp);
		if (!vacio(infoAdicional))
			receptorJaxb.get().setInfoAdicional(infoAdicional);
		if (!vacio(lugarDestEnt))
			receptorJaxb.get().setLugarDestEnt(lugarDestEnt);
		if (!vacio(compraID))
			receptorJaxb.get().setCompraID(compraID);

		if (tipoDocRecep == TipoDocumento.RUC) {
			// Receptor uruguayo con RUC (ej. usuario de zona franca): como en la e-Factura, queda como empresa receptora
			cfe.setEmpresaReceptora(Empresa.getOrCreateEmpresa(docRecep.trim(), rznSocRecep, null));
		} else if (tipoDocRecep != null) {
			// Comprador extranjero: queda como titular si el pais esta en la tabla (como en el e-Ticket)
			Pais pais = Pais.findByCodigo(codPaisRecep, false);
			if (pais != null)
				cfe.setTitular(estrategia.getOrCreateTitular(pais, tipoDocRecep, !vacio(docRecep) ? docRecep.trim() : docRecepExt.trim()));
		}

		cfe.setPdfMailAddress(pdfMailAddress);
	}

}

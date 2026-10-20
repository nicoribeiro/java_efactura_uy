package com.bluedot.efactura.controllers;

import com.bluedot.efactura.commons.Orden;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.model.CFE;
import com.bluedot.commons.security.User;
import com.bluedot.efactura.model.DireccionDocumento;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.pollers.PollerManager;
import com.bluedot.efactura.recibidos.ControlesRecepcion;
import com.bluedot.efactura.recibidos.ControlesSobre;
import com.bluedot.efactura.recibidos.MontosRecibidos;
import com.bluedot.efactura.recibidos.RespuestaCfes;
import com.bluedot.efactura.model.MotivoRechazoCFE;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de Sobres") 
public class SobresController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(SobresController.class);

	@Inject
	public SobresController(PollerManager pollerManager){
		super();
	}
	
	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	/**
	 * Envia ahora la respuesta a los CFE (ME_) de un sobre recibido que la tiene pendiente, sin esperar el plazo de la
	 * empresa. ADMIN y USER con acceso a la empresa.
	 */
	public Promise<Result> responderCfes(String rut, Long id) throws APIException {
		com.bluedot.commons.security.User usuario = PermissionValidator.getSessionUser(ctx());
		Empresa empresa = Empresa.findByRUT(rut, true);
		SobreRecibido sobre = sobreRecibido(empresa, id);
		if (!sobre.isRespuestaCfePendiente())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("sobre", id)
					.setDetailMessage("El sobre no tiene la respuesta a sus CFE pendiente");
		com.bluedot.efactura.model.Respuesta respuesta = RespuestaCfes.enviar(sobre, usuario.getEmailAddress());
		auditoria.info(usuario.getEmailAddress() + " envió la respuesta a los CFE del sobre " + sobre.getId() + " (" + sobre.getNombreArchivo()
				+ ") de la empresa " + rut + ": " + respuesta.getNombreArchivo());
		JSONObject resultado = new JSONObject();
		resultado.put("respuesta", respuesta.getNombreArchivo());
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/**
	 * Rechaza a mano un CFE recibido (rechazo comercial, E20 a E26) o deshace ese rechazo, mientras la respuesta a los CFE
	 * de su sobre esta pendiente: el rechazo va en esa respuesta (ME_). Body: {"motivo": "E20".."E26", "detalle": "..."};
	 * motivo vacio o null deshace el rechazo comercial. ADMIN y USER con acceso a la empresa.
	 */
	public Promise<Result> rechazoCfe(String rut, Long id) throws APIException {
		com.bluedot.commons.security.User usuario = PermissionValidator.getSessionUser(ctx());
		Empresa empresa = Empresa.findByRUT(rut, true);
		CFE cfe = id != null && id <= Integer.MAX_VALUE ? CFE.findById(id.intValue()) : null;
		if (cfe == null || cfe.getSobreRecibido() == null || cfe.getEmpresaReceptora() == null || cfe.getEmpresaReceptora().getId() != empresa.getId())
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("id", id);
		SobreRecibido sobre = cfe.getSobreRecibido();
		if (!sobre.isRespuestaCfePendiente())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("cfe", id)
					.setDetailMessage("La respuesta a los CFE de este sobre ya se envió (o es anterior a esta funcionalidad): ya no se puede rechazar");

		com.fasterxml.jackson.databind.JsonNode body = request().body().asJson();
		String motivoTexto = body != null && body.hasNonNull("motivo") ? body.get("motivo").asText().trim() : "";
		String detalle = body != null && body.hasNonNull("detalle") ? body.get("detalle").asText().trim() : "";

		// Se sacan los rechazos comerciales que tuviera; quedan los de los controles de recepcion (enforce)
		cfe.getMotivo().removeIf(RespuestaCfes::esComercial);
		String doc = cfe.getTipo().value + " " + cfe.getSerie() + "-" + cfe.getNro();
		if (motivoTexto.isEmpty()) {
			cfe.setDetalleRechazo(null);
			cfe.setEstado(cfe.getMotivo().isEmpty() ? dgi.classes.respuestas.cfe.EstadoACKCFEType.AE : dgi.classes.respuestas.cfe.EstadoACKCFEType.BE);
			ControlesRecepcion.observar(cfe, "Rechazo comercial deshecho", "por " + usuario.getEmailAddress());
			auditoria.info(usuario.getEmailAddress() + " deshizo el rechazo comercial del CFE recibido " + doc + " (id " + cfe.getId() + ") de la empresa " + rut);
		} else {
			MotivoRechazoCFE motivo;
			try {
				motivo = MotivoRechazoCFE.valueOf(motivoTexto.toUpperCase());
			} catch (IllegalArgumentException e) {
				motivo = null;
			}
			if (motivo == null || !RespuestaCfes.esComercial(motivo))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("motivo", motivoTexto)
						.setDetailMessage("El motivo tiene que ser un rechazo comercial (E20 a E26)");
			cfe.getMotivo().add(motivo);
			cfe.setDetalleRechazo(detalle.isEmpty() ? null : detalle);
			cfe.setEstado(dgi.classes.respuestas.cfe.EstadoACKCFEType.BE);
			ControlesRecepcion.observar(cfe, motivo.name() + " (rechazo comercial)",
					motivo.getMotivo() + (detalle.isEmpty() ? "" : ": " + detalle) + ", por " + usuario.getEmailAddress());
			auditoria.info(usuario.getEmailAddress() + " rechazó el CFE recibido " + doc + " (id " + cfe.getId() + ") de la empresa " + rut + " con "
					+ motivo.name() + (detalle.isEmpty() ? "" : ": " + detalle));
		}
		cfe.update();

		JSONObject resultado = new JSONObject();
		resultado.put("estado", cfe.getEstado().name());
		resultado.put("motivosRechazo", new JSONArray(cfe.getMotivo().stream().map(Enum::name).toArray()));
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/**
	 * Vista previa de la respuesta a los CFE (ME_) de un sobre recibido: el XML firmado como se enviaria y si cumple el XSD
	 * de DGI. No guarda ni envia nada. Solo ADMIN.
	 */
	public Promise<Result> vistaPreviaRespuestaCfes(String rut, Long id) throws APIException {
		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "GET /empresa/" + rut + "/sobres/recibidos/" + id + "/respuestaCfes", true);
		Empresa empresa = Empresa.findByRUT(rut, true);
		SobreRecibido sobre = sobreRecibido(empresa, id);
		String[] vista = RespuestaCfes.vistaPrevia(sobre);
		JSONObject resultado = new JSONObject();
		resultado.put("sobre", sobre.getId());
		resultado.put("pendiente", sobre.isRespuestaCfePendiente());
		resultado.put("yaRespondido", sobre.getRespuestaCfes() != null);
		java.util.Date programada = RespuestaCfes.programada(sobre);
		resultado.put("programada", programada != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(programada) : JSONObject.NULL);
		resultado.put("cumpleXsd", vista[1] == null);
		resultado.put("errores", vista[1] != null ? vista[1] : JSONObject.NULL);
		resultado.put("xml", vista[0]);
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/** Un sobre recibido por la empresa, o error si no existe o no es de ella */
	private static SobreRecibido sobreRecibido(Empresa empresa, Long id) throws APIException {
		Sobre sobre = id != null ? Sobre.findById(id) : null;
		if (!(sobre instanceof SobreRecibido) || sobre.getEmpresaReceptora() == null || sobre.getEmpresaReceptora().getId() != empresa.getId())
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO).withParams("id", id);
		return (SobreRecibido) sobre;
	}

	/**
	 * Marca como rechazadas con E02 las copias de CFE recibidos repetidos (mismo emisor, tipo, serie y numero recibido en
	 * varios sobres, ej. UTE que reenvia el sobre mientras espera la respuesta a los CFE): queda aceptada la primera copia
	 * aceptada y las demas aceptadas pasan a BE con motivo E02, como con RecepcionCFE.control.E02 = enforce. No borra nada
	 * ni manda mails; la respuesta a los CFE que este pendiente las rechaza con E02. Solo ADMIN.
	 *
	 * Query: guardar=1 para guardar; si no, solo informa que copias marcaria.
	 */
	public Promise<Result> marcarDuplicadosRecibidos(String rut, int guardar) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "POST /empresa/" + rut + "/sobres/recibidos/marcarDuplicados", true);
		Empresa empresa = Empresa.findByRUT(rut, true);
		SimpleDateFormat dia = new SimpleDateFormat("dd/MM/yyyy");

		JSONArray copias = new JSONArray();
		List<String> resumen = new LinkedList<String>();
		String grupo = null;
		CFE original = null;
		for (CFE cfe : CFE.findRecibidosParaDuplicados(empresa)) {
			// Vienen ordenados por emisor, tipo, serie, numero y fecha de recepcion: las copias de un CFE quedan juntas
			String clave = (cfe.getEmpresaEmisora() != null ? cfe.getEmpresaEmisora().getId() : 0) + "|" + cfe.getTipo() + "|" + cfe.getSerie() + "|"
					+ cfe.getNro();
			if (!clave.equals(grupo)) {
				grupo = clave;
				original = null;
			}
			if (cfe.getEstado() != EstadoACKCFEType.AE)
				continue;
			// La primera copia aceptada queda como original
			if (original == null) {
				original = cfe;
				continue;
			}
			String detalle = "ya se había recibido y aceptado: id " + original.getId()
					+ (original.getSobreRecibido().getFecha() != null ? " (sobre del " + dia.format(original.getSobreRecibido().getFecha()) + ")" : "");
			copias.put(new JSONObject().put("id", cfe.getId()).put("emisor", cfe.getEmpresaEmisora().getRut()).put("tipo", cfe.getTipo().name())
					.put("serie", cfe.getSerie()).put("nro", cfe.getNro()).put("sobre", cfe.getSobreRecibido().getId())
					.put("original", original.getId()).put("detalle", detalle));
			resumen.add(cfe.getSerie() + "-" + cfe.getNro() + " id " + cfe.getId() + " (original " + original.getId() + ")");
			if (guardar == 1) {
				cfe.setEstado(EstadoACKCFEType.BE);
				if (!cfe.getMotivo().contains(MotivoRechazoCFE.E02))
					cfe.getMotivo().add(MotivoRechazoCFE.E02);
				ControlesRecepcion.observar(cfe, "E02 (copia marcada)", detalle);
				cfe.update();
			}
		}

		if (guardar == 1)
			auditoria.info(usuario.getEmailAddress() + " marcó con E02 " + copias.length() + " copia(s) de CFE recibidos repetidos de la empresa "
					+ rut + (resumen.isEmpty() ? "" : ": " + String.join(", ", resumen)));

		return json(new JSONObject().put("guardado", guardar == 1).put("cantidad", copias.length()).put("copias", copias).toString());
	}

	/**
	 * Corrige los montos de los CFE recibidos con el XML de cada sobre: los totales (antes se calculaban a partir de las
	 * lineas, sin descuentos ni redondeos) y los descuentos y recargos de las lineas (antes no se guardaban). Solo ADMIN.
	 *
	 * Query: fromDate y toDate (segundos, opcionales, por fecha del sobre); guardar=1 para guardar, si no solo informa.
	 */
	public Promise<Result> corregirMontosRecibidos(String rut, int guardar) throws APIException {

		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "POST /empresa/" + rut + "/sobres/recibidos/corregirMontos", true);

		Empresa empresa = Empresa.findByRUT(rut, true);

		Date fromDate;
		Date toDate;
		try {
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate o toDate invalidos");
		}

		JSONArray corregidos = new JSONArray();
		int sobres = 0;
		for (int page = 1;; page++) {
			List<Sobre> pagina = Sobre.findByEmpresa(empresa, DireccionDocumento.RECIBIDO, (Sobre.Filtros) null, fromDate, toDate, page, 200).item1;
			for (Sobre sobre : pagina) {
				sobres++;
				for (JSONObject corregido : MontosRecibidos.corregirSobre(sobre, guardar > 0)) {
					corregido.put("sobre", sobre.getId());
					corregido.put("emisor", sobre.getEmpresaEmisora() != null ? sobre.getEmpresaEmisora().getRut() : JSONObject.NULL);
					corregidos.put(corregido);
				}
			}
			if (pagina.size() < 200)
				break;
		}

		if (guardar > 0)
			auditoria.info(PermissionValidator.getSessionUser(ctx()).getEmailAddress() + " corrigió los montos de "
					+ corregidos.length() + " CFE recibidos de la empresa " + rut + " (" + sobres + " sobres revisados)");

		JSONObject resultado = new JSONObject();
		resultado.put("guardado", guardar > 0);
		resultado.put("sobresRevisados", sobres);
		resultado.put("cantidad", corregidos.length());
		resultado.put("corregidos", corregidos);
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/**
	 * Simula los controles de recepcion de los sobres (S02, S03, S04, S05, S06, S20) y de los CFE (E02, E03, E04, E05, E07, E10, E12) sobre los CFE de los sobres recibidos ya guardados, sin
	 * cambiar nada: cuantos no pasarian cada control y ejemplos. Sirve para medir un control antes de activarlo. Solo
	 * ADMIN. Query: fromDate y toDate (segundos, por fecha del sobre), limite (sobres, por defecto 500).
	 */
	public Promise<Result> simularControles(String rut) throws APIException {
		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "GET /empresa/" + rut + "/sobres/recibidos/simularControles", true);
		Empresa empresa = Empresa.findByRUT(rut, true);

		Date fromDate;
		Date toDate;
		int limite;
		try {
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
			limite = request().getQueryString("limite") != null ? Integer.parseInt(request().getQueryString("limite")) : 500;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate, toDate o limite invalidos");
		}

		String[] controles = { "S02", "S03", "S04", "S05", "S06", "S20", "E02", "E03", "E04", "E05", "E07", "E10", "E12" };
		Map<String, Integer> fallan = new HashMap<String, Integer>();
		Map<String, JSONArray> ejemplos = new HashMap<String, JSONArray>();
		for (String c : controles) {
			fallan.put(c, 0);
			ejemplos.put(c, new JSONArray());
		}
		int sobres = 0, cfes = 0;

		List<Sobre> lista = Sobre.findByEmpresa(empresa, DireccionDocumento.RECIBIDO, (Sobre.Filtros) null, fromDate, toDate, 1, Math.max(1, limite)).item1;
		for (Sobre sobre : lista) {
			sobres++;
			dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas envio = sobre.getEnvioCFEEntreEmpresas();
			if (envio == null)
				continue;

			// Controles del sobre
			Map<String, String> delSobre = new HashMap<String, String>();
			try {
				ControlesSobre.Sobre xmlSobre = ControlesSobre.leer(sobre.getXmlEmpresa());
				delSobre.put("S02", ControlesSobre.s02(xmlSobre));
				delSobre.put("S03", ControlesSobre.s03(xmlSobre));
				delSobre.put("S04", ControlesSobre.s04(xmlSobre));
				delSobre.put("S05", ControlesSobre.s05(xmlSobre));
				delSobre.put("S06", ControlesSobre.s06(xmlSobre));
			} catch (Exception e) {
				delSobre.put("S04", "no se pudo leer el XML del sobre: " + e.getMessage());
			}
			if (sobre instanceof SobreRecibido && envio.getCaratula() != null && envio.getCaratula().getIdemisor() != null)
				delSobre.put("S20", ControlesSobre.s20((SobreRecibido) sobre, envio.getCaratula().getIdemisor().longValue()));
			for (Map.Entry<String, String> r : delSobre.entrySet()) {
				if (r.getValue() == null)
					continue;
				fallan.put(r.getKey(), fallan.get(r.getKey()) + 1);
				if (ejemplos.get(r.getKey()).length() < 20) {
					JSONObject ej = new JSONObject();
					ej.put("sobre", sobre.getId());
					ej.put("archivo", sobre.getNombreArchivo());
					ej.put("cfe", "(sobre)");
					ej.put("detalle", r.getValue());
					ejemplos.get(r.getKey()).put(ej);
				}
			}
			int ordinal = 0;
			for (dgi.classes.entreEmpresas.CFEEmpresasType cfeEmpresas : envio.getCFEAdendas()) {
				ordinal++;
				dgi.classes.recepcion.CFEDefType xml = cfeEmpresas.getCFE();
				if (xml == null)
					continue;
				cfes++;
				CFE cfe = cfeDelSobre(sobre, xml);
				Map<String, String> resultado = new HashMap<String, String>();
				try {
					if (cfe != null)
						resultado.put("E02", ControlesRecepcion.e02(cfe, empresa));
				} catch (APIException e) {
					resultado.put("E02", "error: " + e.getMessage());
				}
				if (cfe != null)
					resultado.put("E03", ControlesRecepcion.e03(xml, cfe.getNro()));
				resultado.put("E04", ControlesRecepcion.e04(sobre.getXmlEmpresa(), ordinal, xml));
				resultado.put("E05", ControlesRecepcion.e05(sobre.getXmlEmpresa(), ordinal));
				resultado.put("E07", ControlesRecepcion.e07(xml));
				resultado.put("E10", ControlesRecepcion.e10(xml));
				if (cfe != null)
					resultado.put("E12", ControlesRecepcion.e12(xml, empresa, cfe.getEmpresaEmisora()));
				for (String c : controles) {
					String detalle = resultado.get(c);
					if (detalle == null)
						continue;
					fallan.put(c, fallan.get(c) + 1);
					if (ejemplos.get(c).length() < 20) {
						JSONObject ej = new JSONObject();
						ej.put("sobre", sobre.getId());
						ej.put("archivo", sobre.getNombreArchivo());
						ej.put("cfe", cfe != null ? cfe.getTipo().value + " " + cfe.getSerie() + "-" + cfe.getNro() : "(no guardado)");
						ej.put("detalle", detalle);
						ejemplos.get(c).put(ej);
					}
				}
			}
		}

		JSONObject resumen = new JSONObject();
		resumen.put("sobres", sobres);
		resumen.put("cfes", cfes);
		JSONObject porControl = new JSONObject();
		for (String c : controles) {
			JSONObject r = new JSONObject();
			r.put("noPasan", fallan.get(c));
			r.put("ejemplos", ejemplos.get(c));
			porControl.put(c, r);
		}
		resumen.put("controles", porControl);
		return json(JSONUtils.merge(resumen, new JSONObject(OK)).toString());
	}

	/** El CFE guardado que corresponde a un CFE del XML del sobre (por tipo, serie y numero) */
	private static CFE cfeDelSobre(Sobre sobre, dgi.classes.recepcion.CFEDefType xml) {
		java.math.BigInteger tipo = null, nro = null;
		String serie = null;
		if (xml.getEFact() != null) {
			tipo = xml.getEFact().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEFact().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEFact().getEncabezado().getIdDoc().getNro();
		} else if (xml.getETck() != null) {
			tipo = xml.getETck().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getETck().getEncabezado().getIdDoc().getSerie();
			nro = xml.getETck().getEncabezado().getIdDoc().getNro();
		} else if (xml.getERem() != null) {
			tipo = xml.getERem().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getERem().getEncabezado().getIdDoc().getSerie();
			nro = xml.getERem().getEncabezado().getIdDoc().getNro();
		} else if (xml.getEResg() != null) {
			tipo = xml.getEResg().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEResg().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEResg().getEncabezado().getIdDoc().getNro();
		} else if (xml.getEFactExp() != null) {
			tipo = xml.getEFactExp().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEFactExp().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEFactExp().getEncabezado().getIdDoc().getNro();
		} else if (xml.getERemExp() != null) {
			tipo = xml.getERemExp().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getERemExp().getEncabezado().getIdDoc().getSerie();
			nro = xml.getERemExp().getEncabezado().getIdDoc().getNro();
		} else if (xml.getEBoleta() != null) {
			tipo = xml.getEBoleta().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEBoleta().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEBoleta().getEncabezado().getIdDoc().getNro();
		}
		if (tipo == null || nro == null)
			return null;
		return sobre.getCFE(nro.longValue(), serie, com.bluedot.efactura.model.TipoDoc.fromInt(tipo.intValue()));
	}

	/**
	 * Vuelve a procesar la respuesta de DGI a los CFE de un sobre emitido que ya esta guardada, sin consultar a DGI ni mandar
	 * mails: completa estados, motivos y el texto de DGI que no se guardaron (ej. motivos que antes no se conocian). Solo ADMIN.
	 * Devuelve como quedo cada CFE del sobre.
	 */
	public Promise<Result> reprocesarResultadoDgi(String rut, long sobreId) throws APIException {
		com.bluedot.commons.security.User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "POST /empresa/" + rut + "/sobres/emitidos/" + sobreId + "/reprocesarResultadoDgi", true);
		Sobre sobre = sobreDeLaEmpresa(Empresa.findByRUT(rut, true), sobreId);
		if (!(sobre instanceof SobreEmitido))
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO).withParams("id", sobreId);

		new com.bluedot.efactura.services.impl.RecepcionServiceImpl().reprocesarResultadoSobre((SobreEmitido) sobre);

		JSONArray cfes = new JSONArray();
		List<String> resumen = new java.util.ArrayList<String>();
		for (CFE cfe : sobre.getCfes()) {
			JSONArray motivos = new JSONArray();
			for (com.bluedot.efactura.model.MotivoRechazoCFE m : cfe.getMotivo())
				motivos.put(m.name());
			cfes.put(new JSONObject().put("tipo", cfe.getTipo().name()).put("serie", cfe.getSerie()).put("nro", cfe.getNro())
					.put("estado", cfe.getEstado() != null ? cfe.getEstado().name() : JSONObject.NULL).put("motivos", motivos)
					.put("detalleRechazo", cfe.getDetalleRechazo() != null ? cfe.getDetalleRechazo() : JSONObject.NULL));
			resumen.add(cfe.getSerie() + "-" + cfe.getNro() + " " + cfe.getEstado() + " " + motivos);
		}
		auditoria.info(usuario.getEmailAddress() + " reprocesó la respuesta de DGI del sobre emitido " + sobre.getId() + " de la empresa " + rut
				+ ": " + String.join(", ", resumen));
		return json(new JSONObject().put("sobre", sobre.getId()).put("cfes", cfes).toString());
	}

	public Promise<Result> enviarSobreEmpresa(String rut, long sobreId) throws APIException {

		Sobre sobre = sobreDeLaEmpresa(Empresa.findByRUT(rut, true), sobreId);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		JSONObject error = null;
		
		if (sobre instanceof SobreEmitido)
			try {
				factory.getServiceMicroController(sobre.getEmpresaEmisora()).enviarCorreoReceptorElectronico((SobreEmitido)sobre);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}
		
		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}
	
	
	public Promise<Result> getSobre(String rut, long sobreId) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Sobre sobre = sobreDeLaEmpresa(empresa, sobreId);
		boolean emitido = sobre instanceof SobreEmitido;

		JSONObject cfeJson;
		try {	
			cfeJson = EfacturaJSONSerializerProvider.getSobreSerializer().objectToJson(sobre);
			cfeJson.put("direccion", emitido ? DireccionDocumento.EMITIDO.name() : DireccionDocumento.RECIBIDO.name());
			// Comprobantes del sobre identificados por tipo, serie y numero (cfes solo trae los id internos)
			JSONArray comprobantes = new JSONArray();
			for (CFE cfe : sobre.getCfes())
				comprobantes.put(resumenCfe(cfe));
			cfeJson.put("comprobantes", comprobantes);
		}catch (JSONException e) {
			throw APIException.raise(e);
		}
		
		return json(cfeJson.toString());
		
	}

	/**
	 * Rechazos de las empresas receptoras en el periodo, en dos contadores separados (inicio del panel):
	 * <ul>
	 * <li>sobres: sobres emitidos que el receptor rechazo entero (respuesta M_ con BS)</li>
	 * <li>cfes: comprobantes que el receptor rechazo (respuesta ME_ con BE)</li>
	 * </ul>
	 * Cada uno con el total y, por empresa receptora, la cantidad y los motivos. Los motivos salen de las respuestas
	 * guardadas (no hay campo en la base). Solo lectura.
	 *
	 * Parametros: fromDate y toDate (epoch en segundos; si faltan, los ultimos 30 dias, como el resumen de documentos).
	 */
	public Promise<Result> getRechazosReceptor(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);
		Date hasta;
		Date desde;
		try {
			hasta = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : new Date();
			desde = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000)
					: com.bluedot.commons.utils.DateHandler.add(hasta, -29, java.util.Calendar.DAY_OF_MONTH);
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate y toDate deben ser numericos (epoch en segundos)");
		}

		// Sobres rechazados enteros por el receptor (M_ con BS)
		Rechazos sobres = new Rechazos();
		for (SobreEmitido sobre : play.db.jpa.JPA.em()
				.createQuery("select s from SobreEmitido s where s.empresaEmisora = :empresa and s.estadoEmpresa = :rechazado"
						+ " and s.fecha between :desde and :hasta", SobreEmitido.class)
				.setParameter("empresa", empresa).setParameter("rechazado", dgi.classes.respuestas.sobre.EstadoACKSobreType.BS)
				.setParameter("desde", desde).setParameter("hasta", hasta).getResultList())
			sobres.agregarSobre(sobre.getEmpresaReceptora(), payload(sobre.getRespuestaSobre()), sobre.getId());

		// Comprobantes rechazados por el receptor (ME_ con BE): la respuesta es una por sobre, con el estado de cada CFE
		Rechazos cfes = new Rechazos();
		for (SobreEmitido sobre : play.db.jpa.JPA.em()
				.createQuery("select s from SobreEmitido s where s.empresaEmisora = :empresa and s.respuestaCfes is not null"
						+ " and s.fecha between :desde and :hasta", SobreEmitido.class)
				.setParameter("empresa", empresa).setParameter("desde", desde).setParameter("hasta", hasta).getResultList())
			cfes.agregarCfes(sobre.getEmpresaReceptora(), payload(sobre.getRespuestaCfes()), sobre.getId());

		SimpleDateFormat dia = new SimpleDateFormat("yyyy-MM-dd");
		return json(new JSONObject().put("desde", dia.format(desde)).put("hasta", dia.format(hasta)).put("sobres", sobres.json())
				.put("cfes", cfes.json()).toString());
	}

	private static String payload(com.bluedot.efactura.model.Respuesta respuesta) {
		return respuesta != null ? respuesta.getPayload() : null;
	}

	/** Conteo de rechazos por empresa receptora y motivo, a partir de las respuestas M_ y ME_ guardadas */
	static class Rechazos {
		/** Sobres o comprobantes que se listan por empresa, para el detalle */
		static final int MAXIMO_LISTADOS = 20;

		private final Map<String, JSONObject> porEmpresa = new java.util.LinkedHashMap<String, JSONObject>();
		private final Map<String, Map<String, int[]>> motivos = new HashMap<String, Map<String, int[]>>();
		private final Map<String, String> glosas = new HashMap<String, String>();
		private int total;

		/** Un sobre que el receptor rechazo entero (M_ con BS): suma uno, con los motivos de la respuesta */
		void agregarSobre(Empresa receptora, String payloadM, long idSobre) {
			JSONObject fila = sumar(receptora);
			dgi.classes.respuestas.sobre.ACKSobredefType ack = leer(payloadM, dgi.classes.respuestas.sobre.ACKSobredefType.class, idSobre);
			if (ack != null && ack.getDetalle() != null)
				for (dgi.classes.respuestas.sobre.RechazoSobreType m : ack.getDetalle().getMotivosRechazo())
					motivo(receptora, m.getMotivo(), m.getGlosa());
			listar(fila, new JSONObject().put("idSobre", idSobre));
		}

		/** La respuesta a los CFE de un sobre (ME_): suma uno por cada CFE rechazado (BE), con sus motivos */
		void agregarCfes(Empresa receptora, String payloadME, long idSobre) {
			dgi.classes.respuestas.cfe.ACKCFEdefType ack = leer(payloadME, dgi.classes.respuestas.cfe.ACKCFEdefType.class, idSobre);
			if (ack == null)
				return;
			for (dgi.classes.respuestas.cfe.ACKCFEdefType.ACKCFEDet det : ack.getACKCFEDet()) {
				if (det.getEstado() != EstadoACKCFEType.BE)
					continue;
				JSONObject fila = sumar(receptora);
				for (dgi.classes.respuestas.cfe.RechazoCFEDGIType m : det.getMotivosRechazoCF())
					motivo(receptora, m.getMotivo(), m.getGlosa());
				listar(fila, new JSONObject().put("idTipoDoc", det.getTipoCFE() != null ? det.getTipoCFE().intValue() : JSONObject.NULL)
						.put("serie", det.getSerie() != null ? det.getSerie() : JSONObject.NULL)
						.put("nro", det.getNroCFE() != null ? det.getNroCFE().longValue() : JSONObject.NULL).put("idSobre", idSobre));
			}
		}

		JSONObject json() {
			List<JSONObject> filas = new java.util.ArrayList<JSONObject>();
			for (Map.Entry<String, JSONObject> e : porEmpresa.entrySet()) {
				JSONArray lista = new JSONArray();
				for (Map.Entry<String, int[]> m : motivos.get(e.getKey()).entrySet())
					lista.put(new JSONObject().put("motivo", m.getKey()).put("glosa", glosas.containsKey(m.getKey()) ? glosas.get(m.getKey()) : JSONObject.NULL)
							.put("cantidad", m.getValue()[0]));
				filas.add(e.getValue().put("motivos", lista));
			}
			filas.sort((a, b) -> Integer.compare(b.getInt("cantidad"), a.getInt("cantidad")));
			return new JSONObject().put("total", total).put("porEmpresa", new JSONArray(filas));
		}

		private static String clave(Empresa receptora) {
			return receptora != null ? receptora.getRut() : "";
		}

		private JSONObject sumar(Empresa receptora) {
			total++;
			JSONObject fila = porEmpresa.get(clave(receptora));
			if (fila == null) {
				fila = new JSONObject().put("rut", receptora != null ? receptora.getRut() : JSONObject.NULL)
						.put("razon", receptora != null && receptora.getRazon() != null ? receptora.getRazon() : JSONObject.NULL)
						.put("cantidad", 0).put("listados", new JSONArray());
				porEmpresa.put(clave(receptora), fila);
				motivos.put(clave(receptora), new java.util.TreeMap<String, int[]>());
			}
			fila.put("cantidad", fila.getInt("cantidad") + 1);
			return fila;
		}

		private void motivo(Empresa receptora, String codigo, String glosa) {
			String c = codigo != null ? codigo : "?";
			motivos.get(clave(receptora)).computeIfAbsent(c, k -> new int[1])[0]++;
			if (glosa != null)
				glosas.putIfAbsent(c, glosa);
		}

		private void listar(JSONObject fila, JSONObject item) {
			JSONArray listados = fila.getJSONArray("listados");
			if (listados.length() < MAXIMO_LISTADOS)
				listados.put(item);
		}

		/** Respuesta de un receptor leida como la clase de DGI; null si no esta o no se puede leer */
		private static <T> T leer(String payload, Class<T> clase, long idSobre) {
			if (payload == null)
				return null;
			try {
				return clase.cast(com.bluedot.commons.utils.XML.unMarshall(com.bluedot.commons.utils.XML.loadXMLFromString(payload), clase));
			} catch (Exception e) {
				logger.warn("No se pudo leer la respuesta del receptor del sobre " + idSobre + ": " + e.getMessage());
				return null;
			}
		}
	}

	/**
	 * Sobres de la empresa (emitidos o recibidos), paginados, con sus comprobantes resumidos. Solo lectura.
	 * 
	 * Parametros: direccion (EMITIDO por defecto), fromDate y toDate (epoch en segundos), estado (AS, BS o SIN), page y
	 * pageSize (maximo 50).
	 */
	public Promise<Result> getSobres(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		DireccionDocumento direccion;
		Date fromDate;
		Date toDate;
		int page;
		int pageSize;
		try {
			direccion = request().getQueryString("direccion") != null ? DireccionDocumento.valueOf(request().getQueryString("direccion")) : DireccionDocumento.EMITIDO;
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
			page = request().getQueryString("page") != null ? Integer.parseInt(request().getQueryString("page")) : 1;
			pageSize = request().getQueryString("pageSize") != null ? Integer.parseInt(request().getQueryString("pageSize")) : 20;
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("direccion, fromDate, toDate, page o pageSize invalidos");
		}
		if (page <= 0)
			page = 1;
		if (pageSize <= 0)
			pageSize = 20;
		pageSize = Math.min(pageSize, 50);

		// estado (compatibilidad): el de DGI en los emitidos, M en los recibidos
		Sobre.Filtros filtros = new Sobre.Filtros();
		String estado = parametro("estado", "AS|BS|SIN");
		filtros.estadoDgi = parametro("estadoDgi", "AS|BS|SIN");
		filtros.respuestaM = parametro("respuestaM", "AS|BS|SIN|NO_CORRESPONDE");
		filtros.respuestaME = parametro("respuestaME", "PENDIENTE|ENVIADA|SIN");
		if (estado != null) {
			if (direccion == DireccionDocumento.EMITIDO && filtros.estadoDgi == null)
				filtros.estadoDgi = estado;
			else if (direccion == DireccionDocumento.RECIBIDO && filtros.respuestaM == null)
				filtros.respuestaM = estado;
		}
		filtros.rutContraparte = request().getQueryString("rutContraparte");
		filtros.razonContraparte = request().getQueryString("razonContraparte");

		Orden orden = Orden.leer(request(), direccion == DireccionDocumento.EMITIDO ? Sobre.ORDENES_EMITIDOS : Sobre.ORDENES_RECIBIDOS);
		Tuple<List<Sobre>, Long> sobres = Sobre.findByEmpresa(empresa, direccion, filtros, fromDate, toDate, page, pageSize, orden);

		// Comprobantes de todos los sobres de la pagina en una sola consulta
		List<Long> ids = new LinkedList<Long>();
		for (Sobre sobre : sobres.item1)
			ids.add(sobre.getId());
		Map<Long, JSONArray> comprobantes = new HashMap<Long, JSONArray>();
		for (CFE cfe : CFE.findBySobres(ids, direccion)) {
			Sobre sobre = direccion == DireccionDocumento.EMITIDO ? cfe.getSobreEmitido() : cfe.getSobreRecibido();
			if (!comprobantes.containsKey(sobre.getId()))
				comprobantes.put(sobre.getId(), new JSONArray());
			comprobantes.get(sobre.getId()).put(resumenCfe(cfe));
		}

		JSONArray lista = new JSONArray();
		for (Sobre sobre : sobres.item1)
			lista.put(resumenSobre(sobre, comprobantes.containsKey(sobre.getId()) ? comprobantes.get(sobre.getId()) : new JSONArray()));

		return json(JSONUtils.createObjectList(lista, sobres.item2, page, pageSize).toString());
	}

	/** Parametro de la query con valores permitidos (regex, sin distinguir mayusculas); null si no viene o esta vacio */
	private String parametro(String nombre, String permitidos) throws APIException {
		String valor = request().getQueryString(nombre);
		if (valor == null || valor.trim().isEmpty())
			return null;
		if (!valor.trim().matches("(?i)" + permitidos))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(nombre, valor);
		return valor.trim().toUpperCase();
	}

	/**
	 * Sobre por id, solo si es de la empresa del path (lo emitio o lo recibio). Si no, SOBRE_NO_ENCONTRADO, como si no
	 * existiera: con acceso a una empresa no se pueden ver ni reenviar sobres de otra.
	 */
	private static Sobre sobreDeLaEmpresa(Empresa empresa, long sobreId) throws APIException {
		Sobre sobre = Sobre.findById(sobreId, true);
		Empresa duenia = sobre instanceof SobreEmitido ? sobre.getEmpresaEmisora() : sobre.getEmpresaReceptora();
		if (duenia == null || duenia.getId() != empresa.getId())
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO).withParams("id", sobreId);
		return sobre;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	private static Object fecha(Date date) {
		return date != null ? sdf.format(date) : JSONObject.NULL;
	}

	private static Object empresaRef(Empresa empresa) {
		if (empresa == null)
			return JSONObject.NULL;
		JSONObject json = new JSONObject();
		json.put("rut", empresa.getRut());
		json.put("razon", empresa.getRazon());
		return json;
	}

	/** Sobre para listados: sin XML ni respuestas completas */
	private static JSONObject resumenSobre(Sobre sobre, JSONArray comprobantes) {
		JSONObject json = new JSONObject();
		json.put("id", sobre.getId());
		json.put("fecha", fecha(sobre.getFecha()));
		json.put("nombreArchivo", sobre.getNombreArchivo());
		json.put("cantComprobantes", sobre.getCantComprobantes());
		json.put("estadoEmpresa", sobre.getEstadoEmpresa() != null ? sobre.getEstadoEmpresa().name() : JSONObject.NULL);
		json.put("empresaEmisora", empresaRef(sobre.getEmpresaEmisora()));
		json.put("empresaReceptora", empresaRef(sobre.getEmpresaReceptora()));
		if (sobre instanceof SobreEmitido) {
			SobreEmitido emitido = (SobreEmitido) sobre;
			json.put("direccion", DireccionDocumento.EMITIDO.name());
			json.put("estadoDgi", emitido.getEstadoDgi() != null ? emitido.getEstadoDgi().name() : JSONObject.NULL);
			json.put("fechaConsulta", fecha(emitido.getFechaConsulta()));
			// Sin XML para la empresa receptora el sobre nunca se le envio (no es emisor electronico): no va a responder
			json.put("enviadoAEmpresa", emitido.getXmlEmpresa() != null);
		} else if (sobre instanceof SobreRecibido) {
			SobreRecibido recibido = (SobreRecibido) sobre;
			json.put("direccion", DireccionDocumento.RECIBIDO.name());
			json.put("timestampRecibido", fecha(recibido.getTimestampRecibido()));
			json.put("timestampProcesado", fecha(recibido.getTimestampProcesado()));
			// Respuesta a sus CFE (ME_): pendiente y cuando sale sola (null: solo a mano)
			json.put("respuestaCfePendiente", recibido.isRespuestaCfePendiente());
			json.put("respuestaCfeProgramada", fecha(RespuestaCfes.programada(recibido)));
		}
		json.put("comprobantes", comprobantes);
		return json;
	}

	/** Comprobante identificado por tipo, serie y numero, con lo necesario para listarlo y enlazarlo */
	private static JSONObject resumenCfe(CFE cfe) {
		JSONObject json = new JSONObject();
		json.put("id", cfe.getId());
		json.put("tipo", cfe.getTipo().name());
		json.put("idTipoDoc", cfe.getTipo().value);
		json.put("serie", cfe.getSerie());
		json.put("nro", cfe.getNro());
		json.put("fechaEmision", fecha(cfe.getFechaEmision()));
		json.put("moneda", cfe.getMoneda() != null ? cfe.getMoneda().name() : JSONObject.NULL);
		json.put("totMntTotal", cfe.getTotMntTotal() != null ? cfe.getTotMntTotal() : JSONObject.NULL);
		json.put("estado", cfe.getEstado() != null ? cfe.getEstado().name() : JSONObject.NULL);
		json.put("empresaEmisora", empresaRef(cfe.getEmpresaEmisora()));
		json.put("empresaReceptora", empresaRef(cfe.getEmpresaReceptora()));
		json.put("titular", cfe.getTitular() != null ? cfe.getTitular().getDocumento() : JSONObject.NULL);
		return json;
	}
	
	
	
	
}

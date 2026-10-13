package com.bluedot.efactura.empresas;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.efactura.model.EjecucionProceso;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.services.impl.ConsultasServiceImpl;

import dgi.soap.consultas.RucEmisoresMail;
import dgi.soap.consultas.RucEmisoresMailRucEmisoresMailItem;
import play.db.jpa.JPA;

/**
 * Mantiene los emisores electronicos al dia con lo que informa DGI: si cada RUT es emisor electronico, su mail para
 * recibir CFE de otros emisores y sus datos de emisor (fechas de inicio, fin de transicion y fin, URL del web service y
 * contacto tecnico). Dos fuentes, con la misma logica ({@link #aplicar}):
 * <ul>
 * <li>{@link #sincronizar}: EFACCONSULTARRUCEMISORES del servicio de consultas, los emisores nuevos o modificados desde
 * una fecha (lo usa el poller). No trae fin de transicion, URL ni contacto tecnico: esos no se tocan</li>
 * <li>{@link #cargarArchivo}: el archivo completo de emisores de DGI (RucEmisoresTransicionMailProv,
 * POST /empresa/cargar)</li>
 * </ul>
 *
 * Por cada emisor informado:
 * <ul>
 * <li>si la empresa no existe se crea</li>
 * <li>si existe se actualizan emisor electronico (no lo es mas si tiene fecha de fin pasada), mail y razon social</li>
 * <li>las empresas propias (con firma digital) no cambian emisor, mail ni razon: su mail de recepcion es la casilla de la
 * que se leen los sobres; si DGI tiene otro mail solo se informa</li>
 * <li>los datos de emisor de DGI se guardan siempre; solo cuentan como cambio si cambia la fecha de fin de un emisor
 * vigente (se agenda, cambia o se cancela su baja)</li>
 * </ul>
 * Al final se da de baja a los emisores cuya fecha de fin guardada ya paso: DGI no vuelve a informar un RUT cuando su
 * fecha de fin llega (no cambio nada).
 *
 * Con simular=true no se guarda nada: devuelve lo que cambiaria.
 */
public class EmisoresElectronicos {

	final static Logger logger = LoggerFactory.getLogger(EmisoresElectronicos.class);

	/** Cuantos cambios de cada tipo (nueva, actualizada, baja) se devuelven de ejemplo en el resultado */
	private static final int EJEMPLOS = 20;

	/** De a cuantos emisores se procesa (ver aplicar) */
	private static final int LOTE = 1000;

	/** Nombre con el que se registran las ejecuciones (el del poller, para verlas juntas en Procesos) */
	public static final String PROCESO = "EmisoresElectronicosPoller";

	/** Un emisor como lo informa DGI */
	private static class DatoDgi {
		String rut, razon, mail;
		Date inicio, fin;
		/** Datos que solo trae el archivo; con datosArchivo=false no se tocan */
		boolean datosArchivo;
		Date finTransicion;
		String url, contactoTecnico;
	}

	/** Lo que se ejecuta y registra: una sincronizacion o una carga de archivo */
	public interface Operacion {
		JSONObject ejecutar() throws APIException;
	}

	/** Primera empresa con firma digital vigente: la consulta a DGI se firma con su certificado */
	public static Empresa firmantePorDefecto() {
		Date ahora = new Date();
		for (Empresa e : Empresa.find(null, null, true, null, 1, 1000).item1)
			if (e.getFirmaDigital() != null && e.getFirmaDigital().getValidaHasta() != null && e.getFirmaDigital().getValidaHasta().after(ahora))
				return e;
		return null;
	}

	/** Sincroniza de verdad con el web service (ver {@link #ejecutar(String, String, Operacion)}) */
	public static JSONObject ejecutar(final Date desde, final Empresa firmante, String origen) throws APIException {
		return ejecutar(origen, "Desde " + new SimpleDateFormat("dd/MM/yyyy").format(desde), new Operacion() {
			@Override
			public JSONObject ejecutar() throws APIException {
				return sincronizar(desde, firmante, false);
			}
		});
	}

	/**
	 * Ejecuta de verdad (guarda), registra la ejecucion (EjecucionProceso, se ve en Procesos del panel) y deja el resumen
	 * en el log, salga bien o mal.
	 *
	 * @param origen
	 *            "automático" o el mail del usuario que la pidio
	 * @param que
	 *            con que empieza el resumen (ej. "Desde 06/10/2026", "Archivo X")
	 */
	public static JSONObject ejecutar(String origen, String que, Operacion operacion) throws APIException {
		Date inicio = new Date();
		try {
			JSONObject r = operacion.ejecutar();
			String resumen = que + ": " + r.getInt("informadosPorDgi") + " informados por DGI, " + r.getInt("nuevas") + " nuevas, "
					+ r.getInt("actualizadas") + " actualizadas, " + r.getInt("bajas") + " bajas (" + r.getInt("bajasPorFechaVencida")
					+ " por fecha vencida), " + r.getInt("sinCambios") + " sin cambios";
			logger.info("EJECUCION {} ({}) OK en {} s: {}", PROCESO, origen, (new Date().getTime() - inicio.getTime()) / 1000, resumen);
			EjecucionProceso.registrar(PROCESO, inicio, true, origen, resumen, r.toString());
			r.put("resumen", resumen);
			return r;
		} catch (APIException | RuntimeException e) {
			String error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			logger.error("EJECUCION {} ({}) CON ERROR en {} s: {}", PROCESO, origen, (new Date().getTime() - inicio.getTime()) / 1000, error);
			EjecucionProceso.registrar(PROCESO, inicio, false, origen, que + ": error: " + error, null);
			throw e;
		}
	}

	/** Emisores nuevos o modificados en DGI desde la fecha (EFACCONSULTARRUCEMISORES, firmado por firmante) */
	public static JSONObject sincronizar(Date desde, Empresa firmante, boolean simular) throws APIException {
		if (firmante == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("FirmaDigital")
					.setDetailMessage("Hace falta una empresa con firma digital vigente para consultar a DGI");

		RucEmisoresMail respuesta = new ConsultasServiceImpl().consutarRucEmisores("", "", desde, firmante);
		List<DatoDgi> datos = new ArrayList<DatoDgi>();
		if (respuesta != null)
			for (RucEmisoresMailRucEmisoresMailItem item : respuesta.getRucEmisoresMailRucEmisoresMailItem()) {
				DatoDgi d = new DatoDgi();
				d.rut = texto(item.getRUC());
				d.razon = texto(item.getDENOMINACION());
				d.mail = texto(item.getMAIL());
				d.inicio = fecha(item.getFECHAINICIO());
				d.fin = fecha(item.getFECHAFIN());
				datos.add(d);
			}

		JSONObject resultado = aplicar(datos, simular);
		resultado.put("desde", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(desde));
		resultado.put("firmante", firmante.getRut());
		logger.info("Emisores electronicos desde {}{}: {}", resultado.getString("desde"), simular ? " (simulado)" : "", totales(resultado));
		return resultado;
	}

	/**
	 * El archivo de emisores de DGI (RucEmisoresTransicionMailProv), XML o .gz. Se lee en streaming (StAX): el archivo
	 * completo tiene cientos de miles de emisores y en DOM no entra en memoria.
	 */
	public static JSONObject cargarArchivo(File archivo, boolean simular) throws APIException {
		List<DatoDgi> datos = new ArrayList<DatoDgi>();
		try (InputStream in = archivo.getName().toLowerCase().endsWith(".gz")
				? new GZIPInputStream(new BufferedInputStream(new FileInputStream(archivo)), 1 << 16)
				: new BufferedInputStream(new FileInputStream(archivo))) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			XMLStreamReader r = factory.createXMLStreamReader(in);
			DatoDgi d = null;
			while (r.hasNext()) {
				if (r.next() != XMLStreamConstants.START_ELEMENT)
					continue;
				String nombre = r.getLocalName();
				if (nombre.endsWith("Item")) {
					d = new DatoDgi();
					d.datosArchivo = true;
					datos.add(d);
					continue;
				}
				if (d == null)
					continue;
				switch (nombre) {
				case "RUC":
					d.rut = texto(r.getElementText());
					break;
				case "DENOMINACION":
					d.razon = texto(r.getElementText());
					break;
				case "MAIL":
					d.mail = texto(r.getElementText());
					break;
				case "FECHA_INICIO":
					d.inicio = fecha(texto(r.getElementText()));
					break;
				case "FECHA_FIN":
					d.fin = fecha(texto(r.getElementText()));
					break;
				case "FECHA_FIN_TRANSICION":
					d.finTransicion = fecha(texto(r.getElementText()));
					break;
				case "URL_WEBSERVICE":
					d.url = texto(r.getElementText());
					break;
				case "MAIL_CONTACTO_TECNICO":
					d.contactoTecnico = texto(r.getElementText());
					break;
				default:
				}
			}
			r.close();
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE, e).withParams("path", archivo.getName())
					.setDetailMessage("No se pudo leer el archivo de emisores: " + e.getMessage());
		}

		JSONObject resultado = aplicar(datos, simular);
		resultado.put("archivo", archivo.getName());
		logger.info("Emisores electronicos del archivo {}{}: {}", archivo.getName(), simular ? " (simulado)" : "", totales(resultado));
		return resultado;
	}

	private static JSONObject aplicar(List<DatoDgi> datos, boolean simular) {
		Date hoy = new Date();
		int nuevas = 0, actualizadas = 0, bajas = 0, sinCambios = 0, propias = 0;
		JSONArray cambios = new JSONArray();
		JSONArray propiasDistintas = new JSONArray();

		// Las empresas de los RUT informados, en pocas consultas (pueden ser miles)
		Set<String> ruts = new HashSet<String>();
		for (DatoDgi d : datos)
			if (d.rut != null)
				ruts.add(d.rut);
		// De a LOTE emisores: se buscan las empresas del lote y al terminarlo se guardan y se sacan de la sesion (el archivo
		// completo son cientos de miles; todas juntas no entran en memoria)
		Map<String, Empresa> empresas = new HashMap<String, Empresa>();

		// Ojo: con simular no se puede tocar ninguna empresa (las cargadas se guardarian solas al cerrar la transaccion)
		for (int i = 0; i < datos.size(); i++) {
			if (i % LOTE == 0) {
				if (i > 0)
					liberar(simular);
				Set<String> lote = new HashSet<String>();
				for (DatoDgi d : datos.subList(i, Math.min(i + LOTE, datos.size())))
					if (d.rut != null)
						lote.add(d.rut);
				empresas.clear();
				empresas.putAll(Empresa.findByRUTs(lote));
			}
			DatoDgi d = datos.get(i);
			if (d.rut == null)
				continue;
			boolean emisor = d.fin == null || d.fin.after(hoy);

			Empresa empresa = empresas.get(d.rut);
			if (empresa != null && empresa.getFirmaDigital() != null) {
				// Propia: solo se guardan los datos de emisor de DGI; si DGI tiene otro mail se informa
				propias++;
				if (d.mail != null && !d.mail.equalsIgnoreCase(Objects.toString(empresa.getMailRecepcion(), "")))
					propiasDistintas.put(new JSONObject().put("rut", d.rut).put("mailDgi", d.mail).put("mailRecepcion",
							empresa.getMailRecepcion() != null ? empresa.getMailRecepcion() : JSONObject.NULL));
				if (!simular && cambianDatosDgi(empresa, d)) {
					copiarDatosDgi(empresa, d);
					empresa.update();
				}
				continue;
			}

			if (empresa == null) {
				nuevas++;
				ejemplo(cambios, "nueva", d.rut, null, d.razon, false, emisor, null, d.mail, d.fin);
				if (!simular) {
					empresa = new Empresa(d.rut, d.razon, null);
					empresa.setEmisorElectronico(emisor);
					empresa.setMailRecepcion(d.mail);
					copiarDatosDgi(empresa, d);
					empresa.save();
					// Si DGI informa el mismo RUT dos veces, la segunda lo encuentra
					empresas.put(d.rut, empresa);
				}
				continue;
			}

			boolean cambiaEmisor = empresa.isEmisorElectronico() != emisor;
			boolean cambiaMail = d.mail != null && !Objects.equals(d.mail, empresa.getMailRecepcion());
			boolean cambiaRazon = d.razon != null && !Objects.equals(d.razon, empresa.getRazon());
			boolean cambiaFin = !Objects.equals(dia(d.fin), dia(empresa.getFechaFinEmisorDgi()));
			if (!cambiaEmisor && !cambiaMail && !cambiaRazon && !(cambiaFin && emisor)) {
				sinCambios++;
				if (!simular && cambianDatosDgi(empresa, d)) {
					copiarDatosDgi(empresa, d);
					empresa.update();
				}
				continue;
			}
			if (cambiaEmisor && !emisor)
				bajas++;
			else
				actualizadas++;
			ejemplo(cambios, cambiaEmisor && !emisor ? "baja" : "actualizada", d.rut, empresa.getRazon(), d.razon, empresa.isEmisorElectronico(),
					emisor, empresa.getMailRecepcion(), d.mail, d.fin);
			if (!simular) {
				empresa.setEmisorElectronico(emisor);
				if (cambiaMail)
					empresa.setMailRecepcion(d.mail);
				if (cambiaRazon)
					empresa.setRazon(d.razon);
				copiarDatosDgi(empresa, d);
				empresa.update();
			}
		}

		liberar(simular);
		empresas.clear();

		// Emisores cuya fecha de fin (guardada de una vez anterior) ya llego: DGI no los informa de nuevo
		int vencidas = 0;
		for (Empresa empresa : Empresa.findEmisoresVencidos(hoy)) {
			// Los informados ahora ya se procesaron arriba (con la fecha nueva); las propias no se tocan
			if (ruts.contains(empresa.getRut()) || empresa.getFirmaDigital() != null)
				continue;
			vencidas++;
			bajas++;
			ejemplo(cambios, "baja", empresa.getRut(), empresa.getRazon(), empresa.getRazon(), true, false, empresa.getMailRecepcion(),
					empresa.getMailRecepcion(), empresa.getFechaFinEmisorDgi());
			if (!simular) {
				empresa.setEmisorElectronico(false);
				empresa.update();
			}
		}

		JSONObject resultado = new JSONObject();
		resultado.put("simulado", simular);
		resultado.put("informadosPorDgi", datos.size());
		resultado.put("nuevas", nuevas);
		resultado.put("actualizadas", actualizadas);
		resultado.put("bajas", bajas);
		resultado.put("bajasPorFechaVencida", vencidas);
		resultado.put("sinCambios", sinCambios);
		resultado.put("propiasSinTocar", propias);
		resultado.put("propiasConOtroMailEnDgi", propiasDistintas);
		resultado.put("cambios", cambios);
		return resultado;
	}

	/** Termina un lote: guarda lo cambiado (o, si es simulacion, lo descarta) y saca las empresas de la sesion */
	private static void liberar(boolean simular) {
		if (simular)
			JPA.em().clear();
		else
			ThreadMan.forceTransactionFlush(true);
	}

	/** Si alguno de los datos de emisor de DGI es distinto al guardado */
	private static boolean cambianDatosDgi(Empresa empresa, DatoDgi d) {
		if (!Objects.equals(dia(d.inicio), dia(empresa.getFechaInicioEmisorDgi())) || !Objects.equals(dia(d.fin), dia(empresa.getFechaFinEmisorDgi())))
			return true;
		return d.datosArchivo && (!Objects.equals(dia(d.finTransicion), dia(empresa.getFechaFinTransicionDgi()))
				|| !Objects.equals(d.url, empresa.getUrlWebserviceDgi()) || !Objects.equals(d.contactoTecnico, empresa.getMailContactoTecnicoDgi()));
	}

	private static void copiarDatosDgi(Empresa empresa, DatoDgi d) {
		empresa.setFechaInicioEmisorDgi(d.inicio);
		empresa.setFechaFinEmisorDgi(d.fin);
		if (d.datosArchivo) {
			empresa.setFechaFinTransicionDgi(d.finTransicion);
			empresa.setUrlWebserviceDgi(d.url);
			empresa.setMailContactoTecnicoDgi(d.contactoTecnico);
		}
	}

	private static String totales(JSONObject r) {
		return r.getInt("informadosPorDgi") + " informados por DGI, " + r.getInt("nuevas") + " nuevas, " + r.getInt("actualizadas")
				+ " actualizadas, " + r.getInt("bajas") + " bajas (" + r.getInt("bajasPorFechaVencida") + " por fecha vencida), "
				+ r.getInt("sinCambios") + " sin cambios, " + r.getInt("propiasSinTocar") + " propias";
	}

	private static void ejemplo(JSONArray cambios, String tipo, String rut, String razonAntes, String razon, boolean emisorAntes, boolean emisor,
			String mailAntes, String mail, Date fin) {
		int deEseTipo = 0;
		for (int i = 0; i < cambios.length(); i++)
			if (tipo.equals(cambios.getJSONObject(i).getString("tipo")))
				deEseTipo++;
		if (deEseTipo >= EJEMPLOS)
			return;
		JSONObject c = new JSONObject().put("tipo", tipo).put("rut", rut).put("razon", razon != null ? razon : JSONObject.NULL)
				.put("emisorElectronico", emisor).put("mail", mail != null ? mail : JSONObject.NULL)
				.put("fechaFinDgi", fin != null ? new SimpleDateFormat("yyyy-MM-dd").format(fin) : JSONObject.NULL);
		if (!"nueva".equals(tipo)) {
			c.put("razonAntes", razonAntes != null ? razonAntes : JSONObject.NULL);
			c.put("emisorElectronicoAntes", emisorAntes);
			c.put("mailAntes", mailAntes != null ? mailAntes : JSONObject.NULL);
		}
		cambios.put(c);
	}

	private static String texto(String v) {
		if (v == null)
			return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}

	/** El dia (yyyy-MM-dd) de una fecha, o null: para comparar la de DGI con la guardada (java.sql.Date) */
	private static String dia(Date f) {
		return f != null ? new SimpleDateFormat("yyyy-MM-dd").format(f) : null;
	}

	/** DGI manda 0000-00-00 sin fecha: JAXB la deja en null o con año 0 */
	private static Date fecha(XMLGregorianCalendar f) {
		if (f == null || f.getYear() <= 1)
			return null;
		return f.toGregorianCalendar().getTime();
	}

	/** Fecha yyyy-MM-dd del archivo; 0000-00-00 o vacia es sin fecha */
	private static Date fecha(String f) {
		if (f == null || f.startsWith("0000"))
			return null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.setLenient(false);
			return sdf.parse(f);
		} catch (java.text.ParseException e) {
			logger.warn("Fecha invalida en el archivo de emisores: {}", f);
			return null;
		}
	}
}

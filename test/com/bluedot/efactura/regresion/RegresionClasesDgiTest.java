package com.bluedot.efactura.regresion;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.w3c.dom.Document;

import com.bluedot.commons.utils.XML;

/**
 * Regresion de las clases JAXB de DGI (dgi.classes.*): lee XML guardados en produccion (sobres emitidos y recibidos,
 * respuestas, reportes diarios) como lo hace la app (XML.loadXMLFromString + XML.unMarshall) y los vuelve a escribir
 * (XML.objectToString). La salida de cada version de las clases se compara con la de la anterior: tiene que ser igual
 * salvo los cambios buscados.
 *
 * La muestra tiene datos de clientes: no va en el repositorio. Se corre a mano:
 *
 * <pre>
 * DGI_REGRESION_DIR=~/Documents/efactura-regresion DGI_REGRESION_ETIQUETA=actual sbt "testOnly *RegresionClasesDgiTest"
 * </pre>
 *
 * Escribe en {dir}/salida/{etiqueta}/ el XML de cada archivo (o ERROR con la causa) y un resumen.txt.
 */
public class RegresionClasesDgiTest {

	/** Carpeta de la muestra -> clase con la que la app la lee */
	private static final Map<String, Class<?>> GRUPOS = new LinkedHashMap<String, Class<?>>();
	static {
		GRUPOS.put("emitidos/xml_dgi", dgi.classes.recepcion.EnvioCFE.class);
		GRUPOS.put("emitidos/xml_empresa", dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas.class);
		GRUPOS.put("emitidos/respuesta_dgi", dgi.classes.respuestas.sobre.ACKSobredefType.class);
		GRUPOS.put("recibidos", dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas.class);
		GRUPOS.put("reportes/xml", dgi.classes.reporte.ReporteDefType.class);
		GRUPOS.put("reportes/respuesta", dgi.classes.respuestas.reporte.ACKRepDiariodefType.class);
		// respuestas: M_ es la respuesta al sobre, ME_ la respuesta a los CFE
		GRUPOS.put("respuestas", null);
	}

	@Test
	public void releerYEscribir() throws Exception {
		String dir = System.getenv("DGI_REGRESION_DIR");
		assumeTrue("Sin DGI_REGRESION_DIR: no se corre la regresion", dir != null && !dir.trim().isEmpty());
		String etiqueta = System.getenv("DGI_REGRESION_ETIQUETA") != null ? System.getenv("DGI_REGRESION_ETIQUETA") : "actual";
		Path base = Paths.get(dir.replaceFirst("^~", System.getProperty("user.home")));
		Path salida = base.resolve("salida").resolve(etiqueta);

		List<String> resumen = new ArrayList<String>();
		int totalOk = 0, totalError = 0;
		for (Map.Entry<String, Class<?>> grupo : GRUPOS.entrySet()) {
			Path carpeta = base.resolve(grupo.getKey());
			if (!Files.isDirectory(carpeta))
				continue;
			List<Path> archivos;
			try (Stream<Path> s = Files.list(carpeta)) {
				archivos = s.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
			}
			int ok = 0, error = 0;
			Map<String, Integer> errores = new LinkedHashMap<String, Integer>();
			for (Path archivo : archivos) {
				Class<?> clase = grupo.getValue() != null ? grupo.getValue() : claseRespuesta(archivo.getFileName().toString());
				Path destino = salida.resolve(grupo.getKey()).resolve(archivo.getFileName().toString());
				Files.createDirectories(destino.getParent());
				String resultado;
				try {
					String xml = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
					Document doc = XML.loadXMLFromString(xml);
					Object objeto = XML.unMarshall(doc, clase);
					resultado = escribir(objeto, doc);
					ok++;
				} catch (Throwable t) {
					Throwable causa = t;
					while (causa.getCause() != null && causa.getCause() != causa)
						causa = causa.getCause();
					String motivo = causa.getClass().getSimpleName() + ": " + String.valueOf(causa.getMessage());
					resultado = "ERROR " + motivo;
					errores.merge(motivo.length() > 160 ? motivo.substring(0, 160) : motivo, 1, Integer::sum);
					error++;
				}
				Files.write(destino, resultado.getBytes(StandardCharsets.UTF_8));
			}
			totalOk += ok;
			totalError += error;
			resumen.add(String.format("%-24s %5d leidos, %4d con error", grupo.getKey(), ok, error));
			List<String> motivos = new ArrayList<String>();
			for (Map.Entry<String, Integer> e : errores.entrySet())
				motivos.add(String.format("    %4d x %s", e.getValue(), e.getKey()));
			Collections.sort(motivos);
			resumen.addAll(motivos);
		}
		resumen.add(String.format("TOTAL %d leidos, %d con error", totalOk, totalError));
		Files.createDirectories(salida);
		Files.write(salida.resolve("resumen.txt"), String.join("\n", resumen).getBytes(StandardCharsets.UTF_8));
		System.out.println("Regresion clases DGI (" + etiqueta + ") en " + salida + File.separator + "resumen.txt\n" + String.join("\n", resumen));
	}

	/**
	 * Como XML.objectToString; si la clase no tiene @XmlRootElement (ej. ACKRepDiariodefType, que la app solo lee) se
	 * escribe con el elemento raiz del documento original
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String escribir(Object objeto, Document doc) throws Exception {
		if (objeto.getClass().isAnnotationPresent(javax.xml.bind.annotation.XmlRootElement.class))
			return XML.objectToString(objeto);
		javax.xml.namespace.QName raiz = new javax.xml.namespace.QName(doc.getDocumentElement().getNamespaceURI(),
				doc.getDocumentElement().getLocalName());
		java.io.StringWriter sw = new java.io.StringWriter();
		javax.xml.bind.JAXBContext.newInstance(objeto.getClass()).createMarshaller()
				.marshal(new javax.xml.bind.JAXBElement(raiz, objeto.getClass(), objeto), sw);
		return sw.toString();
	}

	private static Class<?> claseRespuesta(String nombre) {
		// El nombre guardado es "{id}_{archivo}"
		String archivo = nombre.substring(nombre.indexOf('_') + 1).toUpperCase();
		return archivo.startsWith("ME_") ? dgi.classes.respuestas.cfe.ACKCFEdefType.class
				: dgi.classes.respuestas.sobre.ACKSobredefType.class;
	}
}

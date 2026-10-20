package com.bluedot.efactura.recibidos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.w3c.dom.Document;

import com.bluedot.commons.utils.XML;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;

/**
 * Controles de firma de los sobres recibidos (S02, S03, S05, S06 y E04 por CFE) sobre una muestra real, para ver cuantos
 * pasarian antes de ponerlos en enforce. La muestra tiene datos de clientes y no esta en el repo:
 *
 * DGI_REGRESION_DIR=~/Documents/efactura-regresion sbt "testOnly *RegresionFirmasRecibidosTest"
 *
 * Escribe el resumen en la salida; falla solo si cfesDelSobre no encuentra los mismos CFE que JAXB (la posicion de cada CFE
 * es la que usan E04 y E05).
 */
public class RegresionFirmasRecibidosTest {

	@Test
	public void controlesDeFirmaSobreLaMuestra() throws Exception {
		String dir = System.getenv("DGI_REGRESION_DIR");
		assumeTrue("Sin DGI_REGRESION_DIR: no se corre la regresion", dir != null && !dir.trim().isEmpty());

		List<Path> archivos;
		try (Stream<Path> s = Files.list(Paths.get(dir, "recibidos"))) {
			archivos = s.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
		}

		Map<String, Integer> resultados = new TreeMap<String, Integer>();
		int sobres = 0, cfes = 0, distintos = 0;
		for (Path archivo : archivos) {
			String xml = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
			Document doc = XML.loadXMLFromString(xml);
			EnvioCFEEntreEmpresas envio = (EnvioCFEEntreEmpresas) XML.unMarshall(doc, EnvioCFEEntreEmpresas.class);
			ControlesSobre.Sobre sobre = ControlesSobre.leer(xml);
			sobres++;
			if (sobre.cfes.size() != envio.getCFEAdendas().size())
				distintos++;

			contar(resultados, "S02", ControlesSobre.s02(sobre));
			contar(resultados, "S03", ControlesSobre.s03(sobre));
			contar(resultados, "S05", ControlesSobre.s05(sobre));
			contar(resultados, "S06", ControlesSobre.s06(sobre));
			for (int i = 0; i < envio.getCFEAdendas().size(); i++) {
				cfes++;
				contar(resultados, "E04", ControlesRecepcion.e04(xml, i + 1, envio.getCFEAdendas().get(i).getCFE()));
			}
		}

		System.out.println("Sobres: " + sobres + ", CFE: " + cfes + ", sobres donde cfesDelSobre difiere de JAXB: " + distintos);
		for (Map.Entry<String, Integer> r : resultados.entrySet())
			System.out.println(String.format("%5d  %s", r.getValue(), r.getKey()));
		assertEquals("cfesDelSobre tiene que encontrar los mismos CFE que JAXB", 0, distintos);
	}

	private static void contar(Map<String, Integer> resultados, String control, String motivo) {
		String clave = control + (motivo == null ? " OK" : " FALLA: " + motivo.replaceAll("[0-9]{6,}", "#").replaceAll("\\d{2}/\\d{2}/\\d{4}", "dd/mm/aaaa"));
		if (clave.length() > 170)
			clave = clave.substring(0, 170);
		resultados.merge(clave, 1, Integer::sum);
	}
}

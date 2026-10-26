package com.bluedot.commons.error;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * APIException.raise(...) solo arma la excepcion: suelto como sentencia no hace nada y el error se pierde (asi el CFE
 * seguia sin IdDoc ni CAEData cuando fallaba el controlador de CAE). Tiene que ir con throw o return.
 */
public class APIExceptionSinThrowTest {

	private static final Pattern SUELTO = Pattern.compile("^\\s*APIException\\.raise\\(.*\\);\\s*$");

	@Test
	public void ningunRaiseSinThrow() throws IOException {
		List<Path> archivos;
		try (Stream<Path> s = Files.walk(Paths.get("app/com/bluedot"))) {
			archivos = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
		}
		List<String> sueltos = new ArrayList<String>();
		for (Path archivo : archivos) {
			List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
			for (int i = 0; i < lineas.size(); i++)
				if (SUELTO.matcher(lineas.get(i)).matches())
					sueltos.add(archivo + ":" + (i + 1));
		}
		assertTrue("APIException.raise sin throw: " + sueltos, sueltos.isEmpty());
	}
}

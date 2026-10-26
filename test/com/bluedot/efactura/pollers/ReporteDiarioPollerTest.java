package com.bluedot.efactura.pollers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
import com.typesafe.config.ConfigFactory;

import play.Configuration;

/** Hasta que dia el poller ya deberia haber mandado el reporte diario, y a que empresas se lo manda */
public class ReporteDiarioPollerTest {

	private static final LocalDateTime DESPUES_DE_LA_HORA = LocalDateTime.of(2026, 10, 26, 16, 0);
	private static final LocalDateTime ANTES_DE_LA_HORA = LocalDateTime.of(2026, 10, 26, 9, 0);

	@Test
	public void conLosValoresDeProduccionHastaHaceOnceDias() {
		assertEquals(LocalDate.of(2026, 10, 15), ReporteDiarioPoller.hastaEsperado(30, 20, 15, DESPUES_DE_LA_HORA));
	}

	@Test
	public void antesDeLaHoraUnDiaMenos() {
		assertEquals(LocalDate.of(2026, 10, 14), ReporteDiarioPoller.hastaEsperado(30, 20, 15, ANTES_DE_LA_HORA));
	}

	@Test
	public void comoMinimoHastaAyer() {
		assertEquals(LocalDate.of(2026, 10, 25), ReporteDiarioPoller.hastaEsperado(5, 20, 15, DESPUES_DE_LA_HORA));
	}

	private static Configuration config(boolean run, String empresas) {
		Map<String, Object> valores = new HashMap<String, Object>();
		valores.put("ReporteDiarioPoller.run", run);
		valores.put("ReporteDiarioPoller.empresas", empresas);
		return new Configuration(ConfigFactory.parseMap(valores));
	}

	private static Empresa empresa(String rut, boolean activa, Date firmaHasta) {
		Empresa e = new Empresa(rut, "Empresa de prueba", null);
		e.setActiva(activa);
		if (firmaHasta != null) {
			FirmaDigital firma = new FirmaDigital();
			firma.setValidaHasta(firmaHasta);
			e.setFirmaDigital(firma);
		}
		return e;
	}

	private static final Date AHORA = new Date();
	private static final Date EL_ANIO_QUE_VIENE = new Date(AHORA.getTime() + 365L * 24 * 3600 * 1000);
	private static final Date EL_ANIO_PASADO = new Date(AHORA.getTime() - 365L * 24 * 3600 * 1000);

	@Test
	public void cubreLasActivasConFirmaVigente() {
		assertTrue(ReporteDiarioPoller.cubre(empresa("100000000001", true, EL_ANIO_QUE_VIENE), config(true, ""), AHORA));
	}

	@Test
	public void noCubreSiElPollerEstaApagado() {
		assertFalse(ReporteDiarioPoller.cubre(empresa("100000000001", true, EL_ANIO_QUE_VIENE), config(false, ""), AHORA));
	}

	@Test
	public void noCubreSinFirmaOConLaFirmaVencida() {
		assertFalse(ReporteDiarioPoller.cubre(empresa("100000000001", true, null), config(true, ""), AHORA));
		assertFalse(ReporteDiarioPoller.cubre(empresa("100000000001", true, EL_ANIO_PASADO), config(true, ""), AHORA));
	}

	@Test
	public void noCubreLasDadasDeBajaSinLista() {
		assertFalse(ReporteDiarioPoller.cubre(empresa("100000000001", false, EL_ANIO_QUE_VIENE), config(true, ""), AHORA));
	}

	@Test
	public void conListaSoloLasDeLaLista() {
		assertTrue(ReporteDiarioPoller.cubre(empresa("100000000001", true, EL_ANIO_QUE_VIENE), config(true, "100000000002, 100000000001"), AHORA));
		assertFalse(ReporteDiarioPoller.cubre(empresa("100000000003", true, EL_ANIO_QUE_VIENE), config(true, "100000000002,100000000001"), AHORA));
	}
}

package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFac;
import dgi.classes.reporte.RsmnDataFacCont;
import dgi.classes.reporte.RsmnDataFacExp;
import dgi.classes.reporte.RsmnDataFacExpCont;
import dgi.classes.reporte.RsmnDataRemExp;
import dgi.classes.reporte.RsmnDataRemExpCont;
import dgi.classes.reporte.RsmnDataTck;
import dgi.classes.reporte.RsmnDataTckCont;

/**
 * Resumen de un tipo de CFE en el reporte diario, armado con las piezas de la seccion (como Strategy_101..282 pero sin
 * una clase por tipo): para los tipos que antes no se reportaban (notas de credito y debito de contingencia, venta por
 * cuenta ajena). Igual que las demas, la seccion solo se agrega si hay comprobantes utilizados de ese tipo.
 *
 * @param <S>
 *            seccion del reporte (ej. ReporteDefType.RsmnTckCtaAj)
 * @param <D>
 *            datos de la seccion (RsmnDataTck, RsmnDataTckCont, RsmnDataFac, RsmnDataFacCont, RsmnDataFacExp,
 *            RsmnDataFacExpCont, RsmnDataRemExp o RsmnDataRemExpCont)
 */
public class ResumenPorTipo<S, D> implements SummaryStrategy {

	private final TipoDoc tipoDoc;
	private final Supplier<S> seccion;
	private final BiConsumer<S, BigInteger> setTipoComp;
	private final BiConsumer<S, D> setDatos;
	private final Datos<D> datos;
	private final Function<D, BigInteger> cantidad;
	private final BiConsumer<ReporteDefType, S> agregar;

	ResumenPorTipo(TipoDoc tipoDoc, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp, BiConsumer<S, D> setDatos,
			Datos<D> datos, Function<D, BigInteger> cantidad, BiConsumer<ReporteDefType, S> agregar) {
		this.tipoDoc = tipoDoc;
		this.seccion = seccion;
		this.setTipoComp = setTipoComp;
		this.setDatos = setDatos;
		this.datos = datos;
		this.cantidad = cantidad;
		this.agregar = agregar;
	}

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException {
		S resumen = seccion.get();
		setTipoComp.accept(resumen, new BigInteger(String.valueOf(tipoDoc.value)));
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, cfes);
		D data = datos.apply(summary);
		setDatos.accept(resumen, data);
		if (summary.cantDocUtilizados > 0) {
			reporte.getCaratula().setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(cantidad.apply(data)));
			agregar.accept(reporte, resumen);
		}
	}

	/** Arma los datos de la seccion a partir del resumen del tipo (getMontosFyT puede lanzar APIException) */
	interface Datos<D> {
		D apply(SummaryDatatype summary) throws APIException;
	}

	/* Datos de cada familia de secciones: los mismos que TicketStrategy, FacturaStrategy, Strategy_201 y Strategy_211 */

	static RsmnDataTck datosTck(SummaryDatatype summary) throws APIException {
		RsmnDataTck data = new RsmnDataTck();
		data.setCantDocsAnulados(new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayorTopeUI)));
		return data;
	}

	static RsmnDataTckCont datosTckCont(SummaryDatatype summary) throws APIException {
		RsmnDataTckCont data = new RsmnDataTckCont();
		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayorTopeUI)));
		return data;
	}

	static RsmnDataFac datosFac(SummaryDatatype summary) throws APIException {
		RsmnDataFac data = new RsmnDataFac();
		data.setCantDocsAnulados(new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		return data;
	}

	static RsmnDataFacCont datosFacCont(SummaryDatatype summary) throws APIException {
		RsmnDataFacCont data = new RsmnDataFacCont();
		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		return data;
	}

	/* e-Factura de exportacion: como la e-Factura pero con Montos_FExp (sin IVA) */

	static RsmnDataFacExp datosFacExp(SummaryDatatype summary) throws APIException {
		RsmnDataFacExp data = new RsmnDataFacExp();
		data.setCantDocsAnulados(new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFExp(summary));
		return data;
	}

	static RsmnDataFacExpCont datosFacExpCont(SummaryDatatype summary) throws APIException {
		RsmnDataFacExpCont data = new RsmnDataFacExpCont();
		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosFExp(summary));
		return data;
	}

	/* e-Remito de exportacion: como la e-Factura de exportacion pero con Montos_RemExp */

	static RsmnDataRemExp datosRemExp(SummaryDatatype summary) throws APIException {
		RsmnDataRemExp data = new RsmnDataRemExp();
		data.setCantDocsAnulados(new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosRemExp(summary));
		return data;
	}

	static RsmnDataRemExpCont datosRemExpCont(SummaryDatatype summary) throws APIException {
		RsmnDataRemExpCont data = new RsmnDataRemExpCont();
		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosRemExp(summary));
		return data;
	}

	/* Una por familia, para que el Builder quede en una linea por tipo */

	static <S> ResumenPorTipo<S, RsmnDataTck> tck(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataTck> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataTck>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosTck, RsmnDataTck::getCantDocsUtil,
				agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataTckCont> tckCont(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataTckCont> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataTckCont>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosTckCont,
				RsmnDataTckCont::getCantCFCEmi, agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataFac> fac(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataFac> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataFac>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosFac, RsmnDataFac::getCantDocsUtil,
				agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataFacCont> facCont(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataFacCont> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataFacCont>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosFacCont,
				RsmnDataFacCont::getCantCFCEmi, agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataFacExp> facExp(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataFacExp> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataFacExp>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosFacExp,
				RsmnDataFacExp::getCantDocsUtil, agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataFacExpCont> facExpCont(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataFacExpCont> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataFacExpCont>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosFacExpCont,
				RsmnDataFacExpCont::getCantCFCEmi, agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataRemExp> remExp(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataRemExp> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataRemExp>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosRemExp,
				RsmnDataRemExp::getCantDocsUtil, agregar);
	}

	static <S> ResumenPorTipo<S, RsmnDataRemExpCont> remExpCont(TipoDoc tipo, Supplier<S> seccion, BiConsumer<S, BigInteger> setTipoComp,
			BiConsumer<S, RsmnDataRemExpCont> setDatos, BiConsumer<ReporteDefType, S> agregar) {
		return new ResumenPorTipo<S, RsmnDataRemExpCont>(tipo, seccion, setTipoComp, setDatos, ResumenPorTipo::datosRemExpCont,
				RsmnDataRemExpCont::getCantCFCEmi, agregar);
	}
}

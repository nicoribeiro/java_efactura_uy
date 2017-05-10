package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFac;

public class Strategy_111 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eFactura;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, Date date, List<SobreEmitido> sobres) throws APIException {
		ReporteDefType.RsmnFac resumen = new ReporteDefType.RsmnFac();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		RsmnDataFac data = new RsmnDataFac();
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, date, sobres);

		data.setCantDocsAnulados(
				new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantDocsUtil()));

		reporte.setRsmnFac(resumen);
		}

	}

}

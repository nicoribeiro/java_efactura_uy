package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataTck;

public class Strategy_103 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.Nota_de_Debito_de_eTicket;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, Date date, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnTckNotaDebito resumen = new ReporteDefType.RsmnTckNotaDebito();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		RsmnDataTck data = new RsmnDataTck();
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, date, cfes);

		data.setCantDocsAnulados(
				new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayor10000UI)));

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantDocsUtil()));

		reporte.setRsmnTckNotaDebito(resumen);
		}

	}

}

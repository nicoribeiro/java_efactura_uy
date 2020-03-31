package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataTck;

public class Strategy_102 extends TicketStrategy {

	private TipoDoc tipoDoc = TipoDoc.Nota_de_Credito_de_eTicket;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnTckNotaCredito resumen = new ReporteDefType.RsmnTckNotaCredito();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, cfes);
		RsmnDataTck data = getData(summary);

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantDocsUtil()));

		reporte.setRsmnTckNotaCredito(resumen);
		}
	}
}

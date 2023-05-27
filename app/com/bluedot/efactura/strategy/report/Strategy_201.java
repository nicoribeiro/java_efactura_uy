package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataTckCont;

public class Strategy_201 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eTicket_Contingencia;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnTckCont resumen = new ReporteDefType.RsmnTckCont();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		RsmnDataTckCont data = new RsmnDataTckCont();
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, cfes);

		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayorTopeUI)));

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantCFCEmi()));

		reporte.setRsmnTckCont(resumen);
		}

	}

}

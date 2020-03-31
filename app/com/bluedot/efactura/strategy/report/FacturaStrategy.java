package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;

import com.bluedot.commons.error.APIException;

import dgi.classes.reporte.RsmnDataFac;

public abstract class FacturaStrategy implements SummaryStrategy {

	protected RsmnDataFac getData(SummaryDatatype summary) throws APIException {
		RsmnDataFac data = new RsmnDataFac();
		
		data.setCantDocsAnulados(
				new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		
		return data;
	}
}

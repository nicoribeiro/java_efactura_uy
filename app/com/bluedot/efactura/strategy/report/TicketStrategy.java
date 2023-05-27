package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;

import com.bluedot.commons.error.APIException;

import dgi.classes.reporte.RsmnDataTck;

public abstract class TicketStrategy implements SummaryStrategy {

	protected RsmnDataTck getData(SummaryDatatype summary) throws APIException {
		RsmnDataTck data = new RsmnDataTck();
		
		data.setCantDocsAnulados(
				new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
		data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDocUtilizados)));
		data.setRngDocsAnulados(summary.rngDocsAnulados);
		data.setRngDocsUtil(summary.rngDocsUtil);
		data.setMontos(SummaryStrategy.getMontosFyT(summary));
		data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayorTopeUI)));
		
		return data;
	}

}

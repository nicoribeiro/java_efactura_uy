package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFacCont;

public class Strategy_211 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eFactura_Contingencia;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, Date date, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnFacCont resumen = new ReporteDefType.RsmnFacCont();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		RsmnDataFacCont data = new RsmnDataFacCont();
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, date, cfes);

		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosFyT(summary));

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantCFCEmi()));

		reporte.setRsmnFacCont(resumen);
		}
	}

}

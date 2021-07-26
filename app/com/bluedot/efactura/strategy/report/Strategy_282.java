package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataResgCont;

public class Strategy_282 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eResguardo_Contingencia;
	
	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnResgCont resumen = new ReporteDefType.RsmnResgCont();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		RsmnDataResgCont data = new RsmnDataResgCont();
		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, cfes);

		data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocEmitidos)));
		data.setMontos(SummaryStrategy.getMontosResg(summary));

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantCFCEmi()));

		reporte.setRsmnResgCont(resumen);
		}

	}

}

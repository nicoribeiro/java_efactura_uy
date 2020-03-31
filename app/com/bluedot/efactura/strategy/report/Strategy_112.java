package com.bluedot.efactura.strategy.report;

import java.math.BigInteger;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFac;

public class Strategy_112 extends FacturaStrategy {

	private TipoDoc tipoDoc = TipoDoc.Nota_de_Credito_de_eFactura;

	@Override
	public void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException {
		ReporteDefType.RsmnFacNotaCredito resumen = new ReporteDefType.RsmnFacNotaCredito();
		resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

		SummaryDatatype summary = SummaryStrategy.getSummary(empresa, tipoDoc, cfes);
		RsmnDataFac data = getData(summary);

		resumen.setRsmnData(data);

		/*
		 * Solo agrego el resumen si hay documentos del tipo correspondiente
		 */
		if (summary.cantDocUtilizados>0){
		reporte.getCaratula()
				.setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantDocsUtil()));

		reporte.setRsmnFacNotaCredito(resumen);
		}

	}

}

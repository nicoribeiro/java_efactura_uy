package com.bluedot.efactura.strategy.report;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;

import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFac;

public class Strategy_113 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.Nota_de_Debito_de_eFactura;
	
	@Override
	public void buildSummary(ReporteDefType reporte, Date date) {
		try {
			ReporteDefType.RsmnFacNotaDebito resumen = new ReporteDefType.RsmnFacNotaDebito();
			resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

			RsmnDataFac data = new RsmnDataFac();
			SummaryDatatype summary = SummaryStrategy.getSummary(tipoDoc, date);

			data.setCantDocsAnulados(new BigInteger(String.valueOf(summary.cantDocRechazados + summary.cantDocSinRespuesta)));
			data.setCantDocsEmi(new BigInteger(String.valueOf(summary.cantDocAceptados)));
			data.setCantDocsUtil(new BigInteger(String.valueOf(summary.cantDoc)));
			data.setRngDocsAnulados(summary.rngDocsAnulados);
			data.setRngDocsUtil(summary.rngDocsUtil);
			data.setMontos(SummaryStrategy.getMontosFyT(summary));

			resumen.setRsmnData(data);
			
			reporte.getCaratula().setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantDocsUtil()));
			
			reporte.setRsmnFacNotaDebito(resumen);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

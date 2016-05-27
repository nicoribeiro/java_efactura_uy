package com.bluedot.efactura.strategy.report;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;

import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.strategy.report.SummaryStrategy.SummaryDatatype;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataFac;
import dgi.classes.reporte.RsmnDataFacCont;
import dgi.classes.reporte.RsmnDataTckCont;

public class Strategy_211 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eFactura_Contingencia;
	
	@Override
	public void buildSummary(ReporteDefType reporte, Date date) {
		try {
			ReporteDefType.RsmnFacCont resumen = new ReporteDefType.RsmnFacCont();
			resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

			RsmnDataFacCont data = new RsmnDataFacCont();
			SummaryDatatype summary = SummaryStrategy.getSummary(tipoDoc, date);

			data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocAceptados)));
			data.setMontos(SummaryStrategy.getMontosFyT(summary));

			resumen.setRsmnData(data);
			
			reporte.getCaratula().setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantCFCEmi()));
			
			reporte.setRsmnFacCont(resumen);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

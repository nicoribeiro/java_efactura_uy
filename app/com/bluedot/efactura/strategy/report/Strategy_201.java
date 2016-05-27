package com.bluedot.efactura.strategy.report;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;

import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RsmnDataResg;
import dgi.classes.reporte.RsmnDataTck;
import dgi.classes.reporte.RsmnDataTckCont;

public class Strategy_201 implements SummaryStrategy {

	private TipoDoc tipoDoc = TipoDoc.eTicket_Contingencia;
	
	@Override
	public void buildSummary(ReporteDefType reporte, Date date) {
		try {
			ReporteDefType.RsmnTckCont resumen = new ReporteDefType.RsmnTckCont();
			resumen.setTipoComp(new BigInteger(String.valueOf(tipoDoc.value)));

			RsmnDataTckCont data = new RsmnDataTckCont();
			SummaryDatatype summary = SummaryStrategy.getSummary(tipoDoc, date);

			data.setCantCFCEmi(new BigInteger(String.valueOf(summary.cantDocAceptados)));
			data.setMontos(SummaryStrategy.getMontosFyT(summary));
			data.setCantDocsMayTopeUI(new BigInteger(String.valueOf(summary.mayor10000UI)));

			resumen.setRsmnData(data);
			
			reporte.getCaratula().setCantComprobantes(reporte.getCaratula().getCantComprobantes().add(data.getCantCFCEmi()));
			
			reporte.setRsmnTckCont(resumen);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

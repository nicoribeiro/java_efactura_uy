package com.bluedot.efactura.services;

import java.util.Date;
import java.util.List;

import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.soap.recepcion.Data;

public interface RecepcionService {

	Data sendCFE(CFEDefType.EFact cfe, String adenda) throws EFacturaException;
	Data sendCFE(CFEDefType.EFactExp cfe, String adenda) throws EFacturaException;
	Data sendCFE(CFEDefType.ERem cfe, String adenda) throws EFacturaException;
	Data sendCFE(CFEDefType.ERemExp cfe, String adenda) throws EFacturaException;
	Data sendCFE(CFEDefType.EResg cfe, String adenda) throws EFacturaException;
	Data sendCFE(CFEDefType.ETck cfe, String adenda) throws EFacturaException;
	
	Data consultaEstado(String token, String idReceptor) throws EFacturaException;

	Data consultaResultado(TipoDoc tipo, String serie, int nro, Date fecha) throws EFacturaException;

	Data generarReporteDiario(Date date) throws EFacturaException;

	List<ResultadoConsulta> consultarResultados(Date date) throws EFacturaException;
	
	void anularDocumento(TipoDoc tipo, String serie, int nro, Date date) throws EFacturaException;

	public class ResultadoConsulta{
		public TipoDoc tipoDoc;
		public int conResultado;
		public int conRespuesta;
		public int conError;
		public int unsigned;
		public int signed;
		
		public ResultadoConsulta(TipoDoc tipoDoc, int conResultado, int conRespuesta, int conError, int unsigned,
				int signed) {
			super();
			this.tipoDoc = tipoDoc;
			this.conResultado = conResultado;
			this.conRespuesta = conRespuesta;
			this.conError = conError;
			this.unsigned = unsigned;
			this.signed = signed;
		}

		@Override
		public String toString() {
			return "ResultadoConsulta [tipoDoc=" + tipoDoc + ", conResultado=" + conResultado + ", conRespuesta="
					+ conRespuesta + ", conError=" + conError + ", unsigned=" + unsigned + ", signed=" + signed
					+ "]";
		}
		
		
		
	}

	void anularNextDocumento(TipoDoc tipo, Date date) throws EFacturaException;
	
}

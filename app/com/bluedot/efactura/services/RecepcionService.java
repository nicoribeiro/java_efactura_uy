package com.bluedot.efactura.services;

import java.util.List;

import java.util.Date;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;

import dgi.soap.recepcion.Data;

public interface RecepcionService {

	void sendCFE(CFE cfe) throws APIException;

	Data consultaResultadoSobre(String token, Long idReceptor) throws APIException;

	void consultaResultadoSobre(SobreEmitido sobre) throws APIException;

	ReporteDiario generarReporteDiario(Date date, Empresa empresa) throws APIException;

//	void consultarResultados(Date date, Empresa empresa) throws APIException;

	void reenviarSobre(SobreEmitido sobre) throws APIException;

	/** Envia el comprobante por mail: XML al receptor electronico, PDF a la direccion del comprobante. Devuelve lo enviado. */
	List<String> enviarCorreoReceptorElectronico(CFE cfe, String origen) throws APIException;

	/** A donde se enviaria el comprobante (sin enviar nada): xml y pdf con destino o motivo por el que no se envia */
	org.json.JSONObject destinosCorreo(CFE cfe);

	void enviarCorreoReceptorElectronico(SobreEmitido sobre) throws APIException;

//	public class ResultadoConsulta {
//		public TipoDoc tipoDoc;
//		public int conResultado;
//		public int conRespuesta;
//		public int conError;
//		public int unsigned;
//		public int signed;
//
//		public ResultadoConsulta(TipoDoc tipoDoc, int conResultado, int conRespuesta, int conError, int unsigned,
//				int signed) {
//			super();
//			this.tipoDoc = tipoDoc;
//			this.conResultado = conResultado;
//			this.conRespuesta = conRespuesta;
//			this.conError = conError;
//			this.unsigned = unsigned;
//			this.signed = signed;
//		}
//
//		@Override
//		public String toString() {
//			return "ResultadoConsulta [tipoDoc=" + tipoDoc + ", conResultado=" + conResultado + ", conRespuesta="
//					+ conRespuesta + ", conError=" + conError + ", unsigned=" + unsigned + ", signed=" + signed + "]";
//		}
//
//	}

}

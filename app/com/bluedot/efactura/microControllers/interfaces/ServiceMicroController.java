package com.bluedot.efactura.microControllers.interfaces;

import java.util.Date;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;

public interface ServiceMicroController
{

	void enviar(CFE cfe) throws APIException;

	void reenviar(SobreEmitido sobre) throws APIException;

	void consultarResultados(Date date) throws APIException;

	SobreEmitido consultaResultado(SobreEmitido sobre) throws APIException;

	ReporteDiario generarReporteDiario(Date date) throws APIException;

	void anularDocumento(CFE cfe) throws APIException;

	List<EmailMessage> obtenerYProcesarEmailsEntrantesDesdeServerCorreo() throws APIException;

	void enviarCfeEmpresa(CFE cfe) throws APIException;

	void enviarCorreoReceptorElectronico(SobreEmitido sobre) throws APIException;

	/**
	 * Hay 3 tipos de correos que pueden llegar a ser validos
	 * 
	 * 1 - Respuesta a un sobre con CFE emitidos por una empresa manejada por este
	 * sistema.
	 * 
	 * 2 - Respuesta a la recepcion de un SobreEmitido
	 * 
	 * 3 - Un sobre con CFE emitidos por otra empresa hacia una empresa manejada por
	 * el sistema.
	 */
	void procesarAttachments(int index, EmailMessage emailModel, boolean retry);

	void procesarAttachment(int index, Attachment attachment, boolean retry);

}

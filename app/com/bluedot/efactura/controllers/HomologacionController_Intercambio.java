package com.bluedot.efactura.controllers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletionStage;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.services.IntercambioService;
import com.bluedot.efactura.services.impl.IntercambioServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.istack.logging.Logger;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class HomologacionController_Intercambio extends AbstractController {

	static Logger logger = Logger.getLogger(HomologacionController_Intercambio.class);
	
	protected HomologacionController_Intercambio() {

	}

	@BodyParser.Of(BodyParser.Json.class)
	public CompletionStage<Result> ingresoSobre() throws APIException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		try {

			JsonNode jsonNode = request().body().asJson();

			JSONObject jsonObject = new JSONObject(jsonNode.toString());
			
			String path = jsonObject.getString("path");
			
			IntercambioService service = new IntercambioServiceImpl();
			
			Document document = XML.readDocument(path);
			
			EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document, EnvioCFEEntreEmpresas.class);
			
			/* TODO pasar a Logger
			 * print!
			 */
			XML.marshall(envioCFEEntreEmpresas, System.out);
			
			ACKSobredefType ackSobredefType = service.procesarSobre(envioCFEEntreEmpresas, new File(path).getName());
			
			XML.marshall(ackSobredefType, System.out);
			
			ACKCFEdefType resultCFE = service.procesarCFESobre(envioCFEEntreEmpresas, ackSobredefType, new File(path).getName());

			if (resultCFE !=null)
				XML.marshall(resultCFE, System.out);
			
		} catch (JSONException e) {
			throw APIException.raise(e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Promise.<Result> pure(ok());

	}
}

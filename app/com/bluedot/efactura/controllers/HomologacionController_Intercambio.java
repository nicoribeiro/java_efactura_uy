package com.bluedot.efactura.controllers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
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
import com.bluedot.commons.error.VerboseAction;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.services.IntercambioService;
import com.bluedot.efactura.services.impl.IntercambioServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.play4jpa.jpa.db.Tx;
import com.sun.istack.logging.Logger;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import play.Application;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;

@With(VerboseAction.class)
@Tx
@Transactional
@Security.Authenticated(Secured.class)
public class HomologacionController_Intercambio extends AbstractController {

	private IntercambioService intercambioService;
	
	@Inject
	public HomologacionController_Intercambio(JPAApi jpaApi, Provider<Application> application, IntercambioService intercambioService) {
		super(jpaApi,application);
		this.intercambioService = intercambioService;
	}

	static Logger logger = Logger.getLogger(HomologacionController_Intercambio.class);

	@BodyParser.Of(BodyParser.Json.class)
	public CompletionStage<Result> ingresoSobre() throws APIException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		try {

			JsonNode jsonNode = request().body().asJson();

			JSONObject jsonObject = new JSONObject(jsonNode.toString());
			
			String path = jsonObject.getString("path");
			
			Document document = XML.readDocument(path);
			
			EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document, EnvioCFEEntreEmpresas.class);
			
			/* TODO pasar a Logger
			 * print!
			 */
			XML.marshall(envioCFEEntreEmpresas, System.out);
			//TODO rehacer
//			ACKSobredefType ackSobredefType = service.procesarSobre(envioCFEEntreEmpresas, new File(path).getName());
			
//			XML.marshall(ackSobredefType, System.out);
			
//			ACKCFEdefType resultCFE = service.procesarCFESobre(envioCFEEntreEmpresas, ackSobredefType, new File(path).getName());

//			if (resultCFE !=null)
//				XML.marshall(resultCFE, System.out);
			
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
		
		return CompletableFuture.completedFuture(ok());

	}
}

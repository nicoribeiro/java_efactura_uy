package com.bluedot.efactura.controllers;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.Environment;
import com.fasterxml.jackson.databind.JsonNode;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import play.Play;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class HomologacionController_Intercambio extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(HomologacionController_Intercambio.class);
	
	protected HomologacionController_Intercambio() {

	}

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> ingresoSobre() throws APIException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		try {

			Environment env = Environment.valueOf(Play.application().configuration().getString(Constants.ENVIRONMENT));
			
			if (env==Environment.produccion) {
				throw APIException.raise(APIErrors.NO_SOPORTADO_EN_PRODUCCION);
			}
			
			JsonNode jsonNode = request().body().asJson();

			JSONObject jsonObject = new JSONObject(jsonNode.toString());
			
			String path = jsonObject.getString("path");
			
//			IntercambioMicroController service = new IntercambioMicroControllerDefault(new CFEMicroControllerDefault(modo, empresa, caeMicroController));
			
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
		
		return Promise.<Result> pure(ok());

	}
}

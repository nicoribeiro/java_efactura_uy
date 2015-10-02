package com.bluedot.efactura.services.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import com.bluedot.commons.PrettyPrint;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.EfacturaSecurity;
import com.bluedot.efactura.pool.WSRecepcionPool;
import com.bluedot.efactura.services.RecepcionService;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.EnvioCFE.Caratula;
import dgi.classes.recepcion.ObjectFactory;
import dgi.soap.recepcion.Data;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBRE;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBREResponse;
import dgi.soap.recepcion.WSEFacturaSoapPort;

public class RecepcionServiceImpl implements RecepcionService
{

	@Override
	public Data send(String cfe) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException
	{

		Objects.requireNonNull(cfe, "Parameter CFE is required");

		WSEFacturaSoapPort port = WSRecepcionPool.getInstance().checkOut();

		WSEFacturaEFACRECEPCIONSOBRE input = new WSEFacturaEFACRECEPCIONSOBRE();
		Data data = new Data();
		// data.setXmlData("<![CDATA[" + cfe + "]]>");
		data.setXmlData(cfe);
		input.setDatain(data);

		WSEFacturaEFACRECEPCIONSOBREResponse output = port.efacrecepcionsobre(input);

		WSRecepcionPool.getInstance().checkIn(port);

		return output.getDataout();

	}

	@Override
	public Data send(EFact efactura) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException, JAXBException, TransformerFactoryConfigurationError,
			TransformerException, UnrecoverableEntryException, DatatypeConfigurationException
	{
		/*
		 * Wrap Efact object with CFEDefType
		 */
		CFEDefType cfe = (new ObjectFactory()).createCFEDefType();
		cfe.setVersion("1.0");
		cfe.setEFact(efactura);

		/*
		 * Add CFE to a EnvioCFE object. EvioCFE object will be sent to the
		 * Service
		 */
		EnvioCFE envioCFE = new EnvioCFE();
		envioCFE.setVersion("1.0");
		envioCFE.getCFE().add(cfe);

		/*
		 * Add signature
		 */
		EnvioCFE signed = EfacturaSecurity.addInternalSignature(envioCFE);

		/*
		 * Add caratula
		 */
		addCaratula(signed);

		return this.send(signed);

	}

	private void addCaratula(EnvioCFE signed) throws EFacturaException, DatatypeConfigurationException, CertificateEncodingException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, FileNotFoundException, CertificateException, IOException
	{
		List<CFEDefType> cfes = signed.getCFE();

		String RUCemisor = null;

		String RUCreceptor = null;

		for (Iterator<CFEDefType> iterator = cfes.iterator(); iterator.hasNext();)
		{
			CFEDefType cfeDefType = iterator.next();

			String rucEmisor = null;
			String rucReceptor = null;

			if (cfeDefType.getEFact() != null)
			{
				rucEmisor = cfeDefType.getEFact().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getEFact().getEncabezado().getReceptor().getDocRecep();
			}

			if (cfeDefType.getEFactExp() != null)
			{
				rucEmisor = cfeDefType.getEFactExp().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getEFactExp().getEncabezado().getReceptor().getDocRecep();
			}

			if (cfeDefType.getERem() != null)
			{
				rucEmisor = cfeDefType.getERem().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getERem().getEncabezado().getReceptor().getDocRecep();
			}

			if (cfeDefType.getERemExp() != null)
			{
				rucEmisor = cfeDefType.getERemExp().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getERemExp().getEncabezado().getReceptor().getDocRecep();
			}

			if (cfeDefType.getEResg() != null)
			{
				rucEmisor = cfeDefType.getEResg().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getEResg().getEncabezado().getReceptor().getDocRecep();
			}

			if (cfeDefType.getETck() != null)
			{
				rucEmisor = cfeDefType.getETck().getEncabezado().getEmisor().getRUCEmisor();
				rucReceptor = cfeDefType.getETck().getEncabezado().getReceptor().getDocRecep();
			}

			if (RUCemisor == null)
				RUCemisor = rucEmisor;
			else if (!RUCemisor.equals(rucEmisor))
				throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("Cannot have many RUCemisor values on one envelope");

			if (RUCreceptor == null)
				RUCreceptor = rucReceptor;
			else if (!RUCemisor.equals(rucReceptor))
				throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("Cannot have many RUCreceptor on one envelope");
			
		}
		
		Caratula caratula = (new ObjectFactory()).createEnvioCFECaratula();
		
		caratula.setCantCFE(cfes.size());
		caratula.setFecha(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		caratula.setIdemisor(new BigInteger("1"));
		caratula.setRUCEmisor(RUCemisor);
		caratula.setRutReceptor(RUCreceptor);
		caratula.setVersion("1.0");
		caratula.setX509Certificate(EfacturaSecurity.getCertificate(Commons.getCetificateAlias(), Commons.getCertificatePassword(), Commons.getKeyStore()));

		signed.setCaratula(caratula);
		
	}

	@Override
	public Data send(EnvioCFE cfe) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException, JAXBException, TransformerFactoryConfigurationError,
			TransformerException
	{
		JAXBContext context = JAXBContext.newInstance(EnvioCFE.class);
		Marshaller marshaller = context.createMarshaller();
		StringWriter sw = new StringWriter();
		marshaller.marshal(cfe, sw);

		return this.send(PrettyPrint.prettyPrintXML(sw.toString()));

	}

}

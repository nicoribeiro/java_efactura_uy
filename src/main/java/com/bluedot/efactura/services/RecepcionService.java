package com.bluedot.efactura.services;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import com.bluedot.efactura.EFacturaException;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.EnvioCFE;
import dgi.soap.recepcion.Data;

public interface RecepcionService
{

	Data send(String cfe) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException, JAXBException;

	Data send(EnvioCFE cfe) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException, JAXBException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException;

	Data send(EFact efactura) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EFacturaException, JAXBException, TransformerFactoryConfigurationError, TransformerException, UnrecoverableEntryException, DatatypeConfigurationException;

}

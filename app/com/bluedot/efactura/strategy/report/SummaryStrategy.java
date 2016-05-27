package com.bluedot.efactura.strategy.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.bluedot.commons.IO;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.reporte.MontosFyT;
import dgi.classes.reporte.MontosFyT.MntsFyTItem;
import dgi.classes.reporte.MontosRes;
import dgi.classes.reporte.MontosRes.MntsResItem;
import dgi.classes.reporte.ReporteDefType;
import dgi.classes.reporte.RngDocsAnulados;
import dgi.classes.reporte.RngDocsAnulados.RDAItem;
import dgi.classes.reporte.RngDocsUtil;
import dgi.classes.reporte.RngDocsUtil.RDUItem;

public interface SummaryStrategy {

	public class Builder {

		private TipoDoc tipo;

		public TipoDoc getTipo() {
			return tipo;
		}

		public void setTipo(TipoDoc tipo) {
			this.tipo = tipo;
		}

		public Builder withTipo(TipoDoc tipo) {
			this.tipo = tipo;
			return this;
		}

		public SummaryStrategy build() throws EFacturaException {
			switch (tipo) {
			// TODO se puede poner el nombre de la clase en el TipoDoc y hacer
			// un class.forName generico
			case Nota_de_Credito_de_eFactura:
				return new Strategy_112();
			case eFactura:
				return new Strategy_111();
			case Nota_de_Debito_de_eFactura:
				return new Strategy_113();
			case eTicket:
				return new Strategy_101();
			case Nota_de_Credito_de_eTicket:
				return new Strategy_102();
			case Nota_de_Debito_de_eTicket:
				return new Strategy_103();
			case eResguardo:
				return new Strategy_182();
			case eTicket_Contingencia:
				return new Strategy_201();
			case eFactura_Contingencia:
				return new Strategy_211();

			case Nota_de_Credito_de_eFactura_Contingencia:
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Credito_de_eTicket_Contingencia:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Debito_de_eFactura_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case Nota_de_Debito_de_eTicket_Contingencia:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:

			
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case eFactura_Venta_por_Cuenta_Ajena:
			case eFactura_Venta_por_Cuenta_Ajena_Contingencia:

			case eRemito:
			case eRemito_Contingencia:
			case eRemito_de_Exportacion:
			case eRemito_de_Exportacion_Contingencia:

			case eResguardo_Contingencia:
 
			
			case eTicket_Venta_por_Cuenta_Ajena:
			case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
				throw EFacturaException.raise(EFacturaErrors.NOT_SUPPORTED)
						.setDetailMessage("Estrategia para el tipo: " + tipo.friendlyName);
			} 
			return null;

		}
	}

	public class SummaryDatatype {
		protected Date fecha = null;
		int cantDoc = 0;
		int cantDocRechazados = 0;
		int cantDocSinRespuesta = 0;
		int cantDocAceptados = 0;
		int mayor10000UI = 0;
		Monto monto = new Monto();
		RngDocsAnulados rngDocsAnulados = new RngDocsAnulados();
		RngDocsUtil rngDocsUtil = new RngDocsUtil();
	}

	public class Monto {

		protected BigDecimal totMntNoGrv = new BigDecimal("0");
		protected BigDecimal totMntExpyAsim = new BigDecimal("0");
		protected BigDecimal totMntImpPerc = new BigDecimal("0");
		protected BigDecimal totMntIVAenSusp = new BigDecimal("0");
		protected BigDecimal totMntIVATasaMin = new BigDecimal("0");
		protected BigDecimal totMntIVATasaBas = new BigDecimal("0");
		protected BigDecimal totMntIVAOtra = new BigDecimal("0");
		protected BigDecimal mntIVATasaMin = new BigDecimal("0");
		protected BigDecimal mntIVATasaBas = new BigDecimal("0");
		protected BigDecimal mntIVAOtra = new BigDecimal("0");
		protected BigDecimal ivaTasaMin = new BigDecimal("10");
		protected BigDecimal ivaTasaBas = new BigDecimal("22");
		protected BigDecimal totMntTotal = new BigDecimal("0");
		protected BigDecimal totMntRetenido = new BigDecimal("0");
		protected BigDecimal totValRetPerc = new BigDecimal("0");
	}

	static SummaryDatatype getSummary(TipoDoc tipo, Date date) throws FileNotFoundException, IOException {
		// TODO esto solo camina para un cfe por sobre
		File directorio = new File(Commons.getCfeFolder(date, tipo));
		SummaryDatatype summary = new SummaryDatatype();
		summary.fecha = date;

		String[] files = directorio.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.contains("unsigned"))
					return true;
				return false;
			}
		});

		summary.cantDoc = files.length;

		for (int i = 0; i < files.length; i++) {
			String serie = files[i].split("_")[0];
			String nroCFE = files[i].split("_")[1];
			String resultFilePath = directorio + File.separator + serie + "_" + nroCFE + "_result.xml";

			RDUItem item = getRDUItem(serie, nroCFE);
			summary.rngDocsUtil.getRDUItem().add(item);
			
			if (new File(resultFilePath).exists()) {
				String content = IO.readFile(resultFilePath, Charset.forName("UTF-8"));
				if (content.contains("<Estado>")) {
					String estado = content.split("<Estado>")[1].split("</Estado>")[0];

					if (estado.equals("AE")) {
						//Estado es "AE", se aceptop correctamente el cfe
						summary.cantDocAceptados++;
						sumarizarMontos(directorio + File.separator + files[i], summary);
					} else {
						// Estado distinto de "AE", se rechazo el cfe
						summary.cantDocRechazados++;
						RDAItem itemAnul = getRDAItem(serie, nroCFE);
						summary.rngDocsAnulados.getRDAItem().add(itemAnul);
					}
				}else{
					// resultado no contiene el tag <Estado>, se asume que fue rechazado
					summary.cantDocRechazados++;
					RDAItem itemAnul = getRDAItem(serie, nroCFE);
					summary.rngDocsAnulados.getRDAItem().add(itemAnul);
				}
					
			} else {
				summary.cantDocSinRespuesta++;
				RDAItem itemAnul = getRDAItem(serie, nroCFE);
				summary.rngDocsAnulados.getRDAItem().add(itemAnul);
			}
		}

		return summary;

	}

	static RDAItem getRDAItem(String serie, String nroCFE) {
		RDAItem item = new RDAItem();
		item.setSerie(serie);
		item.setNroDesde(new BigInteger(nroCFE));
		item.setNroHasta(new BigInteger(nroCFE));
		return item;
	}

	static RDUItem getRDUItem(String serie, String nroCFE) {
		RDUItem item = new RDUItem();
		item.setSerie(serie);
		item.setNroDesde(new BigInteger(nroCFE));
		item.setNroHasta(new BigInteger(nroCFE));
		return item;
	}

	static void sumarizarMontos(String file, SummaryDatatype summary) throws IOException {
		String content = IO.readFile(file, Charset.forName("UTF-8"));
		Monto monto = summary.monto;

		monto.totMntNoGrv = safeAdd(monto.totMntNoGrv, content, "DGICFE:MntNoGrv");

		// TODO tipo moneda es importante, creo que se debe enviar solo pesos,
		// preguntar a pablo
		monto.totMntExpyAsim= safeAdd(monto.totMntExpyAsim, content, "DGICFE:MntExpoyAsim");
		monto.totMntImpPerc= safeAdd(monto.totMntImpPerc, content, "DGICFE:MntImpuestoPerc");
		
		monto.totMntIVAenSusp = safeAdd(monto.totMntIVAenSusp, content, "DGICFE:MntIVaenSusp");
		monto.totMntIVATasaMin=safeAdd(monto.totMntIVATasaMin, content, "DGICFE:MntNetoIvaTasaMin");
		monto.totMntIVATasaBas=safeAdd(monto.totMntIVATasaBas, content, "DGICFE:MntNetoIVATasaBasica");
		monto.totMntIVAOtra=safeAdd(monto.totMntIVAOtra, content, "DGICFE:MntNetoIVAOtra");
		
		monto.mntIVATasaBas=safeAdd(monto.mntIVATasaBas, content, "DGICFE:MntIVATasaBasica");
		monto.mntIVATasaMin=safeAdd(monto.mntIVATasaMin, content, "DGICFE:MntIVATasaMin");
		monto.mntIVAOtra=safeAdd(monto.mntIVAOtra, content, "DGICFE:MntIVAOtra");
		
		monto.totMntTotal=safeAdd(monto.totMntTotal, content, "DGICFE:MntTotal");
		monto.totMntRetenido=safeAdd(monto.totMntRetenido, content, "DGICFE:MntRetenido");
		
		//TODO sacar el monto de 10000 UI a un lado calculable
		if ((monto.totMntTotal.subtract(monto.mntIVATasaBas).subtract(monto.mntIVATasaMin).subtract(monto.mntIVAOtra)).compareTo(new BigDecimal(34036))==1)
			summary.mayor10000UI++;
		
		monto.totValRetPerc=safeAdd(monto.totValRetPerc, content, "DGICFE:MntTotRetenido");


	}

	static BigDecimal safeAdd(BigDecimal acumulado, String content, String key) {
		String open_key = "<" + key + ">";
		String close_key = "</" + key + ">";

		String[] temp = content.split(open_key);
		
		if (acumulado==null)
			acumulado = new BigDecimal("0");
		
		if (temp.length == 1)
			return acumulado;
		
		for (int i = 1; i < temp.length; i++) {
			String add = temp[i].split(close_key)[0];

			if (add != null && !add.equals("0")) {
				 acumulado = acumulado.add(new BigDecimal(add));
			}
		}

		return acumulado;
	}

	void buildSummary(ReporteDefType reporte, Date date);

	static MontosFyT getMontosFyT(SummaryDatatype summary) throws DatatypeConfigurationException {
		MontosFyT montos = new MontosFyT();
		MntsFyTItem item = new MntsFyTItem();

		// TODO sacar el id de sucursal para un lado comun
		item.setCodSuc(new BigInteger("2"));
		XMLGregorianCalendar date = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(summary.fecha));
		item.setFecha(date);
		item.setIVATasaBas(summary.monto.ivaTasaBas);
		item.setIVATasaMin(summary.monto.ivaTasaMin);
		
		item.setMntIVAOtra(summary.monto.mntIVAOtra);
		item.setMntIVATasaBas(summary.monto.mntIVATasaBas);
		item.setMntIVATasaMin(summary.monto.mntIVATasaMin);
		
		item.setTotMntExpyAsim(summary.monto.totMntExpyAsim);
		item.setTotMntImpPerc(summary.monto.totMntImpPerc);
		item.setTotMntIVAenSusp(summary.monto.totMntIVAenSusp);
		item.setTotMntIVAOtra(summary.monto.totMntIVAOtra);
		item.setTotMntIVATasaBas(summary.monto.totMntIVATasaBas);
		item.setTotMntIVATasaMin(summary.monto.totMntIVATasaMin);
		item.setTotMntNoGrv(summary.monto.totMntNoGrv);
		item.setTotMntRetenido(summary.monto.totMntRetenido);
		item.setTotMntTotal(summary.monto.totMntTotal);
		montos.getMntsFyTItem().add(item);
		return montos;
	}

	static MontosRes getMontosResg(SummaryDatatype summary) throws DatatypeConfigurationException {
		MontosRes montos = new MontosRes();
		MntsResItem item = new MntsResItem();

		// TODO sacar el id de sucursal para un lado comun
		item.setCodSuc(new BigInteger("2"));
		XMLGregorianCalendar date = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(summary.fecha));
		item.setFecha(date);
		item.setTotMntRetenido(summary.monto.totValRetPerc);
		montos.getMntsResItem().add(item);
		return montos;
	}
}

package com.bluedot.efactura.strategy.report;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.IVA;
import com.bluedot.efactura.model.IndicadorFacturacion;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.model.UI;

import dgi.classes.recepcion.TipMonType;
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

		public Builder withTipo(TipoDoc tipo) {
			this.tipo = tipo;
			return this;
		}

		public SummaryStrategy build() throws APIException {
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
				return null;
			default:
				return null;
			}

		}
	}

	public class SummaryDatatype {
		int cantDocUtilizados = 0;
		int cantDocRechazados = 0;
		int cantDocSinRespuesta = 0;
		int cantDocEmitidos = 0;
		int mayor10000UI = 0;
		HashMap<Date, Monto> montos = new HashMap<Date, Monto>();
		RngDocsAnulados rngDocsAnulados = new RngDocsAnulados();
		RngDocsUtil rngDocsUtil = new RngDocsUtil();
	}

	public class Monto {
		protected Date fecha = null;
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
		protected BigDecimal ivaTasaMin = new BigDecimal(String
				.valueOf(IVA.findByIndicadorFacturacion(IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA)
						.getPorcentajeIVA()));
		protected BigDecimal ivaTasaBas = new BigDecimal(String
				.valueOf(IVA.findByIndicadorFacturacion(IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA)
						.getPorcentajeIVA()));
		protected BigDecimal totMntTotal = new BigDecimal("0");
		protected BigDecimal totMntRetenido = new BigDecimal("0");
		protected BigDecimal totValRetPerc = new BigDecimal("0");
	}

	static SummaryDatatype getSummary(Empresa empresa, TipoDoc tipo, List<CFE> cfes) throws APIException {
		SummaryDatatype summary = new SummaryDatatype();
		//summary.fecha = date;

		for (Iterator<CFE> iterator2 = cfes.iterator(); iterator2.hasNext();) {
			CFE cfe = iterator2.next();

			if (cfe.getTipo() == tipo) {

				RDUItem item = getRDUItem(cfe.getSerie(), cfe.getNro());
				summary.rngDocsUtil.getRDUItem().add(item);

				if (cfe.getEstado() != null)
					switch (cfe.getEstado()) {
					case AE:
						summary.cantDocEmitidos++;
						sumarizarMontos(cfe, summary);
						break;
					case BE:
					case CE:
						summary.cantDocRechazados++;
						RDAItem itemAnul = getRDAItem(cfe.getSerie(), cfe.getNro());
						summary.rngDocsAnulados.getRDAItem().add(itemAnul);
						break;
					}
				else {
					/*
					 * No tiene estado. Si tiene generador_id entonces esta esperando por la respuesta o anulacion
					 */
					if (cfe.getGeneradorId()!=null)
						throw APIException.raise(APIErrors.HAY_CFE_SIN_RESPUESTA);
					
					summary.cantDocSinRespuesta++;
					RDAItem itemAnul = getRDAItem(cfe.getSerie(), cfe.getNro());
					summary.rngDocsAnulados.getRDAItem().add(itemAnul);
				}

			}
		}
		summary.cantDocUtilizados = summary.cantDocEmitidos + summary.cantDocRechazados + summary.cantDocSinRespuesta;
		return summary;

	}

	static RDAItem getRDAItem(String serie, Long nroCFE) {
		RDAItem item = new RDAItem();
		item.setSerie(serie);
		item.setNroDesde(new BigInteger(String.valueOf(nroCFE)));
		item.setNroHasta(new BigInteger(String.valueOf(nroCFE)));
		return item;
	}

	static RDUItem getRDUItem(String serie, Long nroCFE) {
		RDUItem item = new RDUItem();
		item.setSerie(serie);
		item.setNroDesde(new BigInteger(String.valueOf(nroCFE)));
		item.setNroHasta(new BigInteger(String.valueOf(nroCFE)));
		return item;
	}

	static void sumarizarMontos(CFE cfe, SummaryDatatype summary) throws APIException {
		Monto monto = summary.montos.get(cfe.getFechaEmision());

		if (monto == null) {
			monto = new Monto();
			monto.fecha = cfe.getFechaEmision();
			summary.montos.put(cfe.getFechaEmision(), monto);
		}
			
		
		double tipoCambio = 1;
		
		if (cfe.getMoneda()!=TipMonType.UYU)
			tipoCambio = cfe.getTipoCambio();
		
		monto.totMntNoGrv = safeAdd(monto.totMntNoGrv, cfe.getTotMntNoGrv(), tipoCambio);
		monto.totMntExpyAsim = safeAdd(monto.totMntExpyAsim, cfe.getTotMntExpyAsim(), tipoCambio);
		monto.totMntImpPerc = safeAdd(monto.totMntImpPerc, cfe.getTotMntImpPerc(), tipoCambio);

		monto.totMntIVAenSusp = safeAdd(monto.totMntIVAenSusp, cfe.getTotMntIVAenSusp(), tipoCambio);
		monto.totMntIVATasaMin = safeAdd(monto.totMntIVATasaMin, cfe.getTotMntIVATasaMin(), tipoCambio);
		monto.totMntIVATasaBas = safeAdd(monto.totMntIVATasaBas, cfe.getTotMntIVATasaBas(), tipoCambio);
		monto.totMntIVAOtra = safeAdd(monto.totMntIVAOtra, cfe.getTotMntIVAOtra(), tipoCambio);

		monto.mntIVATasaBas = safeAdd(monto.mntIVATasaBas, cfe.getMntIVATasaBas(), tipoCambio);
		monto.mntIVATasaMin = safeAdd(monto.mntIVATasaMin, cfe.getMntIVATasaMin(), tipoCambio);
		monto.mntIVAOtra = safeAdd(monto.mntIVAOtra, cfe.getMntIVAOtra(), tipoCambio);

		monto.totMntTotal = safeAdd(monto.totMntTotal, cfe.getTotMntTotal(), tipoCambio);
		monto.totMntRetenido = safeAdd(monto.totMntRetenido, cfe.getTotMntRetenido(), tipoCambio);
		monto.totValRetPerc = safeAdd(monto.totValRetPerc, cfe.getTotValRetPerc(), tipoCambio);
		
		Date date = cfe.getFechaEmision();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		int year = calendar.get(Calendar.YEAR);
		UI ui = UI.findByAnio(year, true);
		
		if (safeSubstract(cfe.getTotMntTotal(), cfe.getMntIVATasaBas(), cfe.getMntIVATasaMin(), cfe.getMntIVAOtra()) * tipoCambio > ui.getCotizacion() * UI.MAX_UI)
			summary.mayor10000UI++;

	}
	
	static double safeSubstract(Double a, Double b, Double c, Double d) {
		if (a==null)
			a = 0d;
		if (b==null)
			b = 0d;
		if (c==null)
			c = 0d;
		if (d==null)
			d = 0d;
		return a - b - c - d;
	}

	static BigDecimal safeAdd(BigDecimal acumulado, Double value, double tipoCambio) {

		if (acumulado == null)
			acumulado = new BigDecimal("0");

		if (value==null)
			value = 0d;
		
		acumulado = acumulado.add(new BigDecimal(value * tipoCambio)).setScale(2, BigDecimal.ROUND_HALF_UP);

		return acumulado;
	}

	void buildSummary(Empresa empresa, ReporteDefType reporte, List<CFE> cfes) throws APIException;

	static MontosFyT getMontosFyT(SummaryDatatype summary) throws APIException {
		MontosFyT montos = new MontosFyT();
		for (Iterator<Date> iterator = summary.montos.keySet().iterator(); iterator.hasNext();) {
			Date fecha = iterator.next();
			montos.getMntsFyTItem().add(getMontosFyTItem(summary.montos.get(fecha)));
		}
		
		if (montos.getMntsFyTItem().size()==0)
			return null;
		return montos;
	}
	
	static MntsFyTItem getMontosFyTItem(Monto monto) throws APIException {
		try {
			
			MntsFyTItem item = new MntsFyTItem();

			// TODO sacar el id de sucursal para un lado comun
			item.setCodSuc(new BigInteger("2"));
			XMLGregorianCalendar date = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(monto.fecha));
			item.setFecha(date);
			item.setIVATasaBas(monto.ivaTasaBas);
			item.setIVATasaMin(monto.ivaTasaMin);

			item.setMntIVAOtra(monto.mntIVAOtra);
			item.setMntIVATasaBas(monto.mntIVATasaBas);
			item.setMntIVATasaMin(monto.mntIVATasaMin);

			item.setTotMntExpyAsim(monto.totMntExpyAsim);
			item.setTotMntImpPerc(monto.totMntImpPerc);
			item.setTotMntIVAenSusp(monto.totMntIVAenSusp);
			item.setTotMntIVAOtra(monto.totMntIVAOtra);
			item.setTotMntIVATasaBas(monto.totMntIVATasaBas);
			item.setTotMntIVATasaMin(monto.totMntIVATasaMin);
			item.setTotMntNoGrv(monto.totMntNoGrv);
			item.setTotMntRetenido(monto.totMntRetenido);
			item.setTotMntTotal(monto.totMntTotal);
			
			return item;
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
	}

	static MontosRes getMontosResg(SummaryDatatype summary) throws APIException {
		MontosRes montos = new MontosRes();
		for (Iterator<Date> iterator = summary.montos.keySet().iterator(); iterator.hasNext();) {
			Date fecha = iterator.next();
			montos.getMntsResItem().add(getMontosResItem(summary.montos.get(fecha)));
		}
		return montos;
	}
	
	static MntsResItem getMontosResItem(Monto monto) throws APIException {
		try {
			
			MntsResItem item = new MntsResItem();

			// TODO sacar el id de sucursal para un lado comun
			item.setCodSuc(new BigInteger("2"));
			XMLGregorianCalendar date = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(monto.fecha));
			item.setFecha(date);
			item.setTotMntRetenido(monto.totValRetPerc);
			return item;
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
	}
}

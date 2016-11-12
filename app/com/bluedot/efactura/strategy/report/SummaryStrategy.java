package com.bluedot.efactura.strategy.report;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.TipoDoc;

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
		protected Date fecha = null;
		int cantDocUtilizados = 0;
		int cantDocRechazados = 0;
		int cantDocSinRespuesta = 0;
		int cantDocEmitidos = 0;
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

	static SummaryDatatype getSummary(Empresa empresa, TipoDoc tipo, Date date, List<SobreEmitido> sobres) throws APIException {
		SummaryDatatype summary = new SummaryDatatype();
		summary.fecha = date;

		for (Iterator<SobreEmitido> iterator = sobres.iterator(); iterator.hasNext();) {
			SobreEmitido sobreEmitido = (SobreEmitido) iterator.next();

			for (Iterator<CFE> iterator2 = sobreEmitido.getCfes().iterator(); iterator2.hasNext();) {
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

	static void sumarizarMontos(CFE cfe, SummaryDatatype summary) {
		Monto monto = summary.monto;

		monto.totMntNoGrv = safeAdd(monto.totMntNoGrv, cfe.getTotMntNoGrv());

		// TODO tipo moneda es importante, creo que se debe enviar solo pesos,
		// preguntar a pablo
		monto.totMntExpyAsim = safeAdd(monto.totMntExpyAsim, cfe.getTotMntExpyAsim());
		monto.totMntImpPerc = safeAdd(monto.totMntImpPerc, cfe.getTotMntImpPerc());

		monto.totMntIVAenSusp = safeAdd(monto.totMntIVAenSusp, cfe.getTotMntIVAenSusp());
		monto.totMntIVATasaMin = safeAdd(monto.totMntIVATasaMin, cfe.getTotMntIVATasaMin());
		monto.totMntIVATasaBas = safeAdd(monto.totMntIVATasaBas, cfe.getTotMntIVATasaBas());
		monto.totMntIVAOtra = safeAdd(monto.totMntIVAOtra, cfe.getTotMntIVAOtra());

		monto.mntIVATasaBas = safeAdd(monto.mntIVATasaBas, cfe.getMntIVATasaBas());
		monto.mntIVATasaMin = safeAdd(monto.mntIVATasaMin, cfe.getMntIVATasaMin());
		monto.mntIVAOtra = safeAdd(monto.mntIVAOtra, cfe.getMntIVAOtra());

		monto.totMntTotal = safeAdd(monto.totMntTotal, cfe.getTotMntTotal());
		monto.totMntRetenido = safeAdd(monto.totMntRetenido, cfe.getTotMntRetenido());

		// TODO sacar el monto de 10000 UI a un lado calculable
		if ((monto.totMntTotal.subtract(monto.mntIVATasaBas).subtract(monto.mntIVATasaMin).subtract(monto.mntIVAOtra))
				.compareTo(new BigDecimal(34036)) == 1)
			summary.mayor10000UI++;

		monto.totValRetPerc = safeAdd(monto.totValRetPerc, cfe.getTotValRetPerc());

	}

	static BigDecimal safeAdd(BigDecimal acumulado, Double value) {

		if (acumulado == null)
			acumulado = new BigDecimal("0");

		if (value==null)
			value = 0d;
		
		acumulado = acumulado.add(new BigDecimal(value)).setScale(2, BigDecimal.ROUND_HALF_UP);

		return acumulado;
	}

	void buildSummary(Empresa empresa, ReporteDefType reporte, Date date, List<SobreEmitido> sobres) throws APIException;

	static MontosFyT getMontosFyT(SummaryDatatype summary) throws APIException {
		try {
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
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
	}

	static MontosRes getMontosResg(SummaryDatatype summary) throws APIException {
		try {
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
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
	}
}

package com.bluedot.efactura;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.strategy.builder.CFEStrategy;
import com.bluedot.efactura.strategy.builder.EfactStrategy;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.Totales;
import dgi.classes.recepcion.wrappers.IdDocInterface;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ReceptorInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/*
 * http://www.mysamplecode.com/2012/10/generate-pdf-using-java-and-itext.html
 */
public class GenerateInvoice {

	// DETALLE
	private int itemsPerPage = 15;
	private int detailsRowSize = 15;
	private int detailsLowerLeft_y;
	private int detailsHeight;
	private int totalesHeight = 100;

	// CABEZAL
	private int headerHeight = 200;
	private int headerRowSize = 15;

	// PAGE
	private int pageHeight = 842;
	private int pageWidth = 595;
	private int leftMargin = 20;
	private int rightMargin = 20;
	private BaseFont bfBold;
	private BaseFont bf;
	private int pageNumber = 0;

	// SELLO DIGITAL & CAE DATA
	int selloDigitalAndCAEDataHeight = 130;

	// ADENDA
	int adendaHeight = 150;

	int height;
	int width;
	
	
	private CFEStrategy invoiceStrategy;
	private JSONObject adenda;
	private String pdfFilename;
	

	public static void main(String[] args) throws IOException, JSONException, EFacturaException {

		String pdfFilename = "";
		
		if (args.length < 1) {
			System.err.println("Usage: java " + GenerateInvoice.class.getName() + " PDF_Filename");
			System.exit(1);
		}

		pdfFilename = args[0].trim();

		String file = IO.readFile("resources/json/efactura.json", Charset.defaultCharset());

		JSONObject json = new JSONObject(file);

		EFacturaFactory factory = new EFacturaFactoryImpl();

		EFact efactura = factory.getCFEController().createEfactura(json.getJSONObject("eFact"));

		GenerateInvoice generateInvoice = new GenerateInvoice(pdfFilename, efactura, new JSONObject());
		
		generateInvoice.createPDF();

	}

	private void initializeCoordenates() {
		detailsLowerLeft_y = pageHeight - headerHeight - detailsRowSize * (itemsPerPage + 1);
		detailsHeight = pageHeight - detailsLowerLeft_y - headerHeight + totalesHeight;
		height = detailsRowSize * (itemsPerPage + 1);
		width = pageWidth - leftMargin - rightMargin;
	}
	
	public GenerateInvoice(String pdfFilename, EFact efactura, JSONObject adenda) throws EFacturaException{
		this.invoiceStrategy = new EfactStrategy(efactura, TipoDoc.eFactura);
		this.adenda = adenda;
		this.pdfFilename = pdfFilename;
	}
	

	public void createPDF() {

		/*
		 * El formato de este documento esta definido en
		 * Formato_CFE_v13_23072014.pdf
		 */

		/*
		 * ORDEN:
		 * 
		 * 1) Identificación del emisor electrónico;
		 * 
		 * 2) Identificación del comprobante;
		 * 
		 * 3) Identificación del receptor;
		 * 
		 * 4) Cuerpo del comprobante;
		 * 
		 * 5) Pie del comprobante.(solo ultima pagina)
		 * 
		 */

		Document doc = new Document();
		PdfWriter docWriter = null;
		initializeFonts();

		initializeCoordenates();

		try {
			String path = pdfFilename;
			docWriter = PdfWriter.getInstance(doc, new FileOutputStream(path));
			doc.addAuthor("nicoribeiro");
			doc.addCreationDate();
			doc.addProducer();
			doc.addCreator("https://github.com/nicoribeiro/java_efactura_uy");
			doc.addTitle("Invoice");
			doc.setPageSize(PageSize.A4);

			doc.open();
			PdfContentByte cb = docWriter.getDirectContent();

			boolean beginPage = true;
			int row = 0;

			for (int i = 0; i < invoiceStrategy.getItem().size(); i++) {
				if (beginPage) {
					beginPage = false;
					generateTablaCuerpo(doc, cb);
					generateIdentificacionEmisorElectronico(doc, cb, invoiceStrategy.getEmisor());
					generateIdentificacionComprobante(doc, cb, invoiceStrategy.getEmisor(),
							invoiceStrategy.getCAEData(), 
							invoiceStrategy.getIdDoc(),
							invoiceStrategy.getReceptor());
					generateSelloDigital(doc, cb, invoiceStrategy.getEmisor(),
							invoiceStrategy.getIdDoc());
					row = 1;
				}
				generateLineaDeCuerpo(doc, cb, i, row, invoiceStrategy.getItem().get(i));
				row += 1;
				if (row == itemsPerPage + 1) {
					printPageNumber(cb);
					doc.newPage();
					beginPage = true;
				}

			}
			printPageNumber(cb);

			generateTotales(doc, cb);
			
			generateCaeData(doc, cb, invoiceStrategy.getCAEData(), invoiceStrategy.getEmisor()
					);
			generateAdenda(doc, cb, adenda);

		} catch (DocumentException dex) {
			dex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (doc != null) {
				doc.close();
			}
			if (docWriter != null) {
				docWriter.close();
			}
		}
	}

	/*
	 * 1) Identificación del emisor electrónico;
	 */
	public void generateIdentificacionEmisorElectronico(Document doc, PdfContentByte cb, Emisor emisor)
			throws MalformedURLException, IOException, DocumentException {

		int emisor_y = 680;
		int emisor_x = leftMargin;

		/*
		 * Logo de la empresa
		 */
		Image companyLogo = Image.getInstance("resources/images/Olympic-logo.png");
		companyLogo.setAbsolutePosition(25, 700);
		companyLogo.scalePercent(10);
		doc.add(companyLogo);

		/*
		 * Datos del Emisor
		 */
		createHeadings(cb, emisor_x, emisor_y, emisor.getRznSoc());
		createHeadings(cb, emisor_x, emisor_y - detailsRowSize, emisor.getDomFiscal());
		createHeadings(cb, emisor_x, emisor_y - detailsRowSize * 2, emisor.getCiudad());
		createHeadings(cb, emisor_x, emisor_y - detailsRowSize * 3,
				emisor.getTelefono() != null && emisor.getTelefono().size() > 0 ? emisor.getTelefono().get(0) : null);

	}

	/*
	 * 2) Identificación del comprobante;
	 * 
	 * 3) Identificación del receptor;
	 */
	public void generateIdentificacionComprobante(Document doc, PdfContentByte cb, Emisor emisor, CAEDataType caeData,
			IdDocInterface idDoc, ReceptorInterface receptor) {

		int receptor_y = 780;
		int receptor_x = 280;

		int idDoc_y = 780;
		int idDoc_x = receptor_x + 200;

		/*
		 * Datos del CFE
		 */

		TipoDoc tipoDoc = TipoDoc.fromInt(Integer.parseInt(idDoc.getTipoCFE().toString()));

		createHeadings(cb, idDoc_x, idDoc_y, "RUT " + emisor.getRUCEmisor());
		createHeadings(cb, idDoc_x, idDoc_y - headerRowSize, tipoDoc.friendlyName);
		createHeadings(cb, idDoc_x, idDoc_y - headerRowSize * 2, idDoc.getSerie() + " " + idDoc.getNro());
		createHeadings(cb, idDoc_x, idDoc_y - headerRowSize * 3, idDoc.getFmaPago().toString());

		/*
		 * Identificacion del RECEPTOR
		 */
		cb.rectangle(receptor_x, idDoc_y - (headerRowSize - 3) * 2, 100, 40);
		cb.stroke();

		switch (tipoDoc) {
		case eFactura:
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eFactura_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eRemito:
		case eRemito_Contingencia:
		case eResguardo:
		case eResguardo_Contingencia:

			createHeadings(cb, receptor_x + 50, receptor_y, "RUC COMPRADOR", PdfContentByte.ALIGN_CENTER);
			createHeadings(cb, receptor_x + 50, receptor_y - headerRowSize, "123456789012",
					PdfContentByte.ALIGN_CENTER);
			break;

		case eTicket:
		case eTicket_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			createHeadings(cb, receptor_x + 50, receptor_y, "CONSUMO FINAL", PdfContentByte.ALIGN_CENTER);
			break;

		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
			createHeadings(cb, receptor_x + 50, receptor_y, "EXPORTACION", PdfContentByte.ALIGN_CENTER);
			break;
		}

		createContent(cb, receptor_x, receptor_y - headerRowSize * 3, receptor.getRznSocRecep(),
				PdfContentByte.ALIGN_LEFT);
		createContent(cb, receptor_x, receptor_y - headerRowSize * 4, receptor.getDirRecep(),
				PdfContentByte.ALIGN_LEFT);
		createContent(cb, receptor_x, receptor_y - headerRowSize * 5,
				receptor.getCiudadRecep() + " - " + receptor.getDeptoRecep(), PdfContentByte.ALIGN_LEFT);

	}

	/*
	 * 4) Cuerpo del comprobante;
	 */
	public void generateTablaCuerpo(Document doc, PdfContentByte cb) {

		cb.setLineWidth(1f);

		/*
		 * Detalle de productos
		 */
		cb.rectangle(leftMargin, detailsLowerLeft_y, width, height);

		// horizontal debajo de los column names
		cb.moveTo(leftMargin, detailsLowerLeft_y + height - detailsRowSize);
		cb.lineTo(width + leftMargin, detailsLowerLeft_y + height - detailsRowSize);

		// vertical
		cb.moveTo(50, detailsLowerLeft_y);
		cb.lineTo(50, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(150, detailsLowerLeft_y);
		cb.lineTo(150, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(430, detailsLowerLeft_y);
		cb.lineTo(430, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(500, detailsLowerLeft_y);
		cb.lineTo(500, height + detailsLowerLeft_y);

		cb.stroke();

		// Titulos de detalle de productos
		int title_y = height + detailsLowerLeft_y - detailsRowSize + 5;
		createHeadings(cb, 22, title_y, "Cant");
		createHeadings(cb, 52, title_y, "Codigo");
		createHeadings(cb, 152, title_y, "Item Description");
		createHeadings(cb, 432, title_y, "Predio Unitario");
		createHeadings(cb, 502, title_y, "Monto");

	}

	/*
	 * 4) Cuerpo del comprobante;
	 */
	public void generateLineaDeCuerpo(Document doc, PdfContentByte cb, int index, int row, ItemInterface item) {
		// DecimalFormat df = new DecimalFormat("0.00");

			System.out.println(index + " - " + row);
			int y = pageHeight - headerHeight - detailsRowSize - row * detailsRowSize + 5;

			createContent(cb, 48, y, item.getCantidad().toPlainString(), PdfContentByte.ALIGN_RIGHT);
			createContent(cb, 52, y, item.getGenericCodItem() != null && item.getGenericCodItem().size() > 0
					? item.getGenericCodItem().get(0).getCod() : "", PdfContentByte.ALIGN_LEFT);
			createContent(cb, 152, y, item.getNomItem(), PdfContentByte.ALIGN_LEFT);

			// double price = Double.valueOf(df.format(Math.random() * 10));
			// double extPrice = price * (index + 1);
			createContent(cb, 498, y, safeNull(item.getPrecioUnitario()), PdfContentByte.ALIGN_RIGHT);
			createContent(cb, 568, y, safeNull(item.getMontoItem()), PdfContentByte.ALIGN_RIGHT);

	
	}
	
	public void generateTotales(Document doc, PdfContentByte cb) {
		TotalesInterface totales = invoiceStrategy.getTotales();
		
		
		int frameUp_y = detailsLowerLeft_y;
		int frameDown_y = detailsLowerLeft_y - totalesHeight;
		
		int cantCol = 5;
		int numCol = 0;
		
		
		//Otra Tasa
				numCol = 0;
				generateFrame(cb, frameUp_y, frameDown_y, "IVA OTRA TASA", cantCol, numCol);
				
				//createContent(cb, (pageWidth/cantCol) * numCol + offset, detailsLowerLeft_y - detailsRowSize * 3, "Tasa : ", PdfContentByte.ALIGN_RIGHT);
				//createContent(cb, (pageWidth/cantCol) * numCol + offset, detailsLowerLeft_y - detailsRowSize * 3, totales.getIVATasaMin().toPlainString(), PdfContentByte.ALIGN_LEFT);
				
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, safeNull(totales.getMntNetoIVAOtra()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, safeNull(totales.getMntIVAOtra()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		
				
		//Tasa Minima
		numCol = 1;
		generateFrame(cb, frameUp_y, frameDown_y, "IVA TASA MINIMA", cantCol, numCol);
		
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, "Tasa : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, safeNull(totales.getIVATasaMin()) + "%", PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, safeNull(totales.getMntNetoIvaTasaMin()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
		createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, safeNull(totales.getMntIVATasaMin()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		
		
		
		//Tasa Basica
		numCol = 2;
				generateFrame(cb, frameUp_y, frameDown_y, "IVA TASA BASICA", cantCol, numCol);
				
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, "Tasa : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, safeNull(totales.getIVATasaBasica()) + "%", PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, safeNull(totales.getMntNetoIVATasaBasica()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, safeNull(totales.getMntIVATasaBasica()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		
				//Tasa Basica
				numCol = 3;
						generateFrame(cb, frameUp_y, frameDown_y, "OTROS VALORES", cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, "IVA Suspenso : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, safeNull(totales.getMntIVaenSusp()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
						
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, "Impuesto Percibido : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, safeNull(totales.getMntImpuestoPerc()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
						
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, "Exportacion : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 5, safeNull(totales.getMntExpoyAsim()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 6, "No Facturable : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 6, safeNull(totales.getMontoNF()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				//TOTALES
				numCol = 4;
						generateFrame(cb, frameUp_y, frameDown_y, "TOTALES", cantCol, numCol);
						
						
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, "No gravado:", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 3, safeNull(totales.getMntNoGrv()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
						
						
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, "Total : ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
						createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 4, safeNull(totales.getMntTotal()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
		
	}
		

	

	private String safeNull(BigDecimal mntImpuestoPerc) {
		if (mntImpuestoPerc==null)
			return "0.00";
		else{
			mntImpuestoPerc= mntImpuestoPerc.setScale(2, RoundingMode.CEILING);
			return mntImpuestoPerc.toPlainString();
		}
	}

	/*
	 * 5) Pie del comprobante.(todas las paginas)
	 */
	private void generateSelloDigital(Document doc, PdfContentByte cb, Emisor emisor, IdDocInterface idDoc)
			throws DocumentException, MalformedURLException, IOException {
		TipoDoc tipoDoc = TipoDoc.fromInt(Integer.parseInt(idDoc.getTipoCFE().toString()));

		String hash = "aaaaaa";
		String resolucion = "Res. 1234/2015";

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyy");

		String filename = "resources/images/qr/" + idDoc.getSerie() + idDoc.getNro().toString() + ".jpg";

		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;
		int frameUp_x = 25;
		generateFrame(cb, frameUp_y, frameDown_y, "Sello Digital", 2, 0);

		generateQR(emisor.getRUCEmisor(), tipoDoc.friendlyName, idDoc.getSerie(), idDoc.getNro().toString(),
				idDoc.getMntBruto() != null ? idDoc.getMntBruto().toString() : "",
				dateFormat.format(idDoc.getFchEmis().toGregorianCalendar().getTime()), hash, filename);

		Image qrCode = Image.getInstance(filename);
		qrCode.setAbsolutePosition(25, frameDown_y + 20);
		qrCode.scalePercent(25);
		doc.add(qrCode);

		createContent(cb, frameUp_x, frameDown_y + 5, "Codigo de Seguridad: " + hash,PdfContentByte.ALIGN_LEFT);

		createContent(cb, frameUp_x + 100, frameUp_y - detailsRowSize * 4, resolucion, PdfContentByte.ALIGN_LEFT);

		createContent(cb, frameUp_x + 100, frameUp_y - detailsRowSize * 5, "Puede verificar conprobante en:",
				PdfContentByte.ALIGN_LEFT);
		switch (tipoDoc) {
		case eFactura:
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eFactura_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eRemito:
		case eRemito_Contingencia:
		case eResguardo:
		case eResguardo_Contingencia:
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
			createContent(cb, frameUp_x + 100, frameUp_y - detailsRowSize * 6, "www.dgi.com.uy",
					PdfContentByte.ALIGN_LEFT);

			break;

		case eTicket:
		case eTicket_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			createContent(cb, frameUp_x + 100, frameUp_y - detailsRowSize * 6, "www.lerandgroup.com.uy",
					PdfContentByte.ALIGN_LEFT);

			break;

		}

	}

	private void generateQR(String rut, String tipoCFE, String serie, String nroCFE, String monto, String fecha,
			String hash, String filename) {
		// METODO DE PABLO

		/*
		 * cfeQRDocumento: FORMATO QR DE EFACTURA
		 * 
		 * El hash (base64) puede tener caracteres no válidos en urls
		 * 
		 * ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=
		 * 
		 * Arreglo sustituyendo por sus correspondientes en Percent-Encoding
		 * 
		 */

		String safeHash = hash.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D");

		// &fecha = formatDate.Udp(&fechahora, !"DD/MM/YYYY")

		String data = "https://www.efactura.dgi.gub.uy/consultaQR/cfe?rut,tipoCFE,serie,nroCFE,monto,fecha,hash";
		data = data.replace("rut", rut);
		data = data.replace("tipoCFE", tipoCFE.trim());
		data = data.replace("serie", serie.trim());
		data = data.replace("nroCFE", nroCFE.trim());
		data = data.replace("monto", monto.trim());
		data = data.replace("fecha", fecha);
		data = data.replace("hash", safeHash.trim());

		int size = 300; // 110px/100 -> 1,10" * 2,54 -> 2,79cm * 10 = 28mm
		int quietZone = 0; // Está medido en módulos, uso 0 y del margen me
							// encargo afuera (5mm = 20px)

		generateQR(data, filename, size, quietZone);

	}

	private void generateQR(String url, String filename, int sizeInt, int quietZoneInt) {
		// GENERACIÓN DE QR
		String toEncode = url;
		java.io.File outputFile = new java.io.File(filename);
		int size = sizeInt; // En pixeles
		int quietZone = quietZoneInt; // En módulos (tamaño de cada cuadrado)

		com.google.zxing.qrcode.QRCodeWriter qr = new com.google.zxing.qrcode.QRCodeWriter();

		java.util.HashMap<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<com.google.zxing.EncodeHintType, Object>();
		hints.put(com.google.zxing.EncodeHintType.MARGIN, new Integer(quietZone));

		try {
			/*
			 * Método estándard, según la cantidad de información a codificar
			 * puede dejar excesivo padding
			 */
			// com.google.zxing.common.BitMatrix matrix = qr.encode(toEncode,
			// com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);

			// Método alternativo, para evitar el padding
			com.google.zxing.qrcode.decoder.ErrorCorrectionLevel errorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;
			com.google.zxing.qrcode.encoder.QRCode code = com.google.zxing.qrcode.encoder.Encoder.encode(toEncode,
					errorCorrectionLevel, hints);
			com.google.zxing.qrcode.encoder.ByteMatrix input = code.getMatrix();

			// Constuye la matriz binaria con todos los píxeles
			int multiple = size / input.getHeight();
			int outputSize = multiple * input.getHeight();
			int topPadding = 0;
			int leftPadding = 0;
			com.google.zxing.common.BitMatrix matrix = new com.google.zxing.common.BitMatrix(outputSize, outputSize);
			int inputY = 0;
			for (int outputY = topPadding; inputY < input.getHeight(); outputY += multiple) {
				int inputX = 0;
				for (int outputX = leftPadding; inputX < input.getWidth(); outputX += multiple) {
					if (input.get(inputX, inputY) == 1)
						matrix.setRegion(outputX, outputY, multiple, multiple);
					inputX++;
				}
				inputY++;
			}

			// Renders a BitMatrix as an image, where "false" bits are rendered
			// as white, and "true" bits are rendered as black.
			final int BLACK = 0xFF000000;
			final int WHITE = 0xFFFFFFFF;

			int width = matrix.getWidth();
			int height = matrix.getHeight();
			java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height,
					java.awt.image.BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
				}
			}
			// - Fin Método alternativo

			// Write to file
			boolean ok = javax.imageio.ImageIO.write(image, "png", outputFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 5) Pie del comprobante.(solo ultima pagina)
	 */
	public void generateCaeData(Document doc, PdfContentByte cb, CAEDataType cae, Emisor emisor)
			throws MalformedURLException, IOException, DocumentException {

		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;
		int framUp_x = pageWidth / 2 + 50;

		/*
		 * CAE DATA
		 */
		generateFrame(cb, frameUp_y, frameDown_y, "DATOS CAE", 2, 1);

		createContent(cb, framUp_x, frameUp_y - detailsRowSize * 3, "ID: " + cae.getCAEID(), PdfContentByte.ALIGN_LEFT);
		createContent(cb, framUp_x, frameUp_y - detailsRowSize * 4, "Fecha Vencimiento: " + cae.getFecVenc(),
				PdfContentByte.ALIGN_LEFT);
		createContent(cb, framUp_x, frameUp_y - detailsRowSize * 5, "INICIO: " + cae.getDNro(),
				PdfContentByte.ALIGN_LEFT);
		createContent(cb, framUp_x, frameUp_y - detailsRowSize * 6, "FIN: " + cae.getHNro(), PdfContentByte.ALIGN_LEFT);

	}

	/*
	 * 5) Pie del comprobante.(solo ultima pagina)
	 */
	public void generateAdenda(Document doc, PdfContentByte cb, JSONObject adenda) {

		int frameUp_y = pageHeight - headerHeight - detailsHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight;

		/*
		 * ADENDA
		 */
		generateFrame(cb, frameUp_y, frameDown_y, "Adenda");

	}

	/*
	 * 
	 * 
	 * AUX
	 * 
	 * 
	 * 
	 */
	private void generateFrame(PdfContentByte cb, int frameUp_y, int frameDown_y, String title) {
		generateFrame(cb, frameUp_y, frameDown_y, title, 1, 0);
	}

	private void generateFrame(PdfContentByte cb, int frameUp_y, int frameDown_y, String title, int columns,
			int column) {

		int columnWidth = pageWidth / columns;

		int offset = columnWidth * column;

		int marginWith= 20;
		int leftMargin =  marginWith/2;
		int rightMargin = marginWith/2;
				
		if (column==0){
			leftMargin = marginWith;
		} else if (column==columns-1){
			rightMargin= marginWith;
		}
		createHeadings(cb, offset + (columnWidth / 2), frameUp_y - 20, title, PdfContentByte.ALIGN_CENTER);
		
		cb.moveTo(offset + leftMargin, frameUp_y - 25);
		cb.lineTo(offset + columnWidth - rightMargin, frameUp_y - 25);
		cb.moveTo(offset + leftMargin, frameDown_y);
		cb.lineTo(offset + columnWidth - rightMargin, frameDown_y);
		cb.stroke();
	}
	
	
	

	public void createHeadings(PdfContentByte cb, float x, float y, String text) {

		cb.beginText();
		cb.setFontAndSize(bfBold, 8);
		cb.setTextMatrix(x, y);
		if (text != null)
			cb.showText(text.trim());
		cb.endText();

	}

	public void createHeadings(PdfContentByte cb, float x, float y, String text, int alingment) {

		cb.beginText();
		cb.setFontAndSize(bfBold, 8);
		cb.showTextAligned(alingment, text, x, y, 0);
		cb.endText();

	}

	public void printPageNumber(PdfContentByte cb) {

		cb.beginText();
		cb.setFontAndSize(bfBold, 8);
		cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, "Page No. " + (pageNumber + 1), 570, 25, 0);
		cb.endText();

		pageNumber++;

	}

	public void createContent(PdfContentByte cb, float x, float y, String text, int align) {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.showTextAligned(align, text.trim(), x, y, 0);
		cb.endText();

	}
	
	public void createContentOnFrame(PdfContentByte cb, float y, String text, int align, int columns, int column) {

		int columnWidth = pageWidth / columns;

		int offset = columnWidth * column;

		int marginWith= 20;
		int leftMargin =  marginWith/2;
		int rightMargin = marginWith/2;
				
		if (column==0){
			leftMargin = marginWith;
		} else if (column==columns-1){
			rightMargin= marginWith;
		}
		
		if (align == PdfContentByte.ALIGN_LEFT)
			createContent(cb, offset + leftMargin, y, text, align);

		if (align == PdfContentByte.ALIGN_RIGHT)
			createContent(cb, offset + columnWidth - rightMargin, y, text, align);
		
	}

	public void initializeFonts() {

		try {
			bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

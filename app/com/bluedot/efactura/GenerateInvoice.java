package com.bluedot.efactura;

import com.bluedot.commons.utils.QR;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.RetencionPercepcion;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.serializers.AdendaSerializer;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/*
 * http://www.mysamplecode.com/2012/10/generate-pdf-using-java-and-itext.html
 */
public class GenerateInvoice {

	 /** Template con el total de paginas */
    PdfTemplate total;
	
	// DETALLE
	private int itemsPerPage = 15;
	private int detailsRowSize = 15;
	private int detailsLowerLeft_y;
	private int detailsHeight;
	private int totalesHeight = 90;

	// CABEZAL
	private int headerHeight = 200;
	private int headerRowSize = 12;

	// PAGE
	private int pageHeight = 842;
	private int pageWidth = 595;
	private int leftMargin = 20;
	private int rightMargin = 20;
	private BaseFont bfBold;
	private BaseFont bf;
//	private int pageNumber = 0;

	// SELLO DIGITAL & CAE DATA
	int selloDigitalAndCAEDataHeight = 130;

	// ADENDA
	int adendaHeight = 160;

	int height;
	int width;
	
	// Descripcion de Productos (debe medir todo 555)
	int cant_ancho = 30;
	int codigo_ancho = 105;
	int desc_ancho = 280;
	int punit_ancho = 70;
	int monto_ancho = 70;
	
	DecimalFormat df_2 = new DecimalFormat("0.00");
	DecimalFormat df_0 = new DecimalFormat("##.##");
	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
	
	private CFE cfe;

	private void initializeCoordenates() {
		detailsLowerLeft_y = pageHeight - headerHeight - detailsRowSize * (itemsPerPage + 1);
		detailsHeight = pageHeight - detailsLowerLeft_y - headerHeight + totalesHeight;
		height = detailsRowSize * (itemsPerPage + 1);
		width = pageWidth - leftMargin - rightMargin;
	}
	
	public GenerateInvoice(CFE cfe){
		this.cfe = cfe;
	}
	

	public ByteArrayOutputStream createPDF() {

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
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			docWriter = PdfWriter.getInstance(doc, outputStream);
			doc.addAuthor("nicoribeiro");
			doc.addCreationDate();
			doc.addProducer();
			doc.addCreator("https://github.com/nicoribeiro/java_efactura_uy");
			doc.addTitle("Invoice");
			doc.setPageSize(PageSize.A4);

			doc.open();
			PdfContentByte cb = docWriter.getDirectContent();

			total = docWriter.getDirectContent().createTemplate(30, 16);
			
			boolean beginPage = true;
			int row = 0;
			
			switch (cfe.getTipo()) {
			case eResguardo:
			case eResguardo_Contingencia:
				for (int i = 0; i < cfe.getRetencionesPercepciones().size(); i++) {
					if (beginPage) {
						beginPage = false;
						generateTablaCuerpo(cb);
						generateIdentificacionEmisorElectronico(doc, cb);
						generateIdentificacionComprobante(cb);
						generateSelloDigital(doc, cb);
						row = 1;
					}
					int cant = generateLineaDeCuerpo(cb, row, cfe.getRetencionesPercepciones().get(i));
					row += cant;
					if (row == itemsPerPage + 1) {
						printPageNumber(doc,docWriter, bf, cb);
						doc.newPage();
						beginPage = true;
					}
	
				}
				break;
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
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case eRemito_de_Exportacion:
			case eRemito_de_Exportacion_Contingencia:
				for (int i = 0; i < cfe.getDetalle().size(); i++) {
					if (beginPage) {
						beginPage = false;
						generateTablaCuerpo(cb);
						generateIdentificacionEmisorElectronico(doc, cb);
						generateIdentificacionComprobante(cb);
						generateSelloDigital(doc, cb);
						row = 1;
					}
					int cant = generateLineaDeCuerpo(cb, row, cfe.getDetalle().get(i));
					row += cant;
					if (row == itemsPerPage + 1) {
						printPageNumber(doc,docWriter, bf, cb);
						doc.newPage();
						beginPage = true;
					}
	
				}
				break;
			}

			printPageNumber(doc,docWriter, bf, cb);
			generateTotales(cb);
			generateCaeData(cb);
			generateAdendaDatosInternos(cb);
			generateReferencia(cb);
			generateAdendaEntrega(cb);
			generateAdendaReceptor(cb);
			generateAdendaNotas(cb);
			
			total.setFontAndSize(bf, 8);
			total.beginText();
			total.showText(String.valueOf(docWriter.getPageNumber()));
			total.endText();
//			ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
//	                    new Phrase(String.valueOf(docWriter.getPageNumber())),
//	                    2, 2, 0);
			 
			 
			return outputStream;
			
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
		
		return null;
		
	}

	/*
	 * 1) Identificación del emisor electrónico;
	 */
	public void generateIdentificacionEmisorElectronico(Document doc, PdfContentByte cb)
			throws MalformedURLException, IOException, DocumentException {

		int emisor_y = 700;
		int emisor_x = leftMargin;

		/*
		 * Logo de la empresa
		 */
		if (cfe.getEmpresaEmisora().getLogo()!=null){
			Image companyLogo = Image.getInstance(cfe.getEmpresaEmisora().getLogo());
			companyLogo.setAbsolutePosition(leftMargin, 720);
			companyLogo.scaleToFit(250, 150);
			doc.add(companyLogo);
		}

		/*
		 * Datos del Emisor
		 */
		createHeadings(bf, cb, emisor_x, emisor_y, cfe.getEmpresaEmisora().getRazon());
		createHeadings(bf, cb, emisor_x, emisor_y - headerRowSize, cfe.getSucursal().getDomicilioFiscal() + " CP " + cfe.getSucursal().getCodigoPostal());
		createHeadings(bf, cb, emisor_x, emisor_y - headerRowSize * 2, cfe.getSucursal().getCiudad() + ", " + cfe.getSucursal().getDepartamento() + " - Uruguay") ;
		createHeadings(bf, cb, emisor_x, emisor_y - headerRowSize * 3, cfe.getSucursal().getTelefono());
		createHeadings(bf, cb, emisor_x, emisor_y - headerRowSize * 4, cfe.getEmpresaEmisora().getPaginaWeb());

	}

	/*
	 * 2) Identificación del comprobante;
	 * 
	 * 3) Identificación del receptor;
	 */
	public void generateIdentificacionComprobante(PdfContentByte cb) {

		int receptor_y = 780;
		int receptor_x = 280;

		int idDoc_y = 780;
		int idDoc_x = receptor_x + 180;

		/*
		 * Datos del CFE
		 */
		
		createHeadings(bfBold, cb, idDoc_x, idDoc_y, "RUT " + cfe.getEmpresaEmisora().getRut());
		createHeadings(bfBold, cb, idDoc_x, idDoc_y - headerRowSize, cfe.getTipo().friendlyName);
		createHeadings(bfBold, cb, idDoc_x, idDoc_y - headerRowSize * 2, cfe.getSerie() + " " + cfe.getNro());
		switch (cfe.getTipo()) {
			case eResguardo:
			case eResguardo_Contingencia:
				break;
			default:
				createHeadings(bfBold, cb, idDoc_x, idDoc_y - headerRowSize * 3, cfe.getFormaDePago().toString());
				break;
		}
		
		createHeadings(bfBold, cb, idDoc_x, idDoc_y - headerRowSize * 4, sdf.format(cfe.getFechaEmision()));

		/*
		 * Identificacion del RECEPTOR
		 */
		cb.rectangle(receptor_x, idDoc_y - (headerRowSize - 3) * 2, 100, 40);
		cb.stroke();

		switch (cfe.getTipo()) {
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
			/*
			 * Aca no se puede usar todos los datos de cfe.getEmpresaReceptora() porque pueden haber cambiado, puedo usar los datos del generador_json 
			 * o desde el XML que se envia a DGI. Como el XML esta dentro del sobre prefiero usar los datos del generador_json 
			 * 
			 * Los datos que pueden cambiar son direccion, localidad, departamento 
			 * 
			 */
			JSONObject generador = new JSONObject(cfe.getGeneradorJson());
			
			createHeadings(bf, cb, receptor_x + 50, receptor_y, "RUC COMPRADOR", PdfContentByte.ALIGN_CENTER);
			createHeadings(bf, cb, receptor_x + 50, receptor_y - headerRowSize, cfe.getEmpresaReceptora().getRut(),
					PdfContentByte.ALIGN_CENTER);
			createContent(bf, cb, receptor_x, receptor_y - headerRowSize * 3, generador.getJSONObject("Encabezado").getJSONObject("Receptor").getString("DirRecep"),
					PdfContentByte.ALIGN_LEFT);
			
			String ciudad;
			String depto;
			ciudad = generador.getJSONObject("Encabezado").getJSONObject("Receptor").has("CiudadRecep") ? generador.getJSONObject("Encabezado").getJSONObject("Receptor").getString("CiudadRecep"):"";
			depto = generador.getJSONObject("Encabezado").getJSONObject("Receptor").has("DeptoRecep") ? generador.getJSONObject("Encabezado").getJSONObject("Receptor").getString("DeptoRecep"):"";
			
			createContent(bf, cb, receptor_x, receptor_y - headerRowSize * 4, ciudad + " - " + depto, PdfContentByte.ALIGN_LEFT);
			createContent(bf, cb, receptor_x, receptor_y - headerRowSize * 5, cfe.getEmpresaReceptora().getRazon(), PdfContentByte.ALIGN_LEFT);
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
			createHeadings(bfBold, cb, receptor_x + 50, receptor_y, "CONSUMO FINAL", PdfContentByte.ALIGN_CENTER);
			
			if (cfe.getTitular()!=null){
			
			createContent(bf, cb, receptor_x, receptor_y - headerRowSize * 3, cfe.getTitular().getTipoDocumento().name(),
					PdfContentByte.ALIGN_LEFT);
			createContent(bf, cb, receptor_x, receptor_y - headerRowSize * 4, cfe.getTitular().getPaisEmisorDocumento().getCodigo() + " " + cfe.getTitular().getDocumento(),
					PdfContentByte.ALIGN_LEFT);
			}
			break;

		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
			createHeadings(bfBold, cb, receptor_x + 50, receptor_y, "EXPORTACION", PdfContentByte.ALIGN_CENTER);
			break;
		}

		

	}

	/*
	 * 4) Cuerpo del comprobante;
	 */
	public void generateTablaCuerpo(PdfContentByte cb) {

		
				
		
		cb.setLineWidth(1f);

		/*
		 * Detalle de productos
		 */
		cb.rectangle(leftMargin, detailsLowerLeft_y, width, height);

		// horizontal debajo de los column names
		cb.moveTo(leftMargin, detailsLowerLeft_y + height - detailsRowSize);
		cb.lineTo(width + leftMargin, detailsLowerLeft_y + height - detailsRowSize);

		// vertical
		cb.moveTo(leftMargin+ cant_ancho, detailsLowerLeft_y);
		cb.lineTo(leftMargin+ cant_ancho, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(leftMargin+ cant_ancho + codigo_ancho, detailsLowerLeft_y);
		cb.lineTo(leftMargin+ cant_ancho + codigo_ancho, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(leftMargin+ cant_ancho + codigo_ancho + desc_ancho, detailsLowerLeft_y);
		cb.lineTo(leftMargin+ cant_ancho + codigo_ancho + desc_ancho, height + detailsLowerLeft_y);

		// vertical
		cb.moveTo(leftMargin+ cant_ancho + codigo_ancho + desc_ancho+ punit_ancho, detailsLowerLeft_y);
		cb.lineTo(leftMargin+ cant_ancho + codigo_ancho + desc_ancho +punit_ancho, height + detailsLowerLeft_y);

		cb.stroke();

		// Titulos de detalle de productos
		int title_y = height + detailsLowerLeft_y - detailsRowSize + 5;
		switch (cfe.getTipo()) {
			case eResguardo:
			case eResguardo_Contingencia:
				createHeadings(bfBold, cb, leftMargin + 2, title_y, "%");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + 2, title_y, "Código");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + 2, title_y, "Descripción");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + 2, title_y, "Monto Sujeto");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho +  2, title_y, "Valor Ret");
				break;
			default:
				createHeadings(bfBold, cb, leftMargin + 2, title_y, "Cant");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + 2, title_y, "Código");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + 2, title_y, "Descripción");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + 2, title_y, "Precio Unitario");
				createHeadings(bfBold, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho +  2, title_y, "Monto");
				break;
			
		}
		
		

	}

	/*
	 * 4) Cuerpo del comprobante;
	 */
	public int generateLineaDeCuerpo(PdfContentByte cb, int row, Detalle detalle) {
		 	
		int y = pageHeight - headerHeight - detailsRowSize - row * detailsRowSize + 5;

		double cantidad = detalle.getCantidad();
		
		createContent(bf, cb, leftMargin+ cant_ancho -2, y, df_0.format(cantidad), PdfContentByte.ALIGN_RIGHT);
		createContent(bf, cb, leftMargin+ cant_ancho + 2, y, detalle.getCodItem()!=null?detalle.getCodItem():"", PdfContentByte.ALIGN_LEFT);
		
		
		int cant = printLongDesc(cb, leftMargin+ cant_ancho + codigo_ancho + 2, y,detalle.getNombreItem(), 64);
		

		createContent(bf, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho -2, y, df_2.format(detalle.getPrecioUnitario()), PdfContentByte.ALIGN_RIGHT);
		createContent(bf, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho + monto_ancho-2, y, df_2.format(detalle.getMontoItem()), PdfContentByte.ALIGN_RIGHT);
		
		return cant;
			
	}
	
	private int generateLineaDeCuerpo(PdfContentByte cb, int row, RetencionPercepcion retencionPercepcion) {
		int y = pageHeight - headerHeight - detailsRowSize - row * detailsRowSize + 5;

		double cantidad = retencionPercepcion.getTasa();
		
		createContent(bf, cb, leftMargin+ cant_ancho -2, y, df_0.format(cantidad), PdfContentByte.ALIGN_RIGHT);
		createContent(bf, cb, leftMargin+ cant_ancho + 2, y, retencionPercepcion.getCodigo(), PdfContentByte.ALIGN_LEFT);
		
		int cant = printLongDesc(cb, leftMargin+ cant_ancho + codigo_ancho + 2, y, retencionPercepcion.getDescripcion()!=null?retencionPercepcion.getDescripcion():"", 64);

		createContent(bf, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho -2, y, df_2.format(retencionPercepcion.getMontoSujeto()), PdfContentByte.ALIGN_RIGHT);
		createContent(bf, cb, leftMargin + cant_ancho + codigo_ancho + desc_ancho + punit_ancho + monto_ancho-2, y, df_2.format(retencionPercepcion.getValor()), PdfContentByte.ALIGN_RIGHT);
		
		return cant;
	}
	
	
	public int printLongDesc(PdfContentByte cb, int y, String input, int maxLineLength, int columns, int column) {
	
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
		
		return printLongDesc(cb, offset + leftMargin, y,input, maxLineLength);
		
	}
	public int printLongDesc(PdfContentByte cb, int x, int y, String input, int maxLineLength) {
	    StringTokenizer tok = new StringTokenizer(input, " ");
	    StringBuilder output = new StringBuilder(input.length());
	    int cantLines = 0;
	    int lineLen = 0;
	    while (tok.hasMoreTokens()) {
	        String word = tok.nextToken();

	        if (lineLen + word.length() > maxLineLength) {
	        	createContent(bf, cb, x, y-(detailsRowSize*cantLines), output.toString(), PdfContentByte.ALIGN_LEFT);
	        	output.setLength(0);
	            lineLen = 0;
	            cantLines += 1;
	        }
	        output.append(" " + word);
	        lineLen += word.length()+1;
	    }
	    if  (lineLen>0)
	    	createContent(bf, cb, x, y-(detailsRowSize*cantLines), output.toString(), PdfContentByte.ALIGN_LEFT);
	    
	    return cantLines+1;
	}
	
	public void generateTotales( PdfContentByte cb) {
//		TotalesInterface totales = invoiceStrategy.getTotales();
		
		
		int frameUp_y = detailsLowerLeft_y;
		int frameDown_y = detailsLowerLeft_y - totalesHeight;
		
		int cantCol = 5;
		int numCol = 0;
		
		
		switch (cfe.getTipo()) {
			case eResguardo:
			case eResguardo_Contingencia:
				break;
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
			case Nota_de_Credito_de_eFactura_Exportacion:
			case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			case Nota_de_Debito_de_eFactura_Exportacion:
			case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
			case eFactura_Exportacion:
			case eFactura_Exportacion_Contingencia:
			case eRemito_de_Exportacion:
			case eRemito_de_Exportacion_Contingencia:
				//Otra Tasa
				numCol = 0;
				generateFrame(bfBold, cb, frameUp_y, frameDown_y, "IVA OTRA TASA", cantCol, numCol);
				
				//createContent(bf, cb, (pageWidth/cantCol) * numCol + offset, detailsLowerLeft_y - detailsRowSize * 3, "Tasa: ", PdfContentByte.ALIGN_RIGHT);
				//createContent(bf, cb, (pageWidth/cantCol) * numCol + offset, detailsLowerLeft_y - detailsRowSize * 3, totales.getIVATasaMin().toPlainString(), PdfContentByte.ALIGN_LEFT);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntIVAOtra()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, df_2.format(cfe.getMntIVAOtra()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
						
				//Tasa Minima
				numCol = 1;
				generateFrame(bfBold, cb, frameUp_y, frameDown_y, "IVA TASA MINIMA", cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, "Tasa: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, cfe.getIvaTasaMin() + "%", PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntIVATasaMin()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, df_2.format(cfe.getMntIVATasaMin()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				
				//Tasa Basica
				numCol = 2;
				generateFrame(bfBold, cb, frameUp_y, frameDown_y, "IVA TASA BASICA", cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, "Tasa: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, cfe.getIvaTasaBas() + "%", PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, "Neto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntIVATasaBas()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, "Monto: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, df_2.format(cfe.getMntIVATasaBas()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);

				//Tasa Basica
				numCol = 3;
				generateFrame(bfBold, cb, frameUp_y, frameDown_y, "OTROS VALORES", cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, "IVA Suspenso: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, df_2.format(cfe.getTotMntIVAenSusp()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, "Imp. Per.: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntImpPerc()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
				
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, "Exportación: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
				createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 5, df_2.format(cfe.getTotMntExpyAsim()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);

//								createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 6, "No Facturable: ", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
//								createContentOnFrame(cb, detailsLowerLeft_y - detailsRowSize * 6, cfe.getMontoNF(), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);

				
				break;
		}
		
		//TOTALES
		numCol = 4;
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "TOTALES", cantCol, numCol);
		
		if (cfe.getTotMntNoGrv()!=null) {
			createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, "No gravado:", PdfContentByte.ALIGN_LEFT, cantCol, numCol);
			createContentOnFrame(bf, cb, detailsLowerLeft_y - detailsRowSize * 3, df_2.format(cfe.getTotMntNoGrv()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		}
		
		String moneda = null;
		switch (cfe.getMoneda()) {
			case UYU:
				moneda = "$";
				break;
			case USD:
				moneda = "U$S";
				break;
			default:
				moneda = cfe.getMoneda().name();
				break;
		}
		
		if (cfe.getTotMntTotal()!=null && cfe.getTotMntTotal()>0) {
			createContentOnFrame(bfBold, cb, detailsLowerLeft_y - detailsRowSize * 4, "Total: " + moneda, PdfContentByte.ALIGN_LEFT, cantCol, numCol);
			createContentOnFrame(bfBold, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntTotal()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		}
		
		if (cfe.getTotMntRetenido()!=null && cfe.getTotMntRetenido()>0) {
			createContentOnFrame(bfBold, cb, detailsLowerLeft_y - detailsRowSize * 4, "Total Ret: " + moneda, PdfContentByte.ALIGN_LEFT, cantCol, numCol);
			createContentOnFrame(bfBold, cb, detailsLowerLeft_y - detailsRowSize * 4, df_2.format(cfe.getTotMntRetenido()), PdfContentByte.ALIGN_RIGHT, cantCol, numCol);
		}
	
	}
		

	/*
	 * 5) Pie del comprobante.(todas las paginas)
	 */
	private void generateSelloDigital(Document doc, PdfContentByte cb)
			throws DocumentException, MalformedURLException, IOException {

		// sino tengo el hash no puedo generar le qr, esto pasa en cfe recibidos
		if (cfe.getHash()==null)
			return;
		
		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;
//		int frameUp_x = 25;
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "SELLO DIGITAL", 3, 0);

		if (cfe.getQr()==null){
			BufferedImage qr;
			
			switch (cfe.getTipo()) {
			case eResguardo:
			case eResguardo_Contingencia:
				 qr = generateQR(cfe.getEmpresaEmisora().getRut(), cfe.getTipo(), cfe.getSerie(), cfe.getNro(),
						cfe.getTotMntRetenido(), cfe.getFechaEmision(), cfe.getHash());
				break;
			default:
				 qr = generateQR(cfe.getEmpresaEmisora().getRut(), cfe.getTipo(), cfe.getSerie(), cfe.getNro(),
						cfe.getTotMntTotal(), cfe.getFechaEmision(), cfe.getHash());
				break;
			}
			
			cfe.qrAsImage(qr);
			
			/*
			 * Si quisieramos guardar a archivo el qr seria asi:
			 *
			 * String filename = "resources/images/qr/" + cfe.getSerie() + cfe.getNro() + ".jpg";
			 * 
			 * File outputFile = new File(filename);
			 * 
			 * boolean ok = ImageIO.write(qr, "png", outputFile);
			 */
		}
		
		Image qrCode = Image.getInstance(cfe.getQr());
		qrCode.setAbsolutePosition(25, frameDown_y + 20);
		qrCode.scalePercent(25);
		doc.add(qrCode);

		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 8, "Cod. de Seguridad: " + cfe.getHash().substring(0, 6), PdfContentByte.ALIGN_LEFT, 3,0);

		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 3, "Res. " + cfe.getEmpresaEmisora().getResolucion(), PdfContentByte.ALIGN_RIGHT, 3,0);

		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 5, "Puede verificar         ",PdfContentByte.ALIGN_RIGHT, 3,0);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 6, "comprobante en:         ",PdfContentByte.ALIGN_RIGHT, 3,0);
		
		switch (cfe.getTipo()) {
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
				createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 7, "www.dgi.gub.uy",
						PdfContentByte.ALIGN_RIGHT, 3, 0);
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
				createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 7, cfe.getEmpresaEmisora().getPaginaWeb(),
					PdfContentByte.ALIGN_RIGHT,3,0);
				break;

		}

	}

	private BufferedImage generateQR(String rut, TipoDoc tipoCFE, String serie, long nroCFE, double monto, Date fecha,
			String hash) {

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
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		String safeHash = hash.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D");

		String data = "https://www.efactura.dgi.gub.uy/consultaQR/cfe?rut,tipoCFE,serie,nroCFE,monto,fecha,hash";
		data = data.replace("rut", rut);
		data = data.replace("tipoCFE", String.valueOf(tipoCFE.value));
		data = data.replace("serie", serie);
		data = data.replace("nroCFE", String.valueOf(nroCFE));
		data = data.replace("monto", String.valueOf(monto));
		data = data.replace("fecha", dateFormat.format(fecha));
		data = data.replace("hash", safeHash);

		int size = 300; // 110px/100 -> 1,10" * 2,54 -> 2,79cm * 10 = 28mm
		int quietZone = 0; // Está medido en módulos, uso 0 y del margen me
							// encargo afuera (5mm = 20px)

		return QR.generateQR(data, size, quietZone);

	}

	

	/*
	 * 5) Pie del comprobante.(solo ultima pagina)
	 */
	public void generateCaeData(PdfContentByte cb)
			throws MalformedURLException, IOException, DocumentException {

		// Sino tengo un CAE no puedo generar la info del CAE, esto paa con CFE recibidos
		if (cfe.getCae()==null)
			return;
		
		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;

		/*
		 * CAE DATA
		 */
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "DATOS CAE", 6, 5);

		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 3, "IVA al día ", PdfContentByte.ALIGN_LEFT, 6, 5);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 4, "ID: " + cfe.getCae().getNro(), PdfContentByte.ALIGN_LEFT, 6, 5);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 5, "Vence: " + sdf.format(cfe.getCae().getFechaVencimiento()),
				PdfContentByte.ALIGN_LEFT, 6, 5);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 6, "INICIO: " + cfe.getCae().getInicial(),
				PdfContentByte.ALIGN_LEFT, 6, 5);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 7, "FIN: " + cfe.getCae().getFin(), PdfContentByte.ALIGN_LEFT, 6, 5);

		
		
		
	}
	
	/*
	 * 5) Pie del comprobante.(solo ultima pagina)
	 */
	public void generateReferencia(PdfContentByte cb)
			throws MalformedURLException, IOException, DocumentException {
		
		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;

		
		/*
		 * REFERENCIA
		 */
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "REFERENCIA", 6, 4);
		
		if (cfe.getReferencia()==null){
			if ( cfe.getRazonReferencia()!=null)
				createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 3, "Razon: " + cfe.getRazonReferencia(), PdfContentByte.ALIGN_LEFT, 6, 4);
		}else{
			createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 3, "Tipo: " + cfe.getReferencia().getTipo(), PdfContentByte.ALIGN_LEFT, 6, 4);
			createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 4, "Serie: " + cfe.getReferencia().getSerie(), PdfContentByte.ALIGN_LEFT, 6, 4);
			createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 5, "Nro: " + cfe.getReferencia().getNro(), PdfContentByte.ALIGN_LEFT, 6, 4);
		}
	}
	
	public void generateAdendaEntrega(PdfContentByte cb)
			throws MalformedURLException, IOException, DocumentException {
		
		int frameUp_y = pageHeight - headerHeight - detailsHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight;

		if (cfe.getSobreEmitido()!=null) {
			//Es un CFE emitido
			/*
			 * ENTREGA
			 */
			generateFrame(bfBold, cb, frameUp_y, frameDown_y, "ADENDA - ENTREGA", 3, 1);
			
			if (cfe.getAdenda()==null)
				return;
			
			JSONArray array = new JSONArray(cfe.getAdenda());
			JSONArray entrega = null;
			
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.get("Entrega")!=null)
						entrega = jsonObject.getJSONArray("Entrega");
				} catch (JSONException e) {
				}
				
			}
			
			if (entrega!=null){
				StringBuilder stringBuilder = new StringBuilder();
				AdendaSerializer.convertAdenda(stringBuilder, entrega);
				
				String[] adenda = stringBuilder.toString().split("\\r?\\n");
				
				for (int i = 0; i < adenda.length; i++) {
					createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * (i+3), adenda[i], PdfContentByte.ALIGN_LEFT, 3, 1);
				}
				
			}
		}
	}
	

	/*
	 * 5) Pie del comprobante.(solo ultima pagina)
	 */
	public void generateAdendaDatosInternos(PdfContentByte cb) {

		int frameUp_y = pageHeight - headerHeight - detailsHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight;

		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "ADENDA - DATOS INTERNOS",3,0);
		
		if (cfe.getSobreEmitido()!=null) {
		
			// es un cfe emitido
			
			JSONArray array = new JSONArray(cfe.getAdenda());
			
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.has("Entrega") || jsonObject.has("Notas"))
						array.remove(i);
				} catch (JSONException e) {
				}
				
			}
			
			
			if (cfe.getAdenda() != null){
				StringBuilder stringBuilder = new StringBuilder();
				AdendaSerializer.convertAdenda(stringBuilder, array);
				
				String[] adenda = stringBuilder.toString().split("\\r?\\n");
				int row = 3;
				for (int i = 0; i < adenda.length; i++) {
					int cant = printLongDesc(cb, frameUp_y - detailsRowSize * (row) ,adenda[i], 40, 2, 0);
					row = row+cant;
					//createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * (i+3), adenda[i], PdfContentByte.ALIGN_LEFT, 2, 0);
				}
				
			}
		
		}
		
	}
	
	public void generateAdendaReceptor(PdfContentByte cb){
		int frameUp_y = pageHeight - headerHeight - detailsHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		
		/*
		 * Firma, Aclaracion y CI 
		 */
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "ADENDA - RECEPTOR",3,2);
		
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 4, "Firma:", PdfContentByte.ALIGN_LEFT, 3, 2);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 6, "Aclaración:", PdfContentByte.ALIGN_LEFT, 3, 2);
		createContentOnFrame(bf, cb, frameUp_y - detailsRowSize * 8, "Cédula:", PdfContentByte.ALIGN_LEFT, 3, 2);
	}
	
	public void generateAdendaNotas(PdfContentByte cb){
		int frameUp_y = pageHeight - headerHeight - detailsHeight - adendaHeight;
		int frameDown_y = pageHeight - headerHeight - detailsHeight - adendaHeight - selloDigitalAndCAEDataHeight;
		
		
		generateFrame(bfBold, cb, frameUp_y, frameDown_y, "ADENDA - NOTAS",3,1);
		
		if (cfe.getSobreEmitido()!=null) {
			//Es un CFE emitido
			
			if (cfe.getAdenda()==null)
				return;
			
			JSONArray array = new JSONArray(cfe.getAdenda());
			JSONObject notas = null; 
			for (int i = 0; i < array.length(); i++) {
				try {
					if (array.getJSONObject(i).get("Notas")!=null) {
						notas = array.getJSONObject(i);
						break;
					}
				} catch (JSONException e) {
				}
				
			}
			
			if (notas!=null)
				 printLongDesc(cb, frameUp_y - detailsRowSize * (3) ,notas.getString("Notas"), 50, 3, 1);
		
		}
		
	}

	/*
	 * 
	 * 
	 * AUX
	 * 
	 * 
	 * 
	 */
	private void generateFrame(BaseFont bf, PdfContentByte cb, int frameUp_y, int frameDown_y, String title) {
		generateFrame(bf, cb, frameUp_y, frameDown_y, title, 1, 0);
	}

	private void generateFrame(BaseFont bf, PdfContentByte cb, int frameUp_y, int frameDown_y, String title, int columns,
			int column) {

		int columnWidth = pageWidth / columns;

		int offset = columnWidth * column;

		int marginWith= 20;
		int leftMargin =  marginWith/2;
		int rightMargin = marginWith/2;
				
		if (column==0){
			leftMargin = marginWith;
		}
		
		if (column==columns-1){
			rightMargin= marginWith;
		}
		
		/*
		 * Titulo del frame
		 */
		createHeadings(bf, cb, offset + (columnWidth / 2), frameUp_y - 20, title, PdfContentByte.ALIGN_CENTER);
		
		/*
		 * Linea de arriba del frame
		 */
		cb.moveTo(offset + leftMargin, frameUp_y - 25);
		cb.lineTo(offset + columnWidth - rightMargin, frameUp_y - 25);
		
		/*
		 * Linea de abajo del frame 
		 */
		cb.moveTo(offset + leftMargin, frameDown_y);
		cb.lineTo(offset + columnWidth - rightMargin, frameDown_y);
		cb.stroke();
	}
	
	
	

	public void createHeadings(BaseFont bf, PdfContentByte cb, float x, float y, String text) {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.setTextMatrix(x, y);
		if (text != null)
			cb.showText(text.trim());
		cb.endText();

	}

	public void createHeadings(BaseFont bf,PdfContentByte cb, float x, float y, String text, int alingment) {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.showTextAligned(alingment, text, x, y, 0);
		cb.endText();

	}

	public void printPageNumber(Document doc,PdfWriter docWriter, BaseFont bf, PdfContentByte cb) throws DocumentException {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, "Pag " + docWriter.getPageNumber() + " de", 550, 25, 0);
		cb.endText();
		Image image = Image.getInstance(total);
		image.setAbsolutePosition(560, 25);
		doc.add(image);

	}

	public void createContent(BaseFont bf,PdfContentByte cb, float x, float y, String text, int align) {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.showTextAligned(align, text.trim(), x, y, 0);
		cb.endText();

	}
	
	public void createContent(BaseFont bf,PdfContentByte cb, float x, float y, double text, int align) {
		this.createContent(bf, cb, x, y, String.valueOf(text), align);
	}
	
	public void createContentOnFrame(BaseFont bf,PdfContentByte cb, float y, String text, int align, int columns, int column) {

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
			createContent(bf, cb, offset + leftMargin, y, text, align);

		if (align == PdfContentByte.ALIGN_RIGHT)
			createContent(bf, cb, offset + columnWidth - rightMargin, y, text, align);
		
	}
	
	public void createContentOnFrame(BaseFont bf,PdfContentByte cb, float y, double text, int align, int columns, int column) {
		this.createContentOnFrame(bf, cb, y, String.valueOf(text), align, columns, column);
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

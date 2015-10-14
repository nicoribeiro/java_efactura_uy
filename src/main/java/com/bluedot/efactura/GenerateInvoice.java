package com.bluedot.efactura;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
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
import dgi.classes.recepcion.IdDocFact;
/*
 * http://www.mysamplecode.com/2012/10/generate-pdf-using-java-and-itext.html
 */
public class GenerateInvoice {

	private BaseFont bfBold;
	private BaseFont bf;
	private int pageNumber = 0;
	
	private int detailsRows = 5;
	private int detailsRowSize = 15;
	int heatherHeight = 200;
	int total = 842;
	
	
	public static void main(String[] args) throws IOException, JSONException, EFacturaException {

		String pdfFilename = "";
		GenerateInvoice generateInvoice = new GenerateInvoice();
		if (args.length < 1) {
			System.err.println("Usage: java " + generateInvoice.getClass().getName() + " PDF_Filename");
			System.exit(1);
		}

		pdfFilename = args[0].trim();
		
		
		String file = IO.readFile("resources/json/efactura.json", Charset.defaultCharset());
		
		JSONObject json = new JSONObject(file);

		EFacturaFactory factory = new EFacturaFactoryImpl();
		
		EFact efactura = factory.getCFEController().createEfactura(json.getJSONObject("eFact"));

		generateInvoice.createPDF(pdfFilename, efactura);

	}

	private void createPDF(String pdfFilename, EFact efactura) {

		Document doc = new Document();
		PdfWriter docWriter = null;
		initializeFonts();

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

			for (int i = 0; i < 100; i++) {
				if (beginPage) {
					beginPage = false;
					generateLayout(doc, cb);
					generateHeader(doc, cb, efactura.getEncabezado().getEmisor(), efactura.getCAEData(), efactura.getEncabezado().getIdDoc());
					row = 1;
				}
				generateDetail(doc, cb, i, row);
				row+=1;
				if (row == detailsRows+1) {
					printPageNumber(cb);
					doc.newPage();
					beginPage = true;
				}
			}
			printPageNumber(cb);

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

	private void generateLayout(Document doc, PdfContentByte cb) {

		try {
			
			
			
			cb.setLineWidth(1f);

			// Invoice Header box layout
			cb.rectangle(420, 700, 150, 60);
			cb.moveTo(420, 720);
			cb.lineTo(570, 720);
			cb.moveTo(420, 740);
			cb.lineTo(570, 740);
			cb.moveTo(480, 700);
			cb.lineTo(480, 760);
			cb.stroke();

			// Invoice Header box Text Headings
			createHeadings(cb, 422, 743, "Account No.");
			createHeadings(cb, 422, 723, "Invoice No.");
			createHeadings(cb, 422, 703, "Invoice Date");

			// Invoice Detail box layout
			int x = 20;
			int y = total- heatherHeight - detailsRowSize*(detailsRows+1);	
			int height = detailsRowSize * (detailsRows+1);
			int width = 550;
			cb.rectangle(x, y, width, height);
			
			//horizontal debajo de los column names
			cb.moveTo(x, y + height - detailsRowSize);
			cb.lineTo(width + x, y + height - detailsRowSize);
			
			//vertical
			cb.moveTo(50, y);
			cb.lineTo(50, height + y);
			
			//vertical
			cb.moveTo(150, y);
			cb.lineTo(150, height + y);
			
			//vertical
			cb.moveTo(430, y);
			cb.lineTo(430, height + y);
			
			//vertical
			cb.moveTo(500, y);
			cb.lineTo(500, height + y);
			
			cb.stroke();

			// Invoice Detail box Text Headings
			createHeadings(cb, 22, 633, "Cant");
			createHeadings(cb, 52, 633, "Codigo");
			createHeadings(cb, 152, 633, "Item Description");
			createHeadings(cb, 432, 633, "Predio Unitario");
			createHeadings(cb, 502, 633, "Monto");

			// add the images
			Image companyLogo = Image.getInstance("resources/images/Olympic-logo.png");
			companyLogo.setAbsolutePosition(25, 700);
			companyLogo.scalePercent(25);
			doc.add(companyLogo);

		}

		catch (DocumentException dex) {
			dex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void generateHeader(Document doc, PdfContentByte cb, Emisor emisor, CAEDataType caeData, IdDocFact idDocFact) {

		try {
			int emisor_y = 750;
			int emisor_x = 200;
			
			int datos_y = 743;
			int receptor_x = 482;
					
			
			createHeadings(cb, emisor_x, emisor_y, emisor.getRznSoc());
			createHeadings(cb, emisor_x, emisor_y-detailsRowSize, emisor.getDomFiscal());
			createHeadings(cb, emisor_x, emisor_y-detailsRowSize*2, emisor.getCiudad());
			createHeadings(cb, emisor_x, emisor_y-detailsRowSize*3, emisor.getTelefono()!=null&&emisor.getTelefono().size()>0?emisor.getTelefono().get(0):null);
			createHeadings(cb, emisor_x, emisor_y-detailsRowSize*4, "RUT " + emisor.getRUCEmisor());
			createHeadings(cb, emisor_x, emisor_y-detailsRowSize*5, emisor.getRUCEmisor());

			createHeadings(cb, receptor_x, 743, idDocFact.getSerie() + idDocFact.getNro());
			createHeadings(cb, receptor_x, 723, idDocFact.getTipoCFE().toString());
			createHeadings(cb, receptor_x, 703, idDocFact.getFchEmis().toString());

		}

		catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void generateDetail(Document doc, PdfContentByte cb, int index, int row) {
		DecimalFormat df = new DecimalFormat("0.00");

		try {
			System.out.println(index + " - " + row);
			int y = total- heatherHeight - detailsRowSize - row*detailsRowSize + 5;
			
			createContent(cb, 48, y, String.valueOf(index + 1), PdfContentByte.ALIGN_RIGHT);
			createContent(cb, 52, y, "ITEM" + String.valueOf(index + 1), PdfContentByte.ALIGN_LEFT);
			createContent(cb, 152, y, "Product Description - SIZE " + String.valueOf(index + 1),
					PdfContentByte.ALIGN_LEFT);

			double price = Double.valueOf(df.format(Math.random() * 10));
			double extPrice = price * (index + 1);
			createContent(cb, 498, y, df.format(price), PdfContentByte.ALIGN_RIGHT);
			createContent(cb, 568, y, df.format(extPrice), PdfContentByte.ALIGN_RIGHT);

		}

		catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void createHeadings(PdfContentByte cb, float x, float y, String text) {

		cb.beginText();
		cb.setFontAndSize(bfBold, 8);
		cb.setTextMatrix(x, y);
		if (text!=null)
			cb.showText(text.trim());
		cb.endText();

	}

	private void printPageNumber(PdfContentByte cb) {

		cb.beginText();
		cb.setFontAndSize(bfBold, 8);
		cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, "Page No. " + (pageNumber + 1), 570, 25, 0);
		cb.endText();

		pageNumber++;

	}

	private void createContent(PdfContentByte cb, float x, float y, String text, int align) {

		cb.beginText();
		cb.setFontAndSize(bf, 8);
		cb.showTextAligned(align, text.trim(), x, y, 0);
		cb.endText();

	}

	private void initializeFonts() {

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

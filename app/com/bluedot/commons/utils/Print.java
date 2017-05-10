package com.bluedot.commons.utils;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

public class Print {

	public static void print(String printerNameDesired, File file) throws PrinterException, IOException {

		DocPrintJob docPrintJob = null;

		if (printerNameDesired != null) {
			/*
			 *  list of printers
			 */
			PrintService[] service = PrinterJob.lookupPrintServices(); 
			int count = service.length;

			for (int i = 0; i < count; i++) {
				if (service[i].getName().equalsIgnoreCase(printerNameDesired)) {
					docPrintJob = service[i].createPrintJob();
					i = count;
				}
			}
		}else{
			PrintService service = PrintServiceLookup.lookupDefaultPrintService(); 
			docPrintJob = service.createPrintJob();
		}
		
		PDDocument document = PDDocument.load(file);

		PrinterJob pjob = PrinterJob.getPrinterJob();
		pjob.setPageable(new PDFPageable(document));
		pjob.setPrintService(docPrintJob.getPrintService());
		pjob.setJobName("job");
		pjob.print();
	}

}

package com.bluedot.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * @author javacodepoint.com
 *
 */
public class ExcelToJSONConverter2 {

	private ObjectMapper mapper = new ObjectMapper();
	
	public DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	/**
	 * Method to convert excel sheet data to JSON format
	 * 
	 * @param excel
	 * @return
	 */
	public JsonNode excelToJson(File excel, Logger logger) {
		// hold the excel data sheet wise
		ObjectNode excelData = mapper.createObjectNode();
		FileInputStream fis = null;
		Workbook workbook = null;
		try {
			// Creating file input stream
			fis = new FileInputStream(excel);

			String filename = excel.getName().toLowerCase();
			if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
				// creating workbook object based on excel file format
				if (filename.endsWith(".xls")) {
					workbook = new HSSFWorkbook(fis);
				} else {
					workbook = new XSSFWorkbook(fis);
				}

				// Reading each sheet one by one
				for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
					Sheet sheet = workbook.getSheetAt(i);
					String sheetName = "Cotizaciones";
					logger.debug("Sheet Name:" + sheetName);      

					ObjectNode sheetData = mapper.createObjectNode();

					LocalDate fecha20071008 = LocalDate.parse("2007-10-08");
					LocalDate fecha20181212 = LocalDate.parse("2018-12-12");
					LocalDate fecha20210413 = LocalDate.parse("2021-04-13");
					
					// Reading each row of the sheet
					for (int j = 115; j <= sheet.getLastRowNum(); j++) {
						logger.debug("Row:" + j);
						Row row = sheet.getRow(j);

						ObjectNode rowData = mapper.createObjectNode();
						
						// Si  (dia, compra, venta) estan vacios se termino la lista
						if (row.getCell(0)== null || (row.getCell(0).getCellType() == CellType.BLANK && row.getCell(2).getCellType() == CellType.BLANK && row.getCell(3).getCellType() == CellType.BLANK)) 
							break;

						LocalDate fecha; 
						
						if (row.getCell(0)!= null && row.getCell(0).getCellType() != CellType.BLANK) {
							fecha = LocalDate.parse(row.getCell(0).getStringCellValue(), formatter);

							if (fecha.equals(fecha20071008))
								rowData.put("compra", "22.9");
							else
								rowData.put("compra", row.getCell(2).getNumericCellValue());
								
							if (fecha.equals(fecha20181212))
								rowData.put("venta", "32.89");
							else
								if (fecha.equals(fecha20210413))
									rowData.put("venta", "45.50");
								else
									rowData.put("venta", row.getCell(3).getNumericCellValue());
							
							
							logger.debug("Fecha: " + fecha.toString() + " compra: " + rowData.get("compra") + " venta: " + rowData.get("venta") );
			
							sheetData.set(fecha.toString(), rowData);
						}
						
					}
					excelData.set(sheetName, sheetData);
				}
				return excelData;
			} else {
				throw new IllegalArgumentException("File format not supported.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return null;
	}
}
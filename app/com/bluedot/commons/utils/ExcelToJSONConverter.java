package com.bluedot.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * @author javacodepoint.com
 *
 */
public class ExcelToJSONConverter {

	private ObjectMapper mapper = new ObjectMapper();

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
					String sheetName = sheet.getSheetName();
					logger.debug("Sheet Name:" + sheetName);      


					ObjectNode sheetData = mapper.createObjectNode();

					int dia = 1;
					int anio = 2002;
					int mes = 2;
					
					String mes_string;


					// Reading each row of the sheet
					for (int j = 115; j <= sheet.getLastRowNum(); j++) {
						logger.debug("Row:" + j);
						Row row = sheet.getRow(j);


						ObjectNode rowData = mapper.createObjectNode();
						
						// Si el dia esta vacio se termino la lista
						if (row.getCell(0)== null || row.getCell(0).getCellType() == CellType.BLANK)
							break;

						if (row.getCell(0)!= null && row.getCell(0).getCellType() != CellType.BLANK)
							dia = (int)row.getCell(0).getNumericCellValue();

						if (row.getCell(2)!=null &&  row.getCell(2).getCellType() != CellType.BLANK)
							anio = (int)row.getCell(2).getNumericCellValue();

						if (row.getCell(1)!=null && row.getCell(1).getCellType() != CellType.BLANK) {

							
							mes_string = row.getCell(1).getStringCellValue().trim().toUpperCase().substring(0, Math.min(3, row.getCell(1).getStringCellValue().trim().toUpperCase().length()));
							
							logger.debug("MES: " + mes_string);
							
							if (mes_string.length()>0)
							
								switch (mes_string) {
	
								case "ENE":
									mes = 1;
									break;
								case "FEB":
									mes = 2;
									break;
								case "MAR":
									mes = 3;
									break;
								case "ABR":
									mes = 4;
									break;
								case "MAY":
									mes = 5;
									break;
								case "JUN":
									mes = 6;
									break;
								case "JUL":
									mes = 7;
									break;
								case "AGO":
									mes = 8;
									break;
								case "SET":
									mes = 9;
									break;
								case "OCT":
									mes = 10;
									break;
								case "NOV":
									mes = 11;
									break;
								case "DIC":
									mes = 12;
									break;
								default:
									throw new IllegalArgumentException("No se reconoce el mes");
								}

						}


						LocalDate start = LocalDate.now().withYear(anio).withMonth(mes).withDayOfMonth(dia);
						
						if (mes == 10 && dia == 8 && anio == 2007) {
							rowData.put("compra", 22);
							rowData.put("venta", row.getCell(4).getNumericCellValue());
							logger.debug("Fecha: " + start + " compra: " + 22 + " venta: " + row.getCell(4).getNumericCellValue());
						}
						else
							if (mes == 12 && dia == 12 && anio == 2018) {
								rowData.put("compra", row.getCell(3).getNumericCellValue());
								rowData.put("venta", "32.89");
								logger.debug("Fecha: " + start + " compra: " + row.getCell(3).getNumericCellValue() + " venta: " + "32.89");
							}
							else
								if (mes == 4 && dia == 13 && anio == 2021) {
									rowData.put("compra", row.getCell(3).getNumericCellValue());
									rowData.put("venta", "45.50");
									logger.debug("Fecha: " + start + " compra: " + row.getCell(3).getNumericCellValue() + " venta: " + "45.50");
								}else {
									rowData.put("compra", row.getCell(3).getNumericCellValue());
									rowData.put("venta", row.getCell(4).getNumericCellValue());
									logger.debug("Fecha: " + start + " compra: " + row.getCell(3).getNumericCellValue() + " venta: " + row.getCell(4).getNumericCellValue());
								}
						
			
						sheetData.set(start.toString(), rowData);
						
						
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
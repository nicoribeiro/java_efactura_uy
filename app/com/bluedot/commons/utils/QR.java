package com.bluedot.commons.utils;

import java.awt.image.BufferedImage;

import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

public class QR {

	
	public static BufferedImage generateQR(String toEncode, int sizeInt, int quietZoneInt) {
		
		// GENERACIÓN DE QR
		
		int size = sizeInt; // En pixeles
		int quietZone = quietZoneInt; // En módulos (tamaño de cada cuadrado)

		QRCodeWriter qr = new QRCodeWriter();

		java.util.HashMap<EncodeHintType, Object> hints = new java.util.HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.MARGIN, new Integer(quietZone));

		try {
			/*
			 * Método estándard, según la cantidad de información a codificar
			 * puede dejar excesivo padding
			 */
			// com.google.zxing.common.BitMatrix matrix = qr.encode(toEncode,
			// com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);

			// Método alternativo, para evitar el padding
			ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
			QRCode code = Encoder.encode(toEncode,errorCorrectionLevel, hints);
			ByteMatrix input = code.getMatrix();

			// Constuye la matriz binaria con todos los píxeles
			int multiple = size / input.getHeight();
			int outputSize = multiple * input.getHeight();
			int topPadding = 0;
			int leftPadding = 0;
			BitMatrix matrix = new BitMatrix(outputSize, outputSize);
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
			return image;
		} catch (Exception e) {
		}
		return null;
	}
}

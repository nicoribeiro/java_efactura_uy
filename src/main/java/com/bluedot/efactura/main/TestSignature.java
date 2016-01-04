package com.bluedot.efactura.main;

import com.bluedot.commons.XmlSignature;

public class TestSignature {

	
	 public static void main(String[] args) {
		 XmlSignature xmlSignature = new XmlSignature();
		 xmlSignature.veryfySignatures("./resources/dump/lastSigned.xml");
	}
}

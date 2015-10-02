package com.bluedot.efactura;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class MyNamespacePrefixMapper extends NamespacePrefixMapper
{

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix)
	{
		System.out.println("String namespaceUri:"+namespaceUri+", String suggestion:"+suggestion+", boolean requirePrefix:"+requirePrefix);// TODO Auto-generated method stub
		if ("http://cfe.dgi.gub.uy".equals(namespaceUri))
			return "cfe";
		return "";
	}

}

package com.bluedot.efactura.interceptors;

import java.util.List;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import dgi.soap.recepcion.Data;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBRE;

public class NamespacesInterceptor extends AbstractPhaseInterceptor<Message> {

	public NamespacesInterceptor() {
		super(Phase.MARSHAL);
	}
	
	@Override
	public void handleMessage(Message message) throws Fault {
		List list = message.getContent(java.util.List.class);

		if (!(list.get(0) instanceof WSEFacturaEFACRECEPCIONSOBRE))
			return;
		
		WSEFacturaEFACRECEPCIONSOBRE narf = (WSEFacturaEFACRECEPCIONSOBRE)list.get(0);
		
		Data data = narf.getDatain();
		
		// CFE NAMESPACE CORRECTIONS
		String oldPrefix = "DGICFE";
		String newPrefix = "ns0";
		String split = newPrefix+":CFE";
		
		/*
		 *  change root node
		 */
		// open tag
		data.setXmlData(data.getXmlData().replaceAll("<"+oldPrefix+":CFE", "<"+newPrefix+":CFE xmlns:"+newPrefix+"=\"http://cfe.dgi.gub.uy\" "));
		// close tag 
		data.setXmlData(data.getXmlData().replaceAll("</"+oldPrefix+":CFE>", "</"+newPrefix+":CFE>"));
		
		
		/*
		 *  split the xml in 3 parts:
		 *  1 - the Caratula node + "<"
		 *  2 - the CFE stating with: "xmlns:cfe="http://cfe.dgi.gub.uy"  version="1.0">" and ending with: </
		 *  3 - the rest: "> </DGICFE:EnvioCFE>"
		 */
		String[] dataArray = data.getXmlData().split(split);
		
		/*
		 * change inner prefixes
		 */
		dataArray[1] = dataArray[1].replaceAll(oldPrefix, newPrefix);
		
		/*
		 * concatat 3 parts
		 */
		data.setXmlData(dataArray[0] + split + dataArray[1]+ split + dataArray[2]);
		
		
		/*
		 * update the message
		 */
		message.setContent(List.class, list);
		
	}

}

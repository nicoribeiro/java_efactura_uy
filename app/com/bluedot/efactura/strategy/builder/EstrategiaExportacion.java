package com.bluedot.efactura.strategy.builder;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.TipoDocumento;

/**
 * Lo que CFEBuilderExportacion necesita de la estrategia ademas de CFEStrategy: el receptor del exterior y el NCM de los
 * items. La implementan EFactExpStrategy (121-123) y ERemExpStrategy (124).
 */
public interface EstrategiaExportacion extends CFEStrategy {

	/** Receptor de exportacion: ver ReceptorExportacion */
	void buildReceptor(TipoDocumento tipoDocRecep, String codPaisRecep, String docRecep, String docRecepExt, String rznSocRecep,
			String dirRecep, String ciudadRecep, String deptoRecep, String paisRecep, Integer cp, String infoAdicional,
			String lugarDestEnt, String pdfMailAddress, String compraID, boolean secretoProfesional) throws APIException;

	/**
	 * NCM (B-C7.1, posicion arancelaria) del item, que no esta en ItemInterface
	 *
	 * @param indice
	 *            posicion del item en el detalle (desde 0)
	 */
	void setNCM(int indice, String ncm);

}

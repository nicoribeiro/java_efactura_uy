package com.bluedot.efactura.respuestas;

import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


//<Respuestas>
//<Respuesta>
//	<codigo>108</codigo>
//	<descripcion>Sobre duplicado.</descripcion>
//	<detalle>El sobre fue recibido en un envío anterior</detalle>
//</Respuesta>
//</Respuestas>

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Respuestas", propOrder = {
    "respuesta"
})
@XmlRootElement(name = "Respuestas")
public class Respuestas {

    @XmlElement(name = "Respuesta", required = true)
    protected List<Respuestas.Respuesta> respuesta;


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "codigo",
        "descripcion",
        "detalle"
    })
    public static class Respuesta {

        @XmlElement(name = "codigo", required = true)
        protected BigInteger codigo;
        @XmlElement(name = "descripcion", required = true)
        protected String descripcion;
        @XmlElement(name = "detalle", required = true)
        protected String detalle;
		public String getDescripcion() {
			return descripcion;
		}
		public void setDescripcion(String descripcion) {
			this.descripcion = descripcion;
		}
		public String getDetalle() {
			return detalle;
		}
		public void setDetalle(String detalle) {
			this.detalle = detalle;
		}
		public BigInteger getCodigo() {
			return codigo;
		}
		public void setCodigo(BigInteger codigo) {
			this.codigo = codigo;
		}

    }


	public List<Respuestas.Respuesta> getRespuesta() {
		return respuesta;
	}


	public void setRespuesta(List<Respuestas.Respuesta> respuesta) {
		this.respuesta = respuesta;
	}

}

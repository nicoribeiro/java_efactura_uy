package com.bluedot.efactura.pollers;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado en memoria de un poller, para consultarlo desde la API (GET /api/v1/procesos). Se pierde al reiniciar: cuenta
 * desde que arranco la instancia.
 */
public class EstadoPoller {

	/** Pollers registrados en esta instancia, en el orden en que se registraron */
	private static final Map<String, EstadoPoller> registro = new LinkedHashMap<String, EstadoPoller>();

	/** Cuando arranco esta instancia (registro de los pollers) */
	private static final Date inicio = new Date();

	private final String nombre;
	private boolean activo;
	private long intervaloMs;
	private String programacion;

	private Date ultimoInicio;
	private Date ultimoFin;
	private long ejecuciones;
	private long errores;
	private String ultimoError;
	private Date ultimoErrorFecha;
	private String resumen;
	private Date resumenFecha;

	private EstadoPoller(String nombre) {
		this.nombre = nombre;
	}

	public static synchronized EstadoPoller registrar(String nombre, boolean activo, long intervaloMs) {
		EstadoPoller estado = registro.get(nombre);
		if (estado == null) {
			estado = new EstadoPoller(nombre);
			registro.put(nombre, estado);
		}
		estado.activo = activo;
		estado.intervaloMs = intervaloMs;
		return estado;
	}

	public static synchronized List<EstadoPoller> todos() {
		return new ArrayList<EstadoPoller>(registro.values());
	}

	public static Date getInicio() {
		return inicio;
	}

	synchronized void inicioEjecucion() {
		ultimoInicio = new Date();
	}

	synchronized void finEjecucion(Throwable error) {
		ultimoFin = new Date();
		ejecuciones++;
		if (error != null) {
			errores++;
			ultimoError = error.getClass().getSimpleName() + (error.getMessage() != null ? ": " + error.getMessage() : "");
			ultimoErrorFecha = ultimoFin;
		}
	}

	/** Resultado legible de la ultima ejecucion con trabajo (lo define cada poller) */
	public synchronized void setResumen(String resumen) {
		this.resumen = resumen;
		this.resumenFecha = new Date();
	}

	/** Descripcion de cuando corre, si no es simplemente cada intervaloMs (ej: "diario desde las 15:00") */
	public synchronized void setProgramacion(String programacion) {
		this.programacion = programacion;
	}

	public String getNombre() { return nombre; }
	public synchronized boolean isActivo() { return activo; }
	public synchronized long getIntervaloMs() { return intervaloMs; }
	public synchronized String getProgramacion() { return programacion; }
	public synchronized Date getUltimoInicio() { return ultimoInicio; }
	public synchronized Date getUltimoFin() { return ultimoFin; }
	public synchronized long getEjecuciones() { return ejecuciones; }
	public synchronized long getErrores() { return errores; }
	public synchronized String getUltimoError() { return ultimoError; }
	public synchronized Date getUltimoErrorFecha() { return ultimoErrorFecha; }
	public synchronized String getResumen() { return resumen; }
	public synchronized Date getResumenFecha() { return resumenFecha; }
}

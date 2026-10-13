package com.bluedot.efactura.pollers;

import org.slf4j.Logger;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.DatabaseExecutor;
import com.bluedot.commons.utils.DatabaseExecutor.PromiseBlock;

public abstract class PollerRunner implements Runnable {

	private static final long INTERVAL = 1000L;

	private Logger logger;

	private long sleepTimeInMillis;

	private long runs;

	/** Estado consultable desde la API (GET /api/v1/procesos) */
	protected final EstadoPoller estado;

	/** Resultado de la ejecucion en curso (ver resultado); null si no hizo nada que registrar */
	private String resumenEjecucion;
	private org.json.JSONObject detalleEjecucion;

	public PollerRunner(Logger logger, long sleepTimeInMillis) {
		super();
		this.logger = logger;
		this.sleepTimeInMillis = sleepTimeInMillis;
		runs = 0;
		this.estado = EstadoPoller.registrar(getClass().getSimpleName(), true, sleepTimeInMillis);
	}

	public void run() {

		while (!PollerManager.shutdownInProgress) {
			try {
				if (INTERVAL * runs > sleepTimeInMillis) {
					runs = 0;
					// Error de la ejecucion (de la accion o de la transaccion), para el estado del poller
					final Throwable[] error = new Throwable[1];
					java.util.Date inicio = new java.util.Date();
					resumenEjecucion = null;
					detalleEjecucion = null;
					estado.inicioEjecucion();
					try {
						DatabaseExecutor.syncDatabaseAction(new PromiseBlock<Void>() {
							public Void execute() {

								try {
									executeConcreteAction();
								} catch (APIException e) {
									error[0] = e;
									e.printStackTrace();
								}

								return null;
							}

						}, true);

					} catch (Throwable e) {
						error[0] = e;
						e.printStackTrace();
					}
					estado.finEjecucion(error[0]);
					registrarEjecucion(inicio, error[0]);
				}
				runs++;
				logger.debug("Tick ...");
				Thread.sleep(INTERVAL);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Intervalo del poller desde application.conf ({clave}.intervaloMinutos), en milisegundos. Sin la clave se usa el
	 * valor por defecto. La configuracion la pasa PollerManager: los pollers se crean mientras Play construye la
	 * aplicacion, cuando play.Play.application() todavia no existe ("There is no started application").
	 */
	protected static long intervaloConfig(play.Configuration config, String clave, long minutosPorDefecto) {
		return config.getLong(clave + ".intervaloMinutos", minutosPorDefecto) * 60l * 1000l;
	}

	/**
	 * El resultado de la ejecucion en curso: queda como resumen en /procesos y la ejecucion se registra (EjecucionProceso,
	 * Ejecuciones en el panel) con su resumen en el log. Las ejecuciones que no lo llaman y no fallan (no hubo nada que
	 * hacer) no se registran: los pollers que corren cada pocos minutos llenarian la tabla.
	 *
	 * @param detalle
	 *            JSON con el detalle (opcional)
	 */
	protected void resultado(String resumen, org.json.JSONObject detalle) {
		estado.setResumen(resumen);
		resumenEjecucion = resumen;
		detalleEjecucion = detalle;
	}

	/** true si el poller registra sus ejecuciones por su cuenta (ej. EmisoresElectronicos, que tambien corre a mano) */
	protected boolean registraSusEjecuciones() {
		return false;
	}

	private void registrarEjecucion(java.util.Date inicio, Throwable error) {
		if (registraSusEjecuciones() || (error == null && resumenEjecucion == null))
			return;
		String nombre = getClass().getSimpleName();
		long segundos = (new java.util.Date().getTime() - inicio.getTime()) / 1000;
		String resumen;
		if (error == null) {
			resumen = resumenEjecucion;
			logger.info("EJECUCION {} (automático) OK en {} s: {}", nombre, segundos, resumen);
		} else {
			resumen = "Error: " + (error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName())
					+ (resumenEjecucion != null ? ". " + resumenEjecucion : "");
			logger.error("EJECUCION {} (automático) CON ERROR en {} s: {}", nombre, segundos, resumen);
		}
		com.bluedot.efactura.model.EjecucionProceso.registrar(nombre, inicio, error == null, "automático", resumen,
				detalleEjecucion != null ? detalleEjecucion.toString() : null);
	}

	protected abstract void executeConcreteAction() throws APIException;

}
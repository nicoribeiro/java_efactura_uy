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
	 * valor por defecto.
	 */
	protected static long intervaloConfig(String clave, long minutosPorDefecto) {
		return play.Play.application().configuration().getLong(clave + ".intervaloMinutos", minutosPorDefecto) * 60l * 1000l;
	}

	protected abstract void executeConcreteAction() throws APIException;

}
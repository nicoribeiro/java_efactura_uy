package com.bluedot.efactura.interceptors;

import com.bluedot.efactura.model.SobreEmitido;

public class InterceptorContextHolder {

	private static ThreadLocal<SobreEmitido> sobreEmitidoLocal = new ThreadLocal<>();

	private InterceptorContextHolder() {

	}

	public static SobreEmitido getSobreEmitido() {
		return sobreEmitidoLocal.get();
	}

	public static void setSobreEmitido(SobreEmitido sobreEmitido) {
		sobreEmitidoLocal.set(sobreEmitido) ;
	}

	/**
	 * Clear all the fields saved in the thread context
	 */
	public static void clear() {
		sobreEmitidoLocal.remove();

	}

}

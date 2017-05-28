package com.bluedot.efactura.interceptors;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.SobreEmitido;

public class InterceptorContextHolder {

	private static ThreadLocal<SobreEmitido> sobreEmitidoLocal = new ThreadLocal<>();
	
	private static ThreadLocal<Empresa> empresaLocal = new ThreadLocal<>();

	private InterceptorContextHolder() {

	}

	public static SobreEmitido getSobreEmitido() {
		return sobreEmitidoLocal.get();
	}
	
	public static Empresa getEmpresa() {
		return empresaLocal.get();
	}

	public static void setSobreEmitido(SobreEmitido sobreEmitido) {
		sobreEmitidoLocal.set(sobreEmitido);
		empresaLocal.set(sobreEmitido.getEmpresaEmisora());
	}

	/**
	 * Clear all the fields saved in the thread context
	 */
	public static void clear() {
		sobreEmitidoLocal.remove();
		empresaLocal.remove();
	}

	public static void setEmpresa(Empresa empresa) {
		empresaLocal.set(empresa);
	}

}

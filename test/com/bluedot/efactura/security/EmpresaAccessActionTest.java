package com.bluedot.efactura.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmpresaAccessActionTest {

	@Test
	public void rutasQueExigenAdminEnCualquierModo() {
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/empresa"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/empresa/cargar"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/modo/CONTINGENCIA"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/pruebas/testing"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/pruebas/homologacion_adenda"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/usuarios"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("PUT", "/api/v1/users/7/empresas/219999990016"));
		assertTrue(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/empresa/219999990016/usuarios"));
	}

	@Test
	public void rutasDeEmpresaDependenDelModo() {
		// GET /api/v1/empresa (lista) se filtra en el controlador
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/empresa"));
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/empresa/219999990016"));
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/empresa/219999990016/documentos"));
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/empresa/219999990016/logo"));
		// no confundir prefijos
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("GET", "/api/v1/modos"));
		assertFalse(EmpresaAccessAction.exigeAdminSiempre("POST", "/api/v1/empresa/cargarx"));
	}
}

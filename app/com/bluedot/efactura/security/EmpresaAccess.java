package com.bluedot.efactura.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import play.mvc.With;

/**
 * Aplica el control de acceso por empresa (ver {@link AccesoEmpresa}) a todas las acciones del controlador.
 *
 * Debe declararse despues de @Security.Authenticated para ejecutarse con el usuario ya autenticado.
 */
@With(EmpresaAccessAction.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EmpresaAccess {
}

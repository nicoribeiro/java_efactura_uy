package com.bluedot.efactura.controllers;

public enum EmailAction {

	POLL, RETRY, NO_ACTION,

	/** Marca como omitidos los adjuntos con error o pendientes de un email (solo ADMIN): no se vuelven a reintentar */
	OMIT
}
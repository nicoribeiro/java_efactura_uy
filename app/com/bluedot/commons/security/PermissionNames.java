package com.bluedot.commons.security;

public enum PermissionNames {
	
	/**
	 * Used for actions that won't require permission
	 */
	ANY(""),
	/**
	 * View and modify account settings
	 */
	ACCOUNT_ACCESS("access_account_{0}"),
	/**
	 * View and modify account settings
	 */
	EDIT_ACCOUNT_SETTINGS("edit_settings_on_account_{0}"),
	/**
	 * Account settings management
	 */
	MANAGE_SETTINGS_ON_ACCOUNT("manage_settings_on_account_{0}"),
	/**
	 * 
	 */
	MASTER_ADMIN("master_admin");
	;
	
	public String pattern;
	
	private PermissionNames(String pattern) 
	{
		this.pattern = pattern;
	}
}

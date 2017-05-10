package com.bluedot.commons.security;

public enum AccountAccessLevel {

	VIEWER("Viewer", "Viewer access level is not allowed to modify account settings.", false, PermissionNames.ACCOUNT_ACCESS),

	ADMIN("Admin", "Admin access level can manage account settings.", false,
			PermissionNames.ACCOUNT_ACCESS, PermissionNames.EDIT_ACCOUNT_SETTINGS,
			PermissionNames.MANAGE_SETTINGS_ON_ACCOUNT);

	public PermissionNames[] permissions;
	public String friendlyName;
	public String description;
	public boolean hidden;

	AccountAccessLevel(PermissionNames... permissions) {
		this.permissions = permissions;
	}

	AccountAccessLevel(String friendlyName, String description, boolean hidden, PermissionNames... permissions) {
		this.permissions = permissions;
		this.friendlyName = friendlyName;
		this.description = description;
		this.hidden = hidden;
	}
}

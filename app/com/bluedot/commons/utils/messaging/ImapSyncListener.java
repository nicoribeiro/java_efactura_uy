package com.bluedot.commons.utils.messaging;

/**
 * Callback de {@link EmailAttachmentReceiver#syncAllFolders} para persistir el estado de cada
 * carpeta y procesar los mensajes nuevos.
 */
public interface ImapSyncListener {

	/**
	 * @return estado guardado de la carpeta, o null si nunca se sincronizo
	 */
	FolderSyncState getState(String folderName);

	/**
	 * Guarda el ultimo UID procesado de la carpeta. Se llama despues de cada mensaje.
	 */
	void saveState(String folderName, long uidValidity, long lastUid);

	/**
	 * Procesa un mensaje nuevo. Si tira una excepcion se loguea y se continua con el siguiente mensaje.
	 */
	void onMessage(String folderName, long uid, Email email) throws Exception;

	public static class FolderSyncState {
		private final long uidValidity;
		private final long lastUid;

		public FolderSyncState(long uidValidity, long lastUid) {
			this.uidValidity = uidValidity;
			this.lastUid = lastUid;
		}

		public long getUidValidity() {
			return uidValidity;
		}

		public long getLastUid() {
			return lastUid;
		}
	}
}

package com.bluedot.commons.utils.messaging;

public enum AttachmentType
{
	IMAGE("image/jpeg", "jpeg"), VIDEO("video/mp4", "mp4"), PDF("application/pdf" , "pdf"), PNG("image/png" , "png"), TEXT("application/text" , "txt");
	
	String mimeType;
	String extension;
	
	public String mimeType()
	{
		return mimeType;
	}
	public String extension()
	{
		return extension;
	}
	private AttachmentType(String mimeType, String extension) {
		this.mimeType = mimeType;
		this.extension = extension;
	}
}
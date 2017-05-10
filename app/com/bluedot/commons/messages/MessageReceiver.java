package com.bluedot.commons.messages;



public interface MessageReceiver
{

	String getURN();

	void notify(Message message);

}

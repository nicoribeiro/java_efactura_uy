package com.bluedot.commons.hazelcast;

import com.bluedot.commons.error.APIException;

public interface MutexBlock
{

	void run() throws APIException;
}

package com.bluedot.efactura.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.ext.WSPasswordCallback;

public class KeyPasswordCallback implements CallbackHandler {

    private final Map<String, String> values;

    /**
     * Builds a {@link CallbackHandler} supporting provided alias-password pairs
     * @param values
     */
    public KeyPasswordCallback(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback passCallback = (WSPasswordCallback) callback;
                String id = passCallback.getIdentifier();
                int usage = passCallback.getUsage();
                if (usage == WSPasswordCallback.DECRYPT || usage == WSPasswordCallback.SIGNATURE) {
                    // used to retrieve password for private key
                    if (values.containsKey(id)) {
                        passCallback.setPassword(values.get(id));
                        return;
                    }
                }
            }
        }
    }
}

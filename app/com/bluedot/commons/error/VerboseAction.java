package com.bluedot.commons.error;

import java.util.concurrent.CompletionStage;

import play.Logger;
import play.mvc.Http;
import play.mvc.Result;

public class VerboseAction extends play.mvc.Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        Logger.info("Calling action for {}", ctx);
        return delegate.call(ctx);
    }
}

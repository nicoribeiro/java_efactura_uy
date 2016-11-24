package com.bluedot.efactura.global;

@SuppressWarnings("rawtypes")
public class Global
{
	

	
//TODO para logear los pedidos
//	private class ActionWrapper extends Action.Simple
//	{
//		public ActionWrapper(Action action) {	
//			this.delegate = action;
//			
//		}
//
//		@Override
//		public CompletionStage<Result> call(Context ctx) throws Throwable
//		{
//			CompletionStage<Result> result = this.delegate.call(ctx);
//			final Http.Request request = ctx.request();
//
//			result.onRedeem(new Callback<Result>() {
//				@Override
//				public void invoke(Result r) throws Throwable
//				{
//					Logger.info(RequestUtils.requestAddress(request) + ": " + request.method() + " " + request.path() + " " + r.toScala().header().status() + " "
//							+ request.getHeader(Secured.AUTH_HEADER) + " " + request.getHeader("User-Agent"));
//				}
//			});
//
//			result.onFailure(new Callback<Throwable>() {
//				@Override
//				public void invoke(Throwable t) throws Throwable
//				{
//					Logger.info(RequestUtils.requestAddress(request) + ": " + request.method() + " " + request.path() + " " + t.getLocalizedMessage());
//				}
//			});
//
//			return result;
//		}
//	}

	

	//TODO para logear los pedidos
//	@Override
//	public Action onRequest(Request request, Method method)
//	{
//		return new ActionWrapper(super.onRequest(request, method));
//	}

	

	

	

	
	

	

//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends EssentialFilter> Class<T>[] filters()
//	{
//		return new Class[] { GzipFilter.class };
//	}

	

}

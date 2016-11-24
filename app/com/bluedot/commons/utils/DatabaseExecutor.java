package com.bluedot.commons.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;
import play.libs.F.Promise;

public class DatabaseExecutor
{

	public interface PromiseBlock<T>
	{
		public T execute() throws Throwable;
	}

	public static <T> CompletionStage<T> asyncDatabaseAction(final PromiseBlock<T> block, final String emName, final boolean withTx) throws Throwable
	{

		return CompletableFuture.supplyAsync(new java.util.function.Supplier<T>() {

			@Override
			public T get() {
				try {
					return syncDatabaseAction(block, emName, withTx);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}

		});

	}

	public static <T> CompletionStage<T> asyncDatabaseAction(PromiseBlock<T> block, boolean withTx) throws Throwable
	{
		return asyncDatabaseAction(block, null, withTx);
	}

	public static <T> CompletionStage<T> asyncDatabaseAction(PromiseBlock<T> block) throws Throwable
	{
		return asyncDatabaseAction(block, null, false);
	}

	public static <T> T syncDatabaseAction(final PromiseBlock<T> block, final String emName, boolean withTx) throws Throwable
	{
		String name = emName != null ? emName : "default";
		EntityManager em = play.db.jpa.JPA.em(name);
		JPA.bindForSync(em);
		if (withTx)
			JPA.em().getTransaction().begin();
		try
		{
			T result = block.execute();
			if (withTx)
				JPA.em().getTransaction().commit();
			return result;
		} catch (Throwable t)
		{
			if (withTx)
				JPA.em().getTransaction().rollback();
			throw t;
		} finally
		{
			JPA.bindForSync(null);
			if (em != null)
			{
				em.close();
			}
		}

	}

	public static <T> T syncDatabaseAction(PromiseBlock<T> block, boolean withTx) throws Throwable
	{
		return syncDatabaseAction(block, null, withTx);
	}

	public static <T> T syncDatabaseAction(PromiseBlock<T> block) throws Throwable
	{
		return syncDatabaseAction(block, null, false);
	}

}

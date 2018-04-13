/*
 * ApplicationRuntime.java
 * Copyright © 2018, Richard A. Arriaga
 * All rights reserved.
 */

package org.availlang.raa.utilities;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

/**
 * An {@code ApplicationRuntime} contains the static runtime components of a
 * single running Application. It manages execution of the application.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class ApplicationRuntime
{
	/**
	 * {@code ExitCode} abstracts the allowed values for calls of {@link
	 * System#exit(int)}.
	 *
	 * <ol start="0">
	 *     <li>Normal exit:   {@link #NORMAL_EXIT}</li>
	 *     <li>Abnormal exit: {@link #UNSPECIFIED_ERROR}</li>
	 * </ol>
	 */
	public enum ExitCode
	{
		/** Normal exit. */
		NORMAL_EXIT (0),

		/** An unexpected error. */
		UNSPECIFIED_ERROR(1);

		/**
		 * The status code for {@link System#exit(int)}.
		 */
		private final int status;

		/**
		 * Shutdown the application with this {@link ExitCode}.
		 */
		public void shutdown ()
		{
			System.exit(status);
		}

		/**
		 * Construct an {@link ExitCode}.
		 *
		 * @param status
		 *        The status code for {@link System#exit(int)}.
		 */
		ExitCode (int status)
		{
			this.status = status;
		}
	}

	/**
	 * The sole {@link ApplicationRuntime}.
	 */
	private static ApplicationRuntime soleInstance;

	/**
	 * The {@link ThreadPoolExecutor} used by this application.
	 */
	private final ThreadPoolExecutor threadPoolExecutor;

	/**
	 * Initialize the run-time environment.
	 */
	public static void initialize ()
	{
		// Should only be done once.
		if (soleInstance == null)
		{
			soleInstance = new ApplicationRuntime();
		}
	}

	/**
	 * Schedule the provided {@link Runnable} with the {@link
	 * #threadPoolExecutor}.
	 *
	 * @param r
	 *        The {@link Runnable} to execute.
	 */
	public static void scheduleTask (final Runnable r)
	{
		soleInstance.threadPoolExecutor.execute(r);
	}

	/**
	 * The {@link Semaphore} that is used to keep the application from shutting
	 * down at the end of the a {@code main} method.
	 */
	private final static Semaphore applicationSemaphore =
		new Semaphore(0);

	/**
	 * Block the main thread of execution from completing.
	 */
	public static void block ()
	{
		try
		{
			applicationSemaphore.acquire();
		}
		catch (final InterruptedException e)
		{
			ExitCode.NORMAL_EXIT.shutdown();
		}
	}

	/**
	 * Only allow the {@link ApplicationRuntime} to be created internally.
	 */
	private ApplicationRuntime ()
	{
		this.threadPoolExecutor = new ThreadPoolExecutor(
			Runtime.getRuntime().availableProcessors(),
			Runtime.getRuntime().availableProcessors() << 2,
			10L,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(),
			runnable ->
			{
				final Thread thread = new Thread(runnable);
				thread.setDaemon(true);
				return thread;
			},
			new AbortPolicy());
	}
}

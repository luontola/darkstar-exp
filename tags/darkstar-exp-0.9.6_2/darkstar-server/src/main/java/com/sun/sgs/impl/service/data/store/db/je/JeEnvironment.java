/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.service.data.store.db.je;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ExceptionEvent;
import com.sleepycat.je.ExceptionListener;
import com.sleepycat.je.LockNotGrantedException;
import com.sleepycat.je.RunRecoveryException;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.XAEnvironment;
import com.sun.sgs.app.TransactionAbortedException;
import com.sun.sgs.app.TransactionConflictException;
import com.sun.sgs.app.TransactionTimeoutException;
import com.sun.sgs.impl.service.data.store.Scheduler;
import com.sun.sgs.impl.service.data.store.TaskHandle;
import com.sun.sgs.impl.service.data.store.db.DbDatabase;
import com.sun.sgs.impl.service.data.store.db.DbDatabaseException;
import com.sun.sgs.impl.service.data.store.db.DbEnvironment;
import com.sun.sgs.impl.service.data.store.db.DbTransaction;
import com.sun.sgs.impl.service.transaction.TransactionCoordinator;
import com.sun.sgs.impl.sharedutil.LoggerWrapper;
import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
import com.sun.sgs.service.TransactionParticipant;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.xa.XAException;
import static javax.transaction.xa.XAException.XA_RBBASE;
import static javax.transaction.xa.XAException.XA_RBDEADLOCK;
import static javax.transaction.xa.XAException.XA_RBEND;
import static javax.transaction.xa.XAException.XA_RBTIMEOUT;

/**
 * Provides a database implementation based on <a href=
 * "http://www.oracle.com/database/berkeley-db/je/index.html">Berkeley DB, Java
 * Edition</a>. <p>
 * 
 * Operations on classes in this package will throw an {@link Error} if the
 * underlying Berkeley DB database requires recovery.  In that case, callers
 * need to restart the application or create a new instance of this class. <p>
 *
 * Note that, although databases returned by this class provide support for the
 * {@link DbTransaction#prepare DbTransaction.prepare} method, they do not
 * provide facilities for resolving prepared transactions after a crash.
 * Callers can work around this limitation by insuring that the transaction
 * implementation calls {@link TransactionParticipant#prepareAndCommit
 * TransactionParticipant.prepareAndCommit} to commit transactions on this
 * class.  The current transaction implementation calls
 * <code>prepareAndCommit</code> on durable participants, so the inability to
 * resolve prepared transactions should have no effect at present. <p>
 *
 * The {@link #JeEnvironment constructor} supports the following
 * configuration properties: <p>
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <b>{@value #FLUSH_TO_DISK_PROPERTY}</b> <br>
 *	<i>Default:</i> <code>false</code>
 *
 * <dd style="padding-top: .5em">Whether to flush changes to disk when a
 * transaction commits.  If <code>false</code>, the modifications made in some
 * of the most recent transactions may be lost if the host crashes, although
 * data integrity will be maintained.  Flushing changes to disk avoids data
 * loss but introduces a significant reduction in performance. <p>
 *
 * <dt> <i>Property:</i> <b>{@value #LOCK_TIMEOUT_PROPERTY}</b> <br>
 *	<i>Default:</i> {@value #DEFAULT_LOCK_TIMEOUT_PROPORTION} times the
 *	value of the <code>com.sun.sgs.txn.timeout</code> property, if
 *	specified, otherwise {@value #DEFAULT_LOCK_TIMEOUT}
 *
 * <dd style="padding-top: .5em">The maximum amount of time in milliseconds
 * that an attempt to obtain a lock will be allowed to continue before being
 * aborted.  Since Berkeley DB Java edition only detects deadlocks on lock
 * timeouts, this value is also the amount of time it will take to detect a
 * deadlock.  The value must be greater than {@code 0}, and should be less than
 * the transaction timeout. <p>
 *
 * <dt> <i>Property:</i> <b>{@value #STATS_PROPERTY}</b> <br>
 *	<i>Default:</i> <code>-1</code>
 *
 * <dd style="padding-top: .5em">The interval in milliseconds between calls to
 * log database statistics, or a negative value to disable logging.  The
 * property is set to {@code -1} by default, which disables statistics
 * logging. <p>
 *
 * </dl> <p>
 *
 * The constructor also supports any initialization properties supported by the
 * Berkeley DB {@link Environment} class that start with the {@code je.}
 * prefix. <p>
 *
 * Unless overridden, this implementation provides the following non-default
 * settings for Berkeley DB initialization properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>je.checkpointer.bytesInterval</b></code> <br>
 *	<i>Value:</i> <code>1000000</code>
 *
 * <dd style="padding-top: .5em">Perform checkpoints after 1 MB of changes.
 * This setting improves performance when there are a large number of changes
 * being committed. <p>
 *
 * <dt> <i>Property:</i> <code><b>je.env.sharedLatches</b></code> <br>
 *	<i>Value:</i> <code>true</code>
 *
 * <dd style="padding-top: .5em">Use shared latches to improve concurrency. <p>
 *
 * </dl> <p>
 *
 * This class uses the {@link Logger} named
 * <code>com.sun.sgs.impl.service.data.db.je</code> to log information at
 * the following logging levels: <p>
 *
 * <ul>
 * <li> {@link Level#SEVERE SEVERE} - Berkeley DB failures that require
 *	application restart and recovery
 * <li> {@link Level#WARNING WARNING} - Berkeley DB exceptions
 * <li> {@link Level#INFO INFO} - Berkeley DB statistics
 * <li> {@link Level#CONFIG CONFIG} - Constructor properties
 * </ul>
 */
public class JeEnvironment implements DbEnvironment {

    /** The package name. */
    private static final String PACKAGE =
	"com.sun.sgs.impl.service.data.store.db.je";

    /** The logger for this class. */
    static final LoggerWrapper logger =
	new LoggerWrapper(Logger.getLogger(PACKAGE));

    /**
     * The property that specifies whether to flush changes to disk on
     * transaction boundaries.  The property is set to false by default.  If
     * false, some recent transactions may be lost in the event of a crash,
     * although integrity will be maintained.
     */
    public static final String FLUSH_TO_DISK_PROPERTY =
	PACKAGE + ".flush.to.disk";

    /**
     * The property that specifies the amount of time permitted to obtain a
     * lock, in milliseconds.
     */
    public static final String LOCK_TIMEOUT_PROPERTY =
	PACKAGE + ".lock.timeout";

    /**
     * The default value of the lock timeout property, if no transaction
     * timeout is specified.
     */
    public static final long DEFAULT_LOCK_TIMEOUT = 10;

    /**
     * The default proportion of the transaction timeout to use for the lock
     * timeout, if no lock timeout is specified.
     */
    public static final double DEFAULT_LOCK_TIMEOUT_PROPORTION = 0.1;

    /**
     * The property that specifies the interval in milliseconds between calls
     * to log database statistics, or a negative value to disable logging.  The
     * property is set to -1 by default.
     */
    public static final String STATS_PROPERTY = PACKAGE + ".stats";
    
    /**
     * Default values for Berkeley DB Java Edition properties that are
     * different from the BDB defaults.
     */
    private static final Properties defaultProperties = new Properties();
    static {
	defaultProperties.setProperty("je.checkpointer.bytesInterval",
				      "1000000");
	defaultProperties.setProperty("je.env.sharedLatches", "true");
    }

    /** The Berkeley DB environment. */
    private final XAEnvironment env;

    /** The stats task or null. */
    private StatsRunnable statsTask = null;

    /** Used to cancel the stats task, if non-null. */
    private TaskHandle statsTaskHandle = null;

    /** A Berkeley DB exception listener that uses logging. */
    private static class LoggingExceptionListener
	implements ExceptionListener
    {
	public void exceptionThrown(ExceptionEvent event) {
	    if (logger.isLoggable(Level.WARNING)) {
		logger.logThrow(Level.WARNING, event.getException(),
				"Database exception in thread {0}",
				event.getThreadName());
	    }
	}
    }

    /** A runnable that logs database statistics. */
    private class StatsRunnable implements Runnable {
	private final StatsConfig statsConfig = new StatsConfig();
	private boolean cancelled = false;
	StatsRunnable() {
	    statsConfig.setClear(true);
	}
	/** Prevents this task from running in the future. */
	synchronized void cancel() {
	    cancelled = true;
	}
	public synchronized void run() {
	    if (!cancelled) {
		try {
		    if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, "Database stats:\n{0}",
				   env.getStats(statsConfig));
		    }
		} catch (Throwable e) {
		    logger.logThrow(Level.WARNING, e, "Stats failed");
		}
	    }
	}
    }

    /**
     * Creates an instance of this class.
     *
     * @param	directory the directory containing database files
     * @param	properties the properties to configure this instance
     * @param	scheduler the scheduler for running periodic tasks
     * @throws	DbDatabaseException if an unexpected database problem occurs
     */
    public JeEnvironment(
	String directory, Properties properties, Scheduler scheduler)
    {
	if (logger.isLoggable(Level.CONFIG)) {
	    logger.log(Level.CONFIG,
		       "JeEnvironment directory:{0}, properties:{1}, " +
		       "scheduler:{2}",
		       directory, properties, scheduler);
	}
	Properties propertiesWithDefaults = new Properties(properties);
	for (Enumeration<?> names = defaultProperties.propertyNames();
	     names.hasMoreElements(); )
	{
	    Object key = names.nextElement();
	    if (key instanceof String) {
		String property = (String) key;
		if (propertiesWithDefaults.getProperty(property) == null) {
		    propertiesWithDefaults.setProperty(
			property, defaultProperties.getProperty(property));
		}
	    }
	}
	PropertiesWrapper wrappedProps = new PropertiesWrapper(
	    propertiesWithDefaults);
	boolean flushToDisk = wrappedProps.getBooleanProperty(
	    FLUSH_TO_DISK_PROPERTY, false);
	long txnTimeout = wrappedProps.getLongProperty(
	    TransactionCoordinator.TXN_TIMEOUT_PROPERTY, -1);
	long defaultLockTimeout = (txnTimeout < 1)
	    ? DEFAULT_LOCK_TIMEOUT
	    : (long) (txnTimeout * DEFAULT_LOCK_TIMEOUT_PROPORTION);
	/* Avoid underflow */
	if (defaultLockTimeout < 1) {
	    defaultLockTimeout = 1;
	}
	long lockTimeout = wrappedProps.getLongProperty(
	    LOCK_TIMEOUT_PROPERTY, defaultLockTimeout, 1, Long.MAX_VALUE);
	/* Avoid overflow -- BDB treats 0 as unlimited */
	long lockTimeoutMicros = (lockTimeout < (Long.MAX_VALUE / 1000))
	    ? lockTimeout * 1000 : 0;
	long stats = wrappedProps.getLongProperty(STATS_PROPERTY, -1);
	EnvironmentConfig config = new EnvironmentConfig();
	config.setAllowCreate(true);
	config.setExceptionListener(new LoggingExceptionListener());
	/*
	 * Note that it seems that the lock timeout value needs to be set on
	 * the BDB JE environment in order to control how quickly deadlocks are
	 * detected.  Setting the value on the transaction appears to have no
	 * effect on deadlock detection.  -tjb@sun.com (11/05/2007)
	 */
 	config.setLockTimeout(lockTimeoutMicros);
	config.setTransactional(true);
	config.setTxnSerializableIsolation(true);
	config.setTxnWriteNoSync(!flushToDisk);
	for (Enumeration<?> names = propertiesWithDefaults.propertyNames();
	     names.hasMoreElements(); )
	{
	    Object key = names.nextElement();
	    if (key instanceof String) {
		String property = (String) key;
		if (property.startsWith("je.")) {
		    config.setConfigParam(
			property,
			propertiesWithDefaults.getProperty(property));
		}
	    }
	}
	try {
	    env = new XAEnvironment(new File(directory), config);
	} catch (DatabaseException e) {
	    throw convertException(e, false);
	} catch (Error e) {
	    logger.logThrow(
		Level.SEVERE, e, "JeEnvironment initialization failed");
	    throw e;
	}
	if (stats >= 0) {
	    statsTask = new StatsRunnable();
	    statsTaskHandle = scheduler.scheduleRecurringTask(
		statsTask, stats);
	}
    }

    /**
     * Returns the correct exception for a Berkeley DB DatabaseException, or
     * XAException, thrown during an operation.  Throws an Error if recovery is
     * needed.  Only converts Berkeley DB transaction exceptions to the
     * associated exceptions if convertTxnExceptions is true.
     */
    static RuntimeException convertException(
	Exception e, boolean convertTxnExceptions)
    {
	if (convertTxnExceptions && e instanceof LockNotGrantedException) {
	    return new TransactionTimeoutException(
		"Transaction timed out: " + e.getMessage(), e);
	} else if (convertTxnExceptions && e instanceof DeadlockException) {
	    return new TransactionConflictException(
		"Transaction conflict: " + e.getMessage(), e);
	} else if (e instanceof RunRecoveryException) {
	    /*
	     * It is tricky to clean up the data structures in this instance in
	     * order to reopen the Berkeley DB databases, because it's hard to
	     * know when they are no longer in use.  It's OK to catch this
	     * Error and create a new environment instance, but this instance
	     * is dead.  -tjb@sun.com (10/19/2006)
	     */
	    Error error = new Error(
		"Database requires recovery -- need to restart: " + e, e);
	    logger.logThrow(Level.SEVERE, error, "Database requires recovery");
	    throw error;
	} else if (e instanceof XAException) {
	    int errorCode = ((XAException) e).errorCode;
	    if (errorCode == XA_RBTIMEOUT) {
		throw new TransactionTimeoutException(
		    "Transaction timed out: " + e.getMessage(), e);
	    } else if (errorCode == XA_RBDEADLOCK) {
		throw new TransactionConflictException(
		    "Transaction conflict: " + e.getMessage(), e);
	    } else if (errorCode >= XA_RBBASE && errorCode <= XA_RBEND) {
		throw new TransactionAbortedException(
		    "Transaction aborted: " + e.getMessage(), e);
	    }
	}
	throw new DbDatabaseException(
	    "Unexpected database exception: " + e, e);
    }

    /** Returns the lock timeout in microseconds -- for testing. */
    private long getLockTimeoutMicros() {
	try {
	    return env.getConfig().getLockTimeout();
	} catch (DatabaseException e) {
	    throw convertException(e, false);
	}
    }

    /* -- Implement DbEnvironment -- */

    /** {@inheritDoc} */
    public DbTransaction beginTransaction(long timeout) {
	return new JeTransaction(env, timeout);
    }

    /** {@inheritDoc} */
    public DbDatabase openDatabase(
	DbTransaction txn, String fileName, boolean create)
	throws FileNotFoundException
    {
	return new JeDatabase(
	    env, JeTransaction.getJeTxn(txn), fileName, create);
    }

    /** {@inheritDoc} */
    public void close() {
	if (statsTaskHandle != null) {
	    statsTask.cancel();
	    statsTaskHandle.cancel();
	    statsTaskHandle = null;
	}
	try {
	    env.close();
	} catch (DatabaseException e) {
	    throw convertException(e, false);
	}
    }
}

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

package com.sun.sgs.impl.service.data.store.net;

import com.sun.sgs.app.TransactionAbortedException;
import com.sun.sgs.app.TransactionNotActiveException;
import com.sun.sgs.app.TransactionTimeoutException;
import com.sun.sgs.impl.kernel.StandardProperties;
import com.sun.sgs.impl.service.data.store.ClassInfoNotFoundException;
import com.sun.sgs.impl.service.data.store.DataStore;
import com.sun.sgs.impl.sharedutil.Exceptions;
import com.sun.sgs.impl.sharedutil.LoggerWrapper;
import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionParticipant;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an implementation of {@code DataStore} by communicating over the
 * network to an implementation of {@link DataStoreServer}, and optionally runs
 * the server. <p>
 *
 * The {@link #DataStoreClient(Properties) constructor} supports the following
 * properties:
 * <p>
 *
 * <dl style="margin-left: 1em">
 *
 * <dt>	<i>Property:</i> <code><b>
 *	com.sun.sgs.impl.service.data.store.net.max.txn.timeout
 *	</b></code><br>
 *	<i>Default:</i> {@code 600000}
 *
 * <dd style="padding-top: .5em">The maximum amount of time in milliseconds
 *	that a transaction that uses the data store will be permitted to run
 *	before it is a candidate for being aborted.  This value must be greater
 *	than {@code 0}. <p>
 *
 * <dt> <i>Property:</i> <code><b>
 *	com.sun.sgs.impl.service.data.store.net.server.start
 *	</b></code><br>
 *	<i>Default:</i> the value of the {@code com.sun.sgs.server.start}
 *	property, if present, else {@code true}
 *
 * <dd style="padding-top: .5em">Whether to run the server by creating an
 *	instance of {@link DataStoreServerImpl}, using the properties provided
 *	to this instance's constructor. <p>
 *
 * <dt>	<i>Property:</i> <code><b>
 *	com.sun.sgs.impl.service.data.store.net.server.host
 *	</b></code><br>
 *	<i>Default</i> the value of the {@code com.sun.sgs.server.host}
 *	property, if present, or {@code localhost} if this node is starting the 
 *      server.
 *
 * <dd style="padding-top: .5em">The name of the host running the {@code
 *	DataStoreServer}. <p>
 *
 * <dt>	<i>Property:</i> <code><b>
 *	com.sun.sgs.impl.service.data.store.net.server.port
 *	</b></code><br>
 *	<i>Default:</i> {@code 44530}
 *
 * <dd style="padding-top: .5em">The network port for the {@code
 *	DataStoreServer}.  This value must be no less than {@code 0} and no
 *	greater than {@code 65535}.  The value {@code 0} can only be specified
 *	if the {@code com.sun.sgs.impl.service.data.store.net.server.start}
 *	property is {@code true}, and means that an anonymous port will be
 *	chosen for running the server. <p>
 *
 * </dl> <p>
 *
 * This class uses the {@link Logger} named {@code
 * com.sun.sgs.impl.service.data.store.net.client} to log information
 * at the following levels: <p>
 *
 * <ul>
 * <li> {@link Level#SEVERE SEVERE} - Problems starting the server
 * <li> {@link Level#INFO INFO} - Starting the server
 * <li> {@link Level#CONFIG CONFIG} - Constructor properties
 * <li> {@link Level#FINE FINE} - Allocating object IDs
 * <li> {@link Level#FINEST FINEST} - Object operations
 * </ul>
 */
public final class DataStoreClient
    implements DataStore, TransactionParticipant
{
    /** The package for this class. */
    private static final String PACKAGE =
	"com.sun.sgs.impl.service.data.store.net";

    /** The logger for this class. */
    static final LoggerWrapper logger =
	new LoggerWrapper(Logger.getLogger(PACKAGE + ".client"));

    /** The property that specifies the name of the server host. */
    private static final String SERVER_HOST_PROPERTY =
	PACKAGE + ".server.host";

    /** The property that specifies the server port. */
    private static final String SERVER_PORT_PROPERTY =
	PACKAGE + ".server.port";

    /** The default for the server port. */
    private static final int DEFAULT_SERVER_PORT = 44530;

    /**
     * The number of times to retry attempting to obtain the server after a
     * failure to obtain it initially.
     */
    private static final int GET_SERVER_MAX_RETRIES = 3;

    /**
     * The number of milliseconds to wait between attempts to obtain the
     * server.
     */
    private static final long GET_SERVER_WAIT = 10000;

    /**
     * The name of the undocumented property that controls whether to replace
     * Java(TM) RMI with an experimental, socket-based facility.
     */
    private static final boolean noRmi = Boolean.getBoolean(
	PACKAGE + ".no.rmi");

    /** The property that specifies the maximum transaction timeout. */
    private static final String MAX_TXN_TIMEOUT_PROPERTY =
	PACKAGE + ".max.txn.timeout";

    /** The default maximum transaction timeout. */
    private static final long DEFAULT_MAX_TXN_TIMEOUT = 600000;

    /** The property that specifies to start the server. */
    private static final String SERVER_START_PROPERTY =
	PACKAGE + ".server.start";

    /** The server host name. */
    private final String serverHost;

    /** The server port. */
    private final int serverPort;

    /** The remote server. */
    private final DataStoreServer server;

    /** The local server or null. */
    private DataStoreServerImpl localServer = null;

    /** The maximum transaction timeout. */
    private final long maxTxnTimeout;

    /** Provides information about the transaction for the current thread. */
    private final ThreadLocal<TxnInfo> threadTxnInfo =
	new ThreadLocal<TxnInfo>();

    /** Object to synchronize on when accessing txnCount and shuttingDown. */
    private final Object txnCountLock = new Object();

    /** The number of currently active transactions. */
    private int txnCount = 0;

    /** Whether the client is in the process of shutting down. */
    private boolean shuttingDown = false;

    /** Stores transaction information. */
    private static class TxnInfo {

	/** The transaction. */
	final Transaction txn;

	/** The associated server transaction ID. */
	final long tid;

	/** Whether preparation of the transaction has started. */
	boolean prepared;

	/** Whether the server side has already aborted. */
	boolean serverAborted;

	/** Creates an instance. */
	TxnInfo(Transaction txn, long tid) {
	    this.txn = txn;
	    this.tid = tid;
	}
    }

    /**
     * Creates an instance of this class configured with the specified
     * properties.  See the {@link DataStoreClient class documentation} for a
     * list of supported properties.
     *
     * @param	properties the properties for configuring this instance
     * @throws	IllegalArgumentException if the {@code
     *		com.sun.sgs.impl.service.data.store.net.server.host} property
     *		is not set, or if the value of the {@code
     *		com.sun.sgs.impl.service.data.store.net.server.port} property
     *		is not a valid integer not less than {@code 0} and not greater
     *		than {@code 65535}
     * @throws	IOException if a network problem occurs
     * @throws	NotBoundException if the server is not found in the Java RMI
     *		registry
     */
    public DataStoreClient(Properties properties)
	throws IOException, NotBoundException
    {
	logger.log(Level.CONFIG, "Creating DataStoreClient properties:{0}",
		   properties);
	PropertiesWrapper wrappedProps = new PropertiesWrapper(properties);
	boolean serverStart = wrappedProps.getBooleanProperty(
	    SERVER_START_PROPERTY,
	    wrappedProps.getBooleanProperty(
		StandardProperties.SERVER_START, true));
        if (serverStart) {
            // we default to localHost;  this is useful for starting
            // single node systems
            String localHost = InetAddress.getLocalHost().getHostName();
            serverHost = wrappedProps.getProperty(
                SERVER_HOST_PROPERTY,
                wrappedProps.getProperty(
                    StandardProperties.SERVER_HOST, localHost));
        } else {
            // a server host most be specified
            serverHost = wrappedProps.getProperty(
                SERVER_HOST_PROPERTY,
                wrappedProps.getProperty(
                    StandardProperties.SERVER_HOST));
            if (serverHost == null) {
                throw new IllegalArgumentException(
                                           "A server host must be specified");
            }
        }
	int specifiedServerPort = wrappedProps.getIntProperty(
	    SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT, serverStart ? 0 : 1,
	    65535);
	maxTxnTimeout = wrappedProps.getLongProperty(
	    MAX_TXN_TIMEOUT_PROPERTY, DEFAULT_MAX_TXN_TIMEOUT, 1,
	    Long.MAX_VALUE);
	if (serverStart) {
	    try {
		localServer = new DataStoreServerImpl(properties);
		serverPort = localServer.getPort();
		logger.log(Level.INFO, "Started server: {0}", localServer);
	    } catch (IOException t) {
		logger.logThrow(Level.SEVERE, t, "Problem starting server");
		throw t;
	    } catch (RuntimeException t) {
		logger.logThrow(Level.SEVERE, t, "Problem starting server");
		throw t;
	    }
	} else {
	    serverPort = specifiedServerPort;
	}
	server = getServer();
    }

    /* -- Implement DataStore -- */

    /** {@inheritDoc} */
    public long createObject(Transaction txn) {
	logger.log(Level.FINEST, "createObject txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    long result = server.createObject(txnInfo.tid);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "createObject txn:{0} returns oid:{1,number,#}",
			   txn, result);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINEST, exception, "createObject txn:" + txn);
    }

    /** {@inheritDoc} */
    public void markForUpdate(Transaction txn, long oid) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "markForUpdate txn:{0}, oid:{1,number,#}",
		       txn, oid);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    server.markForUpdate(txnInfo.tid, oid);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "markForUpdate txn:{0}, oid:{1,number,#} returns",
			   txn, oid);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "markForUpdate txn:" + txn + ", oid:" + oid);
    }

    /** {@inheritDoc} */
    public byte[] getObject(Transaction txn, long oid, boolean forUpdate) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST,
		       "getObject txn:{0}, oid:{1,number,#}, forUpdate:{2}",
		       txn, oid, forUpdate);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    byte[] result = server.getObject(txnInfo.tid, oid, forUpdate);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST,
		    "getObject txn:{0}, oid:{1,number,#}, forUpdate:{2} " +
		    "returns",
		    txn, oid, forUpdate);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "getObject txn:" + txn + ", oid:" + oid +
			       ", forUpdate:" + forUpdate);
    }

    /** {@inheritDoc} */
    public void setObject(Transaction txn, long oid, byte[] data) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "setObject txn:{0}, oid:{1,number,#}",
		       txn, oid);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    if (data == null) {
		throw new NullPointerException("The data must not be null");
	    }
	    txnInfo = checkTxn(txn);
	    server.setObject(txnInfo.tid, oid, data);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "setObject txn:{0}, oid:{1,number,#} returns",
			   txn, oid);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "setObject txn:" + txn + ", oid:" + oid);
    }

    /** {@inheritDoc} */
    public void setObjects(Transaction txn, long[] oids, byte[][] dataArray) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "setObjects txn:{0}", txn);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    server.setObjects(txnInfo.tid, oids, dataArray);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "setObjects txn:{0} returns", txn);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "setObjects txn:" + txn);
    }

    /** {@inheritDoc} */
    public void removeObject(Transaction txn, long oid) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "removeObject txn:{0}, oid:{1,number,#}",
		       txn, oid);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    server.removeObject(txnInfo.tid, oid);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "removeObject txn:{0}, oid:{1,number,#} returns",
			   txn, oid);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "removeObject txn:" + txn + ", oid:" + oid);
    }

    /** {@inheritDoc} */
    public long getBinding(Transaction txn, String name) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(
		Level.FINEST, "getBinding txn:{0}, name:{1}", txn, name);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    long result = server.getBinding(txnInfo.tid, name);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST,
		    "getBinding txn:{0}, name:{1} returns oid:{2,number,#}",
		    txn, name, result);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "getBinding txn:" + txn + ", name:" + name);
    }

    /** {@inheritDoc} */
    public void setBinding(Transaction txn, String name, long oid) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(
		Level.FINEST, "setBinding txn:{0}, name:{1}, oid:{2,number,#}",
		txn, name, oid);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    server.setBinding(txnInfo.tid, name, oid);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST,
		    "setBinding txn:{0}, name:{1}, oid:{2,number,#} returns",
		    txn, name, oid);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINEST, exception,
	    "setBinding txn:" + txn + ", name:" + name + ", oid:" + oid);
    }

    /** {@inheritDoc} */
    public void removeBinding(Transaction txn, String name) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(
		Level.FINEST, "removeBinding txn:{0}, name:{1}", txn, name);
	}
	Exception exception;
	TxnInfo txnInfo = null;
	try {
	    txnInfo = checkTxn(txn);
	    server.removeBinding(txnInfo.tid, name);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST, "removeBinding txn:{0}, name:{1} returns",
		    txn, name);
	    }
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "removeBinding txn:" + txn + ", name:" + name);
    }

    /** {@inheritDoc} */
    public String nextBoundName(Transaction txn, String name) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(
		Level.FINEST, "nextBoundName txn:{0}, name:{1}", txn, name);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    String result = server.nextBoundName(txnInfo.tid, name);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "nextBoundName txn:{0}, name:{1} returns {2}",
			   txn, name, result);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINEST, exception,
			       "nextBoundName txn:" + txn + ", name:" + name);
    }

    /** {@inheritDoc} */
    public boolean shutdown() {
	logger.log(Level.FINER, "shutdown");
	try {
	    synchronized (txnCountLock) {
		shuttingDown = true;
		while (txnCount > 0) {
		    try {
			logger.log(Level.FINEST,
				   "shutdown waiting for {0} transactions",
				   txnCount);
			txnCountLock.wait();
		    } catch (InterruptedException e) {
			logger.log(Level.FINEST, "shutdown interrupted");
			break;
		    }
		}
		if (txnCount < 0) {
		    throw new IllegalStateException("DataStore is shut down");
		}
		boolean ok = (txnCount == 0);
		if (ok) {
		    txnCount = -1;
		    if (localServer != null) {
			if (localServer.shutdown()) {
			    localServer = null;
			} else {
			    ok = false;
			}
		    }
		}
		logger.log(Level.FINER, "shutdown returns {0}", ok);
		return ok;
	    }
	} catch (RuntimeException e) {
	    throw convertException(null, null, Level.FINER, e, "shutdown");
	}
    }

    /** {@inheritDoc} */
    public int getClassId(Transaction txn, byte[] classInfo) {
	logger.log(Level.FINER, "getClassId txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    int result = server.getClassId(txnInfo.tid, classInfo);
	    if (logger.isLoggable(Level.FINER)) {
		logger.log(Level.FINER,
			   "getClassId txn:{0} returns {1}", txn, result);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINER, exception, "getClassId txn:" + txn);
    }

    /** {@inheritDoc} */
    public byte[] getClassInfo(Transaction txn, int classId)
	throws ClassInfoNotFoundException
    {
	if (logger.isLoggable(Level.FINER)) {
	    logger.log(Level.FINER,
		       "getClassInfo txn:{0}, classId:{1,number,#}",
		       txn, classId);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    byte[] result = server.getClassInfo(txnInfo.tid, classId);
	    if (logger.isLoggable(Level.FINER)) {
		logger.log(
		    Level.FINER,
		    "getClassInfo txn:{0}, classId:{1,number,#} returns",
		    txn, classId);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINER, exception,
	    "getClassInfo txn:" + txn + ", classId:" + classId);
    }

    /** {@inheritDoc} */
    public long nextObjectId(Transaction txn, long oid) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "nextObjectId txn:{0}, oid:{1,number,#}",
		       txn, oid);
	}
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxn(txn);
	    long result = server.nextObjectId(txnInfo.tid, oid);
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST,
			   "nextObjectId txn:{0}, oid:{1,number,#} " +
			   "returns oid:{2,number,#}",
			   txn, oid, result);
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINEST, exception,
	    "nextObjectId txn:" + txn + ", oid:" + oid + " throws");
    }

    /* -- Implement TransactionParticipant -- */

    /** {@inheritDoc} */
    public boolean prepare(Transaction txn) {
	logger.log(Level.FINER, "prepare txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxnNoJoin(txn, true);
	    checkTimeout(txn);
	    if (txnInfo.prepared) {
		throw new IllegalStateException(
		    "Transaction has already been prepared");
	    }
	    boolean result = server.prepare(txnInfo.tid);
	    txnInfo.prepared = true;
	    if (logger.isLoggable(Level.FINER)) {
		logger.log(
		    Level.FINER, "prepare txn:{0} returns {1}", txn, result);
	    }
	    if (result) {
		threadTxnInfo.set(null);
		decrementTxnCount();
	    }
	    return result;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINER, exception,
			       "prepare txn:" + txn);
    }

    /** {@inheritDoc} */
    public void commit(Transaction txn) {
	logger.log(Level.FINER, "commit txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxnNoJoin(txn, true);
	    if (!txnInfo.prepared) {
		throw new IllegalStateException(
		    "Transaction has not been prepared");
	    }
	    server.commit(txnInfo.tid);
	    threadTxnInfo.set(null);
	    decrementTxnCount();
	    logger.log(Level.FINER, "commit txn:{0} returns", txn);
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINER, exception,
			       "commit txn:" + txn);
    }

    /** {@inheritDoc} */
    public void prepareAndCommit(Transaction txn) {
	logger.log(Level.FINER, "prepareAndCommit txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxnNoJoin(txn, true);
	    checkTimeout(txn);
	    if (txnInfo.prepared) {
		throw new IllegalStateException(
		    "Transaction has already been prepared");
	    }
	    server.prepareAndCommit(txnInfo.tid);
	    threadTxnInfo.set(null);
	    decrementTxnCount();
	    logger.log(Level.FINER, "prepareAndCommit txn:{0} returns", txn);
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(txn, txnInfo, Level.FINER, exception,
			       "prepareAndCommit txn:" + txn);
    }

    /** {@inheritDoc} */
    public void abort(Transaction txn) {
	logger.log(Level.FINER, "abort txn:{0}", txn);
	TxnInfo txnInfo = null;
	Exception exception;
	try {
	    txnInfo = checkTxnNoJoin(txn, false);
	    if (!txnInfo.serverAborted) {
		try {
		    server.abort(txnInfo.tid);
		} catch (TransactionNotActiveException e) {
		    logger.logThrow(Level.FINEST, e,
				    "abort txn:{0} - Transaction already " +
				    "aborted by server",
				    txn);
		}
	    }
	    threadTxnInfo.set(null);
	    decrementTxnCount();
	    logger.log(Level.FINER, "abort txn:{0} returns", txn);
	    return;
	} catch (IOException e) {
	    exception = e;
	} catch (RuntimeException e) {
	    exception = e;
	}
	throw convertException(
	    txn, txnInfo, Level.FINER, exception, "abort txn:" + txn);
    }
    
    /** {@inheritDoc} */
    public String getTypeName() {
        return this.getClass().getName();
    }

    /* -- Other public methods -- */

    /**
     * Returns a string representation of this object.
     *
     * @return	a string representation of this object
     */
    public String toString() {
	return "DataStoreClient[serverHost:" + serverHost +
	    ", serverPort:" + serverPort + "]";
    }

    /* -- Private methods -- */

    /** Obtains the server. */
    private DataStoreServer getServer() throws IOException, NotBoundException {
	boolean done = false;
	for (int i = 0; !done; i++) {
	    if (i == GET_SERVER_MAX_RETRIES) {
		done = true;
	    }
	    try {
		if (!noRmi) {
		    Registry registry = LocateRegistry.getRegistry(
			serverHost, serverPort);
		    return (DataStoreServer) registry.lookup(
			"DataStoreServer");
		} else {
		    return new DataStoreClientRemote(serverHost, serverPort);
		}
	    } catch (IOException e) {
		if (done) {
		    throw e;
		}
	    } catch (NotBoundException e) {
		if (done) {
		    throw e;
		}
	    }
	}
	throw new AssertionError();
    }

    /**
     * Checks that the correct transaction is in progress, and join if none is
     * in progress.
     */
    private TxnInfo checkTxn(Transaction txn) throws IOException {
	if (txn == null) {
	    throw new NullPointerException("Transaction must not be null");
	}
	TxnInfo txnInfo = threadTxnInfo.get();
	if (txnInfo == null) {
	    txnInfo = joinTransaction(txn);
	} else if (!txnInfo.txn.equals(txn)) {
	    throw new IllegalStateException(
		"Wrong transaction: Found " + txnInfo.txn +
		", expected " + txn);
	} else if (txnInfo.prepared) {
	    throw new IllegalStateException("Transaction has been prepared");
	}
	checkTimeout(txn);
	return txnInfo;
    }

    /**
     * Joins the specified transaction, checking first to see if the data store
     * is currently shutting down, and returning the new TxnInfo.
     */
    private TxnInfo joinTransaction(Transaction txn) throws IOException {
	synchronized (txnCountLock) {
	    if (txnCount < 0) {
		throw new IllegalStateException("Service is shut down");
	    } else if (shuttingDown) {
		throw new IllegalStateException("Service is shutting down");
	    }
	    txnCount++;
	}
	boolean joined = false;
	long tid = -1;
	try {
	    tid = server.createTransaction(txn.getTimeout());
	    txn.join(this);
	    joined = true;
	    if (logger.isLoggable(Level.FINER)) {
		logger.log(Level.FINER,
			   "Created server transaction stid:{0,number,#} " +
			   "for transaction {1}",
			   tid, txn);
	    }
	} finally {
	    if (!joined) {
		decrementTxnCount();
		if (tid != -1) {
		    try {
			server.abort(tid);
		    } catch (RuntimeException e) {
			if (logger.isLoggable(Level.FINEST)) {
			    logger.logThrow(
				Level.FINEST, e,
				"Problem aborting server transaction " +
				"stid:{0,number,#} for transaction {1}",
				tid, txn);
			}
		    }
		}
	    }
	}
	TxnInfo txnInfo = new TxnInfo(txn, tid);
	threadTxnInfo.set(txnInfo);
	return txnInfo;
    }

    /**
     * Checks that the correct transaction is in progress, throwing an
     * exception if the transaction has not been joined.  If notAborting is
     * true, then checks if the store is shutting down.
     */
    private TxnInfo checkTxnNoJoin(Transaction txn, boolean notAborting) {
	if (txn == null) {
	    throw new NullPointerException("Transaction must not be null");
	}
	TxnInfo txnInfo = threadTxnInfo.get();
	if (txnInfo == null) {
	    throw new IllegalStateException("Transaction is not active");
	} else if (notAborting && getTxnCount() < 0) {
	    throw new IllegalStateException("DataStore is shutting down");
	} else if (!txnInfo.txn.equals(txn)) {
	    throw new IllegalStateException("Wrong transaction");
	}
	return txnInfo;
    }

    /**
     * Returns the correct SGS exception for a IOException thrown during an
     * operation.  The txn argument, if non-null, is used to abort the
     * transaction if a TransactionAbortedException is going to be thrown.  The
     * txnInfo argument, if non-null, is used to mark the transaction as
     * aborted.  The level argument is used to log the exception.  The
     * operation argument will be included in newly created exceptions and the
     * log, and should describe the operation that was underway when the
     * exception was thrown.  The supplied exception may also be a
     * RuntimeException, which will be logged and returned.
     */
    private RuntimeException convertException(
	Transaction txn, TxnInfo txnInfo, Level level, Exception e,
	String operation)
    {
	RuntimeException re;
	if (e instanceof IOException) {
	    re = new NetworkException(
		operation + " failed due to a communication problem: " +
		e.getMessage(),
		e);
	} else if (e instanceof TransactionAbortedException) {
	    re = (RuntimeException) e;
	} else if (e instanceof TransactionNotActiveException && txn != null) {
	    /*
	     * If the transaction is not active on the server, then it may have
	     * timed out.
	     */
	    long duration = System.currentTimeMillis() - txn.getCreationTime();
	    if (duration > txn.getTimeout()) {
		re = new TransactionTimeoutException(
		    operation + " failed: Transaction timed out after " +
		    duration + " ms",
		    e);
	    } else {
		re = (TransactionNotActiveException) e;
	    }
	} else if (e instanceof RuntimeException) {
	    re = (RuntimeException) e;
	} else {
	    throw Exceptions.initCause(
		new AssertionError(
		    "Expected IOException or RuntimeException: " + e),
		e);
	}
	/*
	 * If we're throwing an exception saying that the transaction was
	 * aborted, then make sure to abort the transaction now.
	 */
	if (re instanceof TransactionAbortedException) {
	    if (txnInfo != null) {
		txnInfo.serverAborted = true;
	    }
	    if (txn != null && !txn.isAborted()) {
		txn.abort(re);
	    }
	}
	logger.logThrow(level, re, "{0} throws", operation);
	return re;
    }

    /** Returns the current transaction count. */
    private int getTxnCount() {
	synchronized (txnCountLock) {
	    return txnCount;
	}
    }

    /** Decrements the current transaction count. */
    private void decrementTxnCount() {
	synchronized (txnCountLock) {
	    txnCount--;
	    if (txnCount <= 0) {
		txnCountLock.notifyAll();
	    }
	}
    }

    /**
     * Checks that the transaction has not timed out, including if it has run
     * for longer than the maximum timeout.
     */
    private void checkTimeout(Transaction txn) {
	long max = Math.min(txn.getTimeout(), maxTxnTimeout);
	long runningTime = System.currentTimeMillis() - txn.getCreationTime();
	if (runningTime > max) {
	    throw new TransactionTimeoutException(
		"Transaction timed out: " + runningTime + " ms");
	}
    }
}

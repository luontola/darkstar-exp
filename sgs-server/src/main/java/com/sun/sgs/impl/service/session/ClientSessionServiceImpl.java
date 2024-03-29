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

package com.sun.sgs.impl.service.session;

import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;
import com.sun.sgs.app.Task;
import com.sun.sgs.app.TransactionNotActiveException;
import com.sun.sgs.app.util.ManagedSerializable;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.auth.IdentityCoordinator;
import com.sun.sgs.impl.kernel.StandardProperties;
import com.sun.sgs.impl.service.channel.ChannelServiceImpl;
import com.sun.sgs.impl.service.session.ClientSessionImpl.
    HandleNextDisconnectedSessionTask;
import com.sun.sgs.impl.sharedutil.HexDumper;
import com.sun.sgs.impl.sharedutil.LoggerWrapper;
import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
import com.sun.sgs.impl.util.AbstractKernelRunnable;
import com.sun.sgs.impl.util.AbstractService;
import com.sun.sgs.impl.util.Exporter;
import com.sun.sgs.impl.util.TransactionContext;
import com.sun.sgs.impl.util.TransactionContextFactory;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.KernelRunnable;
import com.sun.sgs.kernel.RecurringTaskHandle;
import com.sun.sgs.kernel.TaskQueue;
import com.sun.sgs.nio.channels.AsynchronousChannelGroup;
import com.sun.sgs.nio.channels.AsynchronousServerSocketChannel;
import com.sun.sgs.nio.channels.AsynchronousSocketChannel;
import com.sun.sgs.nio.channels.CompletionHandler;
import com.sun.sgs.nio.channels.IoFuture;
import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;
import com.sun.sgs.service.ClientSessionDisconnectListener;
import com.sun.sgs.service.ClientSessionService;
import com.sun.sgs.service.DataService;
import com.sun.sgs.service.Node;
import com.sun.sgs.service.NodeMappingService;
import com.sun.sgs.service.RecoveryCompleteFuture;
import com.sun.sgs.service.RecoveryListener;
import com.sun.sgs.service.TaskService;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.WatchdogService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;   
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages client sessions. <p>
 *
 * The {@link #ClientSessionServiceImpl constructor} requires the <a
 * href="../../../app/doc-files/config-properties.html#com.sun.sgs.app.name">
 * <code>com.sun.sgs.app.name</code></a> and <a
 * href="../../../app/doc-files/config-properties.html#com.sun.sgs.app.port">
 * <code>com.sun.sgs.app.port</code></a> configuration properties and supports
 * these public configuration <a
 * href="../../../app/doc-files/config-properties.html#ClientSessionService">
 * properties</a>. <p>
 */
public final class ClientSessionServiceImpl
    extends AbstractService
    implements ClientSessionService
{
    /** The package name. */
    public static final String PKG_NAME = "com.sun.sgs.impl.service.session";
    
    /** The name of this class. */
    private static final String CLASSNAME =
	ClientSessionServiceImpl.class.getName();
    
    /** The logger for this class. */
    private static final LoggerWrapper logger =
	new LoggerWrapper(Logger.getLogger(PKG_NAME));

    /** The name of the version key. */
    private static final String VERSION_KEY = PKG_NAME + ".service.version";

    /** The major version. */
    private static final int MAJOR_VERSION = 1;
    
    /** The minor version. */
    private static final int MINOR_VERSION = 0;
    
    /**
     * The server listen address property.
     * This is the host interface we are listening on. Default is listen
     * on all interfaces.
     */
    private static final String LISTEN_HOST_PROPERTY =
        PKG_NAME + ".listen.address";
    
    /** The name of the server port property. */
    private static final String SERVER_PORT_PROPERTY =
	PKG_NAME + ".server.port";

    /** The default server port. */
    private static final int DEFAULT_SERVER_PORT = 0;

    /** The name of the acceptor backlog property. */
    private static final String ACCEPTOR_BACKLOG_PROPERTY =
        PKG_NAME + ".acceptor.backlog";

    /** The default acceptor backlog (&lt;= 0 means default). */
    private static final int DEFAULT_ACCEPTOR_BACKLOG = 0;

    private static final String EVENTS_PER_TXN_PROPERTY =
	PKG_NAME + ".events.per.txn";

    /** The default events per transaction. */
    private static final int DEFAULT_EVENTS_PER_TXN = 1;

    /** The name of the read buffer size property. */
    private static final String READ_BUFFER_SIZE_PROPERTY =
        PKG_NAME + ".buffer.read.max";

    /** The default read buffer size: {@value #DEFAULT_READ_BUFFER_SIZE} */
    private static final int DEFAULT_READ_BUFFER_SIZE = 128 * 1024;
    
    /** The name of the write buffer size property. */
    private static final String WRITE_BUFFER_SIZE_PROPERTY =
        PKG_NAME + ".buffer.write.max";

    /** The default write buffer size: {@value #DEFAULT_WRITE_BUFFER_SIZE} */
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 128 * 1024;

    /** The name of the allow new login property. */
    private static final String ALLOW_NEW_LOGIN_PROPERTY =
	PKG_NAME + ".allow.new.login";

    /** The name of the disconnect delay property. */
    private static final String DISCONNECT_DELAY_PROPERTY =
	PKG_NAME + ".disconnect.delay";
    
    /** The time (in milliseconds) that a disconnecting connection is
     * allowed before this service forcibly disconnects it.
     */
    private static final long DEFAULT_DISCONNECT_DELAY = 1000;

    /** The read buffer size for new connections. */
    private final int readBufferSize;

    /** The write buffer size for new connections. */
    private final int writeBufferSize;

    /** The disconnect delay (in milliseconds) for disconnecting sessions. */
    private final long disconnectDelay;

    /** The port for accepting connections. */
    private final int appPort;

    /** The local node's ID. */
    private final long localNodeId;

    /** The async channel group for this service. */
    private final AsynchronousChannelGroup asyncChannelGroup;

    /** The acceptor for listening for new connections. */
    private final AsynchronousServerSocketChannel acceptor;

    /** The currently-active accept operation, or {@code null} if none. */
    volatile IoFuture<?, ?> acceptFuture;

    /** The registered session disconnect listeners. */
    private final Set<ClientSessionDisconnectListener>
	sessionDisconnectListeners =
	    Collections.synchronizedSet(
		new HashSet<ClientSessionDisconnectListener>());

    /** A map of local session handlers, keyed by session ID . */
    private final Map<BigInteger, ClientSessionHandler> handlers =
	Collections.synchronizedMap(
	    new HashMap<BigInteger, ClientSessionHandler>());

    /** Queue of contexts that are prepared (non-readonly) or committed. */
    private final Queue<Context> contextQueue =
	new ConcurrentLinkedQueue<Context>();

    /** Thread for flushing committed contexts. */
    private final Thread flushContextsThread = new FlushContextsThread();
    
    /** Lock for notifying the thread that flushes committed contexts. */
    private final Object flushContextsLock = new Object();

    /** The transaction context factory. */
    private final TransactionContextFactory<Context> contextFactory;

    /** The watchdog service. */
    final WatchdogService watchdogService;

    /** The node mapping service. */
    final NodeMappingService nodeMapService;

    /** The task service. */
    final TaskService taskService;

    /** The channel service. */
    private volatile ChannelServiceImpl channelService;
    
    /** The identity manager. */
    final IdentityCoordinator identityManager;

    /** The exporter for the ClientSessionServer. */
    private final Exporter<ClientSessionServer> exporter;

    /** The ClientSessionServer remote interface implementation. */
    private final SessionServerImpl serverImpl;
	
    /** The proxy for the ClientSessionServer. */
    private final ClientSessionServer serverProxy;

    /** The map of logged in {@code ClientSessionHandler}s, keyed by
     *  identity.
     */
    private final ConcurrentHashMap<Identity, ClientSessionHandler>
	loggedInIdentityMap =
	    new ConcurrentHashMap<Identity, ClientSessionHandler>();
	
    /** The map of session task queues, keyed by session ID. */
    private final ConcurrentHashMap<BigInteger, TaskQueue>
	sessionTaskQueues = new ConcurrentHashMap<BigInteger, TaskQueue>();

    /** The map of disconnecting {@code ClientSessionHandler}s, keyed by
     * the time the connection should expire.
     */
    private final ConcurrentSkipListMap<Long, ClientSessionHandler>
	disconnectingHandlersMap =
	    new ConcurrentSkipListMap<Long, ClientSessionHandler>();

    /** The handle for the task that monitors disconnecting client sessions. */
    private RecurringTaskHandle monitorDisconnectingSessionsTaskHandle;

    /** The maximum number of session events to sevice per transaction. */
    final int eventsPerTxn;

    /** The flag that indicates how to handle same user logins.  If {@code
     * true}, then if the same user logs in, the existing session will be
     * disconnected, and the new login is allowed to proceed.  If {@code
     * false}, then if the same user logs in, the new login will be denied.
     */
    final boolean allowNewLogin;

    /**
     * Constructs an instance of this class with the specified properties.
     *
     * @param properties service properties
     * @param systemRegistry system registry
     * @param txnProxy transaction proxy
     * @throws Exception if a problem occurs when creating the service
     */
    public ClientSessionServiceImpl(Properties properties,
				    ComponentRegistry systemRegistry,
				    TransactionProxy txnProxy)
	throws Exception
    {
	super(properties, systemRegistry, txnProxy, logger);
	
	logger.log(Level.CONFIG,
		   "Creating ClientSessionServiceImpl properties:{0}",
		   properties);
	PropertiesWrapper wrappedProps = new PropertiesWrapper(properties);
	
	try {
            appPort = wrappedProps.getRequiredIntProperty(
                StandardProperties.APP_PORT, 1, 65535);

	    /*
	     * Get the property for controlling session event processing
	     * and connection disconnection.
	     */
	    eventsPerTxn = wrappedProps.getIntProperty(
		EVENTS_PER_TXN_PROPERTY, DEFAULT_EVENTS_PER_TXN,
		1, Integer.MAX_VALUE);
            readBufferSize = wrappedProps.getIntProperty(
                READ_BUFFER_SIZE_PROPERTY, DEFAULT_READ_BUFFER_SIZE,
                8192, Integer.MAX_VALUE);
            writeBufferSize = wrappedProps.getIntProperty(
                WRITE_BUFFER_SIZE_PROPERTY, DEFAULT_WRITE_BUFFER_SIZE,
                8192, Integer.MAX_VALUE);
	    disconnectDelay = wrappedProps.getLongProperty(
		DISCONNECT_DELAY_PROPERTY, DEFAULT_DISCONNECT_DELAY,
		200, Long.MAX_VALUE);
	    allowNewLogin = wrappedProps.getBooleanProperty(
 		ALLOW_NEW_LOGIN_PROPERTY, false);

	    /*
	     * Export the ClientSessionServer.
	     */
	    int serverPort = wrappedProps.getIntProperty(
		SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT, 0, 65535);
	    serverImpl = new SessionServerImpl();
	    exporter =
		new Exporter<ClientSessionServer>(ClientSessionServer.class);
	    try {
		int port = exporter.export(serverImpl, serverPort);
		serverProxy = exporter.getProxy();
		if (logger.isLoggable(Level.CONFIG)) {
		    logger.log(
			Level.CONFIG, "export successful. port:{0,number,#}",
			port);
		}
	    } catch (Exception e) {
		try {
		    exporter.unexport();
		} catch (RuntimeException re) {
		}
		throw e;
	    }

	    /*
	     * Get services and check service version.
	     */
	    identityManager =
		systemRegistry.getComponent(IdentityCoordinator.class);
	    flushContextsThread.start();
	    contextFactory = new ContextFactory(txnProxy);
	    watchdogService = txnProxy.getService(WatchdogService.class);
	    nodeMapService = txnProxy.getService(NodeMappingService.class);
	    taskService = txnProxy.getService(TaskService.class);
	    localNodeId = watchdogService.getLocalNodeId();
	    watchdogService.addRecoveryListener(
		new ClientSessionServiceRecoveryListener());
	    int acceptorBacklog = wrappedProps.getIntProperty(
	                ACCEPTOR_BACKLOG_PROPERTY, DEFAULT_ACCEPTOR_BACKLOG);
	    transactionScheduler.runTask(
		new AbstractKernelRunnable("CheckServiceVersion") {
		    public void run() {
			checkServiceVersion(
			    VERSION_KEY, MAJOR_VERSION, MINOR_VERSION);
		    } },  taskOwner);
	    
	    /*
	     * Store the ClientSessionServer proxy in the data store.
	     */
	    transactionScheduler.runTask(
		new AbstractKernelRunnable("StoreClientSessionServiceProxy") {
		    public void run() {
			dataService.setServiceBinding(
			    getClientSessionServerKey(localNodeId),
			    new ManagedSerializable<ClientSessionServer>(
				serverProxy));
		    } },
		taskOwner);

	    /*
	     * Listen for incoming client connections. If no host address
             * is supplied, default to listen on all interfaces.
	     */
            String hostAddress = properties.getProperty(LISTEN_HOST_PROPERTY);
            InetSocketAddress listenAddress =
                hostAddress == null ?
		new InetSocketAddress(appPort) :
		new InetSocketAddress(hostAddress, appPort);
            AsynchronousChannelProvider provider =
                // TODO fetch from config
                AsynchronousChannelProvider.provider();
            asyncChannelGroup =
                // TODO fetch from config
                provider.openAsynchronousChannelGroup(
                    Executors.newCachedThreadPool());
            acceptor =
                provider.openAsynchronousServerSocketChannel(asyncChannelGroup);
	    try {
                acceptor.bind(listenAddress, acceptorBacklog);
		if (logger.isLoggable(Level.CONFIG)) {
		    logger.log(
			Level.CONFIG, "bound to port:{0,number,#}",
			getListenPort());
		}
	    } catch (Exception e) {
		logger.logThrow(Level.WARNING, e,
                                "acceptor failed to listen on {0}",
                                listenAddress);
		try {
		    acceptor.close();
                } catch (IOException ioe) {
                    logger.logThrow(Level.WARNING, ioe,
                        "problem closing acceptor");
                }
		throw e;
	    }

	    /*
	     * Set up recurring task to monitor disconnecting client sessions.
	     */
	    monitorDisconnectingSessionsTaskHandle =
		taskScheduler.scheduleRecurringTask(
 		    new MonitorDisconnectingSessionsTask(),
		    taskOwner, System.currentTimeMillis(),
		    Math.max(disconnectDelay, DEFAULT_DISCONNECT_DELAY) / 2);
	    monitorDisconnectingSessionsTaskHandle.start();

	} catch (Exception e) {
	    if (logger.isLoggable(Level.CONFIG)) {
		logger.logThrow(
		    Level.CONFIG, e,
		    "Failed to create ClientSessionServiceImpl");
	    }
	    doShutdown();
	    throw e;
	}
    }

    /* -- Implement AbstractService -- */

    /** {@inheritDoc} */
    protected void handleServiceVersionMismatch(
	Version oldVersion, Version currentVersion)
    {
	throw new IllegalStateException(
	    "unable to convert version:" + oldVersion +
	    " to current version:" + currentVersion);
    }
    
    /** {@inheritDoc} */
    public void doReady() {
	channelService = txnProxy.getService(ChannelServiceImpl.class);
        acceptFuture = acceptor.accept(new AcceptorListener());
        try {
            if (logger.isLoggable(Level.CONFIG)) {
                logger.log(
                    Level.CONFIG, "listening on {0}",
                    acceptor.getLocalAddress());
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage(), ioe);
        }
    }

    /** {@inheritDoc} */
    public void doShutdown() {
	final IoFuture<?, ?> future = acceptFuture;
	acceptFuture = null;
	if (future != null) {
	    future.cancel(true);
	}

	if (acceptor != null) {
	    try {
		acceptor.close();
            } catch (IOException e) {
                logger.logThrow(Level.FINEST, e, "closing acceptor throws");
                // swallow exception
            } 
	}

	for (ClientSessionHandler handler : handlers.values()) {
	    handler.shutdown();
	}
	handlers.clear();
	
	if (asyncChannelGroup != null) {
	    asyncChannelGroup.shutdown();
	    boolean groupShutdownCompleted = false;
	    try {
		groupShutdownCompleted =
		    asyncChannelGroup.awaitTermination(1, TimeUnit.SECONDS);
	    } catch (InterruptedException e) {
		logger.logThrow(Level.FINEST, e,
				"shutdown acceptor interrupted");
		Thread.currentThread().interrupt();
	    }
	    if (!groupShutdownCompleted) {
		logger.log(Level.WARNING, "forcing async group shutdown");
		try {
		    asyncChannelGroup.shutdownNow();
		} catch (IOException e) {
		    logger.logThrow(Level.FINEST, e,
				    "shutdown acceptor throws");
		    // swallow exception
		}
	    }
	}
	logger.log(Level.FINEST, "acceptor shutdown");

	if (exporter != null) {
	    try {
		exporter.unexport();
		logger.log(Level.FINEST, "client session server unexported");
	    } catch (RuntimeException e) {
		logger.logThrow(Level.FINEST, e, "unexport server throws");
		// swallow exception
	    }
	}

	if (monitorDisconnectingSessionsTaskHandle != null) {
	    try {
		monitorDisconnectingSessionsTaskHandle.cancel();
	    } finally {
		monitorDisconnectingSessionsTaskHandle = null;
	    }
	}
	    
	disconnectingHandlersMap.clear();

	synchronized (flushContextsLock) {
	    flushContextsLock.notifyAll();
	}
    }

    /**
     * Returns the port this service is listening on for incoming
     * client session connections.
     *
     * @return the port this service is listening on
     * @throws IOException if an IO problem occurs
     */
    public int getListenPort() throws IOException {
        return ((InetSocketAddress) acceptor.getLocalAddress()).getPort();
    }

    /**
     * Returns the proxy for the client session server on the specified
     * {@code nodeId}, or {@code null} if no server exists.
     *
     * @param	nodeId a node ID
     * @return	the proxy for the client session server on the specified
     * 		{@code nodeId}, or {@code null}
     */
    ClientSessionServer getClientSessionServer(long nodeId) {
	if (nodeId == localNodeId) {
	    return serverImpl;
	} else {
	    String sessionServerKey = getClientSessionServerKey(nodeId);
	    try {
		ManagedSerializable wrappedProxy = (ManagedSerializable)
		    dataService.getServiceBinding(sessionServerKey);
		return (ClientSessionServer) wrappedProxy.get();
	    } catch (NameNotBoundException e) {
		return null;
	    }  catch (ObjectNotFoundException e) {
		logger.logThrow(
		    Level.SEVERE, e,
		    "ClientSessionServer binding:{0} exists, " +
		    "but object removed", sessionServerKey);
		throw e;
	    }
	}
    }

    /* -- Implement ClientSessionService -- */

    /** {@inheritDoc} */
    public void registerSessionDisconnectListener(
        ClientSessionDisconnectListener listener)
    {
        if (listener == null) {
            throw new NullPointerException("null listener");
        }
        sessionDisconnectListeners.add(listener);
    }
    
    /** {@inheritDoc} */
    public void sendProtocolMessageNonTransactional(
	BigInteger sessionRefId, ByteBuffer message, Delivery delivery)
    {
	ClientSessionHandler handler = handlers.get(sessionRefId);
	/*
	 * If a local handler exists, forward message to local handler
	 * to send to client session.
	 */
	if (handler != null) {
	    byte[] bytes = new byte[message.remaining()];
	    message.get(bytes);
	    handler.sendProtocolMessage(bytes, delivery);
	} else {
	    logger.log(
		Level.FINE,
		"Discarding messages for unknown session:{0}",
		sessionRefId);
		return;
	}
    }

    /* -- Package access methods for adding commit actions -- */
    
    /**
     * Sends the specified protocol {@code message} to the specified
     * client {@code session} with the specified {@code delivery}
     * guarantee.  This method must be called within a transaction.
     *
     * @param	session	a client session
     * @param	message a complete protocol message
     * @param	delivery a delivery requirement
     *
     * @throws 	TransactionException if there is a problem with the
     *		current transaction
     */
    void sendProtocolMessage(
	ClientSessionImpl session, ByteBuffer message, Delivery delivery)
    {
        byte[] bytes = new byte[message.remaining()];
        message.get(bytes);
	checkContext().addMessage(session, bytes, delivery);
    }

    /**
     * Sends the specified login acknowledgment {@code message} to the
     * specified client {@code session} with the specified {@code delivery}
     * guarantee.  If {@code success} is false, then no further messages
     * can be sent to this session, even if they have been enqueued during
     * the current transaction.
     *
     * <p>When the transaction commits, the login acknowledgment message is
     * delivered to the client session first, and if {@code success} is
     * true, all other enqueued messages will be delivered.
     *
     * @param	session	a client session
     * @param	message a complete protocol message
     * @param	delivery a delivery requirement
     * @param	success if {@code true}, login was successful
     *
     * @throws 	TransactionException if there is a problem with the
     *		current transaction
     */
    void sendLoginAck(
 	ClientSessionImpl session, byte[] message,
	Delivery delivery, boolean success)
    {
	Context context = checkContext();
	context.addLoginAck(session, message, delivery, success);
    }

    /**
     * Disconnects the specified client {@code session}.  This method must
     * be invoked within a transaction.
     *
     * @param	session a client session
     *
     * @throws 	TransactionException if there is a problem with the
     *		current transaction
     */
    void disconnect(ClientSessionImpl session) {
	checkContext().requestDisconnect(session);
    }

    /**
     * Returns the size of the read buffer to use for new connections.
     * 
     * @return the size of the read buffer to use for new connections
     */
    int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * Returns the size of the write buffer to use for new connections.
     * 
     * @return the size of the write buffer to use for new connections
     */
    int getWriteBufferSize() {
        return writeBufferSize;
    }

    /** A completion handler for accepting connections. */
    private class AcceptorListener
        implements CompletionHandler<AsynchronousSocketChannel, Void>
    {

	/** Handle new connection or report failure. */
        public void completed(
	    IoFuture<AsynchronousSocketChannel, Void> result)
        {
            try {
                try {
                    AsynchronousSocketChannel newChannel = result.getNow();
                    logger.log(Level.FINER, "Accepted {0}", newChannel);

		    /* The handler will call addHandler if login succeeds */
		    new ClientSessionHandler(
			ClientSessionServiceImpl.this, dataService,
			new AsynchronousMessageChannel(
			    newChannel, readBufferSize));

                    // Resume accepting connections
                    acceptFuture = acceptor.accept(this);

                } catch (ExecutionException e) {
                    throw (e.getCause() == null) ? e : e.getCause();
                }
            } catch (CancellationException e) {               
                logger.logThrow(Level.FINE, e, "acceptor cancelled"); 
                //ignore
            } catch (Throwable e) {
                SocketAddress addr = null;
                try {
                    addr = acceptor.getLocalAddress();
                } catch (IOException ioe) {
                    // ignore
                }

                logger.logThrow(
		    Level.SEVERE, e, "acceptor error on {0}", addr);

                // TBD: take other actions, such as restarting acceptor?
            }
	}
    }

    /* -- Implement TransactionContextFactory -- */

    private class ContextFactory extends TransactionContextFactory<Context> {
	ContextFactory(TransactionProxy txnProxy) {
	    super(txnProxy, CLASSNAME);
	}

	/** {@inheritDoc} */
	public Context createContext(Transaction txn) {
	    return new Context(txn);
	}
    }

    /* -- Context class to hold transaction state -- */
    
    final class Context extends TransactionContext {

	/** Map of client sessions to an object containing a list of
	 * actions to make upon transaction commit. */
        private final Map<ClientSessionImpl, CommitActions> commitActions =
	    new HashMap<ClientSessionImpl, CommitActions>();

	/**
	 * Constructs a context with the specified transaction.
	 */
        private Context(Transaction txn) {
	    super(txn);
	}

	/**
	 * Adds a login acknowledgment message be sent to the specified
	 * session after this transaction commits.  If {@code success} is
	 * {@code false}, no other messages are sent to the session after
	 * the login acknowledgment.
	 */
	void addLoginAck(
 	    ClientSessionImpl session, byte[] message, Delivery delivery,
	    boolean success)
	{
	    try {
		if (logger.isLoggable(Level.FINEST)) {
		    logger.log(
			Level.FINEST,
			"Context.addLoginAck success:{0} session:{1}," +
			"message:{2}", success, session, message);
		}
		checkPrepared();

		getCommitActions(session).addLoginAck(message, success);

	    
	    } catch (RuntimeException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.logThrow(
			Level.FINE, e,
			"Context.addMessage exception");
                }
                throw e;
            }
	}

	/**
	 * Adds a message to be sent to the specified session after
	 * this transaction commits.
	 */
	private void addMessage(
	    ClientSessionImpl session, byte[] message, Delivery delivery)
	{
	    try {
		if (logger.isLoggable(Level.FINEST)) {
		    logger.log(
			Level.FINEST,
			"Context.addMessage session:{0}, message:{1}",
			session, message);
		}
		checkPrepared();

		getCommitActions(session).addMessage(message);
	    
	    } catch (RuntimeException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.logThrow(
			Level.FINE, e,
			"Context.addMessage exception");
                }
                throw e;
            }
	}

	/**
	 * Requests that the specified session be disconnected when
	 * this transaction commits, but only after all session
	 * messages are sent.
	 */
	void requestDisconnect(ClientSessionImpl session) {
	    try {
		if (logger.isLoggable(Level.FINEST)) {
		    logger.log(
			Level.FINEST,
			"Context.setDisconnect session:{0}", session);
		}
		checkPrepared();

		getCommitActions(session).setDisconnect();
		
	    } catch (RuntimeException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.logThrow(
			Level.FINE, e,
			"Context.setDisconnect throws");
                }
                throw e;
            }
	}

	/**
	 * Returns the commit actions for the given {@code session}.
	 */
	private CommitActions getCommitActions(ClientSessionImpl session) {

	    CommitActions actions = commitActions.get(session);
	    if (actions == null) {
		actions = new CommitActions(session);
		commitActions.put(session, actions);
	    }
	    return actions;
	}
	
	/**
	 * Throws a {@code TransactionNotActiveException} if this
	 * transaction is prepared.
	 */
	private void checkPrepared() {
	    if (isPrepared) {
		throw new TransactionNotActiveException("Already prepared");
	    }
	}
	
	/**
	 * Marks this transaction as prepared, and if there are
	 * pending changes, adds this context to the context queue and
	 * returns {@code false}.  Otherwise, if there are no pending
	 * changes returns {@code true} indicating readonly status.
	 */
        public boolean prepare() {
	    isPrepared = true;
	    boolean readOnly = commitActions.isEmpty();
	    if (!readOnly) {
		contextQueue.add(this);
	    } else {
		isCommitted = true;
	    }
            return readOnly;
        }

	/**
	 * Removes the context from the context queue containing
	 * pending actions, and checks for flushing committed contexts.
	 */
	public void abort(boolean retryable) {
	    contextQueue.remove(this);
	    checkFlush();
	}

	/**
	 * Marks this transaction as committed, and checks for
	 * flushing committed contexts.
	 */
	public void commit() {
	    isCommitted = true;
	    checkFlush();
        }

	/**
	 * Wakes up the thread to process committed contexts in the
	 * context queue if the queue is non-empty and the first
	 * context in the queue is committed.
	 */
	private void checkFlush() {
	    Context context = contextQueue.peek();
	    if ((context != null) && (context.isCommitted)) {
		synchronized (flushContextsLock) {
		    flushContextsLock.notifyAll();
		}
	    }
	}
	
	/**
	 * Sends all protocol messages enqueued during this context's
	 * transaction (via the {@code addMessage} and {@code
	 * addMessageFirst} methods), and disconnects any session
	 * whose disconnection was requested via the {@code
	 * requestDisconnect} method.
	 */
	private boolean flush() {
	    if (shuttingDown()) {
		return false;
	    } else if (isCommitted) {
		for (CommitActions actions : commitActions.values()) {
		    actions.flush();
		}
		return true;
	    } else {
		return false;
	    }
	}
    }
    
    /**
     * Contains pending changes for a given client session.
     */
    private class CommitActions {

	/** The client session ID as a BigInteger. */
	private final BigInteger sessionRefId;

	/** The login ack protocol message, or null. */
	private byte[] loginAck = null;

	/** The login outcome, only valid if {@code loginAck} is non-null. */
	private boolean loginSuccess = false;
	
	/** List of protocol messages to send on commit. */
	private List<byte[]> messages = new ArrayList<byte[]>();

	/** If true, disconnect after sending messages. */
	private boolean disconnect = false;

	CommitActions(ClientSessionImpl sessionImpl) {
	    if (sessionImpl == null) {
		throw new NullPointerException("null sessionImpl");
	    } 
	    this.sessionRefId = sessionImpl.getId();
	}

	void addMessage(byte[] message) {
	    messages.add(message);
	}

	void addLoginAck(byte[] message, boolean success) {
	    loginAck = message;
	    loginSuccess = success;
	}
	
	void setDisconnect() {
	    disconnect = true;
	}

	void flush() {
	    sendMessages();
	    if (disconnect) {
		ClientSessionHandler handler = handlers.get(sessionRefId);
		/*
		 * If session is local, disconnect session; otherwise, log
		 * error message. 
		 */
		if (handler != null) {
		    handler.handleDisconnect(false, true);
		} else {
		    logger.log(
		        Level.FINE,
			"discarding request to disconnect unknown session:{0}",
			sessionRefId);
		}
	    }
	}

	void sendMessages() {

	    ClientSessionHandler handler = handlers.get(sessionRefId);
	    /*
	     * If a local handler exists, forward messages to local
	     * handler to send to client session; otherwise log
	     * error message.
	     */
	    if (handler != null && handler.isConnected()) {
		if (loginAck != null) {
		    handler.sendLoginProtocolMessage(
			loginAck, Delivery.RELIABLE, loginSuccess);
		    if (!loginSuccess) {
			return;
		    }
		}
		for (byte[] message : messages) {
		    handler.sendProtocolMessage(message, Delivery.RELIABLE);
		}
	    } else {
		logger.log(
		    Level.FINE,
		    "Discarding messages for disconnected session:{0}",
		    handler);
	    }
	}
    }

    /**
     * Thread to process the context queue, in order, to flush any
     * committed changes.
     */
    private class FlushContextsThread extends Thread {

	/**
	 * Constructs an instance of this class as a daemon thread.
	 */
	public FlushContextsThread() {
	    super(CLASSNAME + "$FlushContextsThread");
	    setDaemon(true);
	}
	
	/**
	 * Processes the context queue, in order, to flush any
	 * committed changes.  This thread waits to be notified that a
	 * committed context is at the head of the queue, then
	 * iterates through the context queue invoking {@code flush}
	 * on the {@code Context} returned by {@code next}.  Iteration
	 * ceases when either a context's {@code flush} method returns
	 * {@code false} (indicating that the transaction associated
	 * with the context has not yet committed) or when there are
	 * no more contexts in the context queue.
	 */
	public void run() {
	    
	    while (true) {
		
		/*
		 * Wait for a non-empty context queue, returning if
		 * the service is shutting down.
		 */
		synchronized (flushContextsLock) {
		    if (contextQueue.isEmpty()) {
			if (shuttingDown()) {
			    return;
			}
			try {
			    flushContextsLock.wait();
			} catch (InterruptedException e) {
			    return;
			}
		    }
		}
		if (shuttingDown()) {
		    return;
		}

		/*
		 * Remove committed contexts from head of context
		 * queue, and enqueue them to be flushed.
		 */
		if (!contextQueue.isEmpty()) {
		    Iterator<Context> iter = contextQueue.iterator();
		    while (iter.hasNext()) {
			if (shuttingDown()) {
			    return;
			}
			Context context = iter.next();
			if (context.flush()) {
			    iter.remove();
			} else {
			    break;
			}
		    }
		}
	    }
	}
    }

    /* -- Implement ClientSessionServer -- */

    /**
     * Implements the {@code ClientSessionServer} that receives
     * requests from {@code ClientSessionService}s on other nodes to
     * forward messages to or disconnect local client sessions.
     */
    private class SessionServerImpl implements ClientSessionServer {

	/** {@inheritDoc} */
	public void serviceEventQueue(final byte[] sessionId) {
	    callStarted();
	    try {
		if (logger.isLoggable(Level.FINEST)) {
		    logger.log(Level.FINEST, "serviceEventQueue sessionId:{0}",
			       HexDumper.toHexString(sessionId));
		}

		BigInteger sessionRefId = new BigInteger(1, sessionId);
		TaskQueue taskQueue = sessionTaskQueues.get(sessionRefId);
		if (taskQueue == null) {
		    TaskQueue newTaskQueue =
			transactionScheduler.createTaskQueue();
		    taskQueue = sessionTaskQueues.
			putIfAbsent(sessionRefId, newTaskQueue);
		    if (taskQueue == null) {
			taskQueue = newTaskQueue;
		    }
		}
		taskQueue.addTask(
		  new AbstractKernelRunnable("ServiceEventQueue") {
		    public void run() {
			ClientSessionImpl.serviceEventQueue(sessionId);
		    } }, taskOwner);
	    } finally {
		callFinished();
	    }
	    
	}
    }
    
    /* -- Other methods -- */

    TransactionProxy getTransactionProxy() {
	return txnProxy;
    }

    /**
     * Returns the local node's ID.
     * @return	the local node's ID
     */
    long getLocalNodeId() {
	return localNodeId;
    }
    
    /**
     * Returns the key for accessing the {@code ClientSessionServer}
     * instance (which is wrapped in a {@code ManagedSerializable})
     * for the specified {@code nodeId}.
     */
    private static String getClientSessionServerKey(long nodeId) {
	return PKG_NAME + ".server." + nodeId;
    }
    
    /**
     * Checks if the local node is considered alive, and throws an
     * {@code IllegalStateException} if the node is no longer alive.
     * This method should be called within a transaction.
     */
    private void checkLocalNodeAlive() {
	if (!watchdogService.isLocalNodeAlive()) {
	    throw new IllegalStateException(
		"local node is not considered alive");
	}
    }

   /**
     * Obtains information associated with the current transaction,
     * throwing TransactionNotActiveException if there is no current
     * transaction, and throwing IllegalStateException if there is a
     * problem with the state of the transaction or if this service
     * has not been initialized with a transaction proxy.
     */
    Context checkContext() {
	checkLocalNodeAlive();
	return contextFactory.joinTransaction();
    }

    /**
     * Returns the client session service relevant to the current
     * context.
     *
     * @return the client session service relevant to the current
     * context
     */
    static synchronized ClientSessionServiceImpl getInstance() {
	if (txnProxy == null) {
	    throw new IllegalStateException("Service not initialized");
	} else {
	    return (ClientSessionServiceImpl)
		txnProxy.getService(ClientSessionService.class);
	}
    }

    /**
     * Validates the {@code identity} of the user logging in and returns
     * {@code true} if the login is allowed to proceed, and {@code false}
     * if the login is denied.
     *
     * <p>A user with the specified {@code identity} is allowed to log in
     * if one of the following conditions holds:
     *
     * <ul>
     * <li>the {@code identity} is not currently logged in, or
     * <li>the {@code identity} is logged in, and the {@code
     * com.sun.sgs.impl.service.session.allow.new.login} property is
     * set to {@code true}.
     * </ul>
     * In the latter case (new login allowed), the existing user session logged
     * in with {@code identity} is forcibly disconnected.
     *
     * <p>If this method returns {@code true}, the {@link #removeUserLogin}
     * method must be invoked when the user with the specified {@code
     * identity} is disconnected.
     *
     * @param	identity the user identity
     * @param	handler the client session handler
     * @return	{@code true} if the user is allowed to log in with the
     * specified {@code identity}, otherwise returns {@code false}
     */
    boolean validateUserLogin(Identity identity, ClientSessionHandler handler) {
	ClientSessionHandler previousHandler =
	    loggedInIdentityMap.putIfAbsent(identity, handler);
	if (previousHandler == null) {
	    // No user logged in with the same idenity; allow login.
	    return true;
	} else if (!allowNewLogin) {
	    // Same user logged in; new login not allowed, so deny login.
	    return false;
	} else if (!previousHandler.loginHandled()) {
	    // Same user logged in; can't preempt user in the
	    // process of logging in; deny login.
	    return false;
	} else {
	    if (loggedInIdentityMap.replace(
		    identity, previousHandler, handler)) {
		// Disconnect current user; allow new login.
		previousHandler.handleDisconnect(false, true);
		return true;
	    } else {
		// Another same user login beat this one; deny login.	
		return false;
	    }
	}
    }

    /**
     * Notifies this service that the specified {@code identity} is no
     * longer logged in using the specified {@code handler} so that
     * internal bookkeeping can be adjusted accordingly.
     *
     * @param	identity the user identity
     * @param	handler the client session handler
     */
    boolean removeUserLogin(Identity identity, ClientSessionHandler handler) {
	return loggedInIdentityMap.remove(identity, handler);
    }
    
    /**
     * Adds the handler for the specified session to the internal
     * session handler map.  This method is invoked by the handler once the
     * client has successfully logged in.
     */
    void addHandler(BigInteger sessionRefId, ClientSessionHandler handler) {
	handlers.put(sessionRefId, handler);
    }
    
    /**
     * Removes the specified session from the internal session  handler
     * map.  This method is invoked by the handler when the session becomes
     * disconnected.
     */
    void removeHandler(BigInteger sessionRefId) {
	if (shuttingDown()) {
	    return;
	}
	// Notify session listeners of disconnection
	for (ClientSessionDisconnectListener disconnectListener :
		 sessionDisconnectListeners)
	{
	    disconnectListener.disconnected(sessionRefId);
	}
	handlers.remove(sessionRefId);
	sessionTaskQueues.remove(sessionRefId);
    }

    /**
     * Adds the specified {@code handler} to the map containing {@code
     * ClientSessionHandler}s that are disconnecting.  The map is keyed by
     * connection expiration time.  The connection will expire after a fixed
     * delay and will be forcibly terminated if the client hasn't already
     * closed the connection.
     *
     * @param	handler a {@code ClientSessionHandler} for a disconnecting
     *		{@code ClientSession}
     */
    void monitorDisconnection(ClientSessionHandler handler) {
	disconnectingHandlersMap.put(
	    System.currentTimeMillis() + disconnectDelay,  handler);
    }

    /**
     * Schedules a non-durable, transactional task using the given
     * {@code Identity} as the owner.
     */
    void scheduleTask(KernelRunnable task, Identity ownerIdentity) {
	if (ownerIdentity == null) {
	    throw new NullPointerException("Owner identity cannot be null");
	}
        transactionScheduler.scheduleTask(task, ownerIdentity);
    }

    /**
     * Schedules a non-durable, non-transactional task using the given
     * {@code Identity} as the owner.
     */
    void scheduleNonTransactionalTask(
	KernelRunnable task, Identity ownerIdentity)
    {
	// TBD: this check is done because there are known cases where the
	// identity can be null, but when the Handler code changes to ensure
	// that the identity is always valid, this check can be removed
	Identity owner = (ownerIdentity == null ? taskOwner : ownerIdentity);
        taskScheduler.scheduleTask(task, owner);
    }

    /**
     * Schedules a non-durable, transactional task using the task service.
     */
    void scheduleTaskOnCommit(KernelRunnable task) {
        taskService.scheduleNonDurableTask(task, true);
    }

    /**
     * Runs the specified {@code task} immediately, in a transaction.
     */
    void runTransactionalTask(KernelRunnable task, Identity ownerIdentity)
	throws Exception
    {
	if (ownerIdentity == null) {
	    throw new NullPointerException("Owner identity cannot be null");
	}
	transactionScheduler.runTask(task, ownerIdentity);
    }
    
    /**
     * Returns the task service.
     */
    static TaskService getTaskService() {
	return txnProxy.getService(TaskService.class);
    }

    /**
     * Returns the channel service.
     */
    ChannelServiceImpl getChannelService() {
	return channelService;
    }

    /**
     * A task to monitor disconnecting {@code ClientSessionHandler}s to ensure
     * that their associated connections are closed by the client in a
     * timely manner.  If a connection is not terminated by the expiration
     * time, then the connection is forcibly closed.
     */
    private class MonitorDisconnectingSessionsTask
	extends AbstractKernelRunnable
    {
	/** Constructs and instance. */
	MonitorDisconnectingSessionsTask() {
	    super(null);
	}
	
	/** {@inheritDoc} */
	public void run() {
	    long now = System.currentTimeMillis();
	    if (!disconnectingHandlersMap.isEmpty() &&
		disconnectingHandlersMap.firstKey() < now) {

		Map<Long, ClientSessionHandler> expiredSessions = 
		    disconnectingHandlersMap.headMap(now);
		for (ClientSessionHandler handler : expiredSessions.values()) {
		    handler.closeConnection();
		}
		expiredSessions.clear();
	    }
	}
    }

    /**
     * The {@code RecoveryListener} for handling requests to recover
     * for a failed {@code ClientSessionService}.
     */
    private class ClientSessionServiceRecoveryListener
	implements RecoveryListener
    {
	/** {@inheritDoc} */
	public void recover(final Node node, RecoveryCompleteFuture future) {
	    final long nodeId = node.getId();
	    final TaskService taskService = getTaskService();
	    
	    try {
		if (logger.isLoggable(Level.INFO)) {
		    logger.log(Level.INFO, "Node:{0} recovering for node:{1}",
			       localNodeId, nodeId);
		}

		/*
		 * Schedule persistent tasks to perform recovery.
		 */
		transactionScheduler.runTask(
		    new AbstractKernelRunnable("ScheduleRecoveryTasks") {
			public void run() {
			    /*
			     * For each session on the failed node, notify
			     * the session's ClientSessionListener and
			     * clean up the session's persistent data and
			     * bindings. 
			     */
			    taskService.scheduleTask(
				new HandleNextDisconnectedSessionTask(nodeId));
				
			    /*
			     * Remove client session server proxy and
			     * associated binding for failed node.
			     */
			    taskService.scheduleTask(
				new RemoveClientSessionServerProxyTask(nodeId));
			} },
		    taskOwner);
					     
		future.done();
		    
	    } catch (Exception e) {
		logger.logThrow(
 		    Level.WARNING, e,
		    "Node:{0} recovering for node:{1} throws",
		    localNodeId, nodeId);
		// TBD: what should it do if it can't recover?
	    }
	}
    }

    /**
     * A persistent task to remove the client session server proxy for a
     * specified node.
     */
    private static class RemoveClientSessionServerProxyTask
	 implements Task, Serializable
    {
	/** The serialVersionUID for this class. */
	private static final long serialVersionUID = 1L;

	/** The node ID. */
	private final long nodeId;

	/**
	 * Constructs an instance of this class with the specified
	 * {@code nodeId}.
	 */
	RemoveClientSessionServerProxyTask(long nodeId) {
	    this.nodeId = nodeId;
	}

	/**
	 * Removes the client session server proxy and binding for the node
	 * specified during construction.
	 */
	public void run() {
	    String sessionServerKey = getClientSessionServerKey(nodeId);
	    DataService dataService = getDataService();
	    try {
		dataService.removeObject(
		    dataService.getServiceBinding(sessionServerKey));
	    } catch (NameNotBoundException e) {
		// already removed
		return;
	    } catch (ObjectNotFoundException e) {
	    }
	    dataService.removeServiceBinding(sessionServerKey);
	}
    }
}

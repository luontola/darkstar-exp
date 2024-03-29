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

package com.sun.sgs.test.impl.service.task;

import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.service.Node;
import com.sun.sgs.service.NodeListener;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.WatchdogService;
import com.sun.sgs.service.RecoveryListener;

import java.util.Iterator;
import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.concurrent.atomic.AtomicLong;


/**
 * An in-memory testing implementation of WatchdogService. Note that
 * this class is not designed to scale, simply to back the
 * DummyNodeMappingService.
 */
public class DummyWatchdogService implements WatchdogService {

    // the generator for all node identifiers
    private final static AtomicLong idGenerator = new AtomicLong(0);
    // the map from node identifier to Node instance
    private static ConcurrentHashMap<Long,Node> nodeMap;
    // the collection of listeners
    private static ConcurrentLinkedQueue<NodeListener> listeners;

    // a node-local copy of the node's identifier
    private final long localId;
    // a node-local indicator of whether the node is alive
    private boolean isAlive = true;

    /** Creates an instance of the service. */
    public DummyWatchdogService(Properties p, ComponentRegistry cr,
                                TransactionProxy tp) {
        if (p.getProperty("DummyServer", "false").equals("true")) {
            nodeMap = new ConcurrentHashMap<Long,Node>();
            listeners = new ConcurrentLinkedQueue<NodeListener>();
        }
        localId = idGenerator.getAndIncrement();
        nodeMap.put(localId, new NodeImpl(localId));
    }

    /** {@inheritDoc} */
    public String getName() {
        return getClass().getName();
    }

    /** {@inheritDoc} */
    public void ready() {
        Node node = nodeMap.get(localId);
        for (NodeListener listener : listeners)
            listener.nodeStarted(node);
    }

    /** {@inheritDoc} */
    public boolean shutdown() {
        isAlive = false;
        nodeMap.remove(localId);
        Node localNode = new NodeImpl(localId);
        for (NodeListener listener : listeners)
            listener.nodeFailed(localNode);
        return true;
    }

    /** {@inheritDoc} */
    public long getLocalNodeId() {
        return localId;
    }

    /** {@inheritDoc} */
    public boolean isLocalNodeAlive() {
        return isAlive;
    }

    /** {@inheritDoc} */
    public boolean isLocalNodeAliveNonTransactional() {
        return isLocalNodeAlive();
    }

    /** {@inheritDoc} */
    public Iterator<Node> getNodes() {
        return nodeMap.values().iterator();
    }

    /** {@inheritDoc} */
    public Node getNode(long nodeId) {
        Node node = nodeMap.get(nodeId);
        if (node == null)
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        return node;
    }

    /** {@inheritDoc} */
    public void addNodeListener(NodeListener listener) {
        listeners.add(listener);
    }

    /** {@inheritDoc}
     * <p>
     *  This method is not implemented, and will throw an AssertionError.
     */
    public Node getBackup(long nodeId) {
	throw new AssertionError("not implemented");
    }
       
    /** {@inheritDoc} 
     * <p> 
     * This implementation does nothing.
     */
    public void addRecoveryListener(RecoveryListener listener) {
	// Silently do nothing.
    }

    /** A basic, private implementation of Node. */
    private class NodeImpl implements Node {
        private final long nodeId;
        NodeImpl(long nodeId) {
            this.nodeId = nodeId;
        }
        public long getId() {
            return nodeId;
        }
        public String getHostName() {
            return "localhost";
        }
        public int getPort() {
            return 20000;
        }
        public boolean isAlive() {
            return isLocalNodeAlive();
        }
    }

}

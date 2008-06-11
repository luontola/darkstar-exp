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

package com.sun.sgs.test.impl.service.nodemap;

import com.sun.sgs.auth.Identity;
import com.sun.sgs.impl.auth.IdentityImpl;
import com.sun.sgs.impl.kernel.StandardProperties;
import com.sun.sgs.impl.service.nodemap.NodeMappingServerImpl;
import com.sun.sgs.impl.service.nodemap.NodeMappingServiceImpl;
import com.sun.sgs.impl.util.AbstractKernelRunnable;
import com.sun.sgs.impl.util.AbstractService.Version;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.service.DataService;
import com.sun.sgs.service.Node;
import com.sun.sgs.service.NodeMappingListener;
import com.sun.sgs.service.NodeMappingService;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.UnknownIdentityException;
import com.sun.sgs.service.UnknownNodeException;
import com.sun.sgs.service.WatchdogService;
import com.sun.sgs.test.util.SgsTestNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import junit.framework.TestCase;

public class TestNodeMappingServiceImpl extends TestCase {

    /** Number of additional nodes to create for selected tests */
    private static final int NUM_NODES = 3;
    
    /** Reflective stuff */
    private static Method assertValidMethod;
    private static Field localNodeIdField;
    private static String VERSION_KEY;
    private static int MAJOR_VERSION;
    private static int MINOR_VERSION;
    static {
        try {
            localNodeIdField = 
                NodeMappingServiceImpl.class.getDeclaredField("localNodeId");
            localNodeIdField.setAccessible(true);

            assertValidMethod =
                    NodeMappingServiceImpl.class.getDeclaredMethod(
                        "assertValid", Identity.class);
            assertValidMethod.setAccessible(true);
            
            Class nodeMapUtilClass = 
                Class.forName("com.sun.sgs.impl.service.nodemap.NodeMapUtil");
            
            VERSION_KEY = (String) 
                    getField(nodeMapUtilClass, "VERSION_KEY").get(null);
            MAJOR_VERSION = 
                    getField(nodeMapUtilClass, "MAJOR_VERSION").getInt(null);
            MINOR_VERSION =
                    getField(nodeMapUtilClass, "MINOR_VERSION").getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** The node that creates the servers */
    private SgsTestNode serverNode;
    /** Any additional nodes, for tests needing more than one node */
    private SgsTestNode additionalNodes[];
    
    private TransactionProxy txnProxy;
    private ComponentRegistry systemRegistry;
    private Properties serviceProps;
    
    /** A specific property we started with, for remove tests */
    private int removeTime;
    
    /** The transaction scheduler. */
    private TransactionScheduler txnScheduler;
    
    /** The owner for tasks I initiate. */
    private Identity taskOwner;
    
    private NodeMappingService nodeMappingService;
    
    /** A mapping of node id ->NodeMappingListener, for listener checks */
    private Map<Long, TestListener> nodeListenerMap;
 
    private static Field getField(Class cl, String name) throws Exception {
	Field field = cl.getDeclaredField(name);
	field.setAccessible(true);
	return field;
    }
    
    /** Constructs a test instance. */
    public TestNodeMappingServiceImpl(String name) throws Exception {
        super(name);
    }
    
    /** Test setup. */
    protected void setUp() throws Exception {
        System.err.println("Testcase: " + getName());
        setUp(null);
    }

    protected void setUp(Properties props) throws Exception {
        nodeListenerMap = new HashMap<Long, TestListener>();
        
        serverNode = new SgsTestNode("TestNodeMappingServiceImpl", null, props);
        txnProxy = serverNode.getProxy();
        systemRegistry = serverNode.getSystemRegistry();
        serviceProps = serverNode.getServiceProperties();
        removeTime = Integer.valueOf(
            serviceProps.getProperty(
                "com.sun.sgs.impl.service.nodemap.remove.expire.time"));
        
        txnScheduler = systemRegistry.getComponent(TransactionScheduler.class);
        taskOwner = txnProxy.getCurrentOwner();
        
        nodeMappingService = serverNode.getNodeMappingService();
        
        // Add to our test data structures, so we can find these nodes
        // and listeners.
        Long id = (Long) localNodeIdField.get(nodeMappingService);

        TestListener listener = new TestListener();        
        nodeMappingService.addNodeMappingListener(listener);
        nodeListenerMap.put(id, listener);
    }
    
   
    /** 
     * Add additional nodes.  We only do this as required by the tests. 
     *
     * @param props properties for node creation, or {@code null} if default
     *     properties should be used
     */
    private void addNodes(Properties props) throws Exception {
        // Create the other nodes
        additionalNodes = new SgsTestNode[NUM_NODES];
        
        for (int i = 0; i < NUM_NODES; i++) {
            SgsTestNode node =  new SgsTestNode(serverNode, null, props);
            additionalNodes[i] = node;
        
            NodeMappingService nmap = node.getNodeMappingService();

            // Add to our test data structures, so we can find these nodes
            // and listeners.
            Long id = (Long) localNodeIdField.get(nmap);

            TestListener listener = new TestListener();        
            nmap.addNodeMappingListener(listener);
            nodeListenerMap.put(id, listener);
        }
    }
        
    /** Shut down the nodes. */
    protected void tearDown() throws Exception {
        if (additionalNodes != null) {
            for (SgsTestNode node : additionalNodes) {
                node.shutdown(false);
            }
            additionalNodes = null;
        }
        serverNode.shutdown(true);
    }

    
        ////////     The tests     /////////
    public void testConstructor() {
        NodeMappingService nodemap = null;
        try {
            nodemap = 
                new NodeMappingServiceImpl(
                            serviceProps, systemRegistry, txnProxy);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (nodemap != null) { nodemap.shutdown(); }
        }
    }

    public void testConstructorNullProperties() throws Exception {
        NodeMappingService nodemap = null;
        try {
            nodemap = 
                new NodeMappingServiceImpl(null, systemRegistry, txnProxy);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            System.err.println(e);
        } finally {
            if (nodemap != null) { nodemap.shutdown(); }
        }
    }
    
    public void testConstructorNullProxy() throws Exception {
        NodeMappingService nodemap = null;
        try {
            nodemap = 
              new NodeMappingServiceImpl(serviceProps, systemRegistry, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            System.err.println(e);
        } finally {
            if (nodemap != null) { nodemap.shutdown(); }
        }
    }
    
    public void testConstructorAppButNoServerHost() throws Exception {
        // Server start is false but we didn't specify a server host
        Properties props = 
                SgsTestNode.getDefaultProperties(
                    "TestNodeMappingServiceImpl", 
                    serverNode, 
                    SgsTestNode.DummyAppListener.class);
        props.remove(StandardProperties.SERVER_HOST);
	
        try {
            NodeMappingService nmap =
                new NodeMappingServiceImpl(props, systemRegistry, txnProxy);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }
    
    public void testConstructedVersion() throws Exception {
	txnScheduler.runTask(new AbstractKernelRunnable() {
		public void run() {
		    Version version = (Version)
			serverNode.getDataService()
                        .getServiceBinding(VERSION_KEY);
		    if (version.getMajorVersion() != MAJOR_VERSION ||
			version.getMinorVersion() != MINOR_VERSION)
		    {
			fail("Expected service version (major=" +
			     MAJOR_VERSION + ", minor=" + MINOR_VERSION +
			     "), got:" + version);
		    }
		}}, taskOwner);
    }
    
    public void testConstructorWithCurrentVersion() throws Exception {
	txnScheduler.runTask(new AbstractKernelRunnable() {
		public void run() {
		    Version version = new Version(MAJOR_VERSION, MINOR_VERSION);
		    serverNode.getDataService()
                              .setServiceBinding(VERSION_KEY, version);
		}}, taskOwner);

	new NodeMappingServiceImpl(serviceProps, systemRegistry, txnProxy);  
    }

    public void testConstructorWithMajorVersionMismatch() throws Exception {
	txnScheduler.runTask(new AbstractKernelRunnable() {
		public void run() {
		    Version version =
			new Version(MAJOR_VERSION + 1, MINOR_VERSION);
		    serverNode.getDataService()
                              .setServiceBinding(VERSION_KEY, version);
		}}, taskOwner);

	try {
	    new NodeMappingServiceImpl(serviceProps, systemRegistry, txnProxy);  
	    fail("Expected IllegalStateException");
	} catch (IllegalStateException e) {
	    System.err.println(e);
	}
    }

    public void testConstructorWithMinorVersionMismatch() throws Exception {
	txnScheduler.runTask(new AbstractKernelRunnable() {
		public void run() {
		    Version version =
			new Version(MAJOR_VERSION, MINOR_VERSION + 1);
		    serverNode.getDataService()
                              .setServiceBinding(VERSION_KEY, version);
		}}, taskOwner);

	try {
	    new NodeMappingServiceImpl(serviceProps, systemRegistry, txnProxy);  
	    fail("Expected IllegalStateException");
	} catch (IllegalStateException e) {
	    System.err.println(e);
	}
    }
    
    public void testReady() throws Exception {
        NodeMappingService nodemap = null;
        try {
            nodemap = 
                new NodeMappingServiceImpl(
                            serviceProps, systemRegistry, txnProxy);
            TestListener listener = new TestListener();        
            nodemap.addNodeMappingListener(listener);
            
            // We have NOT called ready yet.
            final Identity id = new IdentityImpl("first");
            nodemap.assignNode(NodeMappingService.class, id);
            
            txnScheduler.runTask(
                new AbstractKernelRunnable() {
                    public void run() throws Exception {
                        nodeMappingService.getNode(id);
                    }
                }, taskOwner);
            
            // Ensure the listeners have not been called yet.
            List<Identity> addedIds = listener.getAddedIds();
            List<Node> addedNodes = listener.getAddedNodes();
            assertEquals(0, addedIds.size());
            assertEquals(0, addedNodes.size());
            assertEquals(0, listener.getRemovedIds().size());
            assertEquals(0, listener.getRemovedNodes().size());
            
            nodemap.ready();
            
            // Listeners should be notified.
            Thread.sleep(500);
            
            addedIds = listener.getAddedIds();
            addedNodes = listener.getAddedNodes();
            assertEquals(1, addedIds.size());
            assertEquals(1, addedNodes.size());
            assertTrue(addedIds.contains(id));
            // no old node
            assertTrue(addedNodes.contains(null));

            assertEquals(0, listener.getRemovedIds().size());
            assertEquals(0, listener.getRemovedNodes().size());
            
        } finally {
            if (nodemap != null) { nodemap.shutdown(); }
        }
    }
    
    /* -- Test Service -- */
    public void testGetName() {
        System.out.println(nodeMappingService.getName());
    }
    
    /* -- Test assignNode -- */
    public void testAssignNode() throws Exception {   
        // Assign outside a transaction
        final Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
                
        verifyMapCorrect(id);
       
        // Now expect to be able to find the identity
        txnScheduler.runTask(
            new AbstractKernelRunnable() {
                public void run() throws Exception {
                    Node node = nodeMappingService.getNode(id);
                    // Make sure we got a notification
                    TestListener listener = nodeListenerMap.get(node.getId());
                    List<Identity> addedIds = listener.getAddedIds();
                    List<Node> addedNodes = listener.getAddedNodes();
                    assertEquals(1, addedIds.size());
                    assertEquals(1, addedNodes.size());
                    assertTrue(addedIds.contains(id));
                    // no old node
                    assertTrue(addedNodes.contains(null));

                    assertEquals(0, listener.getRemovedIds().size());
                    assertEquals(0, listener.getRemovedNodes().size());
                }
        }, taskOwner);
    }
    
    public void testAssignNodeNullServer() throws Exception {
        try {
            nodeMappingService.assignNode(null, new IdentityImpl("first"));
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            System.err.println(ex);  
        } 
    }
    
    public void testAssignNodeNullIdentity() throws Exception {
        try {
            nodeMappingService.assignNode(NodeMappingService.class, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            System.err.println(ex);  
        } 
    }
    
    public void testAssignNodeTwice() throws Exception {
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        
        // Now expect to be able to find the identity
        GetNodeTask task1 = new GetNodeTask(id);
        txnScheduler.runTask(task1, taskOwner);
        Node node1 = task1.getNode();
        
        // There shouldn't be a problem if we assign it twice;  as an 
        // optimization we shouldn't call out to the server
        nodeMappingService.assignNode(NodeMappingService.class, id);
        verifyMapCorrect(id);
        
        // Now expect to be able to find the identity
        GetNodeTask task2 = new GetNodeTask(id);
        txnScheduler.runTask(task2, taskOwner);
        Node node2 = task2.getNode();
        assertEquals(node1, node2);
    }
    

    public void testAssignMultNodes() throws Exception {
        // This test is partly so I can compare the time it takes to
        // assign one node, or the same node twice
        addNodes(null);
        
        final int MAX = 25;
        Identity ids[] = new Identity[MAX];
        for (int i = 0; i < MAX; i++) {
            ids[i] = new IdentityImpl("identity" + i);         
            nodeMappingService.assignNode(NodeMappingService.class, ids[i]);
                
            verifyMapCorrect(ids[i]);
        }

        for (int j = 0; j < MAX; j++) {
            final Identity id = ids[j];
            txnScheduler.runTask(
                new AbstractKernelRunnable() {
                    public void run() throws Exception {
                        nodeMappingService.getNode(id);
                    }
            }, taskOwner);
        }
    }
    
    public void testRoundRobinAutoMove() throws Exception {
        // Remove what happened at setup().  I know, I know...
        tearDown();
        
        final int MOVE_COUNT = 5;
        // Create a new nodeMappingServer which will move an identity
        // automatically every so often.  
        serviceProps.setProperty(
                "com.sun.sgs.impl.service.nodemap.policy.movecount", 
                String.valueOf(MOVE_COUNT));

        setUp(serviceProps);
        addNodes(null);

        final List<Identity> ids = new ArrayList<Identity>();
        final List<Node> assignments = new ArrayList<Node>();
        
        final WatchdogService watchdog = serverNode.getWatchdogService();
        // First, Gather up any ids assigned by the other services
        // The set of nodes the watchdog knows about
        final Set<Node> nodes = new HashSet<Node>();
        
        // Gather up the nodes
        txnScheduler.runTask(
            new AbstractKernelRunnable() {
                public void run() throws Exception {
                    Iterator<Node> iter = watchdog.getNodes();
                    while (iter.hasNext()) {
                        nodes.add(iter.next());
                    }       

                }
        }, taskOwner);
        
        // For each node, gather up the identities
        for (final Node node : nodes) {
        txnScheduler.runTask(
            new AbstractKernelRunnable() {
                public void run() throws Exception {
                    Iterator<Identity> idIter = 
                        nodeMappingService.getIdentities(node.getId());
                    while (idIter.hasNext()) {
                        Identity id = idIter.next();
                        ids.add(id);
                        assignments.add(nodeMappingService.getNode(id));
                    }    
                }
            }, taskOwner);
        }
        
        // Now start adding our identities.  The round robin policy
        // should cause a random identity to move while we do this.
        for (int i = 0; i < MOVE_COUNT; i++) {
            Identity id = new IdentityImpl("identity" + i);
            ids.add(id);
            nodeMappingService.assignNode(DataService.class, id);
            verifyMapCorrect(id);

            GetNodeTask task = new GetNodeTask(id);
            txnScheduler.runTask(task, taskOwner);
            assignments.add(task.getNode());
        }

        // We expected an automatic move to have occurred.
        boolean foundDiff = false;
        final int size = ids.size();
        for (int i = 0; i < size; i++) {
            GetNodeTask task = new GetNodeTask(ids.get(i));
            txnScheduler.runTask(task, taskOwner);
            Node current = task.getNode();
            foundDiff = foundDiff || 
                        (current.getId() != assignments.get(i).getId());
        }

        assertTrue("expected an id to move", foundDiff);
     }
    
    public void testAssignNodeInTransaction() throws Exception {
        // TODO should API specify a transaction exception will be thrown?
        txnScheduler.runTask(new AbstractKernelRunnable() {
            public void run() {
                nodeMappingService.assignNode(NodeMappingService.class, new IdentityImpl("first"));
            }
        }, taskOwner);
    }
    
    /* -- Test getNode -- */
    public void testGetNodeNullIdentity() throws Exception {
        try {
            txnScheduler.runTask(
                    new AbstractKernelRunnable() {
                        public void run() throws Exception {
                            nodeMappingService.getNode(null);
                        }
                }, taskOwner);
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            System.err.println(ex);  
        }
    } 
    
    public void testGetNodeBadIdentity() throws Exception {
        try {
            txnScheduler.runTask(
                    new AbstractKernelRunnable() {
                        public void run() throws Exception {
                            nodeMappingService.getNode(new IdentityImpl("first"));
                        }
                }, taskOwner);
            fail("Expected UnknownIdentityException");
        } catch (UnknownIdentityException ex) {
            System.err.println(ex);
        }
    }
   
    public void testGetNode() {
        final Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        try {
            txnScheduler.runTask(
                    new AbstractKernelRunnable() {
                        public void run() throws Exception {
                            nodeMappingService.getNode(id);
                        }
                }, taskOwner);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception");
        }
    }
    

    // Check to see if identities are changing in a transaction
    // and that any caching of identities in transaction works.
    public void testGetNodeMultiple() throws Exception {
        // A better test would have another thread racing to change
        // the identity.
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        
        GetNodeTask task = new GetNodeTask(id);
        txnScheduler.runTask(task, taskOwner);
        Node node1 = task.getNode();
        txnScheduler.runTask(task, taskOwner);
        Node node2 = task.getNode();
        txnScheduler.runTask(task, taskOwner);
        Node node3 = task.getNode();
        assertEquals(node1, node2);
        assertEquals(node1, node3);
        assertEquals(node2, node3);
    }
    
    /*-- Test getIdentities --*/
    
    public void testGetIdentitiesBadNode() throws Exception {
        try {
            txnScheduler.runTask(
                    new AbstractKernelRunnable() {
                        public void run() throws Exception {
                            nodeMappingService.getIdentities(999L);
                        }
                }, taskOwner);
            fail("Expected UnknownNodeException");
        } catch (UnknownNodeException ex) {
            System.err.println(ex);
        }
    }
   
    public void testGetIdentities() throws Exception {
        final Identity id1 = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id1);

        txnScheduler.runTask(
            new AbstractKernelRunnable() {
                public void run() throws Exception {
                    Node node = nodeMappingService.getNode(id1);
		    Set<Identity> foundSet = new HashSet<Identity>();
                    Iterator<Identity> ids = 
                        nodeMappingService.getIdentities(node.getId());
                    while (ids.hasNext()) {
                        foundSet.add(ids.next());
		    }
		    assertTrue(foundSet.contains(id1));
                }
        }, taskOwner);
    }
    
    public void testGetIdentitiesNoIds() throws Exception {
        addNodes(null);
        // This test assumes that we can create a node that has no
        // assignments.  That's currently true (Dec 11 2007).
        final long nodeId = additionalNodes[NUM_NODES - 1].getNodeId();

        txnScheduler.runTask(
            new AbstractKernelRunnable() {
                public void run() throws Exception {
                    Iterator<Identity> ids = 
                        nodeMappingService.getIdentities(nodeId);
                    while (ids.hasNext()) {
                        fail("expected no identities on this node " + 
                             ids.next());
                    }
                }
        }, taskOwner);
    }
    
    public void testGetIdentitiesMultiple() throws Exception {
        addNodes(null);
        
        final int MAX = 8;
        Identity ids[] = new Identity[MAX];
        for (int i = 0; i < MAX; i++ ) {
            ids[i] = new IdentityImpl("dummy" + i);
            nodeMappingService.assignNode(NodeMappingService.class, ids[i]);
        }
            
        Set<Node> nodeset = new HashSet<Node>();
        Node nodes[] = new Node[MAX];
          
        for (int j = 0; j < MAX; j++) {
            GetNodeTask task = new GetNodeTask(ids[j]);
            txnScheduler.runTask(task, taskOwner);
            Node n = task.getNode();
            nodes[j] = n;
            nodeset.add(n);
        }
        
        // Set up our own internal node map based on the info above
        Map<Node, Set<Identity>> nodemap = new HashMap<Node, Set<Identity>>();
        for (Node n : nodeset) {
            nodemap.put(n, new HashSet<Identity>());
        }
        for (int k = 0; k < MAX; k++) {
            Set<Identity> s = nodemap.get(nodes[k]);
            s.add(ids[k]);
        }
        
        for (final Node node : nodeset) {
            final Set s = nodemap.get(node);
            
            txnScheduler.runTask(new AbstractKernelRunnable(){
                public void run() throws Exception {
		    Set<Identity> foundSet = new HashSet<Identity>();
                    Iterator<Identity> idIter = 
                        nodeMappingService.getIdentities(node.getId());
                    while (idIter.hasNext()) {
                        foundSet.add(idIter.next());
		    }
		    assertTrue(foundSet.containsAll(s));
                }
            }, taskOwner);
        }
        
    }
    
    /* -- Test setStatus -- */
    public void testSetStatusNullService() throws Exception {
        try {
            nodeMappingService.setStatus(null, new IdentityImpl("first"), true);
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            System.err.println(ex);  
        }
    }
    
    public void testSetStatusNullIdentity() throws Exception {
        try {
            nodeMappingService.setStatus(NodeMappingService.class, null, true);
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            System.err.println(ex);  
        }
    }
    
    public void testSetStatusRemove() throws Exception {
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        GetNodeTask task = new GetNodeTask(id);
        txnScheduler.runTask(task, taskOwner);
        Node node = task.getNode();
        
        // clear out the listener
        TestListener listener = nodeListenerMap.get(node.getId());
        listener.clear();
        nodeMappingService.setStatus(NodeMappingService.class, id, false);
        Thread.sleep(removeTime * 4);
        
        try {
            txnScheduler.runTask(task, taskOwner);
            fail("Expected UnknownIdentityException");
        } catch (UnknownIdentityException e) {
            // Make sure we got a notification
            assertEquals(0, listener.getAddedIds().size());
            assertEquals(0, listener.getAddedNodes().size());
            
            List<Identity> removedIds = listener.getRemovedIds();
            List<Node> removedNodes = listener.getRemovedNodes();
            assertEquals(1, removedIds.size());
            assertEquals(1, removedNodes.size());
            assertTrue(removedIds.contains(id));
            // no new node
            assertTrue(removedNodes.contains(null));
        }
    }
    
    public void testSetStatusMultRemove() throws Exception {
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        GetNodeTask task = new GetNodeTask(id);
        txnScheduler.runTask(task, taskOwner);
        Node node = task.getNode();
        
        // clear out the listener
        TestListener listener = nodeListenerMap.get(node.getId());
        listener.clear();
        // SetStatus is idempotent:  it doesn't matter how often a particular
        // service says an id is active.
        nodeMappingService.setStatus(NodeMappingService.class, id, true);
        nodeMappingService.setStatus(NodeMappingService.class, id, true);
        // Likewise, it should be OK to make multiple "false" calls.
        nodeMappingService.setStatus(NodeMappingService.class, id, false);
        nodeMappingService.setStatus(NodeMappingService.class, id, false);
        Thread.sleep(removeTime * 4);
        
        try {
            txnScheduler.runTask(task, taskOwner);
            fail("Expected UnknownIdentityException");
        } catch (UnknownIdentityException e) {
            // Make sure we got a notification
            assertEquals(0, listener.getAddedIds().size());
            assertEquals(0, listener.getAddedNodes().size());
            
            List<Identity> removedIds = listener.getRemovedIds();
            List<Node> removedNodes = listener.getRemovedNodes();
            assertEquals(1, removedIds.size());
            assertEquals(1, removedNodes.size());
            assertTrue(removedIds.contains(id));
            // no new node
            assertTrue(removedNodes.contains(null));
        }
    }
        
    public void testSetStatusNoRemove() throws Exception {
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        GetNodeTask task = new GetNodeTask(id);
        try {
            txnScheduler.runTask(task, taskOwner);
        } catch (UnknownIdentityException e) {
            fail("Expected UnknownIdentityException");
        }
        
        nodeMappingService.setStatus(NodeMappingService.class, id, false);
        nodeMappingService.setStatus(NodeMappingService.class, id, true);
        Thread.sleep(removeTime * 4);
        // Error if we cannot find the identity!
        try {
            txnScheduler.runTask(task, taskOwner);
        } catch (UnknownIdentityException e) {
            fail("Unexpected UnknownIdentityException");
        }
    }
    
    /* -- Test private mapToNewNode -- */
    public void testListenersOnMove() throws Exception {   
        // We need some additional nodes for this test to work correctly.
        addNodes(null);
        
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);

        GetNodeTask task = new GetNodeTask(id);
        txnScheduler.runTask(task, taskOwner);
        Node firstNode = task.getNode();
        TestListener firstNodeListener = nodeListenerMap.get(firstNode.getId());
        
        // Get the method, as it's not public
        Field serverImplField = 
            NodeMappingServiceImpl.class.getDeclaredField("serverImpl");
        serverImplField.setAccessible(true);

        NodeMappingServerImpl server = 
            (NodeMappingServerImpl)serverImplField.get(nodeMappingService);

        Method moveMethod = 
                (NodeMappingServerImpl.class).getDeclaredMethod("mapToNewNode", 
                        new Class[]{Identity.class, String.class, Node.class});
        moveMethod.setAccessible(true);
        
        // clear out the listeners
        for (TestListener lis : nodeListenerMap.values()) {
            lis.clear();
        }
        // ... and invoke the method
        moveMethod.invoke(server, id, null, firstNode);
        
        txnScheduler.runTask(task, taskOwner);
        Node secondNode = task.getNode();
        TestListener secondNodeListener = 
                nodeListenerMap.get(secondNode.getId());
        
        // The id was removed from the first node
        assertEquals(0, firstNodeListener.getAddedIds().size());
        assertEquals(0, firstNodeListener.getAddedNodes().size());

        List<Identity> removedIds = firstNodeListener.getRemovedIds();
        List<Node> removedNodes = firstNodeListener.getRemovedNodes();
        assertEquals(1, removedIds.size());
        assertEquals(1, removedNodes.size());
        assertTrue(removedIds.contains(id));
        // It moved to secondNode
        assertTrue(removedNodes.contains(secondNode));
        
        // Check the other node's listener
        assertEquals(0, secondNodeListener.getRemovedIds().size());
        assertEquals(0, secondNodeListener.getRemovedNodes().size());
        
        List<Identity> addedIds = secondNodeListener.getAddedIds();
        List<Node> addedNodes = secondNodeListener.getAddedNodes();
        assertEquals(1, addedIds.size());
        assertEquals(1, addedNodes.size());
        assertTrue(addedIds.contains(id));
        // firstNode was old node
        assertTrue(addedNodes.contains(firstNode));
        
        // Make sure no other listeners were affected
        for (TestListener listener : nodeListenerMap.values()) {
            if (listener != firstNodeListener && 
                listener != secondNodeListener) 
            {
                assertEquals(0, listener.getAddedIds().size());
                assertEquals(0, listener.getAddedNodes().size());
                assertEquals(0, listener.getRemovedIds().size());
                assertEquals(0, listener.getRemovedNodes().size());
            }
        }
    }
    
    /* -- Tests to see what happens if the server isn't available --*/
    public void testEvilServerAssignNode() throws Exception {
        // replace the serverimpl with our evil proxy
        Object oldServer = swapToEvilServer(nodeMappingService);
        
        Identity id = new IdentityImpl("first");

        // Nothing much will happen. Eventually, we'll cause the
        // stack to shut down.
        nodeMappingService.assignNode(NodeMappingService.class, id);   
        swapToNormalServer(nodeMappingService, oldServer);
    }
    
    public void testEvilServerGetNode() throws Exception {
        // replace the serverimpl with our evil proxy
        Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);
        
        Object oldServer = swapToEvilServer(nodeMappingService);
        
        GetNodeTask task = new GetNodeTask(id);
        // Reads should cause no trouble
        txnScheduler.runTask(task, taskOwner);
        swapToNormalServer(nodeMappingService, oldServer);
    }
    
    public void testEvilServerGetIdentities() throws Exception {
        // put an identity in with a node
        // try to getNode that identity.
        final Identity id1 = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id1);
        
        Object oldServer = swapToEvilServer(nodeMappingService);
        
        txnScheduler.runTask(new AbstractKernelRunnable(){
                public void run() throws Exception {
                    Node node = nodeMappingService.getNode(id1);
		    Set<Identity> foundSet = new HashSet<Identity>();
                    Iterator<Identity> idIter = 
                        nodeMappingService.getIdentities(node.getId());   
                    while (idIter.hasNext()) {
                        foundSet.add(idIter.next());
		    }
		    assertTrue(foundSet.contains(id1));
                }
            }, taskOwner);
        swapToNormalServer(nodeMappingService, oldServer);
    }
    
    public void testEvilServerSetStatus() throws Exception {
        final Identity id = new IdentityImpl("first");
        nodeMappingService.assignNode(NodeMappingService.class, id);

        Object oldServer = swapToEvilServer(nodeMappingService);
        nodeMappingService.setStatus(NodeMappingService.class, id, false);
        
        Thread.sleep(removeTime * 4);

        // Identity should now be gone... this is a hole in the
        // implementation, currently.  It won't be removed.  
        txnScheduler.runTask(new AbstractKernelRunnable() {
            public void run() {
                try {
                    Node node = nodeMappingService.getNode(id);
                    // This line should be uncommented if we want to support
                    // disconnected servers.
//                  fail("Expected UnknownIdentityException");
                } catch (UnknownIdentityException e) {
                    
                }
            }
        }, taskOwner);
        swapToNormalServer(nodeMappingService, oldServer);
    }
    
    private Object swapToEvilServer(NodeMappingService service) throws Exception {
        Field serverField = 
            NodeMappingServiceImpl.class.getDeclaredField("server");
        serverField.setAccessible(true);
        
        Object server = serverField.get(service);
        Object proxy = EvilProxy.proxyFor(server);
        serverField.set(service,proxy);
        return server;
    }
    
    private void swapToNormalServer(NodeMappingService service, Object old) 
        throws Exception 
    {
        Field serverField = 
            NodeMappingServiceImpl.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(service, old);
    }
        
//    public void testShutdown() {
//        // queue up a bunch of removes with very long timeouts
//        // make sure we terminate them early
//    }
    
    /** Utilties */
    
    /** Use the invariant checking method */
    private void verifyMapCorrect(final Identity id) throws Exception {  
        txnScheduler.runTask( new AbstractKernelRunnable() {
            public void run() throws Exception {
                boolean valid = 
                    (Boolean) assertValidMethod.invoke(nodeMappingService, id);
                assertTrue(valid);
            }
        },taskOwner);
    }  
    
    /** 
     * Simple task to call getNode and return an id 
     */
    private class GetNodeTask extends AbstractKernelRunnable {
        /** The identity */
        private Identity id;
        /** The node the identity is assigned to */
        private Node node;
        GetNodeTask(Identity id) {
            this.id = id;
        }
        public void run() throws Exception {
            node = nodeMappingService.getNode(id);
        }
        public Node getNode() { return node; }
    }
    
    /** A test node mapping listener */
    private class TestListener implements NodeMappingListener {
        private final List<Identity> addedIds = new ArrayList<Identity>();
        private final List<Node> addedNodes = new ArrayList<Node>();
        private final List<Identity> removedIds = new ArrayList<Identity>();
        private final List<Node> removedNodes = new ArrayList<Node>();
        
        public void mappingAdded(Identity identity, Node node) {
            addedIds.add(identity);
            addedNodes.add(node);
        }

        public void mappingRemoved(Identity identity, Node node) {
            removedIds.add(identity);
            removedNodes.add(node);
        }
        
        public void clear() {
            addedIds.clear();
            addedNodes.clear();
            removedIds.clear();
            removedNodes.clear();
        }
        
        public List<Identity> getAddedIds()   { return addedIds; }
        public List<Node> getAddedNodes()     { return addedNodes; }
        public List<Identity> getRemovedIds() { return removedIds; }
        public List<Node> getRemovedNodes()   { return removedNodes; }
        
        public String toString() {
            return "TestListener: AddedIds size: " + addedIds.size() +
                   " AddedNodes size: " + addedNodes.size() +
                   " removedIds size: " + removedIds.size() +
                   " removedNodes size: " + removedNodes.size();
        }
    }
}

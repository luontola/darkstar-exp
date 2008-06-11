/*
 * Copyright (c) 2007-2008, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sun.sgs.test.client.simple;

import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.simple.SimpleClient;
import com.sun.sgs.client.simple.SimpleClientListener;

import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.util.Properties;
import junit.framework.TestCase;

public class TestSimpleClient extends TestCase {

    public void testLoginNoServer() throws Exception {
	DummySimpleClientListener listener =
	    new DummySimpleClientListener();

	SimpleClient client = new SimpleClient(listener);
	long timeout = 1000;
	Properties props =
	    createProperties(
		"host", "localhost",
		"port", Integer.toString(5382),
		"connectTimeout", Long.toString(timeout));
	client.login(props);
	try {
	    Thread.sleep(timeout * 2);
	} catch (InterruptedException e) {
	}
	if (listener.disconnectReason == null) {
	    fail("Didn't receive disconnected callback");
	}

	System.err.println("reason: " + listener.disconnectReason);
    }

    // TBD: it would be good to have a test that exercises
    // the timeout expiration.

    private class DummySimpleClientListener implements SimpleClientListener {

	private volatile String disconnectReason = null;
	
	public PasswordAuthentication getPasswordAuthentication() {
	    return null;
	}

	public void loggedIn() {
	}

	public void loginFailed(String reason) {
	}

	public ClientChannelListener joinedChannel(ClientChannel channel) {
	    return null;
	}

	public void receivedMessage(ByteBuffer message) {
	}

	public void reconnecting() {
	}
	
	public void reconnected() {
	}
	
	public void disconnected(boolean graceful, String reason){
	    System.err.println("TestSimpleClient.disconnected: graceful: " +
			       graceful + ", reason: " + reason);
	    disconnectReason = reason;
	}
    }

    /** Creates a property list with the specified keys and values. */
    private static Properties createProperties(String... args) {
	Properties props = new Properties();
	if (args.length % 2 != 0) {
	    throw new RuntimeException("Odd number of arguments");
	}
	for (int i = 0; i < args.length; i += 2) {
	    props.setProperty(args[i], args[i + 1]);
	}
	return props;
    }
    
}

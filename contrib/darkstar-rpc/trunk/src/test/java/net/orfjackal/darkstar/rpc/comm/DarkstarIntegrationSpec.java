/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.darkstar.rpc.comm;

import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.ServerSessionListener;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.darkstar.rpc.MockChannel;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Esko Luontola
 * @since 10.6.2008
 */
@RunWith(JDaveRunner.class)
public class DarkstarIntegrationSpec extends Specification<Object> {

/*
    public class AChannelAdapter {

        private ChannelAdapter adapter;
        private DummyChannel channel;
        private ChannelListener channelListener;
        private ClientSession clientSession;

        public Object create() {
            channel = new DummyChannel();
            clientSession = mock(ClientSession.class);

            adapter = new ChannelAdapter(10);
            channelListener = adapter;
            adapter.setChannel(channel);
            return null;
        }

        public void createsAGatewayWhichSendsRequestsToTheChannel() {
            RpcGateway gateway = adapter.getGateway();
            try {
                gateway.remoteFindAll();
            } catch (RuntimeException e) {
                if (e.getCause() != null && e.getCause().getClass().equals(TimeoutException.class)) {
                    // OK because in this test setup no response is sent back
                } else {
                    throw e;
                }
            }
            specify(channel.senders, should.containInOrder((ClientSession) null));
            specify(channel.messages.size(), should.equal(1));
        }
    }
*/

    public class WhenThereIsARpcChannel {

        private ChannelAdapter adapterOnServer;
        private MockChannel mockChannel;
        private ChannelListener channelListenerOnServer;
        private ServerSessionListener serverSessionListenerOnClient;
        private ClientChannelAdapter adapterOnClient;

        private RpcGateway gatewayOnServer;
        private RpcGateway gatewayOnClient;

        public Object create() {
            adapterOnServer = new ChannelAdapter();
            gatewayOnServer = adapterOnServer.getGateway();
            channelListenerOnServer = new ChannelListener() {
                public void receivedMessage(Channel channel, ClientSession sender, ByteBuffer message) {
                    System.out.println("DarkstarIntegrationSpec$WhenThereIsARpcChannel.receivedMessage");
                    adapterOnServer.receivedMessage(channel, sender, message);
                }
            };
            mockChannel = new MockChannel(channelListenerOnServer);
            adapterOnServer.setChannel(mockChannel.getChannel());

            adapterOnClient = new ClientChannelAdapter(1000);
            gatewayOnClient = adapterOnClient.getGateway();
            serverSessionListenerOnClient = new NullServerSessionListener() {
                public ClientChannelListener joinedChannel(ClientChannel channel) {
                    System.out.println("DarkstarIntegrationSpec$WhenThereIsARpcChannel.joinedChannel");
                    return adapterOnClient.joinedChannel(channel);
                }
            };
            mockChannel.joinChannel(serverSessionListenerOnClient);

            return null;
        }

        public void clientCanQueryServicesOnServer() {
            Set<?> services = gatewayOnClient.remoteFindAll();
            specify(services.size(), should.equal(1));
            mockChannel.shutdownAndWait();
        }

        public void serverCanQueryServicesOnClient() {
            Set<?> services = gatewayOnServer.remoteFindAll();
            specify(services.size(), should.equal(1));
            mockChannel.shutdownAndWait();
        }
    }


    private static class NullServerSessionListener implements ServerSessionListener {

        public ClientChannelListener joinedChannel(ClientChannel channel) {
            return null;
        }

        public void receivedMessage(ByteBuffer message) {
        }

        public void reconnecting() {
        }

        public void reconnected() {
        }

        public void disconnected(boolean graceful, String reason) {
        }
    }

    private class DummyChannel implements Channel {

        public List<ClientSession> senders = new ArrayList<ClientSession>();
        public List<ByteBuffer> messages = new ArrayList<ByteBuffer>();

        public String getName() {
            throw new UnsupportedOperationException();
        }

        public Delivery getDeliveryRequirement() {
            throw new UnsupportedOperationException();
        }

        public boolean hasSessions() {
            throw new UnsupportedOperationException();
        }

        public Iterator<ClientSession> getSessions() {
            throw new UnsupportedOperationException();
        }

        public Channel join(ClientSession session) {
            throw new UnsupportedOperationException();
        }

        public Channel join(Set<ClientSession> sessions) {
            throw new UnsupportedOperationException();
        }

        public Channel leave(ClientSession session) {
            throw new UnsupportedOperationException();
        }

        public Channel leave(Set<ClientSession> sessions) {
            throw new UnsupportedOperationException();
        }

        public Channel leaveAll() {
            throw new UnsupportedOperationException();
        }

        public Channel send(ClientSession sender, ByteBuffer message) {
            senders.add(sender);
            messages.add(message);
            return this;
        }
    }
}
